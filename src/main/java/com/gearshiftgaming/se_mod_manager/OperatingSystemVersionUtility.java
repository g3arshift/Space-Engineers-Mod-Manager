package com.gearshiftgaming.se_mod_manager;

import com.sun.jna.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class OperatingSystemVersionUtility {

    private OperatingSystemVersionUtility() {
    }

    public static OperatingSystemVersion getOperatingSystemVersion() throws IOException {
        if (isLinux())
            return OperatingSystemVersion.LINUX;
        else if (isWindows10())
            return OperatingSystemVersion.WINDOWS_10;
        else if (isWindows11())
            return OperatingSystemVersion.WINDOWS_11;
        else
            throw new UnknownOperatingSystemException("The operating system is an unknown operating system.");
    }

    private static boolean isWindows10() throws IOException {
        Process process = new ProcessBuilder("cmd.exe", "/c", "ver").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines()
                    .filter(line -> line.contains("Microsoft Windows"))
                    .map(line -> {
                        Matcher m = Pattern.compile("Version \\d+\\.\\d+\\.(\\d+)").matcher(line);
                        return m.find() ? Integer.parseInt(m.group(1)) : -1;
                    })
                    .filter(buildNumber -> buildNumber != -1)
                    .anyMatch(buildNumber -> buildNumber <= 22000);
        }
    }

    private static boolean isWindows11() throws IOException {
        Process process = new ProcessBuilder("cmd.exe", "/c", "ver").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines()
                    .filter(line -> line.contains("Microsoft Windows"))
                    .map(line -> {
                        Matcher m = Pattern.compile("Version \\d+\\.\\d+\\.(\\d+)").matcher(line);
                        return m.find() ? Integer.parseInt(m.group(1)) : -1;
                    })
                    .filter(buildNumber -> buildNumber != -1)
                    .anyMatch(buildNumber -> buildNumber >= 22000);
        }
    }

    private static boolean isLinux() {
        return Platform.isLinux();
    }
}
