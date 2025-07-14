package com.gearshiftgaming.se_mod_manager.operatingsystem;

import com.gearshiftgaming.se_mod_manager.backend.domain.command.CommandResult;
import com.gearshiftgaming.se_mod_manager.backend.domain.command.CommandRunner;
import com.gearshiftgaming.se_mod_manager.backend.domain.command.CommandRunnerException;
import com.gearshiftgaming.se_mod_manager.backend.domain.command.DefaultCommandRunner;
import com.sun.jna.Platform;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for determining the operating system version.
 * <p>
 * This class attempts to identify whether the host OS is Linux or a supported version of Windows
 * (Windows 10 or 11). It uses the {@link CommandRunner} to execute version-checking commands on Windows
 * and parses the build number to distinguish between versions.
 * <p>
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class OperatingSystemVersionUtility {

    private static final CommandRunner COMMAND_RUNNER;

    static {
        try {
            COMMAND_RUNNER = new DefaultCommandRunner();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read properties ", e);
        }
    }

    private OperatingSystemVersionUtility() {
    }

    /**
     * Determines the current operating system version.
     *
     * @return the {@link OperatingSystemVersion} enum value representing the detected OS.
     * @throws IOException                     if the version check command fails to run.
     * @throws InterruptedException            if the command execution is interrupted.
     * @throws UnknownOperatingSystemException if the OS cannot be determined.
     */
    public static OperatingSystemVersion getOperatingSystemVersion() throws IOException, InterruptedException {
        if (isLinux())
            return OperatingSystemVersion.LINUX;
        else if (Platform.isWindows())
            return getWindowsVersion();
        else
            throw new UnknownOperatingSystemException("The operating system is unable to be determined");
    }

    /**
     * Uses the `cmd /c ver` command to determine whether the Windows version is 10 or 11.
     * Parses the build number from the command output and maps it to a known version.
     *
     * @return the {@link OperatingSystemVersion} for the current Windows system.
     * @throws IOException                     if the command fails to run.
     * @throws InterruptedException            if the command is interrupted.
     * @throws CommandRunnerException          if the command exits unsuccessfully or output is unparseable.
     * @throws UnknownOperatingSystemException if the build number is outside known version ranges.
     */
    private static OperatingSystemVersion getWindowsVersion() throws IOException, InterruptedException {
        CommandResult commandResult = COMMAND_RUNNER.runCommand(List.of("cmd.exe", "/c", "ver"));
        if (!commandResult.wasSuccessful())
            throw new CommandRunnerException("Failed to run command to find operating system version. Exited with code: " + commandResult.exitCode());

        return commandResult.outputLines().stream()
                .filter(line -> line.contains("Microsoft Windows"))
                .map(line -> {
                    Matcher m = Pattern.compile("Version \\d+\\.\\d+\\.(\\d+)").matcher(line);
                    return m.find() ? Integer.parseInt(m.group(1)) : -1;
                })
                .filter(buildNumber -> buildNumber != -1)
                .findFirst()
                .map(buildNumber -> {
                    if (buildNumber >= 22000) //Earliest Windows 11 build
                        return OperatingSystemVersion.WINDOWS_11;
                    else if (buildNumber >= 10240) //Earliest Windows 10 build
                        return OperatingSystemVersion.WINDOWS_10;
                    else
                        throw new UnsupportedOperatingSystemException("The operating system is an unknown build number.");
                })
                .orElseThrow(() -> new CommandRunnerException("Unable to determine Windows version from command output."));
    }

    /**
     * Checks if the current OS is Linux using {@link Platform#isLinux()}.
     *
     * @return true if the OS is Linux; false otherwise.
     */
    private static boolean isLinux() {
        return Platform.isLinux();
    }
}
