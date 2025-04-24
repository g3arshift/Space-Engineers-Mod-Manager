package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.SQLiteConnectionFactory;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataSqliteRepository;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserDataSqliteRepositoryTest {
    private UserDataSqliteRepository userDataSqliteRepository;
    private Jdbi SQLITE_DB;
    private final String DATABASE_PATH = "src/test/resources/DataStorage/TestData.db";

    @BeforeEach
    void setup() throws IOException {
        userDataSqliteRepository = new UserDataSqliteRepository(DATABASE_PATH);
        SQLITE_DB = Jdbi.create(new SQLiteConnectionFactory("jdbc:sqlite:" + DATABASE_PATH));
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.delete(Path.of(DATABASE_PATH));
    }

    @Test
    void shouldCreateDatabaseSchema() {
        List<String> tableNames = SQLITE_DB.withHandle(handle -> handle.createQuery("SELECT name FROM sqlite_master WHERE type='table'")
                .mapTo(String.class)
                .list());
        tableNames.remove("sqlite_sequence");
        assertEquals(11, tableNames.size());
        assertTrue(tableNames.contains("mod"));
        assertTrue(tableNames.contains("mod_category"));
        assertTrue(tableNames.contains("mod_list_profile"));
        assertTrue(tableNames.contains("mod_list_profile_mod"));
        assertTrue(tableNames.contains("mod_modified_path"));
        assertTrue(tableNames.contains("modio_mod"));
        assertTrue(tableNames.contains("save_profile"));
        assertTrue(tableNames.contains("steam_mod"));
        assertTrue(tableNames.contains("user_configuration"));
        assertTrue(tableNames.contains("user_configuration_mod_list_profile"));
        assertTrue(tableNames.contains("user_configuration_save_profile"));
    }
}
