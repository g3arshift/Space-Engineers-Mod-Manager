package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.OperatingSystemVersion;
import com.gearshiftgaming.se_mod_manager.backend.data.SimpleSteamLibraryFoldersVdfParser;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfileInfo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager.OPERATING_SYSTEM_VERSION;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SteamModDownloadService implements ModDownloadService {

    //TODO: Add Space Engineers 2 support. Lotta values here are hardcoded and need to change based on if a save is SE1 or 2.
    //TODO: When j25 comes out, make these stable values.
    /**
     * This is the root path where mods need to be placed. When downloading a mod, create a folder in this directory equal to the mod ID, and download the mod there.
     */
    private final String CLIENT_MOD_DOWNLOAD_ROOT;

    /**
     * This is the root path for dedicated server saves. When downloading a mod, append the save name to this, and then /content/244850
     */
    private final String DEDICATED_SERVER_MOD_DOWNLOAD_ROOT;

    //TODO: For win/linux Torch servers they're saved at: torch/Instance/content/244850/
    // So when we are downloading mods for a torch save, go up two levels to reach the "instance" folder from the .sbc file, to reach content.

    //TODO: We need a fallback location for downloading, let's make a folder called "Downloaded_Mods" in application directory for this.
    // It should be used when we check if our intended path exists, and if not, throw an error but download anyways. Just need to warn the user it's happened.
    // In this we just want to toss a result. At the higher level, if we download single mods have a diff message than if we DL multiple. If we DL multiple,
    // then return the normal error, but handle it at the high level like we normally do for mod scrape fails for multiple.

    private final String STEAM_CMD_PATH;

    public SteamModDownloadService(String steamCmdPath) throws IOException, InterruptedException {
        if (Files.notExists(Path.of(steamCmdPath)))
            throw new SteamInstallMissingException("A valid SteamCMD install was not found at: " + steamCmdPath);

        this.STEAM_CMD_PATH = steamCmdPath;

        this.CLIENT_MOD_DOWNLOAD_ROOT = getSpaceEngineersClientDownloadPath();

        this.DEDICATED_SERVER_MOD_DOWNLOAD_ROOT = getDedicatedServerRoot();
    }

    //For win/linux clients they're saved at: SE_Install_Path/steamapps/workshop/content/244850
    // On windows you can find libraryfolders.vdf in Steam_Install_Path/config/libraryfolders.vdf
    // On Linux this s found in $HOME/.steam/steam/config/libraryfolders.vdf
    public String getSpaceEngineersClientDownloadPath() throws IOException, InterruptedException {
        String steamPath;
        if (OPERATING_SYSTEM_VERSION == OperatingSystemVersion.LINUX) {
            if (Files.notExists(Path.of("$HOME/.steam/steam/config/libraryfolders.vdf")))
                throw new SteamInstallMissingException("Unable to find the steam installation path.");

            steamPath = "$HOME/.steam/steam/config/libraryfolders.vdf";
        } else {
            steamPath = getWindowsSteamInstallPath();
            if (steamPath.isBlank())
                throw new SteamInstallMissingException("Unable to find the steam installation path.");
        }

        String spaceEngineersInstallLocation = getSpaceEngineersDiskLocation(Path.of(steamPath).
                resolve("steamapps").
                resolve("libraryfolders.vdf").
                toString());

        return spaceEngineersInstallLocation + "/steamapps/workshop/content/244850";
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

    @SuppressWarnings("unchecked")
    private String getSpaceEngineersDiskLocation(String filePath) throws IOException {
        SimpleSteamLibraryFoldersVdfParser vdfParser = new SimpleSteamLibraryFoldersVdfParser();
        HashMap<String, Object> steamInstallLocations = (HashMap<String, Object>) vdfParser.parseVdf(filePath).get("libraryfolders");

        //Go through every map and submap we have, which represents the heirarchy of a .vdf file, to find the SE 244850 app ID.
        for (Object diskBlockObj : steamInstallLocations.values()) {
            if (diskBlockObj instanceof HashMap diskBlock) {
                Object appsObj = diskBlock.get("apps");
                if ((appsObj instanceof HashMap<?, ?> appsBlock) && appsBlock.containsKey("244850")) {
                    return (String) diskBlock.get("path");
                }
            }
        }
        throw new SpaceEngineersNotFoundException("Could not find the client installation path for Space Engineers. This is most likely due to it not being installed.");
    }

    //For win SE dedicated server mods are downloaded to: programdata\spaceengineersdedicated\save_name
    //For linux wine SE dedicated server they're saved at: $HOME/.wine/drive_c/users/$USER/AppData/Roaming/SpaceEngineersDedicated/content/244850
    //This is only the root path, we need to append the save name afterward for it to work correctly.
    private String getDedicatedServerRoot() {
        if (OPERATING_SYSTEM_VERSION == OperatingSystemVersion.LINUX)
            return "$HOME/.wine/drive_c/users/$USER/AppData/Roaming/SpaceEngineersDedicated/content/244850";
        else
            return "%programdata%/SpaceEngineersDedicated";
    }

    @Override
    public Result<String> downloadMod(String modId, SaveProfileInfo saveProfileInfo) {
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
