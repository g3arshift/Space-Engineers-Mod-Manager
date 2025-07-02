package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.domain.SteamModDownloadService;
import org.junit.jupiter.api.BeforeEach;
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
        steamModDownloadService = new SteamModDownloadService(tempDir.getPath());
    }
}
