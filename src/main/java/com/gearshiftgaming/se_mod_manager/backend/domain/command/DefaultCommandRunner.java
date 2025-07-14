package com.gearshiftgaming.se_mod_manager.backend.domain.command;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
@Slf4j
public class DefaultCommandRunner implements CommandRunner {

    private final int timeout;

    public DefaultCommandRunner() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
            properties.load(input);
        } catch (IOException | NullPointerException e) {
            log.error("Could not load SEMM.properties. {}", e.getMessage());
            throw (e);
        }

        timeout = Integer.parseInt(properties.getProperty("semm.command.timeout"));
    }

    @Override
    public CommandResult runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        List<String> output = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while((line = reader.readLine()) != null) {
                output.add(line);
            }
        }

        boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
        if(!finished) {
            process.destroyForcibly();
            throw new CommandTimeoutException(String.format("Process timed out after %s seconds.", timeout));
        }
        int exitCode = process.exitValue();
        return new CommandResult(exitCode, output);
    }
}
