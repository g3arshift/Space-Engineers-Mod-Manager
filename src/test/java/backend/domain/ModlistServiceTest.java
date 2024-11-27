package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.ModType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModlistServiceTest {

    ModlistRepository modlistRepository;
    ModlistService service;
    List<Mod> modList;

    String testPath;

    String badExtensionPath;

    Properties properties;

    //TODO: Add test for incorrect file extension
    @BeforeEach
    void setup() throws IOException {
        properties = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM_Test.properties")) {
            properties.load(input);
        }

        modlistRepository = mock(ModlistFileRepository.class);
        service = new ModlistService(modlistRepository, properties);
        service.setWorkshopConnectionActive(true);
        modList = new ArrayList<>();
        modList.add(new Mod("2777644246", ModType.STEAM)); //Binoculars
        modList.add(new Mod("2668820525", ModType.STEAM)); //TouchScreenAPI
        modList.add(new Mod("1902970975", ModType.STEAM)); //Assertive Combat Systems
        badExtensionPath = "src/test/resources/nomods.sbc";
        testPath = "src/test/resources/GoodModList.txt";
    }

    @Test
    void shouldGetModListWithThreeItems() throws IOException {
        when(modlistRepository.getSteamModList(new File(testPath))).thenReturn(modList);

        List<Mod> testModList = service.getModListFromFile(testPath).getPayload();
        assertEquals(modList, testModList);
        assertEquals(3, testModList.size());
    }

    @Test
    void shouldCompleteSteamModListWithFriendlyNameAndServiceName() throws ExecutionException, InterruptedException {

        service.generateModListSteam(modList);
        assertEquals("Binoculars", modList.get(0).getFriendlyName());
        assertEquals("TouchScreenAPI", modList.get(1).getFriendlyName());
        assertEquals("Assertive Combat Systems", modList.get(2).getFriendlyName());

        assertEquals("Steam", modList.get(0).getPublishedServiceName());
        assertEquals("Steam", modList.get(1).getPublishedServiceName());
        assertEquals("Steam", modList.get(2).getPublishedServiceName());
    }

    @Test
    void shouldDownloadSteamInformationForAMod() throws ExecutionException, InterruptedException {
        List<Mod> testModList = new ArrayList<>();
        testModList.add(new Mod("3276848116", ModType.STEAM)); //Maelstrom - Black Hole
        service.generateModListSteam(testModList);
        assertEquals("Maelstrom - Black Hole", testModList.getFirst().getFriendlyName());
    }

    @Test
    void shouldAppendNotASteamModToModNameIfItIsNotAModItem() throws ExecutionException, InterruptedException {
        List<Mod> testModList = new ArrayList<>();
        testModList.add(new Mod("2396138200", ModType.STEAM)); //Big Bird - Blueprint
        testModList.add(new Mod("1653185489", ModType.STEAM)); //Escape From Mars Wico [Update WIP] - World
        service.generateModListSteam(testModList);
        assertEquals("Big Bird_NOT_A_MOD", testModList.getFirst().getFriendlyName());
        assertEquals("Escape From Mars Wico [Update WIP]_NOT_A_MOD", testModList.getLast().getFriendlyName());
    }

    @Test
    void shouldGetFileDoesNotExist() throws IOException {
        Result<List<Mod>> result = service.getModListFromFile("src/this/path/does/not/exist");
        assertFalse(result.isSuccess());
        assertEquals("File does not exist.", result.getCurrentMessage());
    }

    @Test
    void shouldGetIncorrectFileExtension() throws IOException {
        Result<List<Mod>> result = service.getModListFromFile(badExtensionPath);
        assertFalse(result.isSuccess());
        assertEquals("Incorrect file type selected. Please select a .txt or .doc file.", result.getCurrentMessage());
    }
}
