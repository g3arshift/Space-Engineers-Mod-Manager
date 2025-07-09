package com.gearshiftgaming.se_mod_manager.backend.domain;

import java.util.List;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class CommandResult {

    private final int exitCode;

    private final List<String> outputLines;

    public CommandResult(int exitCode, List<String> outputLines) {
        this.exitCode = exitCode;
        this.outputLines = outputLines;
    }

    public int getExitCode() {
        return exitCode;
    }

    public List<String> getOutputLines() {
        return outputLines;
    }

    public String getLastLine() {
        return outputLines.isEmpty() ? "" : outputLines.getLast();
    }

    public boolean wasSuccessful() {
        return exitCode == 0;
    }
}
