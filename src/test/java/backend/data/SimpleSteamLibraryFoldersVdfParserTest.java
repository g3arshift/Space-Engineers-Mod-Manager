package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.SimpleSteamLibraryFoldersVdfParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SimpleSteamLibraryFoldersVdfParserTest {

    private static final String TEST_FILE_PATH = "src/test/resources/test_libraryfolders.vdf";

    private SimpleSteamLibraryFoldersVdfParser steamLibraryFoldersVdfParser;

    @BeforeEach
    void setup() {
        steamLibraryFoldersVdfParser = new SimpleSteamLibraryFoldersVdfParser();
    }

    @Test
    void shouldGenerateValidVdfTable() throws IOException {
        HashMap<String, Object> validTable = steamLibraryFoldersVdfParser.parseVdf(TEST_FILE_PATH);
        System.out.println("");
    }
}
