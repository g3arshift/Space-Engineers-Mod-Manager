package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModInfoService;
import com.gearshiftgaming.se_mod_manager.backend.models.ModIoMod;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.SteamMod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModInfoServiceTest {

    ModlistRepository modlistRepository;

    Properties properties;

    List<String> modListIds;

    ModInfoService modInfoService;

    @BeforeEach
    void setup() throws IOException {
        properties = new Properties();

        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
            properties.load(input);
        }

        modlistRepository = mock(ModlistRepository.class);

        modInfoService = new ModInfoService(modlistRepository, properties);

        modListIds = new ArrayList<>();
        modListIds.add("3329381499"); //Cross Barred Windows (Large Grid Update)
        modListIds.add("2777644246"); //Binoculars
        modListIds.add("2668820525"); //TouchScreenAPI
        modListIds.add("1902970975"); //Assertive Combat Systems
    }

    @Test
    void shouldGetNonSpaceEngineersCollectionError() throws IOException {
        String nonSpaceEngineersCollectionId = "2204937925"; //Some random TF2 collection
        List<Result<String>> badResults = modInfoService.scrapeSteamCollectionModIds(nonSpaceEngineersCollectionId);
        assertEquals(1, badResults.size());
        assertFalse(badResults.getFirst().isSuccess());
        assertEquals("The collection must be a Space Engineers collection!", badResults.getFirst().getCurrentMessage());
    }

    @Test
    void shouldGetNonCollectionItemError() throws IOException {
        String nonModId = "3329381499"; //Cross Barred Windows (Large Grid Update)
        List<Result<String>> badResults = modInfoService.scrapeSteamCollectionModIds(nonModId);
        assertEquals(1, badResults.size());
        assertFalse(badResults.getFirst().isSuccess());
        assertEquals("You must provide a link or ID of a collection!", badResults.getFirst().getCurrentMessage());
    }

    @Test
    void shouldGetEmptyCollection() throws IOException {
        String emptyCollectionId = "3409511657"; //SEMM Empty Test Collection
        List<Result<String>> badResults = modInfoService.scrapeSteamCollectionModIds(emptyCollectionId);
        assertEquals(1, badResults.size());
        assertFalse(badResults.getFirst().isSuccess());
        assertEquals("No items in this collection.", badResults.getFirst().getCurrentMessage());
    }

    @Test
    void shouldGetSevenIdsBack() throws IOException {
        String steamCollectionId = "3408899159";
        List<String> expectedIds = new ArrayList<>();
        expectedIds.add("2396138200"); //Big Bird
        expectedIds.add("548002928"); //PFS - UVG - Shadow Unmanned Guided Missile Corvette (World Version for Full Missile Loadout)
        expectedIds.add("805117008"); //Stairs with Railing
        expectedIds.add("438272891"); //(DX11) XL Thruster - Vanilla Style
        expectedIds.add("479678389"); //Battery Monitor With LCD Images
        expectedIds.add("618982571"); //Tiered Armor Hydrogen Engines
        expectedIds.add("1407133818"); //Medbay Checkpoint System

        List<Result<String>> goodResults = modInfoService.scrapeSteamCollectionModIds(steamCollectionId);
        assertEquals(7, goodResults.size());
        for (int i = 0; i < goodResults.size(); i++) {
            assertTrue(goodResults.get(i).isSuccess());
            assertEquals("Successfully grabbed mod ID.", goodResults.get(i).getCurrentMessage());
            assertEquals(expectedIds.get(i), goodResults.get(i).getPayload());
        }
    }

    @Test
    void shouldGetModIoIdFromName() throws IOException {
        String modIoUrl = "assault-weapons-pack1"; //Assault Weapons Pack
        Result<String> goodResult = modInfoService.getModIoIdFromName(modIoUrl);
        assertTrue(goodResult.isSuccess());
        assertEquals("451208", goodResult.getPayload());
    }

    @Test
    void shouldNotGetModIoId() throws IOException {
        String badModIoUrl = "Wqyk3rJ";
        Result<String> badResult = modInfoService.getModIoIdFromName(badModIoUrl);
        assertFalse(badResult.isSuccess());
        assertEquals("Invalid Mod.io URL entered!", badResult.getCurrentMessage());
    }

    @Test
    void shouldGetSteamNotFoundError() throws InterruptedException {
        String badModId = "122121213232";
        Result<String[]> badResult = modInfoService.scrapeModInformation(new SteamMod(badModId));
        assertFalse(badResult.isSuccess());
        assertEquals("Mod with ID \"122121213232\" cannot be found.", badResult.getCurrentMessage());
    }

    @Test
    void shouldGetSteamNotModItemError() throws InterruptedException {
        String screenShotId = "2396152929";
        Result<String[]> badResult = modInfoService.scrapeModInformation(new SteamMod(screenShotId));
        assertFalse(badResult.isSuccess());
    }

    @Test
    void shouldGetSteamCollectionError() throws InterruptedException {
        String steamCollectionId = "3408899159";
        Result<String[]> badResult = modInfoService.scrapeModInformation(new SteamMod(steamCollectionId));
        assertFalse(badResult.isSuccess());
        assertEquals("\"SEMM Test Collection\" is a collection, not a mod!", badResult.getCurrentMessage());
    }

    @Test
    void shouldGetSteamItemIsAScriptNotAModError() throws InterruptedException {
        String scriptItemId = "479678389";
        Result<String[]> badResult = modInfoService.scrapeModInformation(new SteamMod("479678389"));
        assertFalse(badResult.isSuccess());
        assertEquals("\"Battery Monitor With LCD Images\" is not a mod, it is a IngameScript.", badResult.getCurrentMessage());
    }

    @Test
    void shouldGetValidSteamModWithNoTags() throws InterruptedException {
        String noTagsModId = "1100741659";
        Result<String[]> noTagsResult = modInfoService.scrapeModInformation(new SteamMod(noTagsModId));
        assertTrue(noTagsResult.isSuccess());
        assertEquals("Azimuth Power ST Adaption", noTagsResult.getPayload()[0]);
        assertEquals("None", noTagsResult.getPayload()[1]);
        assertEquals("<div class=\"workshopItemDescription\" id=\"highlightContent\">\n" +
                " Azimuth Power Systems adapted to Firstofficered Star Trek Mod.\n" +
                " <br>\n" +
                " <br>\n" +
                " Fusion reactors burn Liquid Deuterium.\n" +
                " <br>\n" +
                " <br>\n" +
                " V8 burns Liquid Hydrogen\n" +
                " <br>\n" +
                " <br>\n" +
                " Azimuth batteries function more like capacitors... Less capacity, Higher powerlevels.\n" +
                " <br>\n" +
                " <br>\n" +
                " Duranium Plates replace Steel plates in construction.\n" +
                " <br>\n" +
                " <br>\n" +
                " Thanks to SEModder4 who's support made this possible.\n" +
                "</div>", noTagsResult.getPayload()[2]);
        assertEquals("Aug 4, 2017 @ 6:44pm", noTagsResult.getPayload()[3]);
    }

    @Test
    void shouldGetValidSteamMod() throws InterruptedException {
        //The steam mod should have the following traits:
        // Tags: NoScripts
        // Name: Cross Barred Windows (Large Grid Update)
        // Description (in base 64 because it's big):
        // Last Updated: Just check it's not null
        String steamModId = modListIds.getFirst();
        Result<String[]> goodModResult = modInfoService.scrapeModInformation(new SteamMod(steamModId));
        assertTrue(goodModResult.isSuccess());
        assertEquals("Cross Barred Windows (Large Grid Update)", goodModResult.getPayload()[0]);
        assertEquals("Block", goodModResult.getPayload()[1]);
        assertEquals("<div class=\"workshopItemDescription\" id=\"highlightContent\">\n" +
                " I made this mod because I love the combination of placing a barred window with a 90 degree offset onto another barred window. My only problem with it is that it takes up 2 block spaces, so I made this. It's purely cosmetic and practically identical to the barred windows. The blocks are integrated into the progression and block variation groups.\n" +
                " <br>\n" +
                " <br>\n" +
                " Because assets from it are used, the Wasteland DLC is required to use it.\n" +
                " <br>\n" +
                " <br>\n" +
                " <hr>\n" +
                " <br>\n" +
                " Anyway I hope you have fun with my first modeling experience in blender :)\n" +
                " <br>\n" +
                " <br>\n" +
                " For any bugs hit me up in the comments\n" +
                " <br>\n" +
                " <br>\n" +
                " - Grebanton1234\n" +
                "</div>", goodModResult.getPayload()[2]);
        assertNotEquals("Unknown", goodModResult.getPayload()[3]);
    }

    @Test
    void shouldGetModIoTimeoutError() throws InterruptedException {
        String badModIoId = "3123nj121";
        Result<String[]> badResult = modInfoService.scrapeModInformation(new ModIoMod(badModIoId));
        assertFalse(badResult.isSuccess());
    }

    @Test
    void shouldGetModIoValidMod() throws InterruptedException {
        //The Mod.io Mod should have the following traits:
        // Name: Multi-Function Survival Kit with Sifter
        // Tags: Block, NoScripts
        // Description: A survival kit that has many other function like full assembler, refining (via assembling function) and sifting function too.
        // Last Updated: Jun, 24, 2024
        String goodModIoId = "4108543";
        Result<String[]> goodResult = modInfoService.scrapeModInformation(new ModIoMod(goodModIoId));
        assertTrue(goodResult.isSuccess());
        assertEquals("Multi-Function Survival Kit with Sifter", goodResult.getPayload()[0]);
        assertEquals("Block,NoScripts", goodResult.getPayload()[1]);
        assertEquals("""
                <!----><div id="" class="tw-absolute tw--top-20 tw-h-px tw-w-full" style="z-index: -1;"></div><!----><div class="tw-w-full tw-global--border-radius tw-relative tw-rounded-tr-none tw-border-transparent">
                 <div class="">
                  <!----><!---->
                  <div class="tw-flex tw-flex-col">
                   <!---->
                   <div class="tw-content tw-view-text">
                    <p>A survival kit that has many other function like full assembler, refining (via assembling function) and sifting function too.</p>
                   </div><!---->
                  </div>
                 </div><!---->
                </div>""", goodResult.getPayload()[2]);
        assertEquals("2024", goodResult.getPayload()[3]);
        assertEquals("--06-24", goodResult.getPayload()[4]);
        assertNull(goodResult.getPayload()[5]);
    }
}