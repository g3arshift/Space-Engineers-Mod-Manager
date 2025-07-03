package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.OperatingSystemVersion;
import com.gearshiftgaming.se_mod_manager.backend.data.SimpleSteamLibraryFoldersVdfParser;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
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
    private final String clientModDownloadPath;

    /**
     * This is the root path for dedicated server saves. When downloading a mod, append the save name to this, and then /content/244850
     */
    private final String dedicatedServerDownloadPath;

    /**
     * This is the fallback path for when we cannot find the download directory we want to.
     */
    private static final String FALLBACK_DOWNLOAD_ROOT = "./Mod_Downloads";

    private final String steamCmdExePath;

    public SteamModDownloadService(String steamCmdPath) throws IOException, InterruptedException {
        if (Files.notExists(Path.of(steamCmdPath)))
            throw new SteamInstallMissingException("A valid SteamCMD install was not found at: " + steamCmdPath);

        this.steamCmdExePath = steamCmdPath;

        String clientRootCandidate = getSpaceEngineersClientDownloadPath();
        //We shouldn't need this on account of the previous step throwing an exception if it doesn't exist, but there's a very rare scenario it can happen in.
        if (Files.notExists(Path.of(clientRootCandidate)))
            clientModDownloadPath = FALLBACK_DOWNLOAD_ROOT;
        else
            clientModDownloadPath = clientRootCandidate;

        String dedicatedServerRootCandidate = getDedicatedServerRoot();
        if (Files.notExists(Path.of(dedicatedServerRootCandidate)))
            dedicatedServerDownloadPath = FALLBACK_DOWNLOAD_ROOT;
        else
            dedicatedServerDownloadPath = dedicatedServerRootCandidate;
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

        return spaceEngineersInstallLocation;
    }

    private String getWindowsSteamInstallPath() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        String steamInstallPath = getSteamInstallPath(process);

        int exitCode = process.waitFor();
        if (exitCode != 0)
            throw new SteamInstallMissingException("Unable to find the steam installation path. Registry query failed with exit code: " + exitCode);

        return steamInstallPath;
    }

    private static String getSteamInstallPath(Process process) throws IOException {
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
        return steamInstallPath;
    }

    @SuppressWarnings("unchecked")
    private String getSpaceEngineersDiskLocation(String filePath) throws IOException {
        SimpleSteamLibraryFoldersVdfParser vdfParser = new SimpleSteamLibraryFoldersVdfParser();
        HashMap<String, Object> steamInstallLocations = (HashMap<String, Object>) vdfParser.parseVdf(filePath).get("libraryfolders");

        //Go through every map and submap we have, which represents the hierarchy of a .vdf file, to find the SE 244850 app ID.
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
    public Result<Void> downloadMod(String modId, SaveProfileInfo saveProfileInfo) throws IOException, InterruptedException {
        Result<Void> modDownloadResult = new Result<>();
        if (!saveProfileInfo.saveExists()) {
            modDownloadResult.addMessage(String.format("Save does not exist. Cannot download mods for save \"%s\".", saveProfileInfo.getProfileName()), ResultType.FAILED);
            return modDownloadResult;
        }

        String downloadPath = switch (saveProfileInfo.getSaveType()) {
            case GAME:
                yield clientModDownloadPath;
            case DEDICATED_SERVER:
                yield dedicatedServerDownloadPath;
            case TORCH: {
                //This is two levels up from our save path, then down to /content/244850/modId, which is where we need to save stuff.
                yield Path.of(saveProfileInfo.getSavePath())
                        .getParent()
                        .getParent()
                        .toString();
            }
        };

        if(Files.notExists(Path.of(downloadPath))) {
            throw new MissingModDownloadLocationException(String.format("Mod download location does not exist where it should for save \"%s\" of save type \"%s\"." +
                    "This is likely caused by the save having the wrong save type.", saveProfileInfo.getProfileName(), saveProfileInfo.getSaveType()));
        }


        //TODO: When we handle this at higher levels, just handle it like we do scraping many mods/single mods.
        // In this we just want to toss a result. At the higher level, if we download single mods have a diff message than if we DL multiple. If we DL multiple,
        // then return the normal error, but handle it at the high level like we normally do for mod scrape fails for multiple.
        if (downloadPath.contains(FALLBACK_DOWNLOAD_ROOT))
            modDownloadResult.addMessage("Download location does not exist, using fallback location instead!", ResultType.WARN);

        modDownloadResult.addMessage(String.format("Starting download of mod: %s", modId), ResultType.IN_PROGRESS);

        ProcessBuilder processBuilder = new ProcessBuilder(steamCmdExePath,
                "+force_install_dir", downloadPath,
                "+login", "anonymous",
                "+workshop_download_item", "244850", modId,
                "validate", "+quit");

        Process process = processBuilder.start();

        String lastLine = "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastLine = line;
                if(lastLine.toLowerCase().startsWith("success"))
                    break;
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            modDownloadResult.addMessage("SteamCMD failed with exit code: " + exitCode, ResultType.FAILED);
            return modDownloadResult;
        }

        //TODO: We're getting a lot of text after it's done downloading cause of validation. Step through and find something that works properly.
        if (lastLine.isBlank() || !lastLine.toLowerCase().startsWith("success")) {
            modDownloadResult.addMessage(String.format("Mod %s failed to download. SteamCMD reported: \"%s\".", modId, lastLine), ResultType.FAILED);
            return modDownloadResult;
        }

        modDownloadResult.addMessage(String.format("Successfully downloaded mod %s.", modId), ResultType.SUCCESS);
        return modDownloadResult;
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
        return Files.exists(Path.of(steamCmdExePath));
    }
}
