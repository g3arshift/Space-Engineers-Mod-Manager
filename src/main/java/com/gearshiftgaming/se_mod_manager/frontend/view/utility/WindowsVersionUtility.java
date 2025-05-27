package com.gearshiftgaming.se_mod_manager.frontend.view.utility;

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
public class WindowsVersionUtility {

    private WindowsVersionUtility() {
    }

    public static boolean isWindows10() throws IOException {
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

    public static boolean isWindows11() throws IOException {
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
}
