package data;

import com.gearshiftgaming.se_mod_manager.data.ModFileRepository;
import com.gearshiftgaming.se_mod_manager.data.ModRepository;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ModFileRepositoryTest {

    private final ModRepository repository = new ModFileRepository();
    private final String goodModListPath = "src/test/resources/GoodModList.txt";
    private final String emptyModListPath = "src/test/resources/EmptyModList.txt";

    @Test
    void shouldGetValidModList() {
        Result<List<Mod>> result = repository.getModList(goodModListPath);
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldNotLoadFileThatDoesNotExist() {
        assertEquals("File does not exist.", repository.getModList("src/this/file/does/not/exist").getMessages().getLast());
        assertFalse(repository.getModList("src/this/file/does/not/exist").isSuccess());
    }

    @Test
    void shouldGetEmptyModList() {
        Result<List<Mod>> result = repository.getModList(emptyModListPath);
        assertTrue(result.getPayload().isEmpty());
    }

    @Test
    void shouldGet7modIds() {
        Result<List<Mod>> result = repository.getModList(goodModListPath);
        assertEquals(7, result.getPayload().size());
    }

    @Test
    void shouldGetNoModIds() {
        assertTrue(repository.getModListModIds(new File(emptyModListPath)).isEmpty());
    }
}
