package me.shadorc.shadbot.launcher;

public enum ExitCode {
	UNKNWON(-1),
	NORMAL(0),
	FATAL_ERROR(1),
	RESTART(2),
	RESTART_CLEAN(3);

	private final int value;

	ExitCode(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}

	public static ExitCode valueOf(int value) {
		for(ExitCode exitCode : ExitCode.values()) {
			if(exitCode.value == value) {
				return exitCode;
			}
		}
		return ExitCode.UNKNWON;
	}
}
