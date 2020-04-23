package com.shadorc.shadbot.launcher;

import java.io.IOException;

public class DBUtils {

    private static final String DB_NAME = "shadbot";
    private static final String[] COLLECTIONS = {"guilds", "premium", "lottery"};

    public static void exportDb() throws IOException, InterruptedException {
        for (final String collection : COLLECTIONS) {
            final Process process = new ProcessBuilder("mongoexport", "--collection=" + collection,
                    "--db=" + DB_NAME, "--out=" + collection + ".json").inheritIO().start();

            Utils.LOGGER.info("Exporting {} collection (PID: {}): {}",
                    collection, process.pid(), process.info().commandLine().orElse(""));

            process.waitFor();
        }
        Utils.LOGGER.info("Database {} exported.", DB_NAME);
    }

    public static void importDb() throws IOException, InterruptedException {
        for (final String collection : COLLECTIONS) {
            final Process process = new ProcessBuilder("mongoimport", "--collection=" + collection,
                    "--db=" + DB_NAME, "--file=" + collection + ".json").inheritIO().start();

            Utils.LOGGER.info("Importing {} collection (PID: {}): {}",
                    collection, process.pid(), process.info().commandLine().orElse(""));

            process.waitFor();
        }
        Utils.LOGGER.info("Database {} imported.", DB_NAME);
    }

}
