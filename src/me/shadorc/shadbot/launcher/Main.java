package me.shadorc.shadbot.launcher;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("-help")) {
            Utils.LOGGER.info("Minimum GB required to start: -DgbMin (default: 4.5)"
                    + "\nJar file to launch: -Dfile (default: auto-detect)"
                    + "\nRestart period: -DrestartPeriod (default: never)");
            System.exit(ExitCode.NORMAL.value());
        }

        final String jarPath = Main.getJarPath();
        final float gbMin = Float.parseFloat(System.getProperty("gbMin", "4.5"));
        final int restartPeriod = Integer.parseInt(System.getProperty("restartPeriod", "-1"));

        if (jarPath == null) {
            Utils.LOGGER.error("File not found. You can specify the path as an argument using '-Dfile=${file}'.");
            System.exit(ExitCode.FATAL_ERROR.value());
        }

        final Launcher launcher = new Launcher(jarPath, gbMin, restartPeriod);
        launcher.start();
    }

    private static String getJarPath() {
        String jarPath = System.getProperty("file");

        if (jarPath == null) {
            for (final File file : new File(".").listFiles()) {
                final String fileName = file.getName();
                if (file.isFile() && fileName.startsWith("shadbot") && fileName.endsWith(".jar") && !fileName.contains("launcher")) {
                    jarPath = fileName;
                    break;
                }
            }
        }

        return jarPath;
    }

}
