package me.shadorc.shadbot.launcher;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.management.OperatingSystemMXBean;

public class Launcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
	private static final OperatingSystemMXBean OS_BEAN =
			(com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	private static final float GB = 1024 * 1024 * 1024;
	private static final float GB_RAM_TO_START = 4.5f;
	private static final AtomicBoolean SHOULD_RESTART = new AtomicBoolean();

	private static String jarPath;
	private static Process process;

	public static void main(String[] args) {
		if(args.length == 0 ) {
			for(File file : new File(".").listFiles()) {
				final String fileName = file.getName();
				if(file.isFile() && fileName.startsWith("shadbot") && !fileName.contains("launcher")) {
					jarPath = fileName;
					break;
				}
			}
		} else {
			jarPath = args[0];
		}
		
		if(jarPath == null) {
			LOGGER.error("jar not found. You can specify the path as an argument.");
			System.exit(ExitCode.FATAL_ERROR.value());
		}

		LOGGER.info("-------------------- INFO --------------------");
		LOGGER.info(String.format("Available processors: %d cores", Runtime.getRuntime().availableProcessors()));
		LOGGER.info(String.format("Total physical memory size: %.2f GB", Launcher.getTotalRam()));
		LOGGER.info(String.format("Free physical memory size: %.2f GB", Launcher.getFreeRam()));
		LOGGER.info("----------------------------------------------");

		final ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();
		scheduledThreadPool.scheduleAtFixedRate(Launcher::restart, 5, 5, TimeUnit.HOURS);

		ExitCode exitCode;
		do {
			exitCode = Launcher.start();
			LOGGER.info("Exit code: {}", exitCode.toString());
			try {
				Thread.sleep(5000);
			} catch (InterruptedException err) {
				LOGGER.error("An error occurred while waiting.", err);
			}
		} while(!exitCode.equals(ExitCode.NORMAL) || SHOULD_RESTART.getAndSet(false));
	}

	private static ExitCode start() {
		if(Launcher.getFreeRam() < GB_RAM_TO_START) {
			LOGGER.error(String.format(
					"Free physical memory insufficient to start the process (minimum required: %.1f GB)", GB_RAM_TO_START));
			return ExitCode.RESTART;
		}

		LOGGER.info("Starting process...");
		final long start = System.currentTimeMillis();

		try {
			// Allocate as much memory as possible leaving 500 MB free
			final int allocatedRam = (int) (Math.ceil(Launcher.getFreeRam() - 0.5f) * 1000d);
			final String xmx = String.format("-Xmx%dm", allocatedRam);
			process = new ProcessBuilder("java", "-jar", xmx, jarPath).inheritIO().start();

			LOGGER.info(String.format("Process started (PID: %d): %s",
					process.pid(), process.info().commandLine().orElse("")));

			final int exitCode = process.waitFor();

			final long elapsed = System.currentTimeMillis() - start;
			final SimpleDateFormat sdf = new SimpleDateFormat("HH 'hour(s)' mm 'minute(s)' ss 'second(s)'", Locale.FRENCH);
			LOGGER.info("Execution time: {}", sdf.format(new Date(elapsed)));

			return ExitCode.valueOf(exitCode);

		} catch (IOException | InterruptedException err) {
			LOGGER.error("An error occurred while starting the process.", err);
			return ExitCode.FATAL_ERROR;
		}
	}

	private static void restart() {
		LOGGER.info("Restarting process...");
		SHOULD_RESTART.set(true);
		process.destroy();
		try {
			if(!process.waitFor(10, TimeUnit.SECONDS)) {
				LOGGER.warn("The process has not been cleanly destroyed, killing the process forcibly...");
				process.destroyForcibly();
			}
		} catch (InterruptedException err) {
			LOGGER.error("An error occurred while waiting for the process to finish.", err);
		}
	}

	private static float getFreeRam() {
		return OS_BEAN.getFreePhysicalMemorySize() / GB;
	}

	private static float getTotalRam() {
		return OS_BEAN.getTotalPhysicalMemorySize() / GB;
	}

}
