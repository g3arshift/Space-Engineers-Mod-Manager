package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.concurrent.Task;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * This class is designed to manage the downloading of all tools and utilities required for the application to function.
 * It additionally contains all the UI logic surrounding the display elements shown when downloading a tool during initial setup.
 * <p>
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ToolManagerService {

    private final UiService UI_SERVICE;

    private final String STEAM_CMD_PATH;

    private final String STEAM_CMD_SOURCE_LOCATION;

    public ToolManagerService(Properties properties, UiService uiService) {
        STEAM_CMD_PATH = properties.getProperty("semm.steam.steamcmd.folder.path");
        STEAM_CMD_SOURCE_LOCATION = properties.getProperty("semm.steam.steamcmd.download.source");
        this.UI_SERVICE = uiService;
    }

    //TODO: Need to call all this from UI service and master manager
    //TODO: We need some sort of logic around checking to make sure the download isn't frozen, that the connection hasn't broken, and add a timeout + retries, and a popup if it fails to download.
    //TODO: This should return, and be, a task.
    public Task<Result<Void>> setupTools() throws IOException {
        return new Task<>() {
            @Override
            protected Result<Void> call() throws Exception {
                Path steamCmdPath = Path.of(STEAM_CMD_PATH);
                //Make the base directories we need if they don't exist
                if (Files.notExists(steamCmdPath)) {
                    Files.createDirectories(steamCmdPath);
                }

                Result<Void> toolSetupResult;
                //Check if we already have steam CMD downloaded. If it isn't, download it.
                UI_SERVICE.log("Downloading required tools...", MessageType.INFO);
                if (Files.notExists(Path.of(STEAM_CMD_PATH + "/steamcmd.exe"))) {
                    UI_SERVICE.log("Downloading Steam CMD...", MessageType.INFO);
                    //TODO: Add some handling logic here to make sure it can be resumed.
                    URL steamDownloadUrl = Paths.get(STEAM_CMD_SOURCE_LOCATION).toUri().toURL();
                    long remoteSteamCmdFileSize = getSteamCmdRemoteSize(steamDownloadUrl);
                    toolSetupResult = downloadSteamCmd(steamDownloadUrl,
                            STEAM_CMD_PATH,
                            remoteSteamCmdFileSize,
                            this::updateProgress,
                            this::updateMessage);

                    if (toolSetupResult.isSuccess())
                        UI_SERVICE.log("Successfully downloaded Steam CMD.", MessageType.INFO);
                    else
                        UI_SERVICE.log("Failed to download Steam CMD.", MessageType.ERROR);
                } else {
                    toolSetupResult = new Result<>();
                    toolSetupResult.addMessage("Steam CMD already installed. Skipping.", ResultType.SUCCESS);
                }

                //Add a final message to the download chain if everything succeeded. If it isn't, we just want to log the failure from the more specific method.
                if (toolSetupResult.isSuccess())
                    toolSetupResult.addMessage("Successfully downloaded all required tools.", ResultType.SUCCESS);
                return toolSetupResult;
            }
        };
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
    private Result<Void> downloadSteamCmd(URL steamCmdUrl, String downloadLocation, long remoteFileSize, BiConsumer<Long, Long> progressUpdater, Consumer<String> messageUpdater) {
        Result<Void> downloadResult = new Result<>();
        File outputFile = new File(downloadLocation);

        //TODO: Replace with property values and consts in the class.
        int maxRetries = 3;
        int retryCount = 0;
        int connectTimeout = 5000; // 5 seconds
        int readTimeout = 10000; // 10 seconds

        while (retryCount < maxRetries) {
            long existingFileSize = outputFile.exists() ? outputFile.length() : 0;

            //TODO: We should also do "updateMessage" as a retry message using text like "Retrying... (1/3)" or something, if we have to retry.
            //If our file on disk is smaller than the remote, resume the download at the byte location we stopped.
            //We include this to have the retries function. If the outer condition from the setupTools is removed, this can cause issues with downloading steam CMD when already downloaded.
            if (existingFileSize >= remoteFileSize) {
                downloadResult.addMessage("SteamCMD already downloaded.", ResultType.SUCCESS);
                return downloadResult;
            }

            HttpURLConnection httpConnection = null;
            try {
                httpConnection = (HttpURLConnection) steamCmdUrl.openConnection();
                httpConnection.setConnectTimeout(connectTimeout);
                httpConnection.setReadTimeout(readTimeout);

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
                    long totalBytesDownloaded = existingFileSize;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        raf.write(buffer, 0, bytesRead);
                        totalBytesDownloaded += bytesRead;

                        //Get the largest common denominator for the file size and use that to create the update message
                        int divisor;
                        String divisorName;
                        if (remoteFileSize >= 1000000000) {  //Gigabyte
                            divisor = 1000000000;
                            divisorName = "GB";
                        } else if (remoteFileSize < 999999999 && remoteFileSize >= 1000000) { //Megabyte
                            divisor = 1000000;
                            divisorName = "MB";
                        } else { //Kilobyte
                            divisor = 1000;
                            divisorName = "KB";
                        }
                        messageUpdater.accept(String.format("%d%s/%d%s", totalBytesDownloaded / divisor, divisorName, remoteFileSize / divisor, divisorName));
                        progressUpdater.accept(totalBytesDownloaded, remoteFileSize);
                    }

                    downloadResult.addMessage("Successfully downloaded SteamCMD.", ResultType.SUCCESS);
                }
            } catch (SocketTimeoutException e) {
                messageUpdater.accept(String.format("Retrying... (%d/%d)", retryCount, maxRetries));
                retryCount++;
                if (retryCount < maxRetries)
                    downloadResult.addMessage("Download failed, retrying... Attempt " + retryCount, ResultType.WARN);
                else
                    downloadResult.addMessage(getStackTrace(e), ResultType.FAILED);
            } catch (IOException e) {
                downloadResult.addMessage(getStackTrace(e), ResultType.FAILED);
            } finally {
                if (httpConnection != null)
                    httpConnection.disconnect();
            }
        }
        return downloadResult;
    }
}
