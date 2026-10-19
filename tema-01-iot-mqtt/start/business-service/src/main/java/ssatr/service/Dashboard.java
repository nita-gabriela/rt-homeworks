package ssatr.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Ultima stare cunoscută a fiecărui device: ultima temperatură, momentul
 * ultimului mesaj, numărul de alarme etc.
 *
 * Este actualizată de thread-urile MessageProcessor și citită de CommandConsole,
 * deci accesul trebuie sincronizat.
 */
public class Dashboard {

    // TODO 5: alegeți structura de date pentru starea device-urilor
    //   (de exemplu un Map<String, ...> cu cheia deviceId)
    //   și adăugați metodele de actualizare apelate din MessageProcessor.

    private static class DeviceState {
        Double temperature;
        long lastTimestamp = -1;
        int alarms;
        String lastStatus;
    }

    private final Map<String, DeviceState> devices = new LinkedHashMap<>();

    private DeviceState state(String id) {
        return devices.computeIfAbsent(id, key -> new DeviceState());
    }

    public synchronized void updateTelemetry(String id, long timestamp, double temperature) {
        DeviceState s = state(id);
        if (timestamp >= s.lastTimestamp || s.temperature == null) s.temperature = temperature;
        s.lastTimestamp = Math.max(s.lastTimestamp, timestamp);
    }

    public synchronized void updateAlarm(String id, long timestamp, double temperature, double threshold) {
        DeviceState s = state(id);
        s.alarms++;
        s.lastTimestamp = Math.max(s.lastTimestamp, timestamp);
    }

    public synchronized void updateStatus(String id, long timestamp, String text) {
        DeviceState s = state(id);
        s.lastStatus = text;
        s.lastTimestamp = Math.max(s.lastTimestamp, timestamp);
    }

    /** Afișează un tabel cu starea tuturor device-urilor. */
    public void print() {
        // TODO 5: afișați câte un rând pentru fiecare device, de exemplu:
        //   DEVICE     TEMP    ULTIMUL MESAJ   ALARME
        //   device-1   23.41   acum 1 s        2
        System.out.printf("%-18s %-12s %-18s %s%n", "DEVICE", "TEMP", "ULTIMUL MESAJ", "ALARME");
        long now = System.currentTimeMillis();
        for (Map.Entry<String, DeviceState> entry : devices.entrySet()) {
            DeviceState state = entry.getValue();
            String temp = state.temperature == null ? "-" : String.format(Locale.US, "%.2f", state.temperature);
            String last = state.lastTimestamp < 0 ? "-"
                    : "acum " + Math.max(0, (now - state.lastTimestamp) / 1000) + " s";
            System.out.printf("%-18s %-12s %-18s %d%n", entry.getKey(), temp, last, state.alarms);
        }
    }
}
