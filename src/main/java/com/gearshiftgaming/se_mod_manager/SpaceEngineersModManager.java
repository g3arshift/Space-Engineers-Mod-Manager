package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.controller.MasterController;
import jakarta.xml.bind.JAXBException;
import javafx.application.Application;
import javafx.stage.Stage;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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
    public void start(Stage primaryStage) throws IOException, XmlPullParserException, JAXBException {

        MasterController masterController = new MasterController(primaryStage);
        primaryStage.show();
        //TODO: Set a context menu on the menu header ONLY per this https://stackoverflow.com/questions/47786125/how-to-add-a-context-menu-to-an-empty-tableviews-header-row. This may however, not be a good idea. Check the notes for the todo. Fourth item, sub-item of item 3. Also use this to inject checkboxes!
    }
}