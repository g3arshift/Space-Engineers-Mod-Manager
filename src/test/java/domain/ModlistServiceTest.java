package domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.ModRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModlistServiceTest {

    ModRepository modRepository;
    ModlistService service;
    List<Mod> modList;

    String testPath = "src/testpath";

    @BeforeEach
    void setup() {
        modRepository = mock(ModFileRepository.class);
        service = new ModlistService(modRepository, mock(Logger.class));
        service.setWorkshopConnectionActive(true);
        modList = new ArrayList<>();
        modList.add(new Mod("2777644246")); //Binoculars
        modList.add(new Mod("2668820525")); //TouchScreenAPI
        modList.add(new Mod("1902970975")); //Assertive Combat Systems
    }

    @Test
    void shouldGetModListWithThreeItems() {
        Result<List<Mod>> modListResult = new Result<>();
        modListResult.setPayload(modList);
        when(modRepository.getModList(testPath)).thenReturn(modListResult);

        List<Mod> testModList = service.getInjectableModListFromFile(testPath).getPayload();
        assertEquals(modList, testModList);
        assertEquals(3, testModList.size());
    }

    @Test
    void shouldCompleteModListWithFriendlyNameAndServiceName() throws ExecutionException, InterruptedException {
        List<Mod> testModList = new ArrayList<>();
        testModList.add(new Mod("2777644246"));
        testModList.add(new Mod("2668820525"));
        testModList.add(new Mod("1902970975"));

        service.generateModListSteam(testModList);
        assertEquals("Binoculars", testModList.get(0).getFriendlyName());
        assertEquals("TouchScreenAPI", testModList.get(1).getFriendlyName());
        assertEquals("Assertive Combat Systems", testModList.get(2).getFriendlyName());

        assertEquals("Steam", testModList.get(0).getPublishedServiceName());
        assertEquals("Steam", testModList.get(1).getPublishedServiceName());
        assertEquals("Steam", testModList.get(2).getPublishedServiceName());
    }

    @Test
    void shouldDownloadInformationForAMod() throws ExecutionException, InterruptedException {
        List<Mod> testModList = new ArrayList<>();
        testModList.add(new Mod("3276848116")); //Maelstrom - Black Hole
        service.generateModListSteam(testModList);
        assertEquals("Maelstrom - Black Hole", testModList.getFirst().getFriendlyName());
    }

    @Test
    void shouldAppendNotAModToModNameIfItIsNotAModItem() throws ExecutionException, InterruptedException {
        List<Mod> testModList = new ArrayList<>();
        testModList.add(new Mod("2396138200")); //Big Bird - Blueprint
        testModList.add(new Mod("1653185489")); //Escape From Mars Wico [Update WIP] - World
        service.generateModListSteam(testModList);
        assertEquals("Big Bird_NOT_A_MOD", testModList.getFirst().getFriendlyName());
        assertEquals("Escape From Mars Wico [Update WIP]_NOT_A_MOD", testModList.getLast().getFriendlyName());
    }
}
