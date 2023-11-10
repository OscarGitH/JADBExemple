import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

import java.io.*;

public class JADBExample {
    public static void main(String[] args) {
        try {
// Module serveur ADB ----------------------------------------------------------------------------------------
            startADBServer();
            JadbDevice device = getDevice();

// Module SMS ------------------------------------------------------------------------------------------------
            //sendSMS(device, "0782239208", "Ceci\\ est\\ un\\ exemple\\ de\\ SMS\\ envoy\u00e9\\ via\\ JADB.");
            //extractSMS(device, "sms.txt", "0782239208");
            //getUnreadSMS(device);

// Module Appel ----------------------------------------------------------------------------------------------
            //makePhoneCall(device, "0782239208");
            //answerPhoneCall(device);
            //endPhoneCall(device);
            //getNotificationCall(device);

// Module Contacts -------------------------------------------------------------------------------------------
            //extractContact(device, "contacts.txt");
            //System.out.println(searchByNumberContact(device, "0782239208"));
            //System.out.println(searchByNameContact(device, "Jean"));
            //addContact(device, "Jean", "Dupont", "0782239208");

// Module Application ----------------------------------------------------------------------------------------
            //isAppRunning(device.getSerial());

// Module serveur ADB ----------------------------------------------------------------------------------------
            closeADBServer();

        } catch (IOException | JadbException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

// Module serveur ADB --------------------------------------------------------------------------------------
    /**
     * Starts the ADB server
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void startADBServer() throws IOException, InterruptedException {
        System.out.println("StartADBServer");
        Process process = Runtime.getRuntime().exec("adb start-server");
        process.waitFor();
    }

    /**
     * Closes ADB server
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void closeADBServer() throws IOException, InterruptedException {
        System.out.println("CloseADBServer");
        Process process = Runtime.getRuntime().exec("adb kill-server");
        process.waitFor();
    }

    /**
     * Recovers the first device connected
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws JadbException
     */
    public static JadbDevice getDevice() throws IOException, InterruptedException, JadbException {
        JadbConnection jadb = new JadbConnection();
        JadbDevice device = jadb.getDevices().get(0);
        String serialNumber = device.getSerial();

        if (device == null) {
            System.out.println("Aucun appareil connecté");
        } else {
            System.out.println("Appareil connecté : " + device.getSerial());

            // Utiliser la commande adb shell avec un script pour extraire dynamiquement le nom
            String script = "dumpsys bluetooth_manager | grep 'name:'";
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "adb", "-s", serialNumber, "shell", script
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();
            reader.close();
        }
        return device;
    }

// Module SMS ------------------------------------------------------------------------------------------------
    /**
     * Send an SMS
     *
     * @param device
     * @param phoneNumber
     * @param message
     * @throws IOException
     */
    public static void sendSMS(JadbDevice device, String phoneNumber, String message) throws IOException {
        String serialNumber = device.getSerial();
        try {
            boolean startApp = false;
            // Vérifiez si l'application est déjà en cours d'exécution avec une récursivité
            while (isAppRunning(serialNumber)) {
                if (!startApp) {
                    // L'application n'est pas en cours d'exécution, lancez-la
                    String startServiceCmd = String.format("adb -s %s shell am start -n com.example.myapplication/com.example.myapplication.MainActivity", serialNumber);
                    Process startServiceProcess = Runtime.getRuntime().exec(startServiceCmd);
                    startServiceProcess.waitFor();
                    startApp = true;
                }
            }

            String startServiceCmd = String.format("adb -s " + serialNumber + " shell am startservice -n com.example.myapplication/com.example.myapplication.SMSServiceSend --es phoneNumber %s --es message %s", phoneNumber, message);
            Process startServiceProcess = Runtime.getRuntime().exec(startServiceCmd);
            startServiceProcess.waitFor();


            /*ad
            ProcessBuilder processBuilder = new ProcessBuilder("adb", "-s", serialNumber, "shell", "service", "call", "isms", "5","i32", "1", "s16", "com.android.mms", "s16", "null", "s16", phoneNumber, "s16", "null", "s16", message, "s16", "null", "s16", "null", "i32", "0", "i64", "0");
            Process process = processBuilder.start();
            process.waitFor();
            */

        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Extracts SMS messages from the device
     *
     * @param device
     * @param fileName
     * @param phoneNumber
     */
    public static void extractSMS(JadbDevice device, String fileName, String phoneNumber) {
        try {
            String serialNumber = device.getSerial();
            ProcessBuilder processBuilder;

            processBuilder = new ProcessBuilder("adb", "-s", serialNumber, "shell", "content", "query", "--uri", "content://sms");


            processBuilder.redirectOutput(new File(fileName));
            Process process = processBuilder.start();
            process.waitFor();

            // Filtrer les résultats avec busybox grep si un numéro est spécifié
            if (phoneNumber != null ) {
                filterResultsSMS(fileName, phoneNumber);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Filter SMS query results
     *
     * @param fileName
     * @param phoneNumber
     */
    public static void filterResultsSMS(String fileName, String phoneNumber) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName));
             FileWriter filteredWriter = new FileWriter( phoneNumber + "_sms.txt")) {

            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("address=" + phoneNumber)) {
                    filteredWriter.write(line + "\n");
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void getUnreadSMS(JadbDevice device) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("adb", "-s", device.getSerial(), "shell", "content", "query", "--uri", "content://sms/inbox", "--projection", "address,body", "--where", "read=0");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();
            reader.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

// Module Appel -----------------------------------------------------------------------------------------------
    /**
     * Make a phone call
     *
     * @param device
     * @param phoneNumber
     * @throws IOException
     */
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

    /**
     * Answer an incoming call
     *
     * @param device
     * @throws IOException
     */
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

    /**
     * Ends the current call
     *
     * @param device
     * @throws IOException
     */
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

    /**
     * Retrieves current call information
     *
     * @param device
     */
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

                String ret = searchByNumberContact(device, number);
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
    /**
     * Extract contacts from the device
     *
     * @param device
     * @param fileName
     */
    public static void extractContact(JadbDevice device, String fileName) {
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

    /**
     * Search for a contact by number
     *
     * @param device
     * @param phoneNumber
     * @return
     */
    public static String searchByNumberContact(JadbDevice device, String phoneNumber) {
        extractContact(device, "contacts.txt");
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

    /**
     * Search for a contact by name
     *
     * @param device
     * @param name
     * @return
     */
    public static String searchByNameContact(JadbDevice device, String name) {
        extractContact(device, "contacts.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader("contacts.txt"))) {
            String lowerCaseName = name.toLowerCase();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains(lowerCaseName)) {
                    System.out.println(line);
                    return line;
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return "Nom inconnu";
    }

    /**
     * Add a contact to the device
     *
     * @param device
     * @param firstName
     * @param lastName
     * @param phoneNumber
     * @throws IOException
     * @throws InterruptedException
     */
    public static void addContact(JadbDevice device, String firstName, String lastName, String phoneNumber) throws IOException, InterruptedException {
        try {
            String serialNumber = device.getSerial();

            boolean startApp = false;
            // Vérifiez si l'application est déjà en cours d'exécution avec une récursivité
            while (isAppRunning(serialNumber)) {
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
    /**
     * Vérifiez si l'application est en cours d'exécution
     *
     * @param serialNumber
     * @throws IOException
     * @throws InterruptedException
     */
    private static boolean isAppRunning(String serialNumber) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("adb", "-s", serialNumber, "shell", "pidof", "com.example.myapplication");
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        return exitCode != 0;
    }

// Méthode à prévoir ----------------------------------------------------------------------------------------

}