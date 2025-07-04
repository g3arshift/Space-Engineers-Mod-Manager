package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.domain.SpaceEngineersOneSteamModDownloadService;
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
public class SpaceEngineersOneSteamModDownloadServiceTest {

    private SpaceEngineersOneSteamModDownloadService spaceEngineersOneSteamModDownloadService;

    @TempDir
    public File tempDir;


    @Test
    void removeMeImJustForDev() throws IOException, InterruptedException {
        spaceEngineersOneSteamModDownloadService = new SpaceEngineersOneSteamModDownloadService("./Tools/SteamCMD/steamcmd.exe");
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

        spaceEngineersOneSteamModDownloadService.downloadMod("3329381499", saveProfile);
    }
}
