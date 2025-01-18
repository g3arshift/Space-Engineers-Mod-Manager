package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModInfoService;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModInfoServiceTest {

    ModlistRepository modlistRepository;

    Properties properties;

    List<String> modlistUrls;

    ModInfoService modInfoService;

    @BeforeEach
    void setup() throws IOException {
        properties = new Properties();

        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
            properties.load(input);
        }

        modlistRepository = mock(ModlistRepository.class);

        modInfoService = new ModInfoService(modlistRepository, properties);

        modlistUrls = new ArrayList<>();
        modlistUrls.add("https://steamcommunity.com/workshop/filedetails/?id=3329381499"); //Cross Barred Windows (Large Grid Update)
        modlistUrls.add("https://steamcommunity.com/workshop/filedetails/?id=2777644246"); //Binoculars
        modlistUrls.add("https://steamcommunity.com/workshop/filedetails/?id=2668820525"); //TouchScreenAPI
        modlistUrls.add("https://steamcommunity.com/workshop/filedetails/?id=1902970975"); //Assertive Combat Systems
    }

    //TODO: These need to go into the repository.
//    @Test
//    void shouldGetThreeSteamModsFromFile() {
//
//    }
//
//    @Test
//    void shouldNotGetSteamModsFromFile() {
//
//    }
//
//    @Test
//    void shouldGetThreeModIoModsFromFile() {
//
//    }
//
//    @Test
//    void shouldNotGetModIoModsFromFile() {
//
//    }

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
        for(int i = 0; i < goodResults.size(); i++) {
            assertTrue(goodResults.get(i).isSuccess());
            assertEquals("Successfully grabbed mod ID.", goodResults.get(i).getCurrentMessage());
            assertEquals(expectedIds.get(i), goodResults.get(i).getPayload());
        }
    }

    @Test
    void shouldGetModIoId() throws IOException {
        String modIoUrl = "assault-weapons-pack1"; //Assault Weapons Pack
        Result<String> goodResult = modInfoService.getModIoIdFromUrl(modIoUrl);
        assertTrue(goodResult.isSuccess());
        assertEquals("451208", goodResult.getPayload());
    }

    @Test
    void shouldNotGetModIoId() throws IOException {
        String badModIoUrl = "Wqyk3rJ";
        Result<String> badResult = modInfoService.getModIoIdFromUrl(badModIoUrl);
        assertFalse(badResult.isSuccess());
        assertEquals("Invalid Mod.io URL entered!", badResult.getCurrentMessage());
    }

    @Test
    void shouldGetSteamNotFoundError() {
        String badModUrl = "https://steamcommunity.com/sharedfiles/filedetails/?id=340sdasdsadasds";
    }

    @Test
    void shouldGetSteamNotModItemError() {
        String screenShotUrl = "https://steamcommunity.com/sharedfiles/filedetails/?id=2396152929";
    }

    @Test
    void shouldGetSteamCollectionError() {
        String steamCollectionUrl = "https://steamcommunity.com/sharedfiles/filedetails/?id=3408899159";
    }

    @Test
    void shouldGetSteamNotIsAScriptNotAModError() {
        String scriptItemUrl = "https://steamcommunity.com/sharedfiles/filedetails/?id=479678389";
    }

    @Test
    void shouldGetValidSteamModWithNoTags() {
        String noTagsModUrl = "https://steamcommunity.com/workshop/filedetails/?id=1100741659";
    }

    @Test
    void shouldGetValidSteamMod() {
        //The steam mod should have the following traits:
        // Tags: NoScripts
        // Name: Cross Barred Windows (Large Grid Update)
        // Description (in base 64 because it's big): SSBtYWRlIHRoaXMgbW9kIGJlY2F1c2UgSSBsb3ZlIHRoZSBjb21iaW5hdGlvbiBvZiBwbGFjaW5nIGEgYmFycmVkIHdpbmRvdyB3aXRoIGEgOTAgZGVncmVlIG9mZnNldCBvbnRvIGFub3RoZXIgYmFycmVkIHdpbmRvdy4gTXkgb25seSBwcm9ibGVtIHdpdGggaXQgaXMgdGhhdCBpdCB0YWtlcyB1cCAyIGJsb2NrIHNwYWNlcywgc28gSSBtYWRlIHRoaXMuIEl0J3MgcHVyZWx5IGNvc21ldGljIGFuZCBwcmFjdGljYWxseSBpZGVudGljYWwgdG8gdGhlIGJhcnJlZCB3aW5kb3dzLiBUaGUgYmxvY2tzIGFyZSBpbnRlZ3JhdGVkIGludG8gdGhlIHByb2dyZXNzaW9uIGFuZCBibG9jayB2YXJpYXRpb24gZ3JvdXBzLgoKQmVjYXVzZSBhc3NldHMgZnJvbSBpdCBhcmUgdXNlZCwgdGhlIFdhc3RlbGFuZCBETEMgaXMgcmVxdWlyZWQgdG8gdXNlIGl0LgoKCkFueXdheSBJIGhvcGUgeW91IGhhdmUgZnVuIHdpdGggbXkgZmlyc3QgbW9kZWxpbmcgZXhwZXJpZW5jZSBpbiBibGVuZGVyIDopCgpGb3IgYW55IGJ1Z3MgaGl0IG1lIHVwIGluIHRoZSBjb21tZW50cwoKLSBHcmViYW50b24xMjM0
        // Last Updated: Just check it's not null
        String steamModUrl = modlistUrls.getFirst();
    }

    @Test
    void shouldGetModIoTimeoutError() {

    }

    @Test
    void shouldGetModIoValidMod() {
        //The Mod.io Mod should have the following traits:
        // Name: Multi-Function Survival Kit with Sifter
        // Tags: Block, NoScripts
        // Description: A survival kit that has many other function like full assembler, refining (via assembling function) and sifting function too.
        // Last Updated: Jun, 24, 2024
    }
}