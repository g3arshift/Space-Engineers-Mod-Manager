package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager;
import com.gearshiftgaming.se_mod_manager.backend.data.ModFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.UserDataService;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.MessageType;
import jakarta.xml.bind.JAXBException;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;

public class MasterController {

    private final Logger logger;

    private final Stage primaryStage;
    private final SandboxService sandboxService;

    private final ModlistService modlistService;

    private final UiService uiService;

    private final UserDataService userDataService;

    public MasterController(Stage stage) throws IOException, JAXBException, XmlPullParserException {
        logger = LogManager.getLogger(SpaceEngineersModManager.class);
        logger.info("Started application");

        this.primaryStage = stage;

        //TODO: Change the sevice args from wanting a Logger to wanting nothing, and instead having their functions create a Result object, and passing it back up to the
        // main controller and using the UI service to log it. Or maybe don't do this? It's late, and I should think on this tomorrow.
        sandboxService = new SandboxService(new SandboxConfigFileRepository(), logger);
        modlistService = new ModlistService(new ModFileRepository(), logger);

        //Replace the ints with a system property
        uiService = new UiService(logger, 950, 350);
        userDataService = new UserDataService();

        //TODO: Dev. Remove
        uiService.log("Test Message 1: WARN\nTHIS IS LINE 2\nTHIS IS LINE 3", MessageType.WARN);
        uiService.log("Test Message 2: ERROR", MessageType.ERROR);
        uiService.log("Test Message 3: INFO", MessageType.INFO);
        uiService.log("Test Message 4: UNKNOWN", MessageType.UNKNOWN);

        uiService.prepareStage(primaryStage);
    }
}
