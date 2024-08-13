package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


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
        modList.add(new Mod("2777644246")); //Binoculars
        modList.getFirst().setFriendlyName("Binoculars");
        modList.getFirst().setPublishedServiceName("Steam");

        modList.add(new Mod("2668820525")); //TouchScreenAPI
        modList.get(1).setFriendlyName("TouchScreenAPI");
        modList.get(1).setPublishedServiceName("Steam");

        modList.add(new Mod("1902970975")); //Assertive Combat Systems
        modList.getLast().setFriendlyName("Assertive Combat Systems");
        modList.getLast().setPublishedServiceName("Steam");

        expectedSandboxConfig = Files.readString(Path.of("src/test/resources/ExpectedTestResult_Sandbox_config.sbc"));
    }

    @Test
    void shouldGetGoodButFakeConfig() throws IOException {
        Result<String> result = service.getSandboxConfigFromFile(new File(goodConfigPath));

        assertEquals(Files.readString(Path.of(goodConfigPath)), result.getPayload());
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldGetFileDoesNotExist() throws IOException {
        Result<String> result = service.getSandboxConfigFromFile(new File(fakePath));
        assertFalse(result.isSuccess());
        assertEquals("File does not exist.", result.getMessages().getLast());
    }

    @Test
    void shouldGetIncorrectFileExtension() throws IOException {
        Result<String> result = service.getSandboxConfigFromFile(new File(badSavePath));
        assertFalse(result.isSuccess());
        assertEquals("Incorrect file type selected. Please select a .sbc file.", result.getMessages().getLast());
    }


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
        Result<Boolean> result = service.saveSandboxConfig(tempDir.getPath()+"/Sandbox_config.sbc", fakeConfig);
        assertTrue(result.isSuccess());
        assertEquals("Successfully saved sandbox config.", result.getMessages().getLast());
    }

    @Test
    void shouldAppendExtensionToSavePathWithIncorrectExtensionAndWriteCorrectly() throws IOException {
        Result<Boolean> result = service.saveSandboxConfig(tempDir.getPath()+"/Sandbox_config.txt", "Save this config!");
        assertTrue(result.isSuccess());
        assertEquals("File extension .txt not permitted. Changing to .sbc.", result.getMessages().getFirst());
    }

    @Test
    void savingSandboxConfigWillNotAcceptFilePathWithIllegalCharacters() throws IOException {
        Result<Boolean> result = service.saveSandboxConfig(illegalSavePath, "Save this config!");
        assertFalse(result.isSuccess());
        assertEquals("Save path or name contains invalid characters.", result.getMessages().getLast());
    }
}
