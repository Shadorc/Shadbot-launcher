package com.shadorc.shadbot.launcher;

import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;

public class Utils {

    public static final Logger LOGGER = LoggerFactory.getLogger("shadbot-launcher");

    private static final OperatingSystemMXBean OS_BEAN =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final float GB = 1024 * 1024 * 1024;

    public static float getFreeRam() {
        return OS_BEAN.getFreePhysicalMemorySize() / GB;
    }

    public static float getTotalRam() {
        return OS_BEAN.getTotalPhysicalMemorySize() / GB;
    }

    static String getJarPath() {
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
