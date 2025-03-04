package net.arkaine;

public class SudoAuthenticator {
    public static boolean authenticate(String password) {
        try {
            String[] cmd = {
                    "/bin/bash",
                    "-c",
                    "echo " + password + " | sudo -S true"
            };

            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean writeToHostsFile(String content, String password) {
        try {
            // Utilise le mot de passe stocké pour l'écriture
            String[] cmd = {
                    "/bin/bash",
                    "-c",
                    "echo " + password + " | sudo -S bash -c 'echo \"" + content.replace("\"", "\\\"") + "\" > /etc/hosts'"
            };

            Process process = Runtime.getRuntime().exec(cmd);
            return process.waitFor() == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}