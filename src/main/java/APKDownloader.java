import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class APKDownloader {

    public static void main(String[] args) {
        String apkPath = "app/app-debug.apk";
        String packageName = "com.example.myapplication";

        if (isAppInstalled(packageName)) {
            System.out.println("L'application est déjà installée.");
        } else {
            installApp(apkPath);
        }
    }

    public static void installApp(String apkPath) {
        String adbCommand = "adb install " + apkPath;

        try {
            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("L'application a été installée avec succès.");
            } else {
                System.out.println("Erreur lors de l'installation de l'application. Code de sortie : " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean isAppInstalled(String packageName) {
        String adbCommand = "adb shell pm list packages";

        try {
            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("package:" + packageName)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
