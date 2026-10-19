package ssatr.service;

import java.util.concurrent.BlockingQueue;

/**
 * Preia mesajele de un anumit tip ("telemetry", "alarm" sau "status") din coada
 * inbox, le salvează și actualizează dashboard-ul.
 *
 * Formatul mesajelor (câmpuri separate prin ";"):
 *   telemetry:  deviceId;timestamp;temperatura
 *   alarm:      deviceId;timestamp;temperatura;prag
 *   status:     deviceId;timestamp;text
 */
public class MessageProcessor implements Runnable {

    private final String kind;
    private final BlockingQueue<String> inbox;
    private final CsvStorage storage;
    private final Dashboard dashboard;

    public MessageProcessor(String kind, BlockingQueue<String> inbox, CsvStorage storage, Dashboard dashboard) {
        this.kind = kind;
        this.inbox = inbox;
        this.storage = storage;
        this.dashboard = dashboard;
    }

    @Override
    public void run() {
        // TODO 4: într-o buclă:
        //   - preluați un mesaj: inbox.take()
        //   - salvați mesajul în fișierul CSV: storage.append(kind, mesaj)
        //     (CsvStorage este gata implementată)
        //   - actualizați dashboard-ul, în funcție de kind
        //   - alarmele și confirmările se afișează imediat în consolă
        //   - un mesaj greșit (format invalid) nu trebuie să oprească thread-ul


        while (!Thread.currentThread().isInterrupted()) {
            final String message;
            try {
                message = inbox.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            storage.append(kind, message);
            try {
                String[] parts = message.split(";", "status".equals(kind) ? 3 : -1);
                int expected = "alarm".equals(kind) ? 4 : 3;
                if (parts.length != expected || parts[0].trim().isEmpty())
                    throw new IllegalArgumentException("format invalid");
                String id = parts[0].trim();
                long timestamp = Long.parseLong(parts[1]);
                if (timestamp < 0) throw new IllegalArgumentException("timestamp invalid");
                switch (kind) {
                    case "telemetry": {
                        double t = Double.parseDouble(parts[2]);
                        if (!Double.isFinite(t)) throw new IllegalArgumentException("temperatura invalida");
                        dashboard.updateTelemetry(id, timestamp, t);
                        break;
                    }
                    case "alarm": {
                        double t = Double.parseDouble(parts[2]);
                        double threshold = Double.parseDouble(parts[3]);
                        if (!Double.isFinite(t) || !Double.isFinite(threshold))
                            throw new IllegalArgumentException("date numerice invalide");
                        dashboard.updateAlarm(id, timestamp, t, threshold);
                        System.out.println("ALARMA: " + message);
                        break;
                    }
                    case "status":
                        if (parts[2].trim().isEmpty()) throw new IllegalArgumentException("status gol");
                        dashboard.updateStatus(id, timestamp, parts[2]);
                        System.out.println("STATUS: " + message);
                        break;
                    default:
                        throw new IllegalArgumentException("tip necunoscut: " + kind);
                }
            } catch (RuntimeException e) {
                System.err.println("Mesaj " + kind + " invalid: " + e.getMessage());
            }
        }
    }

}
