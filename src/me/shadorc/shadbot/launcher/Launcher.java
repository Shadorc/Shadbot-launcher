package me.shadorc.shadbot.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
            if (exitCode == ExitCode.RESTART_CLEAN) {
                if (this.deleteLogs()) {
                    Utils.LOGGER.info("Logs deleted.");
                } else {
                    Utils.LOGGER.warn("Logs could not be completly deleted.");
                }
            }

            try {
                Thread.sleep(5000);
            } catch (final InterruptedException err) {
                Utils.LOGGER.error("An error occurred while waiting.", err);
            }
        } while (exitCode != ExitCode.NORMAL || this.shouldRestart.getAndSet(false));
    }

    private ExitCode startAndWait() {
        if (Utils.getFreeRam() < this.gbMin) {
            Utils.LOGGER.error(String.format(
                    "Free physical memory insufficient to start the process (minimum required: %.1f GB)", this.gbMin));
            return ExitCode.RESTART;
        }

        Utils.LOGGER.info("Starting process...");

        try {
            // Allocate as much memory as possible leaving 500 MB free
            final int allocatedRam = (int) (Math.ceil(Utils.getFreeRam() - 0.5f) * 1000.0);
            final String xmx = String.format("-Xmx%dm", allocatedRam);
            this.process = new ProcessBuilder("java", "-jar", xmx, this.jarPath).inheritIO().start();

            Utils.LOGGER.info(String.format("Process started (PID: %d): %s",
                    this.process.pid(), this.process.info().commandLine().orElse("")));

            final int exitCode = this.process.waitFor();

            final Duration duration = this.process.info().totalCpuDuration().orElse(Duration.ZERO);
            final SimpleDateFormat sdf = new SimpleDateFormat("HH 'hour(s)' mm 'minute(s)' ss 'second(s)'", Locale.FRENCH);
            Utils.LOGGER.info("Execution time: {}", sdf.format(new Date(duration.toMillis())));

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
            if (!this.process.waitFor(10, TimeUnit.SECONDS)) {
                Utils.LOGGER.warn("The process has not been cleanly destroyed, killing the process forcibly...");
                this.process.destroyForcibly();
            }
        } catch (final InterruptedException err) {
            Utils.LOGGER.error("An error occurred while waiting for the process to finish.", err);
        }
    }

    private boolean deleteLogs() {
        Utils.LOGGER.info("Deleting logs...");
        final File file = new File("./logs");
        if (!file.exists()) {
            Utils.LOGGER.info("There are no logs to delete.");
            return true;
        }

        try (final Stream<Path> stream = Files.walk(new File("./logs").toPath())) {
            return stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .map(File::delete)
                    .allMatch(Predicate.isEqual(Boolean.TRUE));
        } catch (final IOException err) {
            Utils.LOGGER.error("An error occurred while deleting logs: {}", err.getMessage(), err);
        }

        return false;
    }

}
