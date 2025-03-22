package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataSqliteRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UserDataSqliteRepositoryTest {
    private UserDataSqliteRepository userDataSqliteRepository;
    private final String databasePath = "Storage/Test/SEMM_Data_Test.db";

    @BeforeEach
    void setup() {
        userDataSqliteRepository = new UserDataSqliteRepository(databasePath);
    }

//    @AfterEach
//    void cleanup() throws IOException {
//        Files.delete(Path.of(databasePath));
//    }

    @Test
    void developmentTest() throws IOException {
        UserDataFileRepository userDataFileRepository = new UserDataFileRepository(new File("Storage/SEMM_Data.xml"));
        UserConfiguration tempConfig = userDataFileRepository.loadUserData().getPayload();
        userDataSqliteRepository.saveUserData(tempConfig);
        //Files.delete(Path.of(databasePath));
    }
}
