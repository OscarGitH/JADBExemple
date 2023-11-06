import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class JADBExample {

    public static void main(String[] args) {
        try {
            // Lancez le serveur ADB au début de votre programme
            startADBServer();

            // Récupérez le premier appareil connecté
            JadbConnection jadb = new JadbConnection();
            JadbDevice device = jadb.getDevices().get(0);

            // Exemple d'envoi de SMS (il faut une carte SIM donc sur VM ça ne marche pas)
            // sendSMS(device, "0782239208", "Ceci\\ est\\ un\\ exemple\\ de\\ SMS\\ envoy\u00e9\\ via\\ JADB.");

            // Exemple d'extraction des SMS
            // extractSMS(device, "sms.txt");

            // Exemple d'appel téléphonique sortant
            // makePhoneCall(device, "0782239208");

            // Exemple de réponse à un appel
            // answerPhoneCall(device);

            // Exemple de fin d'appel (il faut un appel en cours)
            // endPhoneCall(device);

            // Exemple de récupération des contacts
            // extractContacts(device, "contacts.txt");

            // Fermez le serveur ADB à la fin de votre programme
            closeADBServer();

// En cours de développement --------------------------------------------------------------------------------
            // Exemple d'extraction des notifications
            // getNotifications(device);

            // Exemple d'ajout d'un contact
            // addContact("Jean Louis", "11 11 11 11 11");

        } catch (IOException | JadbException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void startADBServer() throws IOException, InterruptedException {
        System.out.println("Test startADBServer");
        Process process = Runtime.getRuntime().exec("adb start-server");
        process.waitFor();
    }
    public static void closeADBServer() throws IOException, InterruptedException {
        System.out.println("Test closeADBServer");
        Process process = Runtime.getRuntime().exec("adb kill-server");
        process.waitFor();
    }
    public static void sendSMS(JadbDevice device, String phoneNumber, String message) throws IOException {
        System.out.println("Test sendSMS");
        String serialNumber = device.getSerial();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "adb", "-s", serialNumber, "shell", "service", "call", "isms", "5",
                "i32", "1", "s16", "com.android.mms", "s16", "null", "s16", phoneNumber, "s16", "null", "s16", message, "s16", "null", "s16", "null", "i32", "0", "i64", "0"
        );

        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
    public static void extractSMS(JadbDevice device, String fileName) {
        System.out.println("Extracting SMS to file");
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
    public static void makePhoneCall(JadbDevice device, String phoneNumber) throws IOException {
        System.out.println("Test makePhoneCall");
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
        System.out.println("Test answerPhoneCall");
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
        System.out.println("Test endPhoneCall");
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
    public static void extractContacts(JadbDevice device, String fileName) {
        System.out.println("Test saveContactsToFile");
        try {
            String serialNumber = device.getSerial();
            Process process = Runtime.getRuntime().exec("adb -s " + serialNumber +
                    " shell content query --uri content://com.android.contacts/data --projection display_name:data1:data4:contact_id");

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
// En cours de développement --------------------------------------------------------------------------------
    public static void getNotifications(JadbDevice device) throws IOException {
        List<String> notifications = new ArrayList<>();
        // Exécutez les commandes ADB pour obtenir les notifications et ajoutez-les à la liste
        try {
            String command = "adb -s " + device.getSerial() + " shell";
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            command = "dumpsys notification --noredact | grep contentIntent -A 5";
            process = Runtime.getRuntime().exec(command);
            process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                notifications.add(line);
            }

            // Affichez les notifications
            for (String notification : notifications) {
                System.out.println(notification);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static void addContact(String name, String phoneNumber)
    {
        System.out.println("Adding contact");
        try {
            Process process = Runtime.getRuntime().exec("adb shell am start -a android.intent.action.INSERT -t vnd.android.cursor.dir/contact -e name '" + name + "' -e phone " + phoneNumber);
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}