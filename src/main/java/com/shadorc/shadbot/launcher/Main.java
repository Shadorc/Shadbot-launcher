package com.shadorc.shadbot.launcher;

public class Main {

    public static final float DEFAULT_GB_MIN = 3f;
    public static final int DEFAULT_RESTART_PERIOD = -1;

    public static void main(String[] args) {
        if (args.length == 1) {
            switch (args[0]) {
                case "-help":
                    Utils.LOGGER.info("Minimum GB required to start: -DgbMin (default: {})"
                            + "\nJar file to launch: -Dfile (default: auto-detect)"
                            + "\nRestart period: -DrestartPeriod (default: never)", DEFAULT_GB_MIN);
                    break;
                default:
                    Utils.LOGGER.error("Unknown arguments. Options: -help");
                    break;
            }
            System.exit(ExitCode.NORMAL.getValue());
        }

        final String jarPath = Main.getJarPath();
        final float gbMin = Float.parseFloat(System.getProperty("gbMin", Float.toString(DEFAULT_GB_MIN)));
        final int restartPeriod = Integer.parseInt(System.getProperty("restartPeriod", Integer.toString(DEFAULT_RESTART_PERIOD)));

        if (jarPath == null) {
            Utils.LOGGER.error("File not found. You can specify the path as an argument using '-Dfile=${file}'.");
            System.exit(ExitCode.FATAL_ERROR.getValue());
        }

        final Launcher launcher = new Launcher(jarPath, gbMin, restartPeriod);
        launcher.start();
    }

    private static String getJarPath() {
        final String jarPath = System.getProperty("file");
        if (jarPath != null) {
            return jarPath;
        }

        for (final File file : new File(".").listFiles()) {
            final String fileName = file.getName();
            if (file.isFile() && fileName.startsWith("shadbot") && fileName.endsWith(".jar") && !fileName.contains("launcher")) {
                return fileName;
            }
        }

        return null;
    }

}
