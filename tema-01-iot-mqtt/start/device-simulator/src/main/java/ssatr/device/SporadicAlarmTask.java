package ssatr.device;

import java.util.concurrent.BlockingQueue;

/**
 * TASK SPORADIC: tratează alarmele de temperatură.
 *
 * Evenimentele sosesc în coada alarmEvents (puse de taskul periodic).
 * Intervalul minim între sosiri este MIN_INTERARRIVAL_MS: între două eliberări
 * ale taskului (două alarme publicate) trebuie să treacă cel puțin atât.
 * Intervalul este impus de cod: evenimentele care sosesc mai des sunt
 * respinse, ca să nu fie inundat brokerul.
 */
public class SporadicAlarmTask implements Runnable {

    static final long MIN_INTERARRIVAL_MS = 5000;

    private final String deviceId;
    private final MqttConnection mqtt;
    private final BlockingQueue<Double> alarmEvents;

    public SporadicAlarmTask(String deviceId, MqttConnection mqtt, BlockingQueue<Double> alarmEvents) {
        this.deviceId = deviceId;
        this.mqtt = mqtt;
        this.alarmEvents = alarmEvents;
    }

    @Override
    public void run() {
        // TODO 2: implementați taskul sporadic:
        //   - așteptați un eveniment: alarmEvents.take()
        //   - dacă de la ultima eliberare au trecut mai puțin de
        //     MIN_INTERARRIVAL_MS, respingeți evenimentul (afișați un mesaj)
        //   - altfel publicați pe Config.alarmTopic(deviceId) mesajul
        //     "deviceId;timestamp;temperatura;prag"
        //     (pragul curent poate fi transmis împreună cu temperatura sau
        //      citit din PeriodicTelemetryTask; alegeți și justificați în raport)
        //
        // Vezi exemplul exemple/01-taskuri/.../SporadicTaskExample.java
        long lastAccepted = 0;
        boolean firstAlarm = true;
        long minGap = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(MIN_INTERARRIVAL_MS);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                double temperature = alarmEvents.take();
                double eventThreshold = AlarmMetadata.take(deviceId);
                long now = System.nanoTime();
                if (!firstAlarm && now - lastAccepted < minGap) {
                    System.out.println("Alarma respinsa: intervalul minim nu a trecut.");
                    continue;
                }
                firstAlarm = false;
                lastAccepted = now;
                String text = deviceId + ";" + System.currentTimeMillis() + ";"
                        + String.format(java.util.Locale.US, "%.2f", temperature) + ";"
                        + String.format(java.util.Locale.US, "%.2f", eventThreshold);
                mqtt.publish(Config.alarmTopic(deviceId), text);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
