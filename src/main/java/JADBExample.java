import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

import java.io.*;

public class JADBExample {
    public static void main(String[] args) {
        try {
            // Lancez le serveur ADB au début de votre programme
            startADBServer();

            // Récupérez le premier appareil connecté
            JadbConnection jadb = new JadbConnection();
            JadbDevice device = jadb.getDevices().get(0);
            if (device == null) {
                System.out.println("Aucun appareil connecté");
            } else {
                System.out.println("Appareil connecté : " + device.getSerial());
            }

            // Exemple d'envoi de SMS (il faut une carte SIM donc sur VM ça ne marche pas)
            System.out.println("Envoi d'un SMS");
            sendSMS(device, "0782239208", "Ceci\\ est\\ un\\ exemple\\ de\\ SMS\\ envoy\u00e9\\ via\\ JADB.");

            // Exemple d'extraction des SMS
            // System.out.println("Extraction des SMS");extractSMS(device, "sms.txt");

            // Exemple d'appel téléphonique sortant
            // System.out.println("Appel téléphonique sortant");makePhoneCall(device, "0782239208");

            // Exemple pour décrocher à un appel
            // System.out.println("Décrocher à un appel");answerPhoneCall(device);

            // Exemple de fin d'appel ou de refus d'appel (il faut un appel en cours)
            // System.out.println("Fin d'appel ou refus d'appel");endPhoneCall(device);

            // Exemple de récupération des notifications d'appel
            // System.out.println("Récupération des notifications d'appel");getNotificationCall(device);

            // Exemple de récupération des contacts
            // System.out.println("Récupération des contacts");extractContacts(device, "contacts.txt");

            // Exemple de recherche de contact par numéro
            // System.out.println("Recherche de contact par numéro"); contactSearchByNumber(device, "0782239208");

            // Exemple d'ajout d'un contact
            // System.out.println("Ajout d'un contact");addContact(device, "John", "Doe", "0782239208");

            // Fermez le serveur ADB à la fin de votre programme
            closeADBServer();

        } catch (IOException | JadbException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    // Module serveur ADB --------------------------------------------------------------------------------------
    public static void startADBServer() throws IOException, InterruptedException {
        System.out.println("StartADBServer");
        Process process = Runtime.getRuntime().exec("adb start-server");
        process.waitFor();
    }

    public static void closeADBServer() throws IOException, InterruptedException {
        System.out.println("CloseADBServer");
        Process process = Runtime.getRuntime().exec("adb kill-server");
        process.waitFor();
    }

    // Module SMS ------------------------------------------------------------------------------------------------
    public static void sendSMS(JadbDevice device, String phoneNumber, String message) throws IOException {
        String serialNumber = device.getSerial();
        try {
            boolean startApp = false;
            // Vérifiez si l'application est déjà en cours d'exécution avec une récursivité
            while (!isAppRunning(serialNumber)) {
                if (!startApp) {
                    // L'application n'est pas en cours d'exécution, lancez-la
                    String startServiceCmd = String.format("adb -s %s shell am start -n com.example.myapplication/com.example.myapplication.MainActivity", serialNumber);
                    Process startServiceProcess = Runtime.getRuntime().exec(startServiceCmd);
                    startServiceProcess.waitFor();
                    startApp = true;
                }
            }

            // processBuilder = new ProcessBuilder("adb", "-s", serialNumber, "shell", "service", "call", "isms", "5","i32", "1", "s16", "com.android.mms", "s16", "null", "s16", phoneNumber, "s16", "null", "s16", message, "s16", "null", "s16", "null", "i32", "0", "i64", "0");
            String startServiceCmd = String.format("adb -s " + serialNumber + " shell am startservice -n com.example.myapplication/.SMSService --es phoneNumber %s --es message %s", phoneNumber, message);

            Process startServiceProcess = Runtime.getRuntime().exec(startServiceCmd);
            startServiceProcess.waitFor();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void extractSMS(JadbDevice device, String fileName) {
        try {
            String serialNumber = device.getSerial();
            ProcessBuilder processBuilder = new ProcessBuilder("adb", "-s", serialNumber, "shell", "content", "query", "--uri", "content://sms");
            processBuilder.redirectOutput(new File(fileName));
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    // Module Appel -----------------------------------------------------------------------------------------------
    public static void makePhoneCall(JadbDevice device, String phoneNumber) throws IOException {
        String serialNumber = device.getSerial();
        Process process;
        try {
            process = Runtime.getRuntime().exec("adb -s " + serialNumber + " shell am start -a android.intent.action.CALL -d tel:" + phoneNumber);
            process.waitFor();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void answerPhoneCall(JadbDevice device) throws IOException {
        String serialNumber = device.getSerial();
        Process process;
        try {
            process = Runtime.getRuntime().exec("adb -s " + serialNumber + " shell input keyevent KEYCODE_CALL");
            process.waitFor();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void endPhoneCall(JadbDevice device) throws IOException {
        String serialNumber = device.getSerial();
        Process process;
        try {
            // Utilisez la commande ADB pour mettre fin à l'appel
            process = Runtime.getRuntime().exec("adb -s " + serialNumber + " shell input keyevent KEYCODE_ENDCALL");
            process.waitFor();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void getNotificationCall(JadbDevice device) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("adb", "-s", device.getSerial(), "shell", "dumpsys", "telephony.registry", "|", "grep", "-E", "mCallState");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String status = reader.readLine();
            System.out.println(status);
            if (status != null && status.contains("=")) {
                status = status.substring(status.indexOf("=") + 1).trim();
            }

            if (status.equals("2")) {
                System.out.println("Appel en cours");
            } else if (status.equals("1")) {
                System.out.println("Appel sortant");
                processBuilder = new ProcessBuilder("adb", "-s", device.getSerial(), "shell", "dumpsys", "telephony.registry", "|", "grep", "-E", "mCallIncomingNumber");
                processBuilder.redirectErrorStream(true);
                process = processBuilder.start();

                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String number = reader.readLine();
                System.out.println(number);
                if (number != null && number.contains("=")) {
                    number = number.substring(number.indexOf("=") + 1).trim();
                }

                process.waitFor();
                reader.close();

                String ret = contactSearchByNumber(device, number);
                String[] lines = ret.split("\n");
                for (String line : lines) {
                    if (line.contains("display_name=")) {
                        int startIndex = line.indexOf("display_name=") + "display_name=".length();
                        int endIndex = line.indexOf(',', startIndex);
                        String nameAndLastName = line.substring(startIndex, endIndex);
                        System.out.println("\tNom et prénom : " + nameAndLastName);
                    }
                }

            } else if (status.equals("2")) {
                System.out.println("Appel en cours");
            } else {
                System.out.println("Aucun appel");
                process.waitFor();
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Module Contacts --------------------------------------------------------------------------------------------
    public static void extractContacts(JadbDevice device, String fileName) {
        try {
            String serialNumber = device.getSerial();
            Process process = Runtime.getRuntime().exec("adb -s " + serialNumber + " shell content query --uri content://com.android.contacts/data --projection display_name:data1:data4:contact_id");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                // Lecture des données depuis le processus et écriture dans le fichier
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public static String contactSearchByNumber(JadbDevice device, String phoneNumber) {
        extractContacts(device, "contacts.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader("contacts.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(phoneNumber)) {
                    return line;
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return "Numéro inconnu";
    }

    public static void addContact(JadbDevice device, String firstName, String lastName, String phoneNumber) throws IOException, InterruptedException {
        try {
            String serialNumber = device.getSerial();

            boolean startApp = false;
            // Vérifiez si l'application est déjà en cours d'exécution avec une récursivité
            while (!isAppRunning(serialNumber)) {
                if (!startApp) {
                    // L'application n'est pas en cours d'exécution, lancez-la
                    String startServiceCmd = String.format("adb -s %s shell am start -n com.example.myapplication/com.example.myapplication.MainActivity", serialNumber);
                    Process startServiceProcess = Runtime.getRuntime().exec(startServiceCmd);
                    startServiceProcess.waitFor();
                    startApp = true;
                }
            }

            // Start the ContactService with parameters
            String startServiceCmd = String.format("adb -s " + serialNumber + " shell am startservice -n com.example.myapplication/.ContactService --es firstName %s --es lastName %s --es phoneNumber %s", firstName, lastName, phoneNumber);

            Process startServiceProcess = Runtime.getRuntime().exec(startServiceCmd);
            startServiceProcess.waitFor();
        } catch (InterruptedException | IOException e) {
            System.out.println(e.getMessage());
        }
    }

// Module Application -------------------------------------------------------------------------------------
    private static boolean isAppRunning(String serialNumber) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("adb", "-s", serialNumber, "shell", "pidof", "com.example.myapplication");
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        return exitCode == 0;
    }


// Méthode à prévoir ----------------------------------------------------------------------------------------
    // readExtractSMS
    // getNotificationSMS
    // readExtractContacts
}