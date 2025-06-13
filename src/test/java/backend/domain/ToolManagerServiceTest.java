package backend.domain;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */

import com.gearshiftgaming.se_mod_manager.backend.domain.ToolManagerService;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
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
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WireMockTest
class ToolManagerServiceTest {

    private ToolManagerService toolManagerService;

    private String steamCmdLocalPath;

    private String steamCmdSourceLocation;

    private int maxRetries;

    private int connectionTimeout;

    private int readTimeout;

    private int retryDelay;

    @BeforeAll
    static void initJfx() {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
        });
        latch.countDown();
    }

    @BeforeEach
    void setup() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM_ToolManagerTest.properties")) {
            properties.load(input);
        }

        steamCmdLocalPath = properties.getProperty("semm.steam.cmd.localPath");
        steamCmdSourceLocation = properties.getProperty("semm.steam.cmd.download.source");
        maxRetries = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.retry.limit"));
        connectionTimeout = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.connection.timeout"));
        readTimeout = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.read.timeout"));
        retryDelay = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.retry.delay"));
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
     * @param wireMockRuntimeInfo Injected WireMock runtime information used to configure the mock HTTP server.
     * @throws IOException if an I/O error occurs during the test
     * @throws InterruptedException if the test thread is interrupted while waiting for the setup task to complete
     * @throws ExecutionException if the setup task throws an exception during execution
     */
    @Test
    void shouldDownloadFakeSteamCmdAfterRetrying(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException, InterruptedException, ExecutionException {
        //Setup
        int fileSize = 1024 * 768;
        byte[] fakeSteamCmdZip = createFakeZipFile("steamcmd.exe", fileSize); //This is about the real size of the actual steamcmd.zip
        String steamCmdPath = "/steamcmd.zip";

        // HEAD request always returns Content-Length
        wireMockRuntimeInfo.getWireMock().register(
                request("HEAD", urlEqualTo(steamCmdPath))
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
                get(urlEqualTo(steamCmdPath))
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
                get(urlEqualTo(steamCmdPath))
                        .inScenario("RetryDownload")
                        .whenScenarioStateIs("PartialFailed")
                        .withHeader("Range", matching("bytes=\\d+-"))
                        .willReturn(aResponse()
                                .withStatus(500))
                        .willSetStateTo("RetryFailedOnce")
        );

        //Simulates second failure when we try to restart the download
        wireMockRuntimeInfo.getWireMock().register(
                get(urlEqualTo(steamCmdPath))
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
                get(urlEqualTo(steamCmdPath))
                        .inScenario("RetryDownload")
                        .whenScenarioStateIs("FinalAttempt")
                        .withHeader("Range", matching("bytes=\\d+-"))
                        .willReturn(aResponse()
                                .withStatus(206)
                                .withHeader("Content-Type", "application/zip")
                                .withBody(remainingData))  // rest of the file
        );

        // Use WireMock URL
        steamCmdSourceLocation = wireMockRuntimeInfo.getHttpBaseUrl() + steamCmdPath;

        toolManagerService = new ToolManagerService(mock(UiService.class), steamCmdLocalPath, steamCmdSourceLocation, maxRetries, connectionTimeout, readTimeout, retryDelay);
        Task<Result<Void>> setupTask = toolManagerService.setupTools();

        //Track our updates from the task
        List<String> messages = new CopyOnWriteArrayList<>();
        List<Double> progress = new CopyOnWriteArrayList<>();
        CountDownLatch doneLatch = new CountDownLatch(1);

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
        assertEquals("Successfully downloaded all required tools.", result.getCurrentMessage());
        assertEquals(ResultType.SUCCESS, result.getType(), "Result is the wrong type, should be SUCCESS.");
        assertEquals(5, result.getMESSAGES().size(), "Result messages were:\n" + String.join("\n", result.getMESSAGES()));

        //Check we both don't have an empty list of messages and that it's giving us the last expected final message
        assertFalse(messages.isEmpty(), "Should have updated messages");
        assertFalse(progress.isEmpty());
        assertEquals(String.format("%sKB/%sKB", fakeSteamCmdZip.length / 1000, fakeSteamCmdZip.length / 1000), messages.getLast(), "Update messages were:\n" + String.join("\n", messages));
        assertEquals(1.0, progress.getLast(), "Progress messages were:\n" + progress.stream().map(p -> String.format("%.2f", p)).collect(Collectors.joining("\n- ")));

        //Verify the file saved properly.
        assertEquals(fakeSteamCmdZip.length, new File(steamCmdLocalPath).length());

        //Verify our zip file extracted properly
        Path extractedZipPath = Path.of(Path.of(steamCmdLocalPath).getParent() + "/steamcmd.exe");
        assertTrue(Files.exists(extractedZipPath));
        assertEquals(fileSize, Files.size(extractedZipPath));
    }

    private static byte[] createFakeZipFile(String entryInZipName, int uncompressedSize) throws IOException {
        byte[] fakeZip = new byte[uncompressedSize];
        new Random().nextBytes(fakeZip);

        //Write our zip file to a byte array
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try(ZipOutputStream zos = new ZipOutputStream(byteOut)) {
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
}
