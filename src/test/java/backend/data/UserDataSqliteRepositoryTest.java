package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataSqliteRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UserDataSqliteRepositoryTest {
    private UserDataSqliteRepository userDataSqliteRepository;
    private final String databasePath = "Storage/Test/SEMM_Data_Test.db";
    private final String changelogPath = "Database/base_changelog.xml";

    @BeforeEach
    void setup() {
        userDataSqliteRepository = new UserDataSqliteRepository(databasePath, changelogPath);
    }

//    @AfterEach
//    void cleanup() throws IOException {
//        Files.delete(Path.of(databasePath));
//    }

    @Test
    void developmentTest() {
        UserDataFileRepository tempRepo = new UserDataFileRepository(new File("Storage/SEMM_Data.xml"));
        UserConfiguration tempConfig = tempRepo.loadUserData().getPayload();
        userDataSqliteRepository.saveUserData(tempConfig);
    }
}
