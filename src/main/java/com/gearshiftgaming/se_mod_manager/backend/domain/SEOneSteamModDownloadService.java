package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.OperatingSystemVersion;
import com.gearshiftgaming.se_mod_manager.backend.data.SimpleSteamLibraryFoldersVdfParser;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfileInfo;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveType;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager.OPERATING_SYSTEM_VERSION;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SEOneSteamModDownloadService implements ModDownloadService {

    //TODO: When j25 comes out, make these stable values.
    /**
     * This is the root path where mods need to be placed. When downloading a mod, create a folder in this directory equal to the mod ID, and download the mod there.
     */
    private final String clientModDownloadPath;

    /**
     * This is the fallback path for when we cannot find the download directory we want to.
     */
    private final String fallbackDownloadRoot;

    private final String steamCmdLauncherPath;

    private final CommandRunner commandRunner;

    private final SimpleSteamLibraryFoldersVdfParser vdfParser;

    private SEOneSteamModDownloadService(String fallbackDownloadBasePath, String steamCmdPath, CommandRunner commandRunner, SimpleSteamLibraryFoldersVdfParser vdfParser) throws IOException, InterruptedException {
        if (Files.notExists(Path.of(steamCmdPath)))
            throw new SteamInstallMissingException("A valid SteamCMD install was not found at: " + steamCmdPath);

        this.fallbackDownloadRoot = fallbackDownloadBasePath;
        this.steamCmdLauncherPath = steamCmdPath;
        this.commandRunner = commandRunner;
        this.vdfParser = vdfParser;

        String clientRootCandidate = getClientDownloadPath();
        /* We shouldn't need this on account of the previous step throwing an exception if it doesn't exist,
        but there's a very rare scenario it can happen in, so let's just be safe. */
        if (Files.notExists(Path.of(clientRootCandidate)) || clientRootCandidate.isBlank())
            clientModDownloadPath = fallbackDownloadRoot;
        else
            clientModDownloadPath = clientRootCandidate;
    }

    public static SEOneSteamModDownloadService create(String steamCmdLauncherPath, CommandRunner commandRunner, SimpleSteamLibraryFoldersVdfParser vdfParser) throws IOException, InterruptedException {
        return new SEOneSteamModDownloadService("./Mod_Downloads", steamCmdLauncherPath, commandRunner, vdfParser);
    }

    public static SEOneSteamModDownloadService createWithCustomFallbackRoot(String fallbackDownloadRoot, String steamCmdLauncherPath, CommandRunner commandRunner, SimpleSteamLibraryFoldersVdfParser vdfParser) throws IOException, InterruptedException {
        return new SEOneSteamModDownloadService(fallbackDownloadRoot + "/Mod_Downloads", steamCmdLauncherPath, commandRunner, vdfParser);
    }

    /*For win/linux clients they're saved at: SE_Install_Path/steamapps/workshop/content/244850
     On windows you can find libraryfolders.vdf in Steam_Install_Path/config/libraryfolders.vdf
     On Linux this s found in $HOME/.steam/steam/config/libraryfolders.vdf*/
    private String getClientDownloadPath() throws IOException, InterruptedException {
        String steamPath;
        if (OPERATING_SYSTEM_VERSION == OperatingSystemVersion.LINUX) {
            if (Files.notExists(Path.of("$HOME/.steam/steam/config/libraryfolders.vdf")))
                return "";

            steamPath = "$HOME/.steam/steam/config/libraryfolders.vdf";
        } else {
            steamPath = getWindowsSteamInstallPath();
            if (steamPath.isBlank())
                return "";
        }

        return getSpaceEngineersDiskLocation(Path.of(steamPath).
                resolve("steamapps").
                resolve("libraryfolders.vdf").
                toString());
    }

    private String getWindowsSteamInstallPath() throws IOException, InterruptedException {
        CommandResult commandResult = commandRunner.runCommand(List.of("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath"));
        if (!commandResult.wasSuccessful())
            throw new SteamInstallMissingException("Unable to find the steam installation path. Registry query failed with exit code: " + commandResult.exitCode());

        String steamInstallPath = "";
        for (String line : commandResult.outputLines()) {
            if (line.contains("InstallPath")) {
                // Extract the path part (after "REG_SZ")
                String[] parts = line.trim().split("\\s{2,}");
                if (parts.length >= 3) {
                    steamInstallPath = parts[2];
                    break;
                }
            }
        }

        return steamInstallPath;
    }

    @SuppressWarnings("unchecked")
    private String getSpaceEngineersDiskLocation(String filePath) throws IOException {
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
        return "";
    }

    //For win SE dedicated server mods are downloaded to: programdata\spaceengineersdedicated\save_name
    //For linux wine SE dedicated server they're saved at: $HOME/.wine/drive_c/users/$USER/AppData/Roaming/SpaceEngineersDedicated/content/244850
    //This is only the root path, we need to append the save name afterward for it to work correctly.
    private String getDedicatedServerRoot() {
        if (OPERATING_SYSTEM_VERSION == OperatingSystemVersion.LINUX)
            return "$HOME/.wine/drive_c/users/$USER/AppData/Roaming/SpaceEngineersDedicated/content/244850";
        else
            return System.getenv("PROGRAMDATA") + "\\SpaceEngineersDedicated";
    }

    @Override
    public Result<Void> downloadMod(String modId, SaveProfileInfo saveProfileInfo) throws IOException, InterruptedException {
        Result<Void> modDownloadResult = new Result<>();
        if (!saveProfileInfo.saveExists()) {
            modDownloadResult.addMessage(String.format("Save does not exist. Cannot download mods for save \"%s\".", saveProfileInfo.getProfileName()), ResultType.FAILED);
            return modDownloadResult;
        }

        Result<Path> downloadPathResult = getModDownloadPath(saveProfileInfo);
        modDownloadResult.addAllMessages(downloadPathResult);
        Path downloadPath = downloadPathResult.getPayload();

        if (downloadPath.startsWith(fallbackDownloadRoot)) {
            modDownloadResult.addMessage("Download location does not exist, using fallback location instead!", ResultType.WARN);
            if (Files.notExists(downloadPath))
                Files.createDirectories(downloadPath);
        }

        modDownloadResult.addMessage(String.format("Starting download of mod: %s", modId), ResultType.IN_PROGRESS);

        //We have to run this to make sure SteamCMD applies any updates it needs BEFORE downloading the workshop item
        CommandResult steamUpdateCommandResult = commandRunner.runCommand(List.of(steamCmdLauncherPath, "+login", "anonymous", "+quit"));
        if(!steamUpdateCommandResult.wasSuccessful() && steamUpdateCommandResult.exitCode() != 7) { //Exclude 7 code since that returns from an update
            modDownloadResult.addMessage("Failed to update SteamCMD. Exited with code: " + steamUpdateCommandResult.exitCode(), ResultType.FAILED);
            return modDownloadResult;
        }

        CommandResult workshopDownloadCommandResult = commandRunner.runCommand(List.of(steamCmdLauncherPath,
                "+force_install_dir", downloadPath.toString(),
                "+login", "anonymous",
                "+workshop_download_item", "244850", modId,
                "+quit"));
        if (!workshopDownloadCommandResult.wasSuccessful()) {
            modDownloadResult.addMessage("SteamCMD failed with exit code: " + workshopDownloadCommandResult.exitCode(), ResultType.FAILED);
            return modDownloadResult;
        }

        /* We iterate in reverse and only check the ten most recent lines as it will be generally faster
        since our success message will always be at the rear. */
        String lastLine = "";
        int commandOutputLinesSize = workshopDownloadCommandResult.outputLines().size() - 1; //This is adjusted by one so we don't keep having to do it elsewhere
        for (int i = commandOutputLinesSize; i >= Math.max(0, commandOutputLinesSize - 7); i--) {
            lastLine = workshopDownloadCommandResult.outputLines().get(i);
            if (lastLine.toLowerCase().startsWith("success"))
                break;
        }

        if (lastLine.isBlank() || !lastLine.toLowerCase().startsWith("success")) {
            modDownloadResult.addMessage(String.format("Mod %s failed to download. SteamCMD reported: \"%s\".", modId, lastLine), ResultType.FAILED);
            return modDownloadResult;
        }

        //If we have a dedicated or torch server, we want to put the files where they belong.
        if (saveProfileInfo.getSaveType() == SaveType.DEDICATED_SERVER || saveProfileInfo.getSaveType() == SaveType.TORCH) {
            //Get the folder location of the downloaded mod
            String modFileFolderPath = downloadPath
                    .resolve("steamapps")
                    .resolve("workshop")
                    .resolve("content")
                    .resolve("244850")
                    .resolve(modId)
                    .toString();

            //Move the mod folder to the right directory
            Path destinationPath = downloadPath
                    .resolve("content")
                    .resolve("244850");
            Files.createDirectories(destinationPath);
            Files.move(Path.of(modFileFolderPath), downloadPath
                    .resolve("content")
                    .resolve("244850")
                    .resolve(modId));

            //Delete the leftover and unwanted steamapps directory
            FileUtils.deleteDirectory(new File(downloadPath.resolve("steamapps").toString()));
        }

        modDownloadResult.addMessage(String.format("Successfully downloaded mod %s.", modId), ResultType.SUCCESS);
        return modDownloadResult;
    }

    @Override
    public boolean isModDownloaded(String modId, SaveProfileInfo saveProfileInfo) throws IOException {
        Result<Path> downloadPathResult = getModDownloadPath(saveProfileInfo);
        Path modPath = downloadPathResult.getPayload();

        if (saveProfileInfo.getSaveType() != SaveType.CLIENT)
            modPath = modPath.resolve("content").resolve("244850").resolve(modId);

        boolean isModDownloaded = false;
        if (Files.exists(modPath)) {
            try (Stream<Path> entries = Files.list(modPath)) {
                isModDownloaded = entries.findFirst().isPresent();
            }
        }
        return isModDownloaded;
    }

    //TODO: Use this with the conflict check
    @Override
    public Result<Path> getModLocation(String modId, SaveProfileInfo saveProfileInfo) {
        return getModDownloadPath(saveProfileInfo);
    }

    @Override
    public boolean shouldUpdateMod(String modId, int remoteFileSize, SaveProfileInfo saveProfileInfo) {
        //TODO: If our mod exists, check the size of our download files versus the remote, and if different, download. If same, don't.
        //TODO: This is going to require modifying our scraping to also pull back the file size. Don't need to store it though, just scrape it.
        //TODO: Mod.io stores it too!
        return false;
    }


    private Result<Path> getModDownloadPath(SaveProfileInfo saveProfileInfo) {
        Result<Path> modDownloadResult = new Result<>();
        Path downloadPath = switch (saveProfileInfo.getSaveType()) {
            case CLIENT -> Path.of(clientModDownloadPath);
            case DEDICATED_SERVER,
                 TORCH -> //This is two levels up from our save path, it has three parent calls because it includes the file itself.
                    Path.of(saveProfileInfo.getSavePath())
                            .getParent()
                            .getParent()
                            .getParent();
        };

        if (Files.notExists(downloadPath)) {
            modDownloadResult.addMessage(String.format("Mod download location does not exist where it should for save \"%s\" of save type \"%s\"." +
                            "This is likely caused by the save having the wrong save type, or the save associated with the save profile not existing in \"%s.\"",
                    saveProfileInfo.getProfileName(), saveProfileInfo.getSaveType(), downloadPath), ResultType.WARN);
            downloadPath = Path.of(fallbackDownloadRoot).resolve(saveProfileInfo.getSaveName());
        } else
            modDownloadResult.addMessage("Download path for save found.", ResultType.SUCCESS);

        modDownloadResult.setPayload(downloadPath);
        return modDownloadResult;
    }
}
