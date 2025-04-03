package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataSqliteRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

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
        UserConfiguration tempConfig = userDataFileRepository.loadAllData().getPayload();
        for (ModListProfile modListProfile : tempConfig.getModListProfilesBasicInfo()) {
            for (int i = 0; i < modListProfile.getModList().size(); i++) {
                modListProfile.getModList().get(i).setLoadPriority(i + 1);
            }
        }
        userDataFileRepository.saveAllData(tempConfig);
        userDataSqliteRepository.saveAllData(tempConfig);
        //Files.delete(Path.of(databasePath));

        Result<UserConfiguration> loadedResult = userDataSqliteRepository.loadAllData();
    }
}
