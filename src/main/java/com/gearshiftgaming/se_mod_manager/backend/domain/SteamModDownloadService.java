package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

//TODO: CHECK ALL OF THIS.
/**
 * Service class responsible for downloading Steam Workshop mods using SteamCMD.
 * Downloads mods to the Steam Workshop content directory for the Space Engineers install.
 * <p>
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SteamModDownloadService implements ModDownloadService{

    private final String STEAM_APP_ID = "244850"; // Space Engineers Steam App ID
    private final String STEAMCMD_DOWNLOAD_TIMEOUT_MINUTES;
    private final String STEAM_WORKSHOP_CONTENT_PATH;
    private final String STEAMCMD_PATH;
    private final boolean STEAMCMD_VALIDATE_DOWNLOADS;

    public SteamModDownloadService(Properties properties) {
        this.STEAMCMD_DOWNLOAD_TIMEOUT_MINUTES = properties.getProperty("semm.steam.download.timeout.minutes", "10");
        this.STEAM_WORKSHOP_CONTENT_PATH = properties.getProperty("semm.steam.workshop.content.path", getSteamWorkshopPath());
        this.STEAMCMD_PATH = properties.getProperty("semm.steam.steamcmd.folder.path", getSteamCmdPath());
        this.STEAMCMD_VALIDATE_DOWNLOADS = Boolean.parseBoolean(properties.getProperty("semm.steam.download.validate", "true"));
    }

    /**
     * Downloads a Steam Workshop mod using SteamCMD to the Steam Workshop content directory for the Space Engineers install.
     *
     * @param modId The Steam Workshop mod ID to download
     * @return Result containing success/failure information and any error messages, as well as a payload containing the path of the downloaded mod.
     */
    //TODO: Rewrite this whole thing with guard clauses
    @Override
    public Result<String> downloadMod(String modId) {
        Result<String> downloadResult = new Result<>();

        // Validate SteamCMD installation
        Result<Void> steamCmdValidation = validateSteamCmdInstallation();
        if (steamCmdValidation.getType() == ResultType.FAILED) {
            downloadResult.addMessage("SteamCMD validation failed: " + steamCmdValidation.getCurrentMessage(), ResultType.FAILED);
            return downloadResult;
        }

        // Ensure workshop content directory exists
        Path workshopPath = Paths.get(STEAM_WORKSHOP_CONTENT_PATH, STEAM_APP_ID);
        try {
            Files.createDirectories(workshopPath);
        } catch (IOException e) {
            downloadResult.addMessage("Failed to create workshop directory: " + workshopPath, ResultType.FAILED);
            downloadResult.addMessage(getStackTrace(e), ResultType.FAILED);
            return downloadResult;
        }

        // Build SteamCMD command
        List<String> command = buildSteamCmdCommand(modId);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            downloadResult.addMessage("Starting download of mod " + modId + " using SteamCMD...", ResultType.SUCCESS);

            Process process = processBuilder.start();

            // Read output from SteamCMD
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Log important SteamCMD messages
                    if (line.contains("Success") || line.contains("ERROR") || line.contains("FAILED")) {
                        downloadResult.addMessage("SteamCMD: " + line, line.contains("ERROR") || line.contains("FAILED") ? ResultType.FAILED : ResultType.SUCCESS);
                    }
                }
            }

            // Wait for process to complete with timeout
            boolean finished = process.waitFor(Long.parseLong(STEAMCMD_DOWNLOAD_TIMEOUT_MINUTES), TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                downloadResult.addMessage("SteamCMD download timed out after " + STEAMCMD_DOWNLOAD_TIMEOUT_MINUTES + " minutes", ResultType.FAILED);
                return downloadResult;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                // Verify the mod was actually downloaded
                Path modPath = workshopPath.resolve(modId);
                if (Files.exists(modPath) && Files.isDirectory(modPath)) {
                    downloadResult.addMessage("Successfully downloaded mod " + modId + " to " + modPath, ResultType.SUCCESS);
                    downloadResult.setPayload(modPath.toString());
                } else {
                    downloadResult.addMessage("SteamCMD completed but mod directory not found: " + modPath, ResultType.FAILED);
                }
            } else {
                downloadResult.addMessage("SteamCMD failed with exit code: " + exitCode, ResultType.FAILED);
                downloadResult.addMessage("SteamCMD output: " + output, ResultType.FAILED);
            }

        } catch (IOException e) {
            downloadResult.addMessage("Failed to execute SteamCMD: " + e.getMessage(), ResultType.FAILED);
            downloadResult.addMessage(getStackTrace(e), ResultType.FAILED);
        } catch (InterruptedException e) {
            downloadResult.addMessage("SteamCMD download was interrupted", ResultType.FAILED);
            downloadResult.addMessage(getStackTrace(e), ResultType.FAILED);
            Thread.currentThread().interrupt();
        }

        return downloadResult;
    }

    /**
     * Downloads multiple Steam Workshop mods sequentially.
     *
     * @param modIds List of Steam Workshop mod IDs to download
     * @return List of results for each mod download
     */
    @Override
    public List<Result<String>> downloadModList(List<String> modIds) {
        List<Result<String>> results = new ArrayList<>();

        for (String modId : modIds) {
            Result<String> result = downloadMod(modId);
            results.add(result);

            // Add a small delay between downloads to be respectful to Steam servers
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return results;
    }

    /**
     * Checks if a mod is already downloaded in the workshop content directory.
     *
     * @param modId The Steam Workshop mod ID to check
     * @return true if the mod directory exists, false otherwise
     */
    @Override
    public boolean isModDownloaded(String modId) {
        Path modPath = Paths.get(STEAM_WORKSHOP_CONTENT_PATH, STEAM_APP_ID, modId);
        return Files.exists(modPath) && Files.isDirectory(modPath);
    }

    /**
     * Gets the path to a downloaded mod.
     *
     * @param modId The Steam Workshop mod ID
     * @return Path to the mod directory if it exists, null otherwise
     */
    @Override
    public String getModPath(String modId) {
        Path modPath = Paths.get(STEAM_WORKSHOP_CONTENT_PATH, STEAM_APP_ID, modId);
        return Files.exists(modPath) ? modPath.toString() : null;
    }

    private Result<Void> validateSteamCmdInstallation() {
        Result<Void> validationResult = new Result<>();

        File steamCmdFile = new File(STEAMCMD_PATH);
        if (!steamCmdFile.exists()) {
            validationResult.addMessage("SteamCMD not found at: " + STEAMCMD_PATH + ". Please install SteamCMD and configure the path in properties.", ResultType.FAILED);
            return validationResult;
        }

        if (!steamCmdFile.canExecute()) {
            validationResult.addMessage("SteamCMD is not executable: " + STEAMCMD_PATH, ResultType.FAILED);
            return validationResult;
        }

        validationResult.addMessage("SteamCMD validation successful", ResultType.SUCCESS);
        return validationResult;
    }

    private List<String> buildSteamCmdCommand(String modId) {
        List<String> command = new ArrayList<>();
        command.add(STEAMCMD_PATH);
        command.add("+login");
        command.add("anonymous");
        command.add("+workshop_download_item");
        command.add(STEAM_APP_ID);
        command.add(modId);

        if (STEAMCMD_VALIDATE_DOWNLOADS) {
            command.add("validate");
        }

        command.add("+quit");

        return command;
    }

    private String getSteamWorkshopPath() {
        //TODO: I think we can do this better to check OS type.
        //TODO: We can do the entire path check better by running the command running the command "REG QUERY HKLM\SOFTWARE\Wow6432Node\Valve\Steam /v InstallPath"
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            // Default Steam installation path on Windows
            String programFiles = System.getenv("ProgramFiles(x86)");
            if (programFiles == null) {
                programFiles = System.getenv("ProgramFiles");
            }
            if (programFiles != null) {
                return Paths.get(programFiles, "Steam", "steamapps", "workshop", "content").toString();
            }
            return "C:\\Program Files (x86)\\Steam\\steamapps\\workshop\\content";
        } else if (osName.contains("mac")) {
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support", "Steam", "steamapps", "workshop", "content").toString();
        } else {
            // Linux
            return Paths.get(System.getProperty("user.home"), ".steam", "steam", "steamapps", "workshop", "content").toString();
        }
    }

    private String getSteamCmdPath() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "steamcmd.exe";
        } else {
            return "steamcmd";
        }
    }
}
