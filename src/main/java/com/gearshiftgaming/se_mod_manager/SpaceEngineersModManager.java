package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.backend.data.ModFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.controller.MainController;
import com.gearshiftgaming.se_mod_manager.frontend.models.MessageType;
import jakarta.xml.bind.JAXBException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class SpaceEngineersModManager extends Application {

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        launch(args);
/*
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        List<Mod> modList = new ArrayList<>();
        ModManagerView modManagerView = new ModManagerView();
        SandboxService sandboxService = new SandboxService(new SandboxConfigFileRepository(), logger);

        final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
        final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

        if (!Files.isDirectory(Path.of(APP_DATA_PATH))) {
            logger.warn("Space Engineers save location not found.");
        }

        ModService modService = new ModService(new ModFileRepository(), logger);


        ModManagerController modManagerController = new ModManagerController(logger, modList, modManagerView, modService, sandboxService, DESKTOP_PATH, APP_DATA_PATH);

        //Get the party started
        modManagerController.injectModList();
        logger.info("Application finished. Closing.");
*/
    }

    @Override
    public void start(Stage primaryStage) throws IOException, XmlPullParserException, ParserConfigurationException, JAXBException {
        final Logger logger = LogManager.getLogger(SpaceEngineersModManager.class);

        logger.info("Started application");

        SandboxService sandboxService = new SandboxService(new SandboxConfigFileRepository(), logger);
        ModlistService modlistService = new ModlistService(new ModFileRepository(), logger);


        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
        Parent root = loader.load();
        final MainController mainController = loader.getController();
        UiService uiService = new UiService(logger, sandboxService, modlistService, mainController, root, 950, 350);
        mainController.initController(uiService.getApplicationLog(), uiService.getLogger());

        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.saveXMLTest();

        //TODO: Dev. Remove
        uiService.log("Test Message 1: WARN\nTHIS IS LINE 2\nTHIS IS LINE 3", MessageType.WARN);
        uiService.log("Test Message 2: ERROR", MessageType.ERROR);
        uiService.log("Test Message 3: INFO", MessageType.INFO);
        uiService.log("Test Message 4: UNKNOWN", MessageType.UNKNOWN);

        uiService.prepareStage(primaryStage);
        primaryStage.show();
        //TODO: Set a context menu on the menu header ONLY per this https://stackoverflow.com/questions/47786125/how-to-add-a-context-menu-to-an-empty-tableviews-header-row. This may however, not be a good idea. Check the notes for the todo. Fourth item, sub-item of item 3. Also use this to inject checkboxes!
    }
}