package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.domain.archive.ArchiveTool;
import com.gearshiftgaming.se_mod_manager.backend.domain.archive.TarballArchiveTool;
import com.gearshiftgaming.se_mod_manager.backend.domain.archive.ZipArchiveTool;
import com.gearshiftgaming.se_mod_manager.backend.domain.tool.ToolManagerService;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.ResultType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import helper.JavaFXTestHelper;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
@WireMockTest
class ProgressDisplayServiceTest {

    private ToolManagerService toolManagerService;

    private String steamCmdZipPath;

    private String steamCmdTarPath;

    private String steamCmdWindowsSourceLocation;

    private String steamCmdLinuxSourceLocation;

    private int maxRetries;

    private int connectionTimeout;

    private int readTimeout;

    private int retryDelay;

    private List<String> messages;
    private List<Double> progress;
    private CountDownLatch doneLatch;

    private int fakeFileSize;
    private byte[] fakeSteamCmdZip;
    private String fakeSteamCmdResourcePath;

    private static byte[] createFakeZipFile(String entryInZipName, int uncompressedSize) throws IOException {
        byte[] fakeZip = new byte[uncompressedSize];
        new Random().nextBytes(fakeZip);

        //Write our zip file to a byte array
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(byteOut)) {
            ZipEntry entry = new ZipEntry(entryInZipName);
            // Calculate CRC32 for the entry
            CRC32 crc = new CRC32();
            crc.update(fakeZip);

            // Set the entry properties for a valid ZIP
            entry.setSize(fakeZip.length);
            entry.setCrc(crc.getValue());
            entry.setMethod(ZipEntry.DEFLATED);

            zos.putNextEntry(entry);
            zos.write(fakeZip);
            zos.closeEntry();
        }
        return byteOut.toByteArray();
    }

    /**
     * Returns true if the file has an .exe file signature (4D 5A for first two bytes)
     *
     * @param exeFile is the file we are checking
     * @return true if the file is an .exe, and false if it is not or does not exist
     * @throws IOException when we fail to read the file or our buffer of the read bytes is empty.
     */
    private static boolean isExe(File exeFile) throws IOException {
        if (!exeFile.exists())
            return false;

        byte[] buffer = new byte[2];
        boolean isExe = false;
        try (InputStream is = new FileInputStream(exeFile)) {
            //Exe signature is "4D 5A"
            if (is.read(buffer) == buffer.length) {
                isExe = buffer[0] == (byte) 0x4D && buffer[1] == (byte) 0x5A;
            }
        }
        return isExe;
    }

    /**
     * Returns true if the file starts with a shebang (23 21 for first two bytes)
     *
     * @param shellScriptFile is the file we are checking
     * @return true if the file starts with a shebang, and false if it is not or does not exist
     * @throws IOException when we fail to read the file or our buffer of the read bytes is empty.
     */
    private static boolean isShellScript(File shellScriptFile) throws IOException {
        if(!shellScriptFile.exists())
            return false;

        byte[] buffer = new byte[2];
        boolean isShellScript = false;
        try(InputStream is = new FileInputStream(shellScriptFile)) {
            //Shebang signature is 23 21
            if(is.read(buffer) == buffer.length) {
                isShellScript = buffer[0] == (byte) 0x23 && buffer[1] == (byte) 0x21;
            }
        }
        return isShellScript;
    }

    private Result<Void> runDownload(WireMockRuntimeInfo wireMockRuntimeInfo, ArchiveTool archiveTool) throws InterruptedException, ExecutionException {
        steamCmdWindowsSourceLocation = wireMockRuntimeInfo.getHttpBaseUrl() + fakeSteamCmdResourcePath;

        toolManagerService = new ToolManagerService(mock(UiService.class),
                steamCmdZipPath,
                steamCmdWindowsSourceLocation,
                maxRetries,
                connectionTimeout,
                readTimeout,
                retryDelay, archiveTool);
        Task<Result<Void>> setupTask = toolManagerService.setupSteamCmd();

        //Add the listeners so our lists get updated properly when the task updates
        setupTask.messageProperty().addListener((obs, oldVal, newVal) -> messages.add(newVal));
        setupTask.progressProperty().addListener((obs, oldVal, newVal) -> progress.add(newVal.doubleValue()));
        setupTask.setOnSucceeded(e -> doneLatch.countDown());
        setupTask.setOnFailed(e -> doneLatch.countDown());

        //Run the task
        Thread taskThread = Thread.ofVirtual().unstarted(setupTask);
        taskThread.start();

        //Pause the test until our task is done, but give it a timeout.
        assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "Task did not complete in time");
        return setupTask.get();
    }

    @BeforeAll
    static void initJfx() {
        JavaFXTestHelper.initJavaFx();
    }

    @BeforeEach
    void setup() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM_ToolManagerTest.properties")) {
            properties.load(input);
        }

        steamCmdZipPath = properties.getProperty("semm.steam.cmd.windows.localFolderPath");
        steamCmdTarPath = properties.getProperty("semm.steam.cmd.linux.localFolderPath");
        steamCmdWindowsSourceLocation = properties.getProperty("semm.steam.cmd.windows.download.source");
        steamCmdLinuxSourceLocation = properties.getProperty("semm.steam.cmd.linux.download.source");
        maxRetries = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.retry.limit"));
        connectionTimeout = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.connection.timeout"));
        readTimeout = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.read.timeout"));
        retryDelay = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.retry.delay"));

        messages = new CopyOnWriteArrayList<>();
        progress = new CopyOnWriteArrayList<>();
        doneLatch = new CountDownLatch(1);

        //Setup
        fakeFileSize = 1024 * 768;
        fakeSteamCmdZip = createFakeZipFile("steamcmd.exe", fakeFileSize); //This is about the real size of the actual steamcmd.zip
        fakeSteamCmdResourcePath = "/steamcmd.zip";
    }

    @AfterEach
    void cleanup() throws IOException {
        //Delete the download so we can test fresh
        String testDataToDeletePath = "./Tools/Test";
        if (Files.exists(Path.of(testDataToDeletePath)))
            FileUtils.deleteDirectory(new File(testDataToDeletePath));
    }


    /**
     * Tests that the SteamCMD ZIP file can be successfully downloaded and extracted
     * using the {@link ToolManagerService}, even when multiple download attempts fail before succeeding.
     * <p>This test simulates the following behavior using WireMock:</p>
     * <ol>
     *   <li>Initial HEAD request returns the correct Content-Length and headers.</li>
     *   <li>The first GET request returns a partial ZIP file (simulating a truncated response).</li>
     *   <li>Two subsequent retry attempts to resume the download fail with HTTP 500 errors.</li>
     *   <li>The final retry succeeds with a 206 Partial Content response containing the remainder of the file.</li>
     * </ol>
     * <p>The test then verifies that:</p>
     * <ul>
     *   <li>The overall tool setup task completes successfully.</li>
     *   <li>The result is of type {@code SUCCESS} and contains the correct final message.</li>
     *   <li>Progress and message updates are tracked and reach expected final values.</li>
     *   <li>The ZIP file is saved to disk with the correct size.</li>
     *   <li>The ZIP file is extracted, and the expected file (steamcmd.exe) exists with the correct size.</li>
     * </ul>
     *
     * @param wireMockRuntimeInfo Injected WireMock runtime information used to configure the mock HTTP server.
     * @throws IOException          if an I/O error occurs during the test
     * @throws InterruptedException if the test thread is interrupted while waiting for the setup task to complete
     * @throws ExecutionException   if the setup task throws an exception during execution
     */
    @Test
    void shouldDownloadFakeSteamCmdAfterRetrying(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException, InterruptedException, ExecutionException {
        // HEAD request always returns Content-Length
        wireMockRuntimeInfo.getWireMock().register(
                request("HEAD", urlEqualTo(fakeSteamCmdResourcePath))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Length", String.valueOf(fakeSteamCmdZip.length))
                                .withHeader("Accept-Ranges", "bytes")
                                .withHeader("Content-Type", "application/zip"))
        );

        //Initial download request that fails halfway through.
        int cutoff = 375000;
        byte[] partialData = Arrays.copyOfRange(fakeSteamCmdZip, 0, cutoff);
        wireMockRuntimeInfo.getWireMock().register(
                get(urlEqualTo(fakeSteamCmdResourcePath))
                        .inScenario("RetryDownload")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/zip")
                                .withBody(partialData))  // first part only
                        .willSetStateTo("PartialFailed")
        );

        //Simulate a failure when we try to restart the download
        wireMockRuntimeInfo.getWireMock().register(
                get(urlEqualTo(fakeSteamCmdResourcePath))
                        .inScenario("RetryDownload")
                        .whenScenarioStateIs("PartialFailed")
                        .withHeader("Range", matching("bytes=\\d+-"))
                        .willReturn(aResponse()
                                .withStatus(500))
                        .willSetStateTo("RetryFailedOnce")
        );

        //Simulates second failure when we try to restart the download
        wireMockRuntimeInfo.getWireMock().register(
                get(urlEqualTo(fakeSteamCmdResourcePath))
                        .inScenario("RetryDownload")
                        .whenScenarioStateIs("RetryFailedOnce")
                        .withHeader("Range", matching("bytes=\\d+-"))
                        .willReturn(aResponse()
                                .withStatus(500))
                        .willSetStateTo("FinalAttempt")
        );

        //Final successful retry - resume with partial content
        byte[] remainingData = Arrays.copyOfRange(fakeSteamCmdZip, cutoff, fakeSteamCmdZip.length);
        wireMockRuntimeInfo.getWireMock().register(
                get(urlEqualTo(fakeSteamCmdResourcePath))
                        .inScenario("RetryDownload")
                        .whenScenarioStateIs("FinalAttempt")
                        .withHeader("Range", matching("bytes=\\d+-"))
                        .willReturn(aResponse()
                                .withStatus(206)
                                .withHeader("Content-Type", "application/zip")
                                .withBody(remainingData))  // rest of the file
        );

        Result<Void> result = runDownload(wireMockRuntimeInfo, new ZipArchiveTool());

        //Check we get both the expected number and type of messages from our result
        assertTrue(result.isSuccess(), "Download result should be a success");
        assertEquals("Successfully downloaded SteamCMD", result.getCurrentMessage());
        assertEquals(ResultType.SUCCESS, result.getType(), "Result is the wrong type, should be SUCCESS.");
        assertEquals(4, result.getMessages().size(), "Result messages were:\n" + String.join("\n", result.getMessages()));

        //Check that we tried the correct number of times to redownload steamCMD
        assertEquals(maxRetries + 1, result.getMessages().size());
        for (int i = 0; i < maxRetries; i++) {
            assertEquals(String.format("SteamCMD download failed, retrying... Attempt %d", i + 1), result.getMessages().get(i));
        }

        //Check we both don't have an empty list of messages and that it's giving us the last expected final message
        assertFalse(messages.isEmpty(), "Should have updated messages");
        assertFalse(progress.isEmpty());
        assertEquals(String.format("%sKB/%sKB", fakeSteamCmdZip.length / 1024, fakeSteamCmdZip.length / 1024), messages.get(messages.size() - 2), "Update messages were:\n" + String.join("\n", messages));
        assertEquals(1.0, progress.getLast(), "Progress messages were:\n" + progress.stream().map(p -> String.format("%.2f", p)).collect(Collectors.joining("\n- ")));

        //Verify the file saved properly.
        assertEquals(fakeSteamCmdZip.length, new File(steamCmdZipPath).length());

        //Verify our zip file extracted properly
        Path extractedZipPath = Path.of(Path.of(steamCmdZipPath).getParent() + "/steamcmd.exe");
        assertTrue(Files.exists(extractedZipPath));
        assertEquals(fakeFileSize, Files.size(extractedZipPath));
    }

    /**
     * Verifies that the SteamCMD setup process completes successfully using {@link ToolManagerService#setupSteamCmd()}.
     * <p>
     * This test simulates a full download-and-extract operation and ensures:
     * <ul>
     *   <li>The SteamCMD ZIP is downloaded if not already present</li>
     *   <li>Progress and message updates occur correctly during the download</li>
     *   <li>The final {@link Result} indicates success with the correct messages</li>
     *   <li>The ZIP file is extracted correctly, and key files (e.g., steamcmd.exe) exist</li>
     * </ul>
     * <p>
     * The download runs asynchronously on a virtual thread. The test waits up to 60 seconds for completion and uses a latch to detect when the task finishes.
     * Assertions are made on:
     * <ul>
     *   <li>Download result status and message contents</li>
     *   <li>Progress and message update lists</li>
     *   <li>File system checks for the downloaded ZIP and extracted contents</li>
     * </ul>
     * <p>
     *
     * @throws InterruptedException if the current thread is interrupted while waiting for the task to finish
     * @throws ExecutionException   if the task throws an exception during execution
     * @throws IOException          if an I/O error occurs while verifying downloaded or extracted files
     */
    @Test
    void shouldDownloadSteamCmd() throws InterruptedException, ExecutionException, IOException {
        toolManagerService = new ToolManagerService(mock(UiService.class), steamCmdZipPath, steamCmdWindowsSourceLocation, maxRetries, connectionTimeout, readTimeout, retryDelay, new ZipArchiveTool());
        Task<Result<Void>> setupTask = toolManagerService.setupSteamCmd();

        //Add the listeners so our lists get updated properly when the task updates
        setupTask.messageProperty().addListener((obs, oldVal, newVal) -> messages.add(newVal));
        setupTask.progressProperty().addListener((obs, oldVal, newVal) -> progress.add(newVal.doubleValue()));
        setupTask.setOnSucceeded(e -> doneLatch.countDown());
        setupTask.setOnFailed(e -> doneLatch.countDown());

        //Run the task
        Thread taskThread = Thread.ofVirtual().unstarted(setupTask);
        taskThread.setDaemon(true);
        taskThread.start();

        //Pause the test until our task is done, but give it a timeout.
        assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "Task did not complete in time");
        Result<Void> result = setupTask.get();

        //Check we get both the expected number and type of messages from our result
        assertTrue(result.isSuccess(), "Download result should be a success");
        assertEquals("Successfully downloaded SteamCMD", result.getCurrentMessage());
        assertEquals(ResultType.SUCCESS, result.getType(), "Result is the wrong type, should be SUCCESS.");
        assertEquals(1, result.getMessages().size(), "Result messages were:\n" + String.join("\n", result.getMessages()));

        //Check we both don't have an empty list of messages and that it's giving us the last expected final message
        assertFalse(messages.isEmpty(), "Should have updated messages");
        assertFalse(progress.isEmpty());
        File steamCmdDownload = new File(Path.of(steamCmdZipPath).toString());
        //Verify our messages are the correct value for the length.
        assertEquals(String.format("%d%s/%d%s", steamCmdDownload.length() / toolManagerService.getDivisor(),
                toolManagerService.getDivisorName(),
                steamCmdDownload.length() / toolManagerService.getDivisor(),
                toolManagerService.getDivisorName()), messages.get(messages.size() - 2), "Update messages were:\n" + String.join("\n", messages));
        assertEquals(1.0, progress.getLast(), "Progress messages were:\n" + progress.stream().map(p -> String.format("%.2f", p)).collect(Collectors.joining("\n- ")));


        //Verify the file downloaded properly.
        assertTrue(steamCmdDownload.exists());
        assertEquals(steamCmdDownload.length(), new File(steamCmdZipPath).length());

        //Verify our zip file extracted properly
        Path extractedZipPath = Path.of(Path.of(steamCmdZipPath).getParent() + "/steamcmd.exe");
        assertTrue(Files.exists(extractedZipPath));
        assertTrue(isExe(new File(extractedZipPath.toString())));
    }

    /**
     * Tests the {@code ToolManagerService}'s behavior when a partial download of SteamCMD fails and all retry attempts are unsuccessful.
     * <p>
     * This test simulates a scenario using WireMock where:
     * <ul>
     *   <li>A valid {@code HEAD} request returns expected headers including {@code Content-Length}</li>
     *   <li>The initial {@code GET} request begins successfully but returns only part of the file</li>
     *   <li>All subsequent ranged {@code GET} requests to resume the download fail with HTTP 500</li>
     * </ul>
     * <p>
     * Assertions verify that:
     * <ul>
     *   <li>The result is a failure, with a message indicating the HTTP 500 error</li>
     *   <li>The retry logic performs the correct number of attempts</li>
     *   <li>User-facing messages reflect each retry attempt and the final failure</li>
     *   <li>Progress updates were made and include a partial progress value (e.g., 0.48)</li>
     *   <li>The messages and progress lists are populated as expected</li>
     * </ul>
     *
     * @param wireMockRuntimeInfo injected by the test framework to provide runtime access to the WireMock server
     * @throws ExecutionException   if an exception occurs during asynchronous task execution
     * @throws InterruptedException if the thread waiting for the result is interrupted
     * @throws IOException          if an I/O error occurs during partial file creation or verification
     */
    @Test
    void shouldStartButThenFailToDownloadFakeSteamCmd(WireMockRuntimeInfo wireMockRuntimeInfo) throws ExecutionException, InterruptedException, IOException {
        // HEAD request always returns Content-Length
        wireMockRuntimeInfo.getWireMock().register(
                request("HEAD", urlEqualTo(fakeSteamCmdResourcePath))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Length", String.valueOf(fakeSteamCmdZip.length))
                                .withHeader("Accept-Ranges", "bytes")
                                .withHeader("Content-Type", "application/zip"))
        );

        //Initial download request that fails halfway through.
        int cutoff = 375000;
        byte[] partialData = Arrays.copyOfRange(fakeSteamCmdZip, 0, cutoff);
        wireMockRuntimeInfo.getWireMock().register(
                get(urlEqualTo(fakeSteamCmdResourcePath))
                        .inScenario("RetryDownload")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/zip")
                                .withBody(partialData))  // first part only
                        .willSetStateTo("FullFailed")
        );

        wireMockRuntimeInfo.getWireMock().register(
                get(urlEqualTo(fakeSteamCmdResourcePath))
                        .inScenario("RetryDownload")
                        .whenScenarioStateIs("FullFailed")
                        .withHeader("Range", matching("bytes=\\d+-"))
                        .willReturn(aResponse()
                                .withStatus(500))
        );

        Result<Void> result = runDownload(wireMockRuntimeInfo, new ZipArchiveTool());

        //Check that we failed the way we expected
        assertFalse(result.isSuccess(), "Download result should be a failure.");
        assertEquals("com.gearshiftgaming.se_mod_manager.backend.domain.tool.ToolDownloadFailedException: Received HTTP response code 500 from server.", result.getCurrentMessage().split("\n")[0].trim());

        //Check that we tried the correct number of times to redownload steamCMD
        assertEquals(maxRetries + 1, result.getMessages().size());
        for (int i = 0; i < maxRetries; i++) {
            assertEquals(String.format("SteamCMD download failed, retrying... Attempt %d", i + 1), result.getMessages().get(i));
        }

        //Check we both don't have an empty list of messages and that it's giving us the last expected final message
        assertFalse(messages.isEmpty(), "Should have updated messages");
        assertFalse(progress.isEmpty());
        assertEquals("SteamCMD download failed!", messages.getLast());
        assertEquals("0.48", String.format("%.2f", progress.getLast()));
    }

    /**
     * Tests the behavior of {@code ToolManagerService} when the connection repeatedly fails with HTTP 500 errors, simulating a scenario where the server is consistently unavailable.
     * <p>
     * This test uses WireMock to simulate:
     * <ul>
     *   <li>A successful {@code HEAD} request providing expected file metadata</li>
     *   <li>Repeated {@code GET} requests that always fail with HTTP 500</li>
     * </ul>
     * <p>
     * Verifies that:
     * <ul>
     *   <li>The download fails with a {@code ToolDownloadFailedException} due to server error</li>
     *   <li>No partial or incomplete file is left on disk</li>
     *   <li>The retry mechanism attempts the download the expected number of times ({@code maxRetries})</li>
     *   <li>The user-facing retry messages match the expected format for each attempt</li>
     * </ul>
     *
     * @param wireMockRuntimeInfo runtime info for WireMock, injected by the test framework
     * @throws InterruptedException if the thread waiting for task completion is interrupted
     * @throws ExecutionException   if an exception occurs during asynchronous task execution
     */
    @Test
    void shouldTimeOutConnection(WireMockRuntimeInfo wireMockRuntimeInfo) throws InterruptedException, ExecutionException {

        // HEAD request always returns Content-Length
        wireMockRuntimeInfo.getWireMock().register(
                request("HEAD", urlEqualTo(fakeSteamCmdResourcePath))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Length", String.valueOf(fakeSteamCmdZip.length))
                                .withHeader("Accept-Ranges", "bytes")
                                .withHeader("Content-Type", "application/zip"))
        );

        wireMockRuntimeInfo.getWireMock().register(
                get(urlEqualTo(fakeSteamCmdResourcePath))
                        .willReturn(aResponse()
                                .withStatus(500))
        );

        Result<Void> result = runDownload(wireMockRuntimeInfo, new ZipArchiveTool());

        //Check it failed how we expected
        assertFalse(result.isSuccess());
        assertTrue(Files.notExists(Path.of(steamCmdZipPath)));
        assertEquals("com.gearshiftgaming.se_mod_manager.backend.domain.tool.ToolDownloadFailedException: Received HTTP response code 500 from server.", result.getCurrentMessage().split("\n")[0].trim());

        //Check that we tried the correct number of times to redownload steamCMD
        assertEquals(maxRetries + 1, result.getMessages().size());
        for (int i = 0; i < maxRetries; i++) {
            assertEquals(String.format("SteamCMD download failed, retrying... Attempt %d", i + 1), result.getMessages().get(i));
        }
    }

    /**
     * Tests the behavior of {@code ToolManagerService} when the initial {@code HEAD} request to determine the size of the SteamCMD file fails with an HTTP 500 error.
     * <p>
     * This simulates a server-side failure during the metadata fetch phase,
     * before the download even begins.
     * <p>
     * Verifies that:
     * <ul>
     *   <li>The service correctly interprets the failure and does not proceed with the download</li>
     *   <li>The result indicates failure and provides the expected user-facing error message</li>
     * </ul>
     * <p>
     *
     * @param wireMockRuntimeInfo runtime info for WireMock, injected by the test framework
     * @throws InterruptedException if the thread waiting for the result is interrupted
     * @throws ExecutionException   if an exception occurs during asynchronous task execution
     */
    @Test
    void shouldFailToGetSizeOfFakeSteamCmd(WireMockRuntimeInfo wireMockRuntimeInfo) throws InterruptedException, ExecutionException {
        //HEAD request fails to get size of file
        wireMockRuntimeInfo.getWireMock().register(
                request("HEAD", urlEqualTo(fakeSteamCmdResourcePath))
                        .willReturn(aResponse()
                                .withStatus(500))
        );

        Result<Void> result = runDownload(wireMockRuntimeInfo, new ZipArchiveTool());

        //Check we failed the way we expected
        assertFalse(result.isSuccess());
        assertEquals("Failed to get size of SteamCMD.", result.getCurrentMessage());
    }

    @Test
    void downloadedFileShouldNotBeZip(WireMockRuntimeInfo wireMockRuntimeInfo) throws ExecutionException, InterruptedException {
        //Just give it some random bytes. We just need to trigger not zip check
        byte[] notZipFile = new byte[fakeFileSize];
        new Random().nextBytes(notZipFile);

        // HEAD request always returns Content-Length
        wireMockRuntimeInfo.getWireMock().register(
                request("HEAD", urlEqualTo(fakeSteamCmdResourcePath))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Length", String.valueOf(notZipFile.length))
                                .withHeader("Accept-Ranges", "bytes")
                                .withHeader("Content-Type", "application/zip"))
        );

        wireMockRuntimeInfo.getWireMock().register(
                get(urlEqualTo(fakeSteamCmdResourcePath))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/zip")
                                .withHeader("Content-Length", String.valueOf(fakeFileSize))
                                .withBody(notZipFile))
        );

        Result<Void> result = runDownload(wireMockRuntimeInfo, new ZipArchiveTool());

        assertFalse(result.isSuccess());
        assertEquals("Downloaded SteamCMD archive is not in the correct file format. (.zip)", result.getCurrentMessage());

        //Check we both don't have an empty list of messages and that it's giving us the last expected final message
        assertFalse(messages.isEmpty(), "Should have updated messages");
        assertFalse(progress.isEmpty());
        assertEquals(String.format("%sKB/%sKB", fakeSteamCmdZip.length / 1024, fakeSteamCmdZip.length / 1024), messages.get(messages.size() - 2), "Update messages were:\n" + String.join("\n", messages));
        assertEquals(1.0, progress.getLast(), "Progress messages were:\n" + progress.stream().map(p -> String.format("%.2f", p)).collect(Collectors.joining("\n- ")));

        assertTrue(Files.exists(Path.of(steamCmdZipPath)));
    }

    @Test
    void downloadFileWithoutHeaderRequest(WireMockRuntimeInfo wireMockRuntimeInfo) throws ExecutionException, InterruptedException, IOException {
        // HEAD request always returns Content-Length
        wireMockRuntimeInfo.getWireMock().register(
                request("HEAD", urlEqualTo(fakeSteamCmdResourcePath))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Length", String.valueOf(fakeSteamCmdZip.length))
                                .withHeader("Accept-Ranges", "bytes")
                                .withHeader("Content-Type", "application/zip"))
        );

        wireMockRuntimeInfo.getWireMock().register(
                get(urlEqualTo(fakeSteamCmdResourcePath))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/zip")
                                .withBody(fakeSteamCmdZip))
        );

        Result<Void> result = runDownload(wireMockRuntimeInfo, new ZipArchiveTool());

        //Check we get both the expected number and type of messages from our result
        assertTrue(result.isSuccess(), "Download result should be a success");
        assertEquals("Successfully downloaded SteamCMD", result.getCurrentMessage());
        assertEquals(ResultType.SUCCESS, result.getType(), "Result is the wrong type, should be SUCCESS.");
        assertEquals(1, result.getMessages().size(), "Result messages were:\n" + String.join("\n", result.getMessages()));

        //Check we both don't have an empty list of messages and that it's giving us the last expected final message
        assertFalse(messages.isEmpty(), "Should have updated messages");
        assertFalse(progress.isEmpty(), "Should have progress messages");
        assertEquals(String.format("%sKB/%sKB", fakeSteamCmdZip.length / 1024, fakeSteamCmdZip.length / 1024), messages.get(messages.size() - 2), "Update messages were:\n" + String.join("\n", messages));
        assertEquals(1.0, progress.getLast(), "Progress messages were:\n" + progress.stream().map(p -> String.format("%.2f", p)).collect(Collectors.joining("\n- ")));

        //Verify the file saved properly.
        assertEquals(fakeSteamCmdZip.length, new File(steamCmdZipPath).length());

        //Verify our zip file extracted properly
        Path extractedZipPath = Path.of(Path.of(steamCmdZipPath).getParent() + "/steamcmd.exe");
        assertTrue(Files.exists(extractedZipPath));
        assertEquals(fakeFileSize, Files.size(extractedZipPath));
    }

    @Test
    void steamCmdShouldAlreadyBeDownloaded(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException, ExecutionException, InterruptedException {
        String fakeExePath = String.valueOf(Path.of(steamCmdZipPath).getParent().resolve("steamcmd.exe"));
        Files.createDirectories(Path.of(steamCmdZipPath).getParent());

        byte[] fakeExe = new byte[fakeFileSize];
        new Random().nextBytes(fakeExe);

        try (FileOutputStream out = new FileOutputStream(fakeExePath)) {
            out.write(fakeExe);
        }

        // HEAD request always returns Content-Length
        wireMockRuntimeInfo.getWireMock().register(
                request("HEAD", urlEqualTo(fakeSteamCmdResourcePath))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Length", String.valueOf(fakeSteamCmdZip.length))
                                .withHeader("Accept-Ranges", "bytes")
                                .withHeader("Content-Type", "application/zip"))
        );

        Result<Void> result = runDownload(wireMockRuntimeInfo, new ZipArchiveTool());

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMessages().size());
        assertEquals("Steam CMD already installed. Skipping.", result.getMessages().getFirst());
    }

    @Test
    void downloadsShellVersionOfSteamCmdIfOnLinux() throws ExecutionException, InterruptedException, IOException {
        toolManagerService = new ToolManagerService(mock(UiService.class),
                steamCmdTarPath,
                steamCmdLinuxSourceLocation,
                maxRetries,
                connectionTimeout,
                readTimeout,
                retryDelay,
                new TarballArchiveTool());

        Task<Result<Void>> setupTask = toolManagerService.setupSteamCmd();

        //Add the listeners so our lists get updated properly when the task updates
        setupTask.messageProperty().addListener((obs, oldVal, newVal) -> messages.add(newVal));
        setupTask.progressProperty().addListener((obs, oldVal, newVal) -> progress.add(newVal.doubleValue()));
        setupTask.setOnSucceeded(e -> doneLatch.countDown());
        setupTask.setOnFailed(e -> doneLatch.countDown());

        //Run the task
        Thread taskThread = Thread.ofVirtual().unstarted(setupTask);
        taskThread.setDaemon(true);
        taskThread.start();

        //Pause the test until our task is done, but give it a timeout.
        assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "Task did not complete in time");
        Result<Void> result = setupTask.get();

        //Check we get both the expected number and type of messages from our result
        assertTrue(result.isSuccess(), "Download result should be a success");
        assertEquals("Successfully downloaded SteamCMD", result.getCurrentMessage());
        assertEquals(ResultType.SUCCESS, result.getType(), "Result is the wrong type, should be SUCCESS.");
        assertEquals(1, result.getMessages().size(), "Result messages were:\n" + String.join("\n", result.getMessages()));

        //Check we both don't have an empty list of messages and that it's giving us the last expected final message
        assertFalse(messages.isEmpty(), "Should have updated messages");
        assertFalse(progress.isEmpty());
        File steamCmdDownload = new File(Path.of(steamCmdTarPath).toString());
        //Verify our messages are the correct value for the length.
        assertEquals(String.format("%d%s/%d%s", steamCmdDownload.length() / toolManagerService.getDivisor(),
                toolManagerService.getDivisorName(),
                steamCmdDownload.length() / toolManagerService.getDivisor(),
                toolManagerService.getDivisorName()), messages.get(messages.size() - 2), "Update messages were:\n" + String.join("\n", messages));
        assertEquals(1.0, progress.getLast(), "Progress messages were:\n" + progress.stream().map(p -> String.format("%.2f", p)).collect(Collectors.joining("\n- ")));

        //Verify the file downloaded properly.
        assertTrue(steamCmdDownload.exists());
        assertEquals(steamCmdDownload.length(), new File(steamCmdZipPath).length());

        //Verify our tar file extracted properly
        Path extractedTarPath = Path.of(Path.of(steamCmdTarPath).getParent() + "/steamcmd.exe");
        assertTrue(Files.exists(extractedTarPath));
        assertTrue(isShellScript(new File(extractedTarPath.toString())));
    }
}
