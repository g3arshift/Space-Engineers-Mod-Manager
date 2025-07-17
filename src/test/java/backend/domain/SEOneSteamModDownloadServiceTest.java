package backend.domain;

import com.gearshiftgaming.se_mod_manager.operatingsystem.OperatingSystemVersion;
import com.gearshiftgaming.se_mod_manager.operatingsystem.OperatingSystemVersionUtility;
import com.gearshiftgaming.se_mod_manager.backend.data.steam.SimpleSteamLibraryFoldersVdfParser;
import com.gearshiftgaming.se_mod_manager.backend.domain.command.CommandResult;
import com.gearshiftgaming.se_mod_manager.backend.domain.command.CommandRunner;
import com.gearshiftgaming.se_mod_manager.backend.domain.command.DefaultCommandRunner;
import com.gearshiftgaming.se_mod_manager.backend.domain.mod.SteamInstallMissingException;
import com.gearshiftgaming.se_mod_manager.backend.domain.tool.ToolManagerService;
import com.gearshiftgaming.se_mod_manager.backend.domain.mod.SEOneSteamModDownloadService;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfileInfo;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveType;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import helper.JavaFXTestHelper;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
class SEOneSteamModDownloadServiceTest {

    @TempDir
    private Path tempDir;

    private String fallbackPath = "Mod_Downloads";

    private Path fakeClientSavePath;

    private Path fakeDedicatedServerPath;

    private Path fakeTorchPath;

    private String fakeSaveFolderName;

    private String fakeSaveName;

    private String steamCmdPath;

    private SaveProfileInfo saveProfileInfo;

    private final String fakeLinuxSeInstallLocation = Path.of(System.getProperty("user.home")).resolve(".local")
            .resolve("share")
            .resolve("Steam")
            .toString();

    private String fakeWindowsSeInstallLocation;

    @Mock
    private SimpleSteamLibraryFoldersVdfParser mockedVdfParser;

    @Mock
    private CommandRunner mockedCommandRunner;

    @Captor
    ArgumentCaptor<List<String>> captor;

    @BeforeAll
    static void init() {
        JavaFXTestHelper.initJavaFx();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws IOException {
        steamCmdPath = tempDir.resolve("steamcmd.exe").toString();
        saveProfileInfo = mock(SaveProfile.class);
        Files.createFile(Path.of(steamCmdPath));

        fakeSaveFolderName = "Test Save";
        fakeSaveName = "Alien Planet 06-24-2025 18-44-44";

        mockedCommandRunner = mock(DefaultCommandRunner.class);
        mockedVdfParser = mock(SimpleSteamLibraryFoldersVdfParser.class);
        captor = ArgumentCaptor.forClass(List.class);

        fakeClientSavePath = tempDir.resolve("Saves")
                .resolve("12345")
                .resolve(fakeSaveFolderName)
                .resolve(fakeSaveName);
        Files.createDirectories(fakeClientSavePath);
        Files.createFile(fakeClientSavePath.resolve("Sandbox_config.sbc"));

        fakeDedicatedServerPath = tempDir.resolve("SpaceEngineersDedicated")
                .resolve(fakeSaveFolderName)
                .resolve("Saves")
                .resolve(fakeSaveName);
        Files.createDirectories(fakeDedicatedServerPath);
        Files.createFile(fakeDedicatedServerPath.resolve("Sandbox_config.sbc"));

        fakeTorchPath = tempDir.resolve("Torch")
                .resolve("Instance")
                .resolve("Saves")
                .resolve(fakeSaveFolderName);
        Files.createDirectories(fakeTorchPath);
        Files.createFile(fakeTorchPath.resolve("Sandbox_config.sbc"));

        fakeWindowsSeInstallLocation = tempDir.resolve("Steam").toString();
        if(Files.exists(Path.of(fakeWindowsSeInstallLocation)))
            FileUtils.deleteDirectory(new File(fakeWindowsSeInstallLocation));
        Files.createDirectories(Path.of(fakeWindowsSeInstallLocation));

        //This has to be in the home directory for the linux tests.
        Files.createDirectories(Path.of(System.getProperty("user.home")).resolve(".steam").resolve("steam").resolve("config"));
        Path fakeLinuxSeInstallPath = Path.of(fakeLinuxSeInstallLocation);
        if(Files.exists(fakeLinuxSeInstallPath))
            FileUtils.deleteDirectory(new File(fakeLinuxSeInstallLocation));
        Files.createDirectories(fakeLinuxSeInstallPath);
        Files.deleteIfExists(Path.of(System.getProperty("user.home") + "/.steam/steam/config/libraryfolders.vdf"));
        Files.createFile(Path.of(System.getProperty("user.home") + "/.steam/steam/config/libraryfolders.vdf"));
    }

    @NotNull
    private Map<String, Object> getFakeWindowsLibraryVdf() {
        Map<String, Object> diskBlock = new HashMap<>();
        diskBlock.put("path", fakeWindowsSeInstallLocation);
        diskBlock.put("label", "");
        diskBlock.put("contentid", "5375801296658044253");
        diskBlock.put("totalsize", "0");
        diskBlock.put("update_clean_bytes_tally", "2147496717");
        diskBlock.put("time_last_update_verified", "1750723252");

        Map<String, String> appBlock = new HashMap<>();
        appBlock.put("244850", "43636628614");
        appBlock.put("12810", "4187527136");
        appBlock.put("223850", "8070176267");
        diskBlock.put("apps", appBlock);

        Map<String, Object> libraryFolders = new HashMap<>();
        libraryFolders.put("0", diskBlock);

        Map<String, Object> fakeLibraryFolders = new HashMap<>();
        fakeLibraryFolders.put("libraryfolders", libraryFolders);
        return fakeLibraryFolders;
    }

    @NotNull
    private Map<String, Object> getFakeLinuxLibraryVdf() {
        Map<String, Object> diskBlock = new HashMap<>();
        diskBlock.put("path", fakeLinuxSeInstallLocation);
        diskBlock.put("label", "");
        diskBlock.put("contentid", "5375801296658044253");
        diskBlock.put("totalsize", "0");
        diskBlock.put("update_clean_bytes_tally", "2147496717");
        diskBlock.put("time_last_update_verified", "1750723252");

        Map<String, String> appBlock = new HashMap<>();
        appBlock.put("244850", "43636628614");
        appBlock.put("12810", "4187527136");
        appBlock.put("223850", "8070176267");
        diskBlock.put("apps", appBlock);

        Map<String, Object> libraryFolders = new HashMap<>();
        libraryFolders.put("0", diskBlock);

        Map<String, Object> fakeLibraryFolders = new HashMap<>();
        fakeLibraryFolders.put("libraryfolders", libraryFolders);
        return fakeLibraryFolders;
    }

    @Test
    void constructorShouldThrowSteamInstallMissingException() {
        assertThrows(SteamInstallMissingException.class, () -> SEOneSteamModDownloadService.create("nonexistent/path/to/steamcmd.exe",
                mockedCommandRunner,
                mockedVdfParser));
    }

    @Test
    void clientPathShouldBeFallbackWhenGameNotInstalled() throws IOException, InterruptedException {
        //When we run the command to get the install location of steam, return our temp dir.
        when(mockedCommandRunner.runCommand(List.of("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath")))
                .thenReturn(new CommandResult(0, List.of("")));

        when(mockedCommandRunner.runCommand(List.of(steamCmdPath, "+login", "anonymous", "+quit")))
                .thenReturn(new CommandResult(0, List.of("Success")));

        String modId = "123456";
        Path fakeClientRoot = tempDir.resolve(fallbackPath);
        when(mockedCommandRunner.runCommand(List.of(steamCmdPath,
                "+force_install_dir", fakeClientRoot.resolve(fakeSaveName).toString(),
                "+login", "anonymous",
                "+workshop_download_item", "244850", modId,
                "+quit")))
                .thenReturn(new CommandResult(0, List.of("Success")));

        //When we try to parse a VDF file, normally our steam library, return our fake.
        when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeWindowsLibraryVdf());

        //Mock the behavior we need from our save profile
        when(saveProfileInfo.isSaveExists()).thenReturn(true);
        when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
        when(saveProfileInfo.getSaveType()).thenReturn(SaveType.CLIENT);
        when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeClientSavePath.resolve("Sandbox_config.sbc")));
        when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

        SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.createWithCustomFallbackRoot(tempDir.toString(),
                steamCmdPath,
                mockedCommandRunner,
                mockedVdfParser);

        Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);
        verify(mockedCommandRunner, times(3)).runCommand(captor.capture());
        List<String> actualCommand = captor.getValue();
        assertEquals(tempDir.resolve("Mod_Downloads").resolve(fakeSaveName).toString(), actualCommand.get(2));
        assertTrue(downloadResult.isSuccess());
    }


    @Test
    void getClientDownloadPathShouldUseWindowsPath() throws IOException, InterruptedException {
        try (MockedStatic<OperatingSystemVersionUtility> utilMock = mockStatic(OperatingSystemVersionUtility.class)) {
            utilMock.when(OperatingSystemVersionUtility::getOperatingSystemVersion)
                    .thenReturn(OperatingSystemVersion.WINDOWS_11);

            //When we run the command to get the install location of steam, return our temp dir.
            when(mockedCommandRunner.runCommand(List.of("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath")))
                    .thenReturn(new CommandResult(0, List.of("    InstallPath    REG_SZ    " + tempDir)));

            when(mockedCommandRunner.runCommand(List.of(steamCmdPath, "+login", "anonymous", "+quit")))
                    .thenReturn(new CommandResult(0, List.of("Success")));

            String modId = "123456";
            when(mockedCommandRunner.runCommand(List.of(steamCmdPath,
                    "+force_install_dir", fakeWindowsSeInstallLocation,
                    "+login", "anonymous",
                    "+workshop_download_item", "244850", modId,
                    "+quit")))
                    .thenReturn(new CommandResult(0, List.of("Success")));

            //When we try to parse a VDF file, normally our steam library, return our fake.
            when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeWindowsLibraryVdf());

            //Mock the behavior we need from our save profile
            when(saveProfileInfo.isSaveExists()).thenReturn(true);
            when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
            when(saveProfileInfo.getSaveType()).thenReturn(SaveType.CLIENT);
            when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeClientSavePath.resolve("Sandbox_config.sbc")));
            when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

            SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.createWithCustomFallbackRoot(tempDir.toString(),
                    steamCmdPath,
                    mockedCommandRunner,
                    mockedVdfParser);

            Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);

            //Our bread and butter. Now we can actually verify our args.
            verify(mockedCommandRunner, times(3)).runCommand(captor.capture());
            List<String> actualCommand = captor.getValue();
            assertEquals(fakeWindowsSeInstallLocation, actualCommand.get(2));
            assertTrue(downloadResult.isSuccess());
        }
    }

    @Test
    void getClientDownloadPathShouldUseLinuxPath() throws Exception {
        try (MockedStatic<OperatingSystemVersionUtility> utilMock = mockStatic(OperatingSystemVersionUtility.class)) {
            utilMock.when(OperatingSystemVersionUtility::getOperatingSystemVersion)
                    .thenReturn(OperatingSystemVersion.LINUX);

            when(mockedCommandRunner.runCommand(List.of(steamCmdPath, "+login", "anonymous", "+quit")))
                    .thenReturn(new CommandResult(0, List.of("Success")));

            String modId = "123456";
            //When we run the download command, get a valid result.
            when(mockedCommandRunner.runCommand(List.of(steamCmdPath,
                    "+force_install_dir", fakeLinuxSeInstallLocation,
                    "+login", "anonymous",
                    "+workshop_download_item", "244850", modId,
                    "+quit")))
                    .thenReturn(new CommandResult(0, List.of("Success")));

            //When we try to parse a VDF file, normally our steam library, return our fake.
            when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeLinuxLibraryVdf());

            //Mock the behavior we need from our save profile
            when(saveProfileInfo.isSaveExists()).thenReturn(true);
            when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
            when(saveProfileInfo.getSaveType()).thenReturn(SaveType.CLIENT);
            when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeClientSavePath.resolve("Sandbox_config.sbc")));
            when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

            SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.createWithCustomFallbackRoot(tempDir.toString(),
                    steamCmdPath,
                    mockedCommandRunner,
                    mockedVdfParser);

            Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);

            //Our bread and butter. Now we can actually verify our args.
            verify(mockedCommandRunner, times(2)).runCommand(captor.capture());
            List<String> actualCommand = captor.getValue();
            assertEquals(fakeLinuxSeInstallLocation, actualCommand.get(2));
            assertTrue(downloadResult.isSuccess());
        }
    }

    @Test
    void getDedicatedServerRootShouldReturnWindowsPathWhenOsIsWindows() throws IOException, InterruptedException {
        try (MockedStatic<OperatingSystemVersionUtility> utilMock = mockStatic(OperatingSystemVersionUtility.class)) {
            utilMock.when(OperatingSystemVersionUtility::getOperatingSystemVersion)
                    .thenReturn(OperatingSystemVersion.WINDOWS_11);

            //When we run the command to get the install location of steam, return our temp dir.
            when(mockedCommandRunner.runCommand(List.of("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath")))
                    .thenReturn(new CommandResult(0, List.of("    InstallPath    REG_SZ    " + tempDir)));

            when(mockedCommandRunner.runCommand(List.of(steamCmdPath, "+login", "anonymous", "+quit")))
                    .thenReturn(new CommandResult(0, List.of("Success")));

            String modId = "123456";
            Path fakeServerRoot = fakeDedicatedServerPath.getParent().getParent();
            Files.createDirectories(fakeServerRoot.resolve("steamapps").resolve("workshop").resolve("content").resolve("244850").resolve(modId));
            //When we run the download command, get a valid result.
            when(mockedCommandRunner.runCommand(List.of(steamCmdPath,
                    "+force_install_dir", fakeServerRoot.toString(),
                    "+login", "anonymous",
                    "+workshop_download_item", "244850", modId,
                    "+quit")))
                    .thenReturn(new CommandResult(0, List.of("Success")));

            //When we try to parse a VDF file, normally our steam library, return our fake.
            when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeWindowsLibraryVdf());

            //Mock the behavior we need from our save profile
            when(saveProfileInfo.isSaveExists()).thenReturn(true);
            when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
            when(saveProfileInfo.getSaveType()).thenReturn(SaveType.DEDICATED_SERVER);
            when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeDedicatedServerPath.resolve("Sandbox_config.sbc")));
            when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

            SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.createWithCustomFallbackRoot(tempDir.toString(),
                    steamCmdPath,
                    mockedCommandRunner,
                    mockedVdfParser);

            Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);

            //Our bread and butter. Now we can actually verify our args.
            verify(mockedCommandRunner, times(3)).runCommand(captor.capture());
            List<String> actualCommand = captor.getValue();
            System.out.println(actualCommand.get(2));
            assertTrue(actualCommand.get(2).startsWith("C:\\Users"));
            assertEquals(String.valueOf(fakeServerRoot), actualCommand.get(2));
            assertTrue(downloadResult.isSuccess());
        }
    }

    //This generally won't actually matter, but we add it for verification anyways in case something down the line changes.
    @Test
    void getDedicatedServerRootShouldReturnLinuxPathWhenOsIsLinux() throws IOException, InterruptedException {
        try (MockedStatic<OperatingSystemVersionUtility> utilMock = mockStatic(OperatingSystemVersionUtility.class)) {
            utilMock.when(OperatingSystemVersionUtility::getOperatingSystemVersion)
                    .thenReturn(OperatingSystemVersion.LINUX);

            when(mockedCommandRunner.runCommand(List.of(steamCmdPath, "+login", "anonymous", "+quit")))
                    .thenReturn(new CommandResult(0, List.of("Success")));

            String modId = "123456";
            Path fakeServerRoot = fakeDedicatedServerPath.getParent().getParent();
            Files.createDirectories(fakeServerRoot.resolve("steamapps").resolve("workshop").resolve("content").resolve("244850").resolve(modId));
            //When we run the download command, get a valid result.
            when(mockedCommandRunner.runCommand(List.of(steamCmdPath,
                    "+force_install_dir", fakeServerRoot.toString(),
                    "+login", "anonymous",
                    "+workshop_download_item", "244850", modId,
                    "+quit")))
                    .thenReturn(new CommandResult(0, List.of("Success")));

            //When we try to parse a VDF file, normally our steam library, return our fake.
            when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeLinuxLibraryVdf());

            //Mock the behavior we need from our save profile
            when(saveProfileInfo.isSaveExists()).thenReturn(true);
            when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
            when(saveProfileInfo.getSaveType()).thenReturn(SaveType.DEDICATED_SERVER);
            when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeDedicatedServerPath.resolve("Sandbox_config.sbc")));
            when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

            SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.createWithCustomFallbackRoot(tempDir.toString(),
                    steamCmdPath,
                    mockedCommandRunner,
                    mockedVdfParser);

            Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);

            //Our bread and butter. Now we can actually verify our args.
            verify(mockedCommandRunner, times(2)).runCommand(captor.capture());
            List<String> actualCommand = captor.getValue();
            assertEquals(String.valueOf(fakeServerRoot), actualCommand.get(2));
            assertTrue(downloadResult.isSuccess());
        }
    }

    @Test
    void downloadModShouldFailWhenSaveDoesNotExist() throws IOException, InterruptedException {
        String modId = "123456";

        //When we run the command to get the install location of steam, return our temp dir.
        when(mockedCommandRunner.runCommand(List.of("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath")))
                .thenReturn(new CommandResult(0, List.of("    InstallPath    REG_SZ    " + tempDir)));

        //When we try to parse a VDF file, normally our steam library, return our fake.
        when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeWindowsLibraryVdf());

        //Mock the behavior we need from our save profile
        when(saveProfileInfo.isSaveExists()).thenReturn(false);
        when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
        when(saveProfileInfo.getSaveType()).thenReturn(SaveType.DEDICATED_SERVER);
        when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeDedicatedServerPath.resolve("Sandbox_config.sbc")));
        when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

        SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.createWithCustomFallbackRoot(tempDir.toString(),
                steamCmdPath,
                mockedCommandRunner,
                mockedVdfParser);

        Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);
        assertTrue(downloadResult.isFailure());
        assertEquals(String.format("Save does not exist. Cannot download mods for save \"%s\".", saveProfileInfo.getProfileName()), downloadResult.getCurrentMessage());
    }

    @Test
    void downloadModShouldFailWhenSteamCmdExitsWithError() throws IOException, InterruptedException {
        //When we run the command to get the install location of steam, return our temp dir.
        when(mockedCommandRunner.runCommand(List.of("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath")))
                .thenReturn(new CommandResult(0, List.of("    InstallPath    REG_SZ    " + tempDir)));

        when(mockedCommandRunner.runCommand(List.of(steamCmdPath, "+login", "anonymous", "+quit")))
                .thenReturn(new CommandResult(0, List.of("Success")));

        String modId = "123456";
        //When we run the download command, get a valid result.
        when(mockedCommandRunner.runCommand(List.of(steamCmdPath,
                "+force_install_dir", fakeWindowsSeInstallLocation,
                "+login", "anonymous",
                "+workshop_download_item", "244850", modId,
                "+quit")))
                .thenReturn(new CommandResult(99, List.of("Test Failure")));

        //When we try to parse a VDF file, normally our steam library, return our fake.
        when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeWindowsLibraryVdf());

        //Mock the behavior we need from our save profile
        when(saveProfileInfo.isSaveExists()).thenReturn(true);
        when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
        when(saveProfileInfo.getSaveType()).thenReturn(SaveType.CLIENT);
        when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeClientSavePath.resolve("Sandbox_config.sbc")));
        when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

        SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.createWithCustomFallbackRoot(tempDir.toString(),
                steamCmdPath,
                mockedCommandRunner,
                mockedVdfParser);

        Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);
        assertTrue(downloadResult.isFailure());
        assertEquals("SteamCMD failed with exit code: 99", downloadResult.getCurrentMessage());
    }

    @Test
    void downloadModShouldFailWhenNoSuccessOutput() throws IOException, InterruptedException {
        //When we run the command to get the install location of steam, return our temp dir.
        when(mockedCommandRunner.runCommand(List.of("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath")))
                .thenReturn(new CommandResult(0, List.of("    InstallPath    REG_SZ    " + tempDir)));

        when(mockedCommandRunner.runCommand(List.of(steamCmdPath, "+login", "anonymous", "+quit")))
                .thenReturn(new CommandResult(0, List.of("Success")));

        String modId = "123456";
        String steamFailMessage = "This is a test failure for steamcmd that isn't a critical failure";
        when(mockedCommandRunner.runCommand(List.of(steamCmdPath,
                "+force_install_dir", fakeWindowsSeInstallLocation,
                "+login", "anonymous",
                "+workshop_download_item", "244850", modId,
                "+quit")))
                .thenReturn(new CommandResult(0, List.of(steamFailMessage)));

        //When we try to parse a VDF file, normally our steam library, return our fake.
        when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeWindowsLibraryVdf());

        //Mock the behavior we need from our save profile
        when(saveProfileInfo.isSaveExists()).thenReturn(true);
        when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
        when(saveProfileInfo.getSaveType()).thenReturn(SaveType.CLIENT);
        when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeClientSavePath.resolve("Sandbox_config.sbc")));
        when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

        SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.createWithCustomFallbackRoot(tempDir.toString(),
                steamCmdPath,
                mockedCommandRunner,
                mockedVdfParser);

        Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);

        assertTrue(downloadResult.isFailure());
        assertEquals(String.format("Mod %s failed to download. SteamCMD reported: \"%s\".", modId, steamFailMessage), downloadResult.getCurrentMessage());
    }

    @Test
    void downloadModShouldSucceedWithValidClientDownloadPathOnWindows() throws IOException, InterruptedException, ExecutionException {
        setupRealDownloadBehavior();
        //When we try to parse a VDF file, normally our steam library, return our fake.
        when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeWindowsLibraryVdf());
        when(saveProfileInfo.getSaveType()).thenReturn(SaveType.CLIENT);
        when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeClientSavePath.resolve("Sandbox_config.sbc")));

        SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.create(tempDir.resolve("steamcmd.exe").toString(),
                new DefaultCommandRunner(),
                mockedVdfParser);

        String modId = "3329381499"; // Cross Barred windows (Large Grid Update)
        Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);
        assertTrue(downloadResult.isSuccess());
        assertEquals(String.format("Successfully downloaded mod %s.", modId), downloadResult.getCurrentMessage());
        assertTrue(Files.exists(Path.of(fakeWindowsSeInstallLocation).resolve("steamapps")
                .resolve("workshop")
                .resolve("content")
                .resolve("244850")
                .resolve(modId)
                .resolve("Data")));
    }

    @Test
    void downloadModShouldSucceedWithDedicatedServerSaveTypeOnWindows() throws IOException, ExecutionException, InterruptedException {
        setupRealDownloadBehavior();
        when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeWindowsLibraryVdf());
        when(saveProfileInfo.getSaveType()).thenReturn(SaveType.DEDICATED_SERVER);
        when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeDedicatedServerPath.resolve("Sandbox_config.sbc")));

        SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.create(tempDir.resolve("steamcmd.exe").toString(),
                new DefaultCommandRunner(),
                mockedVdfParser);

        String modId = "3329381499"; // Cross Barred windows (Large Grid Update)
        Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);
        assertTrue(downloadResult.isSuccess());
        assertEquals(String.format("Successfully downloaded mod %s.", modId), downloadResult.getCurrentMessage());
        assertTrue(Files.exists(tempDir.resolve("SpaceEngineersDedicated")
                .resolve(fakeSaveFolderName)
                .resolve("content")
                .resolve("244850")
                .resolve(modId)
                .resolve("Data")));
    }

    @Test
    void downloadModShouldSucceedWithTorchSaveTypeOnWindows() throws IOException, ExecutionException, InterruptedException {
        setupRealDownloadBehavior();
        when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeWindowsLibraryVdf());
        when(saveProfileInfo.getSaveType()).thenReturn(SaveType.TORCH);
        when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeTorchPath.resolve("Sandbox_config.sbc")));

        SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.create(tempDir.resolve("steamcmd.exe").toString(),
                new DefaultCommandRunner(),
                mockedVdfParser);

        String modId = "3329381499"; // Cross Barred windows (Large Grid Update)
        Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);
        assertTrue(downloadResult.isSuccess(), downloadResult.getCurrentMessage());
        assertEquals(String.format("Successfully downloaded mod %s.", modId), downloadResult.getCurrentMessage());

        assertTrue(Files.exists(tempDir.resolve("Torch")
                .resolve("Instance")
                .resolve("content")
                .resolve("244850")
                .resolve(modId)
                .resolve("Data")));
    }

    private void setupRealDownloadBehavior() throws IOException, InterruptedException, ExecutionException {
        Properties properties = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM_ToolManagerTest.properties")) {
            properties.load(input);
        }

        Result<Void> steamCmdDownloadResult = downloadSteamCmd(properties);
        assertTrue(steamCmdDownloadResult.isSuccess(), steamCmdDownloadResult.getCurrentMessage());
        assertEquals("Successfully downloaded SteamCMD", steamCmdDownloadResult.getCurrentMessage());

        //When we run the command to get the install location of steam, return our temp dir.
        when(mockedCommandRunner.runCommand(List.of("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath")))
                .thenReturn(new CommandResult(0, List.of("    InstallPath    REG_SZ    " + tempDir)));

        //Mock the behavior we need from our save profile
        when(saveProfileInfo.isSaveExists()).thenReturn(true);
        when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
        when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);
    }

    private Result<Void> downloadSteamCmd(Properties properties) throws InterruptedException, ExecutionException, IOException {
        Files.deleteIfExists(tempDir.resolve("steamcmd.exe"));
        CountDownLatch doneLatch = new CountDownLatch(1);

        String steamCmdSourceLocation = properties.getProperty("semm.steam.cmd.download.source");
        int maxRetries = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.retry.limit"));
        int connectionTimeout = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.connection.timeout"));
        int readTimeout = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.read.timeout"));
        int retryDelay = Integer.parseInt(properties.getProperty("semm.steam.cmd.download.retry.delay"));

        ToolManagerService toolManagerService = new ToolManagerService(mock(UiService.class),
                tempDir.resolve("steamcmd.zip").toString(),
                steamCmdSourceLocation,
                maxRetries,
                connectionTimeout,
                readTimeout,
                retryDelay);
        Task<Result<Void>> setupTask = toolManagerService.setupSteamCmd();

        setupTask.setOnSucceeded(e -> doneLatch.countDown());
        setupTask.setOnFailed(e -> doneLatch.countDown());

        //Run the task
        Thread taskThread = Thread.ofVirtual().unstarted(setupTask);
        taskThread.start();

        //Pause the test until our task is done, but give it a timeout.
        assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "Could not download SteamCMD.");
        return setupTask.get();
    }

    @Test
    void shouldFailWhenSteamCmdExitCodeIsNotZeroOrSevenAfterUpdate() throws IOException, InterruptedException {
        //When we run the command to get the install location of steam, return our temp dir.
        when(mockedCommandRunner.runCommand(List.of("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath")))
                .thenReturn(new CommandResult(0, List.of("")));

        int testFailCode = 6;
        when(mockedCommandRunner.runCommand(List.of(steamCmdPath, "+login", "anonymous", "+quit")))
                .thenReturn(new CommandResult(testFailCode, List.of("Test Failure")));

        //When we try to parse a VDF file, normally our steam library, return our fake.
        when(mockedVdfParser.parseVdf(any())).thenReturn(getFakeWindowsLibraryVdf());

        String modId = "123456";

        //Mock the behavior we need from our save profile
        when(saveProfileInfo.isSaveExists()).thenReturn(true);
        when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
        when(saveProfileInfo.getSaveType()).thenReturn(SaveType.CLIENT);
        when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeClientSavePath.resolve("Sandbox_config.sbc")));
        when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

        SEOneSteamModDownloadService downloadService = SEOneSteamModDownloadService.createWithCustomFallbackRoot(tempDir.toString(),
                steamCmdPath,
                mockedCommandRunner,
                mockedVdfParser);

        Result<Void> downloadResult = downloadService.downloadMod(modId, saveProfileInfo);

        assertTrue(downloadResult.isFailure());
        assertEquals(String.format("Failed to update SteamCMD. Exited with code: %d", testFailCode), downloadResult.getCurrentMessage());
    }

    @Test
    void isModDownloadedShouldReturnFalseWhenNotDownloaded() {

    }

    @Test
    void isModDownloadedShouldReturnTrueWhenDownloaded() {

    }

    @Test
    void getModPathShouldReturnEmptyStringWhenNotDownloaded() {

    }

    @Test
    void getModPathShouldReturnValidStringWhenDownloaded() {

    }

    @Test
    void shouldFailToUpdateSteamCmdAndExitWithFailedStatus() {

    }
}
