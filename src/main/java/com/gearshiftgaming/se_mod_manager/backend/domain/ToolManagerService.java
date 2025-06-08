package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.MasterManager;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * This class is designed to manage the downloading of all tools and utilities required for the application to function.
 * It additionally manages the UI elements for this process, used in conjunction with a MasterManager where they will be displayed.
 * <p>
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ToolManagerService {

    @Setter
    private IntegerProperty downloadNumerator;

    @Setter
    private IntegerProperty downloadDenominator;

    @Setter
    private DoubleProperty downloadPercentage;

    private ProgressBar downloadProgressBar;

    private ProgressIndicator downloadProgressWheel;

    private Label toolNamePrefix;

    private Label toolName;

    private final MasterManager MASTER_MANAGER;

    private final UiService UI_SERVICE;

    private final String STEAM_CMD_PATH;

    private final String STEAM_CMD_SOURCE_LOCATION;

    public ToolManagerService(Properties properties, MasterManager masterManager, UiService uiService) {
        STEAM_CMD_PATH = properties.getProperty("semm.steam.steamcmd.folder.path");
        STEAM_CMD_SOURCE_LOCATION = properties.getProperty("semm.steam.steamcmd.download.source");
        this.MASTER_MANAGER = masterManager;
        this.UI_SERVICE = uiService;

        downloadNumerator = new SimpleIntegerProperty(-1);
        downloadDenominator = new SimpleIntegerProperty(-1);
        downloadPercentage = new SimpleDoubleProperty(0d);
        toolNamePrefix = new Label();
        toolName = new Label();
        downloadProgressBar = new ProgressBar();
        downloadProgressWheel = new ProgressIndicator();
    }

    //TODO: Need to call all this from UI service and master manager
    private void setupTools() throws IOException {
        //TODO: Display some UI thing here.

        Path steamCmdPath = Path.of(STEAM_CMD_PATH);
        //Make the base directories we need if they don't exist
        if (Files.notExists(steamCmdPath)) {
            Files.createDirectories(steamCmdPath);
        }

        //Check if we already have steam CMD downloaded. If it isn't, download it.
        if (Files.notExists(Path.of(STEAM_CMD_PATH + "/steamcmd.exe"))) {
            //TODO: Add some handling logic here to make sure it can be resumed.
            //TODO: We should probably create a custom "downloadBox" class for the UI that has a numerator and denominator we can bind values to.
            URL steamDownloadUrl = Paths.get(STEAM_CMD_SOURCE_LOCATION).toUri().toURL();
            long remoteSteamCmdFileSize = getSteamCmdRemoteSize(steamDownloadUrl);
            downloadSteamCmd(steamDownloadUrl, STEAM_CMD_PATH, remoteSteamCmdFileSize);
        }

    }

    /**
     * Gets the size of the SteamCMD zip file on the remote server.
     *
     * @param steamCmdUrl The location we will download SteamCMD from.
     * @return The size of the SteamCMD zip on the remote server.
     */
    private long getSteamCmdRemoteSize(URL steamCmdUrl) {
        HttpURLConnection sizeCheckConnection = null;

        //Get the size of the remote file
        try {
            sizeCheckConnection = (HttpURLConnection) steamCmdUrl.openConnection();
            sizeCheckConnection.setRequestMethod("HEAD");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            //Make sure our connection is closed if it was opened.
            if (sizeCheckConnection != null)
                sizeCheckConnection.disconnect();
        }

        return sizeCheckConnection.getContentLengthLong();
    }

    /**
     * Download SteamCMD with a resumable HTTP Connection.
     *
     * @param downloadLocation The location we want to save the SteamCMD zip file to.
     * @return Whether the download is finished or not
     */
    //TODO: Examine this to see if we need to return a value
    private void downloadSteamCmd(URL steamCmdUrl, String downloadLocation, long remoteFileSize) {
        File outputFile = new File(downloadLocation);
        long existingFileSize = outputFile.exists() ? outputFile.length() : 0;

        //If our file on disk is smaller than the remote, resume the download at the byte location we stopped.
        if (existingFileSize < remoteFileSize) {
            HttpURLConnection httpConnection = null;
            try {
                httpConnection = (HttpURLConnection) steamCmdUrl.openConnection();

                if (existingFileSize > 0) {
                    //Request the remaining data we're missing
                    httpConnection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
                }

                int responseCode = httpConnection.getResponseCode();
                boolean isResuming = (responseCode == HttpURLConnection.HTTP_PARTIAL);

                //Writes our file at the specified byte location so we can download it safely.
                try (InputStream inputStream = httpConnection.getInputStream(); RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
                    if (isResuming) {
                        raf.seek(existingFileSize);
                    }

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        raf.write(buffer, 0, bytesRead);
                    }

                    UI_SERVICE.log("Successfully downloaded SteamCMD.", MessageType.INFO);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (httpConnection != null)
                    httpConnection.disconnect();
            }
        } else
            UI_SERVICE.log("SteamCMD already downloaded.", MessageType.WARN);
    }
}
