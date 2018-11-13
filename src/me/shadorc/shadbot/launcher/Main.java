package me.shadorc.shadbot.launcher;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

public class Main {

	private static final float REQUIRED_FREE_RAM = 4.5f;

	private static final float GB = 1024 * 1024 * 1024;
	private static final OperatingSystemMXBean OS_BEAN =
			(com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	private static final Runtime RUNTIME = Runtime.getRuntime();

	private static String jarPath;
	private static Process process;

	public static void main(String[] args) {
		if(args.length != 1) {
			System.err.println("You need to specify only the path to the jar file.");
			System.exit(ExitCode.FATAL_ERROR.value());
		}

		jarPath = args[0];

		System.out.println(String.format("Available processors: %d cores", RUNTIME.availableProcessors()));
		System.out.println(String.format("Total physical memory size: %.2f GB", Main.getTotalRam()));
		System.out.println(String.format("Free physical memory size: %.2f GB", Main.getFreeRam()));

		//final ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();
		//scheduledThreadPool.scheduleAtFixedRate(Main::watchRam, 5, 5, TimeUnit.MINUTES);

		ExitCode exitCode;
		do {
			exitCode = Main.start();
			System.out.println(String.format("Exit code: %s", exitCode.toString()));
		} while(exitCode.equals(ExitCode.RESTART));

		//scheduledThreadPool.shutdown();
	}

	private static ExitCode start() {
		System.out.println("Starting...");

		if(Main.getFreeRam() < REQUIRED_FREE_RAM) {
			System.err.println(String.format(
					"Free physical memory insufficient to start the bot (minimum required: %.1f GB)", REQUIRED_FREE_RAM));
			return ExitCode.FATAL_ERROR;
		}

		try {
			final String xms = String.format("-Xms%dm", (int) (REQUIRED_FREE_RAM * 1000));
			final String xmx = String.format("-Xmx%dm", (int) (Math.ceil(Main.getFreeRam() - 0.5f) * 1000d));
			process = new ProcessBuilder("java", "-jar", xms, xmx, jarPath).inheritIO().start();

			System.out.println(String.format("Process started (PID: %d) with: %s",
					process.pid(), process.info().commandLine().orElse("")));

			return ExitCode.valueOf(process.waitFor());

		} catch (IOException | InterruptedException err) {
			System.err.println(String.format("An error occurred while starting the process: ", err.getMessage()));
			err.printStackTrace();
			return ExitCode.FATAL_ERROR;
		}
	}

	/* TODO
	private static void watchRam() {
		System.out.println(String.format("RAM available: %d", Main.getFreeRam()));
		if(Main.getFreeRam() < 0.5f) {
			System.err.println("The available RAM is too low, restarting process...");
			process.destroy();
			try {
				if(!process.waitFor(10, TimeUnit.SECONDS)) {
					System.err.println("The process has not been cleanly destroyed, killing the process forcibly...");
					process.destroyForcibly();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Main.start();
		}
	}
	 */

	private static float getFreeRam() {
		return OS_BEAN.getFreePhysicalMemorySize() / GB;
	}

	private static float getTotalRam() {
		return OS_BEAN.getTotalPhysicalMemorySize() / GB;
	}
}
