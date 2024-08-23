package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ModlistFileRepositoryTest {

    private final ModlistRepository repository = new ModlistFileRepository();
    private final String goodModListPath = "src/test/resources/GoodModList.txt";
    private final String emptyModListPath = "src/test/resources/EmptyModList.txt";

	@Test
    void shouldGetValidModList() throws IOException {
        assertFalse(repository.getModList(new File(goodModListPath)).isEmpty());
    }

    @Test
    void shouldNotLoadFileThatDoesNotExist() {
        assertThrows(IOException.class, () ->
                repository.getModList(new File("src/this/file/does/not/exist")));
    }

    @Test
    void shouldGetEmptyModList() throws IOException {
        assertTrue(repository.getModList(new File(emptyModListPath)).isEmpty());
    }

    @Test
    void shouldGetSevenModIds() throws IOException {
        assertEquals(7, repository.getModList(new File(goodModListPath)).size());
    }

    @Test
    void shouldIgnoreBlankLinesAndGetNoMods() throws IOException {
		String blankLinesModListPath = "src/test/resources/BlankLinesModlist.txt";
		assertEquals(0, repository.getModList(new File(blankLinesModListPath)).size());
    }

    @Test
    void shouldIgnoreDuplicateModsAndGetOneMod() throws IOException {
		String duplicateModListPath = "src/test/resources/DuplicateModList.txt";
		assertEquals(1, repository.getModList(new File(duplicateModListPath)).size());
    }

    @Test
    void shouldGetNoModIds() throws IOException {
        assertTrue(repository.getModList(new File(emptyModListPath)).isEmpty());
    }
}
