package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.concurrent.Task;
import lombok.Getter;

import java.io.*;
import java.net.*;
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

    private final String steamCmdZipPath;

    private final String steamCmdSourceLocation;

    private final int maxRetries;

    private final int connectionTimeout;

    private final int readTimeout;

    private final int retryDelay;

    @Getter
    private int divisor;

    @Getter
    private String divisorName;


    public ToolManagerService(UiService uiService, String steamCmdZipPath, String steamCmdSourceLocation, int maxRetries, int connectionTimeout, int readTimeout, int retryDelay) {
        this.uiService = uiService;
        this.steamCmdZipPath = steamCmdZipPath;
        this.steamCmdSourceLocation = steamCmdSourceLocation;
        this.maxRetries = maxRetries;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.retryDelay = retryDelay;
    }

    //TODO: Add support for chunked downloads
    public Task<Result<Void>> setupSteamCmd() {
        return new Task<>() {
            @Override
            protected Result<Void> call() throws Exception {
                //Download and extract SteamCMD.
                return downloadSteamCmd(this::updateProgress, this::updateMessage);
            }
        };
    }

    /**
     * Gets the size of a file on a remote server by using HEAD requests.
     *
     * @param remoteFileUrl The location we will download our file from.
     * @return The size of the file on the remote server.
     */
    private long getRemoteFileSize(URL remoteFileUrl) throws IOException {
        HttpURLConnection sizeCheckConnection = null;

        //Get the size of the remote file
        try {
            sizeCheckConnection = (HttpURLConnection) remoteFileUrl.openConnection();
            sizeCheckConnection.setRequestMethod("HEAD");
        } finally {
            //Make sure our connection is closed if it was opened.
            if (sizeCheckConnection != null)
                sizeCheckConnection.disconnect();
        }

        return sizeCheckConnection.getContentLengthLong();
    }

    private Result<Void> downloadSteamCmd(BiConsumer<Long, Long> progressUpdater, Consumer<String> messageUpdater) throws IOException, InterruptedException, URISyntaxException {
        StringBuilder downloadMessage = new StringBuilder();
        Result<Void> steamCmdSetupResult;

        //Make the base directories and file we need if they don't exist
        Path steamDownloadPath = Path.of(steamCmdZipPath);
        if (Files.notExists(steamDownloadPath))
            Files.createDirectories(steamDownloadPath.getParent());

        if (Files.notExists(steamDownloadPath))
            Files.createFile(steamDownloadPath);

        //Check if we already have steam CMD downloaded. If it isn't, download it.
        if (Files.exists(steamDownloadPath.getParent().resolve("steamcmd.exe"))) {
            steamCmdSetupResult = new Result<>();
            steamCmdSetupResult.addMessage("Steam CMD already installed. Skipping.", ResultType.SUCCESS);
            return steamCmdSetupResult;
        }

        uiService.log("Downloading Steam CMD...", MessageType.INFO);
        URL steamDownloadUrl = new URI(steamCmdSourceLocation).toURL();

        //Get the size of the steamcmd.zip we're downloading
        long remoteSteamCmdFileSize = getRemoteFileSize(steamDownloadUrl);
        if (remoteSteamCmdFileSize == -1) {
            steamCmdSetupResult = new Result<>();
            setNewStringBuilderMessage(downloadMessage, "Failed to get size of SteamCMD.");
            steamCmdSetupResult.addMessage(downloadMessage.toString(), ResultType.FAILED);
            return steamCmdSetupResult;
        }

        //Get the divisor and divisor name we're using based on the remote file size
        getFileSizeDivisor(remoteSteamCmdFileSize);

        setNewStringBuilderMessage(downloadMessage, "Downloading SteamCMD...");
        messageUpdater.accept(downloadMessage.toString());

        //Download SteamCMD
        steamCmdSetupResult = downloadFileWithResumeAndRetries(steamDownloadUrl,
                steamCmdZipPath,
                "SteamCMD",
                remoteSteamCmdFileSize,
                progressUpdater,
                messageUpdater);

        if (steamCmdSetupResult.isFailure()) {
            setNewStringBuilderMessage(downloadMessage, "Failed to download Steam CMD.");
            uiService.log(downloadMessage.toString(), MessageType.ERROR);
            messageUpdater.accept(downloadMessage.toString());
            return steamCmdSetupResult;
        }

        setNewStringBuilderMessage(downloadMessage, "Successfully downloaded Steam CMD.");
        uiService.log(downloadMessage.toString(), MessageType.INFO);
        messageUpdater.accept(downloadMessage.toString());

        //Check that we've actually downloaded a .zip file by checking the first four bytes.
        if (!isZip(new File(steamCmdZipPath))) {
            steamCmdSetupResult.addMessage("Downloaded SteamCMD file is not a .zip file.", ResultType.FAILED);
            return steamCmdSetupResult;
        }

        int extractedFileCount = extractZipArchive(steamDownloadPath, steamDownloadPath.getParent());

        if (extractedFileCount == 0)
            steamCmdSetupResult.addMessage("Failed to extract steamcmd.exe from .zip.", ResultType.FAILED);

        return steamCmdSetupResult;
    }

    /**
     * Download a file with a resumable HTTP Connection and retry the connection after a set time if it is interrupted.
     *
     * @param steamCmdUrl      The URL at which the remote file can be downloaded from.
     * @param downloadLocation The location we want to save the file to.
     * @param toolName         The name of the tool we're downloading.
     * @param remoteFileSize   The size of the file on the remote server
     * @param progressUpdater  Callback method for Task.updateProgress().
     * @param messageUpdater   Callback method for Task.updateMessage().
     * @return Whether the download is finished or not.
     * @throws InterruptedException if any thread has interrupted the current thread. The interrupted status of the current thread is cleared when this exception is thrown.
     */
    private Result<Void> downloadFileWithResumeAndRetries(URL steamCmdUrl, String downloadLocation, String toolName, long remoteFileSize, BiConsumer<Long, Long> progressUpdater, Consumer<String> messageUpdater) throws InterruptedException, IOException {
        Result<Void> downloadResult = new Result<>();
        File outputFile = new File(downloadLocation);

        int retryCount = 0;
        while (retryCount <= maxRetries) {
            long existingFileSize = outputFile.exists() ? outputFile.length() : 0;

            //If our file on disk is smaller than the remote, resume the download at the byte location we stopped.
            //We include this to have the retries function. If the outer condition from the setupTools is removed, this can cause issues with downloading steam CMD when already downloaded.
            if (existingFileSize >= remoteFileSize) {
                downloadResult.addMessage(String.format("%s is already downloaded.", toolName), ResultType.SUCCESS);
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
                        downloadResult.addMessage(String.format("Successfully downloaded %s", toolName), ResultType.SUCCESS);
                        retryCount = 99;
                    } else
                        throw new ToolDownloadFailedException(String.format("Download of %s was interrupted mid download and was unable to complete.", toolName));
                }
            } catch (SocketTimeoutException | ToolDownloadFailedException e) {
                retryCount++;
                if (retryCount <= maxRetries) {
                    messageUpdater.accept(String.format("Retrying... (%d/%d)", retryCount, maxRetries));
                    downloadResult.addMessage(String.format("%s download failed, retrying... Attempt %d", toolName, retryCount), ResultType.WARN);
                    //Wait the specified time of our retry delay before retrying the download.
                    Thread.sleep(retryDelay);
                } else {
                    Files.deleteIfExists(Path.of(steamCmdZipPath));
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
        if (remoteFileSize <= 999999) { //Kibibyte
            divisor = 1024;
            divisorName = "KB";
        } else if (remoteFileSize <= 999999999) { //Mebibyte
            divisor = 1048576;
            divisorName = "MB";
        } else { //Gibibyte
            divisor = 1073741824;
            divisorName = "GB";
        }
    }

    private static void setNewStringBuilderMessage(StringBuilder downloadMessage, String message) {
        downloadMessage.setLength(0);
        downloadMessage.append(message);
    }
}
