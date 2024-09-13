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
        assertFalse(repository.getSteamModList(new File(goodModListPath)).isEmpty());
    }

    @Test
    void shouldNotLoadFileThatDoesNotExist() {
        assertThrows(IOException.class, () ->
                repository.getSteamModList(new File("src/this/file/does/not/exist")));
    }

    @Test
    void shouldGetEmptyModList() throws IOException {
        assertTrue(repository.getSteamModList(new File(emptyModListPath)).isEmpty());
    }

    @Test
    void shouldGetSevenModIds() throws IOException {
        assertEquals(7, repository.getSteamModList(new File(goodModListPath)).size());
    }

    @Test
    void shouldIgnoreBlankLinesAndGetNoMods() throws IOException {
		String blankLinesModListPath = "src/test/resources/BlankLinesModlist.txt";
		assertEquals(0, repository.getSteamModList(new File(blankLinesModListPath)).size());
    }

    @Test
    void shouldIgnoreDuplicateModsAndGetOneMod() throws IOException {
		String duplicateModListPath = "src/test/resources/DuplicateModList.txt";
		assertEquals(1, repository.getSteamModList(new File(duplicateModListPath)).size());
    }

    @Test
    void shouldGetNoModIds() throws IOException {
        assertTrue(repository.getSteamModList(new File(emptyModListPath)).isEmpty());
    }
}
