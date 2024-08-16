package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class SandboxConfigFileRepositoryTest {

    private final SandboxConfigRepository repository = new SandboxConfigFileRepository();

    @Test
    void shouldGetValidSandboxConfigFile() throws IOException {
        assertFalse(repository.getSandboxInfo(new File("src/test/resources/TestSandbox_config.sbc")).isBlank());
    }

    @Test
    void shouldNotLoadFileThatDoesNotExist() {
        assertThrows(IOException.class, () ->
                repository.getSandboxInfo(new File("src/this/file/does/not/exist")));
    }

    @Test
    void shouldSaveToFile() throws IOException {
        File tempConfig = File.createTempFile("tempConfig", ".sbc");
        repository.saveSandboxInfo(tempConfig, "This is a modified string.");
        assertEquals("This is a modified string.", Files.readString(tempConfig.toPath()));
    }
}
