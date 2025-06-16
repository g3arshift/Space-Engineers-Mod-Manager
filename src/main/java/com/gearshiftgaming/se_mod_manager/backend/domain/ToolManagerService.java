package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.concurrent.Task;
import lombok.Getter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.gearshiftgaming.se_mod_manager.backend.domain.utility.ZipUtility.extractZipArchive;
import static com.gearshiftgaming.se_mod_manager.backend.domain.utility.ZipUtility.isZip;
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

    private final UiService uiService;

    private final String steamCmdLocalPath;

    private final String steamCmdSourceLocation;

    private final int maxRetries;

    private final int connectionTimeout;

    private final int readTimeout;

    private final int retryDelay;

    @Getter
    private int divisor;

    @Getter
    private String divisorName;


    public ToolManagerService(UiService uiService, String steamCmdLocalPath, String steamCmdSourceLocation, int maxRetries, int connectionTimeout, int readTimeout, int retryDelay) {
        this.uiService = uiService;
        this.steamCmdLocalPath = steamCmdLocalPath;
        this.steamCmdSourceLocation = steamCmdSourceLocation;
        this.maxRetries = maxRetries;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.retryDelay = retryDelay;
    }

    //TODO: Need to call all this from UI service and the UI part from master manager
    public Task<Result<Void>> setupTools() {
        return new Task<>() {
            @Override
            protected Result<Void> call() throws Exception {
                //Make the base directories and file we need if they don't exist
                Path steamDownloadPath = Path.of(steamCmdLocalPath);
                if (Files.notExists(steamDownloadPath)) {
                    Files.createDirectories(steamDownloadPath.getParent());
                }

                if (Files.notExists(steamDownloadPath)) {
                    Files.createFile(steamDownloadPath);
                }

                Result<Void> toolSetupResult;
                //Check if we already have steam CMD downloaded. If it isn't, download it.
                uiService.log("Downloading required tools...", MessageType.INFO);
                if (Files.notExists(Path.of(steamCmdLocalPath + "/steamcmd.exe"))) {
                    uiService.log("Downloading Steam CMD...", MessageType.INFO);
                    URL steamDownloadUrl = new URI(steamCmdSourceLocation).toURL();
                    long remoteSteamCmdFileSize = getSteamCmdRemoteSize(steamDownloadUrl);
                    if (remoteSteamCmdFileSize == -1) {
                        toolSetupResult = new Result<>();
                        toolSetupResult.addMessage("Failed to get size of SteamCMD.", ResultType.FAILED);
                        return toolSetupResult;
                    }

                    getFileSizeDivisor(remoteSteamCmdFileSize);

                    toolSetupResult = downloadSteamCmd(steamDownloadUrl,
                            steamCmdLocalPath,
                            remoteSteamCmdFileSize,
                            divisor,
                            divisorName,
                            this::updateProgress,
                            this::updateMessage);

                    if (toolSetupResult.isSuccess())
                        uiService.log("Successfully downloaded Steam CMD.", MessageType.INFO);
                    else
                        uiService.log("Failed to download Steam CMD.", MessageType.ERROR);
                } else {
                    toolSetupResult = new Result<>();
                    toolSetupResult.addMessage("Steam CMD already installed. Skipping.", ResultType.SUCCESS);
                }

                if (!toolSetupResult.isSuccess())
                    return toolSetupResult;

                //Read the file signature of our downloaded file to make sure it's a .zip
                if (!isZip(new File(steamCmdLocalPath))) {
                    toolSetupResult.addMessage("Downloaded file is not a .zip file.", ResultType.FAILED);
                    return toolSetupResult;
                }

                extractZipArchive(steamDownloadPath, steamDownloadPath.getParent());

                //Add a final message to the download chain if everything succeeded
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
    private long getSteamCmdRemoteSize(URL steamCmdUrl) throws IOException {
        HttpURLConnection sizeCheckConnection = null;

        //Get the size of the remote file
        try {
            sizeCheckConnection = (HttpURLConnection) steamCmdUrl.openConnection();
            sizeCheckConnection.setRequestMethod("HEAD");
        } finally {
            //Make sure our connection is closed if it was opened.
            if (sizeCheckConnection != null)
                sizeCheckConnection.disconnect();
        }

        return sizeCheckConnection.getContentLengthLong();
    }

    /**
     * Download SteamCMD with a resumable HTTP Connection.
     * @param steamCmdUrl The URL at which steamCMD can be downloaded from.
     * @param downloadLocation The location we want to save the SteamCMD zip file to.
     * @param remoteFileSize The size of the SteamCMD.zip on the remote server
     * @param divisor The number we will divide the byte count for remote file size and downloaded byte count by when we create the update message for the task.
     * @param divisorName The string displayed next to the update message's divided byte count. EG: 768KB where the divisor name is KB.
     * @param progressUpdater Callback method for Task.updateProgress().
     * @param messageUpdater Callback method for Task.updateMessage().
     * @return Whether the download is finished or not.
     * @throws InterruptedException if any thread has interrupted the current thread. The interrupted status of the current thread is cleared when this exception is thrown.
     */
    private Result<Void> downloadSteamCmd(URL steamCmdUrl, String downloadLocation, long remoteFileSize, int divisor, String divisorName, BiConsumer<Long, Long> progressUpdater, Consumer<String> messageUpdater) throws InterruptedException, IOException {
        Result<Void> downloadResult = new Result<>();
        File outputFile = new File(downloadLocation);

        int retryCount = 0;
        while (retryCount <= maxRetries) {
            long existingFileSize = outputFile.exists() ? outputFile.length() : 0;

            //If our file on disk is smaller than the remote, resume the download at the byte location we stopped.
            //We include this to have the retries function. If the outer condition from the setupTools is removed, this can cause issues with downloading steam CMD when already downloaded.
            if (existingFileSize >= remoteFileSize) {
                downloadResult.addMessage("SteamCMD already downloaded.", ResultType.SUCCESS);
                return downloadResult;
            }

            HttpURLConnection httpConnection = null;
            try {
                httpConnection = (HttpURLConnection) steamCmdUrl.openConnection();
                httpConnection.setConnectTimeout(connectionTimeout);
                httpConnection.setReadTimeout(readTimeout);

                if (existingFileSize > 0) {
                    //Request the remaining data we're missing
                    httpConnection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
                }

                int responseCode = httpConnection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    String errorMessage = String.format("Received HTTP response code %d from server.", responseCode);
                    uiService.log(errorMessage, MessageType.ERROR);
                    throw new ToolDownloadFailedException(errorMessage);
                }
                boolean isResuming = (responseCode == HttpURLConnection.HTTP_PARTIAL);

                //Writes our file at the specified byte location so we can download it safely.
                try (InputStream inputStream = httpConnection.getInputStream(); RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
                    if (isResuming) {
                        raf.seek(existingFileSize);
                    }

                    //Download the file 8KB at a time.
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesDownloaded = existingFileSize;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        raf.write(buffer, 0, bytesRead);
                        totalBytesDownloaded += bytesRead;

                        //Update our task with the percentage progress done and the amount of bytes downloaded out of the whole, divided by the LCD.
                        messageUpdater.accept(String.format("%d%s/%d%s", totalBytesDownloaded / divisor, divisorName, remoteFileSize / divisor, divisorName));
                        progressUpdater.accept(totalBytesDownloaded, remoteFileSize);
                    }

                    if (totalBytesDownloaded == remoteFileSize) {
                        downloadResult.addMessage("Successfully downloaded SteamCMD.", ResultType.SUCCESS);
                        retryCount = 99;
                    } else
                        throw new ToolDownloadFailedException("Download of SteamCMD was interrupted mid download and was unable to complete.");
                }
            } catch (SocketTimeoutException | ToolDownloadFailedException e) {
                retryCount++;
                if (retryCount <= maxRetries) {
                    messageUpdater.accept(String.format("Retrying... (%d/%d)", retryCount, maxRetries));
                    downloadResult.addMessage("SteamCMD download failed, retrying... Attempt " + retryCount, ResultType.WARN);
                    //Wait the specified time of our retry delay before retrying the download.
                    Thread.sleep(retryDelay);
                } else {
                    Files.deleteIfExists(Path.of(steamCmdLocalPath));
                    downloadResult.addMessage(getStackTrace(e), ResultType.FAILED);
                }
            } catch (IOException e) {
                downloadResult.addMessage(getStackTrace(e), ResultType.FAILED);
            } finally {
                if (httpConnection != null)
                    httpConnection.disconnect();
            }
        }
        return downloadResult;
    }

    private void getFileSizeDivisor(long remoteFileSize) {
        //Get the largest common denominator for the file size and use that to create the update message
        if (remoteFileSize <= 999999) { //Kilobyte
            divisor = 1000;
            divisorName = "KB";
        } else if (remoteFileSize <= 999999999) { //Megabyte
            divisor = 1000000;
            divisorName = "MB";
        } else { //Gigabyte
            divisor = 1000000000;
            divisorName = "GB";
        }
    }
}
