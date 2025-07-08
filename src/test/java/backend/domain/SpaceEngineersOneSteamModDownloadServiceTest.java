package backend.domain;

import com.gearshiftgaming.se_mod_manager.OperatingSystemVersion;
import com.gearshiftgaming.se_mod_manager.OperatingSystemVersionUtility;
import com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager;
import com.gearshiftgaming.se_mod_manager.backend.domain.SpaceEngineersOneSteamModDownloadService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SteamInstallMissingException;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfileInfo;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveType;
import com.gearshiftgaming.se_mod_manager.backend.models.SpaceEngineersVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
class SpaceEngineersOneSteamModDownloadServiceTest {

    @TempDir
    private Path tempDir;

    private String steamCmdPath;

    private SaveProfileInfo saveProfileInfo;

    @BeforeEach
    void setup() {
        steamCmdPath = tempDir.resolve("steamcmd.exe").toString();
        saveProfileInfo = mock(SaveProfile.class);
    }

    @Test
    void constructorShouldThrowSteamInstallMissingException() {
        assertThrows(SteamInstallMissingException.class, () -> new SpaceEngineersOneSteamModDownloadService("nonexistent/path/to/steamcmd.exe"));
    }

    @Test
    void constructorShouldUseFallbackWhenClientDownloadPathMissing() throws IOException, ClassNotFoundException, InterruptedException, NoSuchMethodException {
        Files.createFile(Path.of(steamCmdPath));
    }

    @Test
    void getDedicatedServerRootShouldReturnWindowsPathWhenOsIsWindows() {

    }

    @Test
    void getDedicatedServerRootShouldReturnLinuxPathWhenOsIsLinux() {

    }

    @Test
    void getClientDownloadPathShouldUseWindowsRegistry() throws Exception {
    }


    @Test
    void getClientDownloadPathShouldUseLinuxPath() throws Exception {
        try (MockedStatic<OperatingSystemVersionUtility> utilMock = mockStatic(OperatingSystemVersionUtility.class)) {
            utilMock.when(OperatingSystemVersionUtility::getOperatingSystemVersion)
                    .thenReturn(OperatingSystemVersion.LINUX);

            Class.forName("com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager");
            assertEquals(OperatingSystemVersion.LINUX, SpaceEngineersModManager.OPERATING_SYSTEM_VERSION);
            //TODO: Do the stuff in the try
        }
    }


    @Test
    void downloadModShouldFailWhenSaveDoesNotExist() {

    }

    @Test
    void downloadModShouldUseFallbackWhenDownloadPathIsInvalid() {

    }

    @Test
    void downloadModShouldFailwhenSteamCmdExitsWithError() {

    }

    @Test
    void downloadModShouldFailWhenNoSuccessOutput() {

    }

    @Test
    void downloadModShouldSucceedWithValidClientDownloadPath() {

    }

    void downloadModShouldSucceedWithDedicatedServerSaveType() {

    }

    @Test
    void downloadModShouldSucceedWithTorchSaveType() {

    }

    @Test
    void isModDownloadedShouldReturnFalse() {

    }

    @Test
    void isModDownloadedShouldReturnTrue() {

    }

    @Test
    void getModPathSHouldReturnEmptyString() {

    }

    @Test
    void getModPathSHouldReturnValidString() {

    }

    @Test
    void removeMeImJustForDev() throws IOException, InterruptedException {
        //spaceEngineersOneSteamModDownloadService = new SpaceEngineersOneSteamModDownloadService("./Tools/SteamCMD/steamcmd.exe");
        //TODO: We need a real test profile here with all the fields.
//        SaveProfile saveProfile = new SaveProfile("pr7",
//                "C:\\Users\\Gear Shift\\AppData\\Roaming\\SpaceEngineers\\Saves\\76561198072313924\\Phoenix Rising Test 7\\Sandbox_config.sbc",
//                "Phoenix Rising Test 7",
//                SpaceEngineersVersion.SPACE_ENGINEERS_ONE,
//                SaveType.GAME);

//        SaveProfile saveProfile = new SaveProfile("alien planet",
//                "C:\\ProgramData\\SpaceEngineersDedicated\\New Test World\\Saves\\Alien Planet 06-24-2025 18-44-44\\Sandbox_config.sbc",
//                "Alien Planet",
//                SpaceEngineersVersion.SPACE_ENGINEERS_ONE,
//                SaveType.DEDICATED_SERVER);

        SaveProfile saveProfile = new SaveProfile("STar system",
                "G:\\Support\\Torch\\Instance\\Saves\\Star System [PC]\\Sandbox_config.sbc",
                "Star System [PC]",
                SpaceEngineersVersion.SPACE_ENGINEERS_ONE,
                SaveType.TORCH);

        //spaceEngineersOneSteamModDownloadService.downloadMod("3329381499", saveProfile);
    }

}
