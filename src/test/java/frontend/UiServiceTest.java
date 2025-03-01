package frontend;

import com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager;
import com.gearshiftgaming.se_mod_manager.backend.models.LogMessage;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;
//TODO: IMPLEMENT

public class UiServiceTest {

    UiService uiService;

    @BeforeEach
    void setup() {
        Logger logger = LogManager.getLogger(SpaceEngineersModManager.class);
        ObservableList<LogMessage> userLog = FXCollections.observableList(new ArrayList<>());
        //uiService = new UiService(mock(Logger.class), mock((ObservableList<LogMessage>)));
    }
}
