package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager;
import com.gearshiftgaming.se_mod_manager.backend.data.ModlistFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.UserDataService;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import jakarta.xml.bind.JAXBException;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class MasterController {

    private final Logger logger;

    private final Stage primaryStage;
    private final SandboxService sandboxService;

    private final ModlistService modlistService;

    private final UiService uiService;

    private final UserDataService userDataService;

    private final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
    private final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";


    public MasterController(Stage stage) throws IOException, XmlPullParserException, JAXBException {
        logger = LogManager.getLogger(SpaceEngineersModManager.class);
        logger.info("Started application");

        this.primaryStage = stage;

        Properties properties = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
            properties.load(input);
        } catch (IOException | NullPointerException e) {
            logger.error("Could not load SEMM.properties. " + e.getMessage());
            throw (e);
        }

        //TODO: Use Result in only on service layer, and pass it back up to MC for logging. Switch all the existing services over to this model.
        //TODO: Add validation to services for input AND USE RESULT
        //TODO: When we launch the app for the first time, walk the user through first making a save profile, then naming a new mod profile, then IMMEDIATELY save to file.
        sandboxService = new SandboxService(new SandboxConfigFileRepository());
        modlistService = new ModlistService(new ModlistFileRepository(), properties);
        userDataService = new UserDataService(new UserDataFileRepository());

        //Load user data
        Result<UserConfiguration> userConfigurationResult = userDataService.getUserData(new File(properties.getProperty("semm.userData.default.location")));
        UserConfiguration userConfiguration = userConfigurationResult.getPayload();

        uiService = new UiService(userConfiguration,
                Integer.parseInt(properties.getProperty("semm.mainView.resolution.minWidth")),
                Integer.parseInt(properties.getProperty("semm.mainView.resolution.minHeight")));

        if (userConfigurationResult.isSuccess()) {
            log(userConfigurationResult.getMessages().getLast(), MessageType.INFO);
        } else {
            log(userConfigurationResult.getMessages().getLast(), MessageType.ERROR);
        }

        if (!Files.isDirectory(Path.of(APP_DATA_PATH))) {
            log("Space Engineers save location not found.", MessageType.WARN);
        }

        uiService.prepareMainStage(primaryStage);
    }

    private void log(String message, MessageType messageType) {
        LogMessage logMessage = new LogMessage(message, messageType, logger);
        uiService.addMessageToLog(logMessage);
    }

    private <T> void log(Result<T> result) {
        MessageType messageType;
        switch (result.getType()) {
            case INVALID -> messageType = MessageType.WARN;
            case CANCELLED, NOT_INITIALIZED, FAILED -> messageType = MessageType.ERROR;
            default -> messageType = MessageType.INFO;
        }
        log(result.getMessages().getLast(), messageType);
    }
}
