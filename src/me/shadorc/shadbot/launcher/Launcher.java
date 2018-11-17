package me.shadorc.shadbot.launcher;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.management.OperatingSystemMXBean;

public class Launcher {

	private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);
	private static final float GB = 1024 * 1024 * 1024;
	private static final OperatingSystemMXBean OS_BEAN =
			(com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

	private static final float GB_RAM_TO_START = 4.5f;

	private static String jarPath;
	private static Process process;

	public static void main(String[] args) {
		if(args.length != 1) {
			LOG.error("You need to specify only the path to the jar file.");
			System.exit(ExitCode.FATAL_ERROR.value());
		}

		jarPath = args[0];

		LOG.info(String.format("Available processors: %d cores", Runtime.getRuntime().availableProcessors()));
		LOG.info(String.format("Total physical memory size: %.2f GB", Launcher.getTotalRam()));
		LOG.info(String.format("Free physical memory size: %.2f GB", Launcher.getFreeRam()));

		final ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();
		scheduledThreadPool.scheduleAtFixedRate(Launcher::watchRam, 5, 5, TimeUnit.MINUTES);

		Launcher.loop();
	}

	private static void loop() {
		ExitCode exitCode;
		do {
			exitCode = Launcher.start();
			LOG.info(String.format("Exit code: %s", exitCode.toString()));
		} while(exitCode.equals(ExitCode.RESTART));
	}

	private static ExitCode start() {
		LOG.info("Starting...");

		if(Launcher.getFreeRam() < GB_RAM_TO_START) {
			LOG.error(String.format(
					"Free physical memory insufficient to start the process (minimum required: %.1f GB)", GB_RAM_TO_START));
			return ExitCode.FATAL_ERROR;
		}

		try {
			// Allocate as much memory as possible leaving 500 MB free
			final int allocatedRam = (int) (Math.ceil(Launcher.getFreeRam() - 0.5f) * 1000d);
			final String xmx = String.format("-Xmx%dm", allocatedRam);
			process = new ProcessBuilder("java", "-jar", xmx, jarPath).inheritIO().start();

			LOG.info(String.format("Process started (PID: %d) with: %s",
					process.pid(), process.info().commandLine().orElse("")));

			return ExitCode.valueOf(process.waitFor());

		} catch (IOException | InterruptedException err) {
			LOG.error("An error occurred while starting the process.", err);
			return ExitCode.FATAL_ERROR;
		}
	}

	private static void watchRam() {
		LOG.info(String.format("RAM available: %.1f GB", Launcher.getFreeRam()));
		if(Launcher.getFreeRam() < 0.75f) {
			LOG.warn("The available RAM is too low, restarting process...");
			process.destroy();
			try {
				if(!process.waitFor(10, TimeUnit.SECONDS)) {
					LOG.warn("The process has not been cleanly destroyed, killing the process forcibly...");
					process.destroyForcibly();
				}
			} catch (InterruptedException err) {
				LOG.error("An error occurred while waiting for the process to finish.", err);
			}
			Launcher.loop();
		}
	}

	private static float getFreeRam() {
		return OS_BEAN.getFreePhysicalMemorySize() / GB;
	}

	private static float getTotalRam() {
		return OS_BEAN.getTotalPhysicalMemorySize() / GB;
	}

}
