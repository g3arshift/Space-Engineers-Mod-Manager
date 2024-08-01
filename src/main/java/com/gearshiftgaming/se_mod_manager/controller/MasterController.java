package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager;
import com.gearshiftgaming.se_mod_manager.backend.data.ModlistFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.UserDataService;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.MainWindowView;
import jakarta.xml.bind.JAXBException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Used to prepare basic environment setup for the application.
 */
public class MasterController {


    private final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
    private final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

    //TODO: Change the services to an abstract factory method

    public MasterController(Stage stage) throws IOException, XmlPullParserException, JAXBException {
        Logger logger = LogManager.getLogger(SpaceEngineersModManager.class);
        logger.info("Started application");

        Properties properties = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
            properties.load(input);
        } catch (IOException | NullPointerException e) {
            logger.error("Could not load SEMM.properties. " + e.getMessage());
            throw (e);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-window.fxml"));
        Parent root = loader.load();
        MainWindowView mainWindowView = loader.getController();
        mainWindowView.initController(properties, logger, stage, root);

        //TODO: Use Result in only on service layer, and pass it back up to MC for logging. Switch all the existing services over to this model.
        //TODO: Add validation to services for input AND USE RESULT
        //TODO: When we launch the app for the first time, walk the user through first making a save profile, then naming a new mod profile, then IMMEDIATELY save to file.
    }
}
