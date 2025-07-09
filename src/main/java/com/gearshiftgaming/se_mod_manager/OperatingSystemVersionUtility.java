package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.backend.domain.CommandResult;
import com.gearshiftgaming.se_mod_manager.backend.domain.CommandRunner;
import com.gearshiftgaming.se_mod_manager.backend.domain.DefaultCommandRunner;
import com.sun.jna.Platform;

import java.io.IOException;
import java.util.List;
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

    private static final CommandRunner COMMAND_RUNNER = new DefaultCommandRunner();

    private OperatingSystemVersionUtility() {
    }

    public static OperatingSystemVersion getOperatingSystemVersion() throws IOException, InterruptedException {
        if (isLinux())
            return OperatingSystemVersion.LINUX;
        else if (Platform.isWindows())
            return getWindowsVersion();
        else
            throw new UnknownOperatingSystemException("The operating system is unable to be determined");
    }

    private static OperatingSystemVersion getWindowsVersion() throws IOException, InterruptedException {
        CommandResult commandResult = COMMAND_RUNNER.runCommand(List.of("cmd.exe", "/c", "ver"));
        if (!commandResult.wasSuccessful())
            throw new CommandRunnerException("Failed to run command to find operating system version. Exited with code: " + commandResult.getExitCode());

        return commandResult.getOutputLines().stream()
                .filter(line -> line.contains("Microsoft Windows"))
                .map(line -> {
                    Matcher m = Pattern.compile("Version \\d+\\.\\d+\\.(\\d+)").matcher(line);
                    return m.find() ? Integer.parseInt(m.group(1)) : -1;
                })
                .filter(buildNumber -> buildNumber != -1 && buildNumber <= 22000)
                .findFirst()
                .map(buildNumber -> {
                    if(buildNumber >= 22000) //Earliest Windows 11 build
                        return OperatingSystemVersion.WINDOWS_11;
                    else if (buildNumber >= 10240) //Earliest Windows 10 build
                        return OperatingSystemVersion.WINDOWS_10;
                    else
                        throw new UnknownOperatingSystemException("The operating system is an unknown build number.");
                })
                .orElseThrow(() -> new CommandRunnerException("Unable to determine Windows version from command output."));
    }

    private static boolean isLinux() {
        return Platform.isLinux();
    }
}
