package domain;

import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SandboxServiceTest {

    SandboxConfigRepository mockSandboxConfigRepository;
    SandboxService service;
    String fakeConfig;
    String fakePath;
    String goodConfigPath;
    String noModConfigPath;
    String noPath;
    List<Mod> modList;
    String expectedSandboxConfig;

    @BeforeEach
    void setup() throws IOException {
        mockSandboxConfigRepository = mock(SandboxConfigFileRepository.class);
        fakeConfig = "This is a fake config.";
        service = new SandboxService(mockSandboxConfigRepository, mock(Logger.class));
        fakePath = "src/test/fake";
        noPath = "src/there/is/no/path";
        goodConfigPath = "src/test/resources/GoodSandbox_config.sbc";
        noModConfigPath = "src/test/resources/NoModsSandbox_config.sbc";

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
    void shouldGetFakeConfig() {
        Result<File> result = new Result<>(ResultType.SUCCESS);
        result.setPayload(new File(fakePath));

        when(mockSandboxConfigRepository.getSandboxConfig(fakePath)).thenReturn(result);
        assertTrue(service.getSandboxConfigFromFile(fakePath).isSuccess());
    }

    @Test
    void shouldGetFileDoesNotExist() {
        Result<File> result = new Result<>();
        result.setPayload(new File(noPath));
        result.addMessage("File does not exist.", ResultType.INVALID);
        when(mockSandboxConfigRepository.getSandboxConfig(noPath)).thenReturn(result);

        assertEquals("File does not exist.", service.getSandboxConfigFromFile(noPath).getMessages().getLast());
    }

    @Test
    void shouldGetIncorrectFileType() {
        Result<File> result = new Result<>();
        result.setPayload(new File(noPath));
        result.addMessage("Incorrect file type selected. Please select a .sbc file.", ResultType.INVALID);
        when(mockSandboxConfigRepository.getSandboxConfig(noPath)).thenReturn(result);

        assertEquals("Incorrect file type selected. Please select a .sbc file.", service.getSandboxConfigFromFile(noPath).getMessages().getLast());
    }


    @Test
    void shouldSuccessfullyInjectModsToConfigWithAlreadyExistingMods() throws IOException {
        String modifiedConfig = service.addModsToSandboxConfigFile(new File(goodConfigPath), modList);
        assertEquals(expectedSandboxConfig, modifiedConfig);
    }

    @Test
    void shouldSuccessfullyInjectModsToConfigWithNoPreexistingMods() throws IOException {
        String modifiedConfig = service.addModsToSandboxConfigFile(new File(noModConfigPath), modList);
        assertEquals(expectedSandboxConfig, modifiedConfig);
    }

    @Test
    void shouldSuccessfullySaveConfigFile() throws IOException {
        Result<Boolean> result = new Result<>(ResultType.SUCCESS);
        result.setPayload(true);
        when(mockSandboxConfigRepository.saveSandboxConfig(goodConfigPath, fakeConfig)).thenReturn(result);

        service.saveSandboxConfig(goodConfigPath, fakeConfig);
        assertTrue(result.isSuccess());
    }
}
