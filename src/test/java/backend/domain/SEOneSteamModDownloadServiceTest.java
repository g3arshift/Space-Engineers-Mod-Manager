package backend.domain;

import com.gearshiftgaming.se_mod_manager.OperatingSystemVersion;
import com.gearshiftgaming.se_mod_manager.OperatingSystemVersionUtility;
import com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager;
import com.gearshiftgaming.se_mod_manager.backend.data.SimpleSteamLibraryFoldersVdfParser;
import com.gearshiftgaming.se_mod_manager.backend.domain.*;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Path fakeClientPath;

    private Path fakeDedicatedServerPath;

    private Path fakeTorchPath;

    private String fakeSaveFolderName;

    private String fakeSaveName;

    private String steamCmdPath;

    private SaveProfileInfo saveProfileInfo;

    private Map<String, Object> fakeLibraryFolders;

    @Mock
    private SimpleSteamLibraryFoldersVdfParser mockedVdfParser;

    @Mock
    private CommandRunner mockedCommandRunner;

    @Captor
    ArgumentCaptor<List<String>> captor;

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

        fakeLibraryFolders = new HashMap<>();
        Map<String, Object> libraryFolders = getFakeLibraryFoldersVdf();
        fakeLibraryFolders.put("libraryfolders", libraryFolders);

        fakeClientPath = tempDir.resolve("Saves")
                .resolve("12345")
                .resolve(fakeSaveFolderName)
                .resolve(fakeSaveName);
        Files.createDirectories(fakeClientPath);
        Files.createFile(fakeClientPath.resolve("Sandbox_config.sbc"));

        fakeDedicatedServerPath = tempDir.resolve("SpaceEngineersDedicated")
                .resolve(fakeSaveFolderName)
                .resolve("Saves")
                .resolve(fakeSaveName);
        Files.createDirectories(fakeDedicatedServerPath);
        Files.createFile(fakeDedicatedServerPath.resolve("Sandbox_config.sbc"));
    }

    @NotNull
    private Map<String, Object> getFakeLibraryFoldersVdf() {
        Map<String, Object> diskBlock = new HashMap<>();
        diskBlock.put("path", tempDir.toString());
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
        return libraryFolders;
    }

    @Test
    void constructorShouldThrowSteamInstallMissingException() {
        assertThrows(SteamInstallMissingException.class, () -> new SEOneSteamModDownloadService("nonexistent/path/to/steamcmd.exe",
                mockedCommandRunner,
                mockedVdfParser));
    }

    @Test
    void clientPathShouldBeFallbackWhenGameNotInstalled() throws IOException, InterruptedException {
        //When we run the command to get the install location of steam, return our temp dir.
        when(mockedCommandRunner.runCommand(List.of("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath")))
                .thenReturn(new CommandResult(0, List.of("")));

        String modId = "123456";
        Path fakeClientRoot = tempDir.resolve(fallbackPath);
        when(mockedCommandRunner.runCommand(List.of(steamCmdPath,
                "+force_install_dir", fakeClientRoot.resolve(fakeSaveName).toString(),
                "+login", "anonymous",
                "+workshop_download_item", "244850", modId,
                "validate", "+quit")))
                .thenReturn(new CommandResult(0, List.of("Success")));

        //When we try to parse a VDF file, normally our steam library, return our fake.
        when(mockedVdfParser.parseVdf(any())).thenReturn(fakeLibraryFolders);

        //Mock the behavior we need from our save profile
        when(saveProfileInfo.saveExists()).thenReturn(true);
        when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
        when(saveProfileInfo.getSaveType()).thenReturn(SaveType.CLIENT);
        when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeClientPath.resolve("Sandbox_config.sbc")));
        when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

        SEOneSteamModDownloadService SEOneSteamModDownloadService = new SEOneSteamModDownloadService(tempDir.toString(),
                steamCmdPath,
                mockedCommandRunner,
                mockedVdfParser);

        Result<Void> downloadResult = SEOneSteamModDownloadService.downloadMod(modId, saveProfileInfo);
        verify(mockedCommandRunner, times(2)).runCommand(captor.capture());
        List<String> actualCommand = captor.getValue();
        assertEquals(fakeClientRoot.resolve(fakeSaveName).toString(), actualCommand.get(2));
        assertTrue(downloadResult.isSuccess());
    }

    @Test
    void getDedicatedServerRootShouldReturnWindowsPathWhenOsIsWindows() throws ClassNotFoundException, IOException, InterruptedException {
        try (MockedStatic<OperatingSystemVersionUtility> utilMock = mockStatic(OperatingSystemVersionUtility.class)) {
            utilMock.when(OperatingSystemVersionUtility::getOperatingSystemVersion)
                    .thenReturn(OperatingSystemVersion.WINDOWS_11);

            Class.forName("com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager");
            assertEquals(OperatingSystemVersion.WINDOWS_11, SpaceEngineersModManager.OPERATING_SYSTEM_VERSION);

            //When we run the command to get the install location of steam, return our temp dir.
            when(mockedCommandRunner.runCommand(List.of("REG", "QUERY", "HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam", "/v", "InstallPath")))
                    .thenReturn(new CommandResult(0, List.of("    InstallPath    REG_SZ    " + tempDir)));

            String modId = "123456";
            Path fakeServerRoot = fakeDedicatedServerPath.getParent().getParent();
            Files.createDirectories(fakeServerRoot.resolve("steamapps").resolve("workshop").resolve("content").resolve("244850").resolve(modId));
            //When we run the download command, get a valid result.
            when(mockedCommandRunner.runCommand(List.of(steamCmdPath,
                    "+force_install_dir", fakeServerRoot.toString(),
                    "+login", "anonymous",
                    "+workshop_download_item", "244850", modId,
                    "validate", "+quit")))
                    .thenReturn(new CommandResult(0, List.of("Success")));

            //When we try to parse a VDF file, normally our steam library, return our fake.
            when(mockedVdfParser.parseVdf(any())).thenReturn(fakeLibraryFolders);

            //Mock the behavior we need from our save profile
            when(saveProfileInfo.saveExists()).thenReturn(true);
            when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
            when(saveProfileInfo.getSaveType()).thenReturn(SaveType.DEDICATED_SERVER);
            when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeDedicatedServerPath.resolve("Sandbox_config.sbc")));
            when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

            SEOneSteamModDownloadService SEOneSteamModDownloadService = new SEOneSteamModDownloadService(tempDir.toString(),
                    steamCmdPath,
                    mockedCommandRunner,
                    mockedVdfParser);

            Result<Void> downloadResult = SEOneSteamModDownloadService.downloadMod(modId, saveProfileInfo);

            //Our bread and butter. Now we can actually verify our args.
            verify(mockedCommandRunner, times(2)).runCommand(captor.capture());
            List<String> actualCommand = captor.getValue();
            assertEquals(String.valueOf(fakeServerRoot), actualCommand.get(2));
            assertTrue(downloadResult.isSuccess());
        }
    }

    //This generally won't actually matter, but we add it for verification anyways in case something down the line changes.
    @Test
    void getDedicatedServerRootShouldReturnLinuxPathWhenOsIsLinux() throws ClassNotFoundException, IOException, InterruptedException {
        try (MockedStatic<OperatingSystemVersionUtility> utilMock = mockStatic(OperatingSystemVersionUtility.class)) {
            utilMock.when(OperatingSystemVersionUtility::getOperatingSystemVersion)
                    .thenReturn(OperatingSystemVersion.LINUX);

            Class.forName("com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager");
            assertEquals(OperatingSystemVersion.LINUX, SpaceEngineersModManager.OPERATING_SYSTEM_VERSION);

            String modId = "123456";
            Path fakeServerRoot = fakeDedicatedServerPath.getParent().getParent();
            Files.createDirectories(fakeServerRoot.resolve("steamapps").resolve("workshop").resolve("content").resolve("244850").resolve(modId));
            //When we run the download command, get a valid result.
            when(mockedCommandRunner.runCommand(List.of(steamCmdPath,
                    "+force_install_dir", fakeServerRoot.toString(),
                    "+login", "anonymous",
                    "+workshop_download_item", "244850", modId,
                    "validate", "+quit")))
                    .thenReturn(new CommandResult(0, List.of("Success")));

            //When we try to parse a VDF file, normally our steam library, return our fake.
            when(mockedVdfParser.parseVdf(any())).thenReturn(fakeLibraryFolders);

            //Mock the behavior we need from our save profile
            when(saveProfileInfo.saveExists()).thenReturn(true);
            when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
            when(saveProfileInfo.getSaveType()).thenReturn(SaveType.DEDICATED_SERVER);
            when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeDedicatedServerPath.resolve("Sandbox_config.sbc")));
            when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

            SEOneSteamModDownloadService SEOneSteamModDownloadService = new SEOneSteamModDownloadService(tempDir.toString(),
                    steamCmdPath,
                    mockedCommandRunner,
                    mockedVdfParser);

            Result<Void> downloadResult = SEOneSteamModDownloadService.downloadMod(modId, saveProfileInfo);

            //Our bread and butter. Now we can actually verify our args.
            verify(mockedCommandRunner, times(1)).runCommand(captor.capture());
            List<String> actualCommand = captor.getValue();
            assertEquals(String.valueOf(fakeServerRoot), actualCommand.get(2));
            assertTrue(downloadResult.isSuccess());
        }
    }


    @Test
    void getClientDownloadPathShouldUseLinuxPath() throws Exception {
        try (MockedStatic<OperatingSystemVersionUtility> utilMock = mockStatic(OperatingSystemVersionUtility.class)) {
            utilMock.when(OperatingSystemVersionUtility::getOperatingSystemVersion)
                    .thenReturn(OperatingSystemVersion.LINUX);

            Class.forName("com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager");
            assertEquals(OperatingSystemVersion.LINUX, SpaceEngineersModManager.OPERATING_SYSTEM_VERSION);

            String modId = "123456";
            Path fakeClientRoot = tempDir.resolve(fallbackPath);
            //When we run the download command, get a valid result.
            when(mockedCommandRunner.runCommand(List.of(steamCmdPath,
                    "+force_install_dir", fakeClientRoot.resolve(fakeSaveName).toString(),
                    "+login", "anonymous",
                    "+workshop_download_item", "244850", modId,
                    "validate", "+quit")))
                    .thenReturn(new CommandResult(0, List.of("Success")));

            //When we try to parse a VDF file, normally our steam library, return our fake.
            when(mockedVdfParser.parseVdf(any())).thenReturn(fakeLibraryFolders);

            //Mock the behavior we need from our save profile
            when(saveProfileInfo.saveExists()).thenReturn(true);
            when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
            when(saveProfileInfo.getSaveType()).thenReturn(SaveType.CLIENT);
            when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeDedicatedServerPath.resolve("Sandbox_config.sbc")));
            when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

            SEOneSteamModDownloadService SEOneSteamModDownloadService = new SEOneSteamModDownloadService(tempDir.toString(),
                    steamCmdPath,
                    mockedCommandRunner,
                    mockedVdfParser);

            Result<Void> downloadResult = SEOneSteamModDownloadService.downloadMod(modId, saveProfileInfo);

            //Our bread and butter. Now we can actually verify our args.
            verify(mockedCommandRunner, times(1)).runCommand(captor.capture());
            List<String> actualCommand = captor.getValue();
            assertEquals(fakeClientRoot.resolve(fakeSaveName).toString(), actualCommand.get(2));
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
        when(mockedVdfParser.parseVdf(any())).thenReturn(fakeLibraryFolders);

        //Mock the behavior we need from our save profile
        when(saveProfileInfo.saveExists()).thenReturn(false);
        when(saveProfileInfo.getProfileName()).thenReturn("Test Profile");
        when(saveProfileInfo.getSaveType()).thenReturn(SaveType.CLIENT);
        when(saveProfileInfo.getSavePath()).thenReturn(String.valueOf(fakeDedicatedServerPath.resolve("Sandbox_config.sbc")));
        when(saveProfileInfo.getSaveName()).thenReturn(fakeSaveName);

        SEOneSteamModDownloadService SEOneSteamModDownloadService = new SEOneSteamModDownloadService(tempDir.toString(),
                steamCmdPath,
                mockedCommandRunner,
                mockedVdfParser);

        Result<Void> downloadResult = SEOneSteamModDownloadService.downloadMod(modId, saveProfileInfo);
        assertTrue(downloadResult.isFailure());
        assertEquals(String.format("Save does not exist. Cannot download mods for save \"%s\".", saveProfileInfo.getProfileName()), downloadResult.getCurrentMessage());
    }

    @Test
    void downloadModShouldUseFallbackWhenDownloadPathIsInvalid() {

    }

    @Test
    void downloadModShouldFailWhenSteamCmdExitsWithError() {

    }

    @Test
    void downloadModShouldFailWhenNoSuccessOutput() {

    }

    @Test
    void downloadModShouldSucceedWithValidClientDownloadPath() {
        //TODO: We are going to have to download steamcmd for this
    }

    @Test
    void downloadModShouldSucceedWithDedicatedServerSaveType() {
        //TODO: We are going to have to download steamcmd for this
    }

    @Test
    void downloadModShouldSucceedWithTorchSaveType() {
        //TODO: We are going to have to download steamcmd for this
    }

    @Test
    void isModDownloadedShouldReturnFalse() {

    }

    @Test
    void isModDownloadedShouldReturnTrue() {

    }

    @Test
    void getModPathShouldReturnEmptyString() {

    }

    @Test
    void getModPathShouldReturnValidString() {

    }

    @Test
    void removeMeImJustForDev() throws IOException, InterruptedException {
        SEOneSteamModDownloadService SEOneSteamModDownloadService = new SEOneSteamModDownloadService("./Tools/SteamCMD/steamcmd.exe",
                new DefaultCommandRunner(), new SimpleSteamLibraryFoldersVdfParser());
        //TODO: We need a real test profile here with all the fields.
//        SaveProfile saveProfile = new SaveProfile("pr7",
//                "C:\\Users\\Gear Shift\\AppData\\Roaming\\SpaceEngineers\\Saves\\76561198072313924\\Phoenix Rising Test 7\\Sandbox_config.sbc",
//                "Phoenix Rising Test 7",
//                SpaceEngineersVersion.SPACE_ENGINEERS_ONE,
//                SaveType.GAME);

        SaveProfile saveProfile = new SaveProfile("alien planet",
                "C:\\ProgramData\\SpaceEngineersDedicated\\New Test World\\Saves\\Alien Planet 06-24-2025 18-44-44\\Sandbox_config.sbc",
                "Alien Planet",
                SpaceEngineersVersion.SPACE_ENGINEERS_ONE,
                SaveType.DEDICATED_SERVER);

//        SaveProfile saveProfile = new SaveProfile("STar system",
//                "G:\\Support\\Torch\\Instance\\Saves\\Star System [PC]\\Sandbox_config.sbc",
//                "Star System [PC]",
//                SpaceEngineersVersion.SPACE_ENGINEERS_ONE,
//                SaveType.TORCH);

        SEOneSteamModDownloadService.downloadMod("3329381499", saveProfile);
    }

}
