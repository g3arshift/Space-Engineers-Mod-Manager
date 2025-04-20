package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataSqliteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserDataSqliteRepositoryTest {
    private UserDataSqliteRepository userDataSqliteRepository;
    private final String databasePath = ":memory:";

    @BeforeEach
    void setup() {
        userDataSqliteRepository = new UserDataSqliteRepository(databasePath);
    }


}
