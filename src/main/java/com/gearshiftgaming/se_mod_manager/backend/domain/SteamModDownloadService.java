package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.OperatingSystemVersion;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveType;
import com.gearshiftgaming.se_mod_manager.backend.models.SteamMod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager.OPERATING_SYSTEM_VERSION;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SteamModDownloadService implements ModDownloadService{

    //TODO: When j25 comes out, make these stable values.
    //TODO: For win/linux clients they're saved at: SE_Install_Path/steamapps/workshop/content/244850
    // On windows you can find libraryfolders.vdf in Steam_Install_Path/config/libraryfolders.vdf
    // On Linux this s found in $HOME/.steam/steam/config/libraryfolders.vdf
    private final String CLIENT_MOD_DOWNLOAD_PATH;

    //TODO: For win SE dedicated server mods are downloaded to: programdata\spaceengineersdedicated\save_name
    //TODO: For linux wine SE dedicated server they're saved at: $HOME/.wine/drive_c/users/$USER/AppData/Roaming/SpaceEngineersDedicated/content/244850
    //private final String DEDICATED_SERVER_MOD_DOWNLOAD_PATH;

    //TODO: For win/linux Torch servers they're saved at: torch/Instance/content/244850/
    //private final String TORCH_SERVER_MOD_DOWNLOAD_PATH;

    private final String STEAM_CMD_PATH;

    public SteamModDownloadService(String steamCmdPath, Properties properties) throws IOException, InterruptedException {
        this.CLIENT_MOD_DOWNLOAD_PATH = getSpaceEngineersInstallPath();

        //this.DEDICATED_SERVER_MOD_DOWNLOAD_PATH = getDedicatedServerPath();
        //this.TORCH_SERVER_MOD_DOWNLOAD_PATH = getTorchServerPath();
        this.STEAM_CMD_PATH = steamCmdPath;
    }

    public String getSpaceEngineersInstallPath() throws IOException, InterruptedException {
        String steamPath;
        if(OPERATING_SYSTEM_VERSION == OperatingSystemVersion.LINUX) {
            if(Files.notExists(Path.of("$HOME/.steam/steam/config/libraryfolders.vdf")))
                throw new SteamInstallMissingException("Unable to find the steam installation path.");

            steamPath = "$HOME/.steam/steam/config/libraryfolders.vdf";
        } else {
            steamPath = getWindowsSteamInstallPath();
            if(steamPath.isBlank())
                throw new SteamInstallMissingException("Unable to find the steam installation path.");
        }

        //TODO: Find our client path based on the libraryvdf file. Throw an exception if we can't find it.


        //return + "/steamapps/workshop/content/244850";
        return null;
    }

    private String getWindowsSteamInstallPath() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        String steamInstallPath = "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("InstallPath")) {
                    // Extract the path part (after "REG_SZ")
                    String[] parts = line.trim().split("\\s{2,}");
                    if (parts.length >= 3) {
                        steamInstallPath = parts[2];
                    }
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0)
            throw new SteamInstallMissingException("Unable to find the steam installation path. Registry query failed with exit code: " + exitCode);

        return steamInstallPath;
    }

    @Override
    public Result<String> downloadMod(String modId, SaveType saveType) {
        //TODO: Let's do something smarter.
        // When the user adds a save profile, ask them what kind of save it is. Torch, Dedicated server, or normal game?
        //TODO: As a part of the above process, depending on our save mode it will alter our download location.
        // That makes this entire class pointless, or rather, we need to move it somewhere else since, depending on our save profile, the install path will change.
        //TODO: To summarize:
        // 1. Find the type of install the current save is
        // 2. Download our mods to the correct path based on our install.
        //     2a. For client installs, this means we need to find libraryfolders.vdf and find our path.
        //         For win query the registry, for linux... Hope it's in the right place, and if not, have them locate it manually.

        //TODO:
        // 1. Store our above paths
        // 2. When we start the app, set a global variable for if the system is linux or windows, and also the windows version.
        //    We should also replace our stuff in other places we do an OS check with this variable call!
        // 3. When we download mods, use the appropriate path based on OS and save type.
        //    3a. When we download mods we need to always check the fully resolved path exists because people can select the wrong save type accidentally.
        //        If it doesn't, throw a custom exception and error, say they probably set the wrong profile type since the path for that save doesn't exist.

        //TODO: Also use our steamcmd check function to make sure it exists. It should by the time we call this, but let's play it safe.

        //TODO: Our basic code flow path is that we check for the libraryfolders.vdf file in some expected places in either linux or windows, then set a var if we found it or not.
        // We also need to ask them if they're using it for SE dedicated server, or SE game.
        // Make sure to tell the user that it'll still work for the other when you choose SE server or Game, but the downloaded mods will go into the right folder for the option they chose.
        // If the var is false, we want to prompt the user to select where they have SE installed. Also present the SE dedicated server as an option if they're using SEMM to manage server config.
        // When they select a folder check if the SE.exe or the SE dedicated server.exe are there. If not, say it's a bad selection. Let them continuously try until they choose to quit or they give a valid path.
        return null;
    }

    @Override
    public List<Result<String>> downloadModList(List<String> modIds) {
        return List.of();
    }

    @Override
    public boolean isModDownloaded(String modId) {
        return false;
    }

    @Override
    public String getModPath(String modId) {
        return "";
    }

    private boolean isSteamCmdInstalled() {
        return Files.exists(Path.of(STEAM_CMD_PATH));
    }
}
