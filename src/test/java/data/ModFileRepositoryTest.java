package data;

import com.gearshiftgaming.se_mod_manager.backend.data.ModFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.ModRepository;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class ModFileRepositoryTest {

    private final ModRepository repository = new ModFileRepository();
    private final String goodModListPath = "src/test/resources/GoodModList.txt";
    private final String emptyModListPath = "src/test/resources/EmptyModList.txt";
    private final String blankLinesModListPath = "src/test/resources/BlankLinesModlist.txt";
    private final String duplicateModListPath = "src/test/resources/DuplicateModList.txt";

    @Test
    void shouldGetValidModList() {
        assertTrue(repository.getModList(goodModListPath).isSuccess());
    }

    @Test
    void shouldNotLoadFileThatDoesNotExist() {
        assertEquals("File does not exist.", repository.getModList("src/this/file/does/not/exist").getMessages().getLast());
        assertFalse(repository.getModList("src/this/file/does/not/exist").isSuccess());
    }

    @Test
    void shouldGetEmptyModList() {
        assertTrue(repository.getModList(emptyModListPath).getPayload().isEmpty());
    }

    @Test
    void shouldGetSevenModIds() {
        assertEquals(7, repository.getModList(goodModListPath).getPayload().size());
    }

    @Test
    void shouldIgnoreBlankLinesAndGetNoMods() {
        assertEquals(0, repository.getModList(blankLinesModListPath).getPayload().size());
    }

    @Test
    void shouldIgnoreDuplicateModsAndGetOneMod() {
        assertEquals(1, repository.getModList(duplicateModListPath).getPayload().size());
    }

    @Test
    void shouldGetNoModIds() {
        assertTrue(repository.getModListModIds(new File(emptyModListPath)).isEmpty());
    }
}
