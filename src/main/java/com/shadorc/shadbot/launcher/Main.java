package com.shadorc.shadbot.launcher;

public class Main {

    public static final float DEFAULT_GB_MIN = 3f;
    public static final int DEFAULT_RESTART_PERIOD = -1;

    public static void main(String[] args) {
        if (args.length == 1) {
            if ("-help".equals(args[0])) {
                Utils.LOGGER.info("""
                        Minimum GB required to start: -DgbMin (default: {})
                        Jar file to launch: -Dfile (default: auto-detect)
                        Restart period: -DrestartPeriod (default: never)""", DEFAULT_GB_MIN);
            } else {
                Utils.LOGGER.error("Unknown arguments. Options: -help");
            }
            System.exit(ExitCode.NORMAL.getValue());
        }

        final String jarPath = Utils.getJarPath();
        final float gbMin = Float.parseFloat(System.getProperty("gbMin", Float.toString(DEFAULT_GB_MIN)));
        final int restartPeriod = Integer.parseInt(System.getProperty("restartPeriod", Integer.toString(DEFAULT_RESTART_PERIOD)));

        if (jarPath == null) {
            Utils.LOGGER.error("File not found. You can specify the path as an argument using '-Dfile=${file}'.");
            System.exit(ExitCode.FATAL_ERROR.getValue());
        }

        final Launcher launcher = new Launcher(jarPath, gbMin, restartPeriod);
        launcher.start();
    }

}
