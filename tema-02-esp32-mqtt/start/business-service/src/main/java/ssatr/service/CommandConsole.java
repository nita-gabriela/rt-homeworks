package ssatr.service;

import java.util.Scanner;

/**
 * Consola operatorului: citește comenzi de la tastatură.
 *
 *   show                               afișează dashboard-ul
 *   <deviceId> <COMANDA> [parametru]   trimite o comandă unui device, de exemplu
 *                                      device-1 SET_INTERVAL 1000
 *   help                               afișează comenzile
 *   exit                               oprește serviciul
 */
public class CommandConsole implements Runnable {

    private final MqttConnection mqtt;
    private final Dashboard dashboard;

    public CommandConsole(MqttConnection mqtt, Dashboard dashboard) {
        this.mqtt = mqtt;
        this.dashboard = dashboard;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        printHelp();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            } else if (line.equals("exit")) {
                break;
            } else if (line.equals("help")) {
                printHelp();
            } else if (line.equals("show")) {
                dashboard.print();
            } else {
                sendCommand(line);
            }
        }
    }

    private void sendCommand(String line) {
        // TODO 6: separați deviceId de restul liniei
        //   ("device-1 SET_INTERVAL 1000" -> "device-1" și "SET_INTERVAL 1000")
        //   și publicați comanda pe Config.commandTopic(deviceId).
        //   Afișați un mesaj de eroare dacă linia nu are formatul corect.
        String[] input = line.split("\\s+", 2);
        if (input.length != 2 || !input[0].matches("[A-Za-z0-9_-]+")) {
            System.out.println("Eroare: folositi <deviceId> <COMANDA> [parametru]");
            return;
        }
        String deviceId = input[0];
        String command = input[1].trim();
        String[] parts = command.split("\\s+");
        try {
            switch (parts[0]) {
                case "SET_INTERVAL":
                    if (parts.length != 2) throw new IllegalArgumentException("SET_INTERVAL necesita un parametru");
                    long interval = Long.parseLong(parts[1]);
                    if (interval <= 0 || interval > Long.MAX_VALUE / 1_000_000L)
                        throw new IllegalArgumentException("interval invalid");
                    break;
                case "SET_THRESHOLD":
                    if (parts.length != 2) throw new IllegalArgumentException("SET_THRESHOLD necesita un parametru");
                    if (!Double.isFinite(Double.parseDouble(parts[1])))
                        throw new IllegalArgumentException("prag invalid");
                    break;
                case "RESET":
                case "STATUS":
                    if (parts.length != 1) throw new IllegalArgumentException("comanda nu accepta parametri");
                    break;
                default:
                    throw new IllegalArgumentException("comanda necunoscuta");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Eroare: " + e.getMessage());
            return;
        }
        mqtt.publish(Config.commandTopic(deviceId), command);
        System.out.println("Comanda publicata pentru " + deviceId + ": " + command);
    }


    private void printHelp() {
        System.out.println("Comenzi: show | <deviceId> SET_INTERVAL <ms> | <deviceId> SET_THRESHOLD <valoare>");
        System.out.println("         <deviceId> RESET | <deviceId> STATUS | help | exit");
    }
}
