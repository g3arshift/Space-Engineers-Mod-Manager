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
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
        if(Files.exists(Path.of(testDataToDeletePath)))
            FileUtils.deleteDirectory(new File(testDataToDeletePath));
    }

    @Test
    void shouldDownloadRegularSteamCmdAfterRetryingWithFakeWebServer(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException, InterruptedException, ExecutionException {
        //Setup
        byte[] fakeSteamCmdZip = new byte[1024 * 750]; //768KB file. We want at least this since steam CMD is usually close to this size
        new Random().nextBytes(fakeSteamCmdZip);
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

        setupTask.messageProperty().addListener((obs, oldVal, newVal) -> messages.add(newVal));
        setupTask.progressProperty().addListener((obs, oldVal, newVal) -> progress.add(newVal.doubleValue()));
        setupTask.setOnSucceeded(e -> doneLatch.countDown());
        setupTask.setOnFailed(e -> doneLatch.countDown());

        //Run the task
        Thread taskThread = Thread.ofVirtual().unstarted(setupTask);
        taskThread.setDaemon(true);
        taskThread.start();

        //Pause the test until our task is done
        assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "Task did not complete in time");
        Result<Void> result = setupTask.get();

        //Assert we don't have an empty list of messages or progress updates
        assertTrue(result.isSuccess(), "Download result should be successful");
        assertFalse(messages.isEmpty(), "Should have updated messages");
        assertFalse(progress.isEmpty());
        assertEquals("768KB/768KB", messages.getLast());
        assertEquals(1.0, progress.getLast());

        //For sanity's sake, print the messages to console for debugging.
        System.out.println("Update Messages:");
        messages.forEach(System.out::println);

        System.out.println("Progress Messages:");
        progress.forEach(p -> System.out.printf("%.2f%n", p));
    }
}
