package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataSqliteRepository;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

public class UserDataSqliteRepositoryTest {
    private UserDataSqliteRepository userDataSqliteRepository;
    private final String databasePath = ":memory:";

    @BeforeEach
    void setup() throws IOException {
        userDataSqliteRepository = new UserDataSqliteRepository(databasePath);
    }


}
