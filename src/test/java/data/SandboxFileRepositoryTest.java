package data;

import com.gearshiftgaming.se_mod_manager.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.data.SandboxConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SandboxFileRepositoryTest {

    private final SandboxConfigRepository repository = new SandboxConfigFileRepository();

    @TempDir
    public File tempDir;

    //TODO: Do the tests with short strings, not a full config file, except for reading one.
    @Test
    void shouldGetValidSandboxConfigFile() {
        assertTrue(repository.getSandboxConfig("src/test/resources/TestSandbox_config.sbc").isSuccess());
    }

    @Test
    void shouldNotLoadFileThatDoesNotExist() {
        assertEquals("File does not exist.", repository.getSandboxConfig("src/this/file/does/not/exist").getMessages().getLast());
        assertFalse(repository.getSandboxConfig("src/this/file/does/not/exist").isSuccess());
    }

    @Test
    void shouldNotLoadFileWithIncorrectExtension() {
        assertEquals("Incorrect file type selected. Please select a .sbc file.", repository.getSandboxConfig("src/test/resources/GoodModList.txt").getMessages().getLast());
        assertFalse(repository.getSandboxConfig("src/test/resources/GoodModList.txt").isSuccess());
    }

    @Test
    void shouldSaveToFile() throws IOException {
        File tempConfig = File.createTempFile("tempConfig", ".sbc");
        assertTrue(repository.saveSandboxConfig(tempConfig.getPath(), "This is a modified string.").isSuccess());
        assertEquals("This is a modified string.", Files.readString(tempConfig.toPath()));
    }

    @Test
    void shouldAppendExtensionToSavePathWithIncorrectExtensionAndWriteCorrectly() throws IOException {
        Path tempFile = Files.createFile(tempDir.toPath().resolve("configFile.txt"));
        Files.writeString(tempFile, "This is the original string.");
        repository.saveSandboxConfig(tempFile.toString(), "This is a modified string.");

        Path tempPath = Path.of(tempDir + "/configFile.sbc");
        assertTrue(Files.exists(tempPath));
        assertEquals("This is a modified string.", Files.readString(tempPath));
    }

    @Test
    void savingSandboxConfigWillNotAcceptFilePathWithIllegalCharacters() throws IOException {
        String badSavePath = "/src/test/resources/test?/<.sbc";
        assertEquals("File path or name contains invalid characters.", repository.saveSandboxConfig(badSavePath, "This should not save.").getMessages().getLast());
    }
}
