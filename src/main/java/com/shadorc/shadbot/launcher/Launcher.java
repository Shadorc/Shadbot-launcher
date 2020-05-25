package com.shadorc.shadbot.launcher;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Launcher {

    private final String jarPath;
    private final float gbMin;
    private final int restartPeriod;
    private final AtomicBoolean shouldRestart;

    private Process process;

    public Launcher(String jarPath, float gbMin, int restartPeriod) {
        this.jarPath = jarPath;
        this.gbMin = gbMin;
        this.restartPeriod = restartPeriod;
        this.shouldRestart = new AtomicBoolean();
    }

    public void start() {
        Utils.LOGGER.info("------------------------------ INFO ------------------------------");
        Utils.LOGGER.info(String.format("Available processors: %d cores", Runtime.getRuntime().availableProcessors()));
        Utils.LOGGER.info(String.format("Total physical memory size: %.2f GB", Utils.getTotalRam()));
        Utils.LOGGER.info(String.format("Free physical memory size: %.2f GB", Utils.getFreeRam()));
        Utils.LOGGER.info("------------------------------------------------------------------");

        if (this.restartPeriod != -1) {
            final ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();
            scheduledThreadPool.scheduleAtFixedRate(this::restart, this.restartPeriod, this.restartPeriod, TimeUnit.HOURS);
            Utils.LOGGER.info("Restart scheduled every {} hours.", this.restartPeriod);
        }

        ExitCode exitCode;
        do {
            exitCode = this.startAndWait();
            Utils.LOGGER.info("Exit code: {}", exitCode);

            try {
                Thread.sleep(2_500);
            } catch (final InterruptedException err) {
                Utils.LOGGER.error("An error occurred while waiting.", err);
            }
        } while (exitCode == ExitCode.RESTART || this.shouldRestart.getAndSet(false));
    }

    private ExitCode startAndWait() {
        if (Utils.getFreeRam() < this.gbMin) {
            Utils.LOGGER.error(String.format(
                    "Free physical memory insufficient to start the process (minimum required: %.1f GB)", this.gbMin));
            return ExitCode.FATAL_ERROR;
        }

        Utils.LOGGER.info("Starting process...");

        try {
            // Allocate as much memory as possible leaving 500 MB free
            final int allocatedRam = (int) (Utils.getFreeRam() * 1000.0f - 500.0f);
            final String xmx = String.format("-Xmx%dm", allocatedRam);
            this.process = new ProcessBuilder("java", "-jar", xmx, this.jarPath).inheritIO().start();

            Utils.LOGGER.info(String.format("Process started (PID: %d): %s",
                    this.process.pid(), this.process.info().commandLine().orElse("")));

            final int exitCode = this.process.waitFor();
            return ExitCode.valueOf(exitCode);

        } catch (final IOException | InterruptedException err) {
            Utils.LOGGER.error("An error occurred while starting the process.", err);
            return ExitCode.FATAL_ERROR;
        }
    }

    private void restart() {
        Utils.LOGGER.info("Restarting process...");
        this.shouldRestart.set(true);
        this.process.destroy();
        try {
            if (!this.process.waitFor(2_500, TimeUnit.MILLISECONDS)) {
                Utils.LOGGER.warn("The process has not been cleanly destroyed, killing the process forcibly...");
                this.process.destroyForcibly();
            }
        } catch (final InterruptedException err) {
            Utils.LOGGER.error("An error occurred while waiting for the process to finish.", err);
        }
    }

}
