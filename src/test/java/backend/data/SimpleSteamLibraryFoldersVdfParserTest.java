package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.SimpleSteamLibraryFoldersVdfParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    //FIXME: This test is really, really basic.
    @Test
    void shouldGenerateValidVdfTable() throws IOException {
        HashMap<String, Object> validTable = (HashMap<String, Object>) steamLibraryFoldersVdfParser.parseVdf(TEST_FILE_PATH);
        assertEquals(1, validTable.size());
        HashMap<String, Object> firstBlock = (HashMap<String, Object>) validTable.get("libraryfolders");
        assertEquals(6, firstBlock.size());

        HashMap<String, Object> secondBlock = (HashMap<String, Object>) firstBlock.get("0");
        assertEquals(7, secondBlock.size());

        HashMap<String, Object> thirdBlock = (HashMap<String, Object>) secondBlock.get("apps");
        assertEquals(22, thirdBlock.size());

        HashMap<String, Object> spaceEngineersBlock = (HashMap<String, Object>) ((HashMap<String, Object>) firstBlock.get("5")).get("apps");
        assertTrue(spaceEngineersBlock.containsKey("244850"));
    }
}
