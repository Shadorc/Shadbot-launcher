package com.shadorc.shadbot.launcher;

import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class Utils {

    public static final Logger LOGGER = LoggerFactory.getLogger("shadbot-launcher");

    private static final OperatingSystemMXBean OS_BEAN =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final float GB = 1024 * 1024 * 1024;

    public static float getFreeRam() {
        return OS_BEAN.getFreeMemorySize() / GB;
    }

    public static float getTotalRam() {
        return OS_BEAN.getTotalMemorySize() / GB;
    }

    static String getJarPath() {
        final String jarPath = System.getProperty("file");
        if (jarPath != null) {
            return jarPath;
        }

        final List<String> filenames = new ArrayList<>(1);

        final Pattern pattern = Pattern.compile("shadbot-[0-9]+.[0-9]+.[0-9]+.jar");
        for (final File file : Objects.requireNonNull(new File(".").listFiles())) {
            final String fileName = file.getName();
            if (file.isFile() && pattern.matcher(fileName).matches()) {
                filenames.add(fileName);
            }
        }

        if (filenames.isEmpty()) {
            return null;
        }

        Collections.sort(filenames);
        Collections.reverse(filenames);
        return filenames.get(0);
    }

}
