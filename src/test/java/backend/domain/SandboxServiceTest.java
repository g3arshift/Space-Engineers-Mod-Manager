package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.sandbox.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.sandbox.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.SteamMod;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.ResultType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;


//TODO: Replace real paths with mocks
public class SandboxServiceTest {
    SandboxService service;
    String fakeConfig;
    String fakePath;
    String goodConfigPath;
    String noModConfigPath;
    List<Mod> modList;
    String expectedSandboxConfig;

    String badSavePath;

    String illegalSavePath;

    @TempDir
    public File tempDir;

    @BeforeEach
    void setup() throws IOException {
        service = new SandboxService(new SandboxConfigFileRepository());
        fakeConfig = "This is a fake config.";
        fakePath = "src/test/fake";
        goodConfigPath = "src/test/resources/GoodSandbox_config.sbc";
        noModConfigPath = "src/test/resources/NoModsSandbox_config.sbc";
        badSavePath = "src/test/resources/GoodModList.txt";
        illegalSavePath = "src/test/resources/Save$#%.sbc";

        modList = new ArrayList<>();
        modList.add(new SteamMod("2777644246")); //Binoculars
        modList.getFirst().setFriendlyName("Binoculars");
        modList.getFirst().setPublishedServiceName("Steam");

        modList.add(new SteamMod("2668820525")); //TouchScreenAPI
        modList.get(1).setFriendlyName("TouchScreenAPI");
        modList.get(1).setPublishedServiceName("Steam");

        modList.add(new SteamMod("1902970975")); //Assertive Combat Systems
        modList.getLast().setFriendlyName("Assertive Combat Systems");
        modList.getLast().setPublishedServiceName("Steam");

        expectedSandboxConfig = Files.readString(Path.of("src/test/resources/ExpectedTestResult_Sandbox_config.sbc"));
    }

    @Test
    void shouldGetGoodButFakeConfig() throws IOException {
        Result<String> result = service.getSandboxFromFile(new File(goodConfigPath));

        assertEquals(Files.readString(Path.of(goodConfigPath)), result.getPayload());
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldGetModListFromSandboxConfig() {
        Result<List<Mod>> result = service.getModListFromSandboxConfig(new File(goodConfigPath));
        assertTrue(result.isSuccess());
        List<Mod> modList = result.getPayload();
        assertEquals(153, modList.size());
    }

    @Test
    void shouldGetEmptyModList() {
        Result<List<Mod>> result = service.getModListFromSandboxConfig(new File(noModConfigPath));
        assertEquals(ResultType.INVALID, result.getType());
        assertNull(result.getPayload());
        assertEquals("There are no mods in this save!", result.getCurrentMessage());
    }

    @Test
    void shouldNotFindModSectionInConfig() {
        Result<List<Mod>> result = service.getModListFromSandboxConfig(new File("src/test/resources/MissingModSection.sbc"));
        assertFalse(result.isSuccess());
        assertNull(result.getPayload());
        assertEquals(ResultType.FAILED, result.getType());
        assertEquals("No valid mod section found.", result.getCurrentMessage());
    }

    @Test
    void shouldGetFileDoesNotExist() throws IOException {
        Result<String> result = service.getSandboxFromFile(new File(fakePath));
        assertFalse(result.isSuccess());
        assertEquals("File does not exist.", result.getCurrentMessage());
    }

    @Test
    void shouldGetIncorrectFileExtension() throws IOException {
        Result<String> result = service.getSandboxFromFile(new File(badSavePath));
        assertFalse(result.isSuccess());
        assertEquals("Incorrect file type selected. Please select a .sbc file.", result.getCurrentMessage());
    }

    //TODO: We need to have our mod injection tests now also check for the Sandbox.sbc file injection.

    @Test
    void shouldSuccessfullyInjectModsToConfigWithAlreadyExistingMods() throws IOException {
        Result<String> modifiedConfigResult = service.injectModsIntoSandboxConfig(new File(goodConfigPath), modList);
        assertTrue(modifiedConfigResult.isSuccess());
        assertEquals(expectedSandboxConfig, modifiedConfigResult.getPayload());
    }

    @Test
    void shouldSuccessfullyInjectModsToConfigWithNoPreexistingMods() throws IOException {
        Result<String> modifiedConfigResult = service.injectModsIntoSandboxConfig(new File(noModConfigPath), modList);
        assertTrue(modifiedConfigResult.isSuccess());
        assertEquals(expectedSandboxConfig, modifiedConfigResult.getPayload());
    }

    @Test
    void shouldSuccessfullySaveConfigFile() throws IOException {
        Result<Void> result = service.saveSandboxConfigToFile(tempDir.getPath() + "/Sandbox_config.sbc", fakeConfig);
        assertTrue(result.isSuccess());
        assertEquals("Successfully saved sandbox config.", result.getCurrentMessage());
    }

    @Test
    void shouldAppendExtensionToSavePathWithIncorrectExtensionAndWriteCorrectly() throws IOException {
        Result<Void> result = service.saveSandboxConfigToFile(tempDir.getPath() + "/Sandbox_config.txt", "Save this config!");
        assertTrue(result.isSuccess());
        assertEquals("File extension .txt not permitted. Changing to .sbc.", result.getMessages().getFirst());
    }

    @Test
    void savingSandboxConfigWillNotAcceptFilePathWithIllegalCharacters() throws IOException {
        Result<Void> result = service.saveSandboxConfigToFile(illegalSavePath, "Save this config!");
        assertFalse(result.isSuccess());
        assertEquals("Save path or name contains invalid characters.", result.getCurrentMessage());
    }

    @Test
    void shouldChangeConfigSessionName() throws IOException {
        SaveProfile saveProfile = new SaveProfile();
        saveProfile.setSaveName("Test");
        saveProfile.setSavePath(tempDir.getPath() + "/Sandbox_config.sbc");
        Result<Void> result = service.changeConfigSessionName(fakeConfig, saveProfile, new int[]{0, 4});
        assertTrue(result.isSuccess());
        String change = Files.readString(Path.of(tempDir.getPath() + "/Sandbox_config.sbc"));
        assertTrue(change.startsWith("Test"));
    }

    @Test
    void shouldChangeSandboxSessionName() throws IOException {
        SaveProfile saveProfile = new SaveProfile();
        saveProfile.setSaveName("Test");
        saveProfile.setSavePath(tempDir.getPath() + "/Sandbox_config.sbc");
        Files.createFile(Path.of(tempDir.getPath() + "/Sandbox.sbc"));
        Result<Void> result = service.changeSandboxSessionName(fakeConfig, saveProfile, new int[]{0, 4});

        assertTrue(result.isSuccess());
        String change = Files.readString(Path.of(tempDir.getPath() + "/Sandbox.sbc"));
        assertTrue(change.startsWith("Test"));
    }
}
