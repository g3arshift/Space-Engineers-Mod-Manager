package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SandboxConfigFileRepositoryTest {

    private final SandboxConfigRepository repository = new SandboxConfigFileRepository();

    @Test
    void shouldGetValidSandboxConfigFile() throws IOException {
        assertFalse(repository.getSandboxConfig("src/test/resources/TestSandbox_config.sbc").isBlank());
    }

    @Test
    void shouldNotLoadFileThatDoesNotExist() {
        assertThrows(IOException.class, () ->
                repository.getSandboxConfig("src/this/file/does/not/exist"));
    }

    @Test
    void shouldSaveToFile() throws IOException {
        File tempConfig = File.createTempFile("tempConfig", ".sbc");
        repository.saveSandboxConfig(tempConfig, "This is a modified string.");
        assertEquals("This is a modified string.", Files.readString(tempConfig.toPath()));
    }
}
