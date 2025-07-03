package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.domain.SteamModDownloadService;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveType;
import com.gearshiftgaming.se_mod_manager.backend.models.SpaceEngineersVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SteamModDownloadServiceTest {

    private SteamModDownloadService steamModDownloadService;

    @TempDir
    public File tempDir;


    @Test
    void removeMeImJustForDev() throws IOException, InterruptedException {
        steamModDownloadService = new SteamModDownloadService("./Tools/SteamCMD/steamcmd.exe");
        //TODO: We need a real test profile here with all the fields.
        SaveProfile saveProfile = new SaveProfile("pr7",
                "C:\\Users\\Gear Shift\\AppData\\Roaming\\SpaceEngineers\\Saves\\76561198072313924\\Phoenix Rising Test 7\\Sandbox_config.sbc",
                "Phoenix Rising Test 7",
                SpaceEngineersVersion.SPACE_ENGINEERS_ONE,
                SaveType.GAME);
        steamModDownloadService.downloadMod("3329381499", saveProfile);
        //TODO: Verify dedi and torch functionality
    }
}
