package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager;
import com.gearshiftgaming.se_mod_manager.backend.data.ModlistFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.MainWindowView;
import com.gearshiftgaming.se_mod_manager.frontend.view.ModProfileCreateView;
import com.gearshiftgaming.se_mod_manager.frontend.view.ModProfileView;
import jakarta.xml.bind.JAXBException;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Used to prepare basic environment setup for the application.
 */
public class ViewController {


    private final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
    private final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

    public ViewController(Stage stage) throws IOException, XmlPullParserException, JAXBException {
        Logger logger = LogManager.getLogger(SpaceEngineersModManager.class);
        logger.info("Started application");

        Properties properties = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
            properties.load(input);
        } catch (IOException | NullPointerException e) {
            logger.error("Could not load SEMM.properties. " + e.getMessage());
            throw (e);
        }

        BackendController backendController = new BackendFileController(new SandboxConfigFileRepository(),
                new ModlistFileRepository(), properties,
                new UserDataFileRepository());

        Result<UserConfiguration> userConfigurationResult = backendController.getUserData(new File(properties.getProperty("semm.userData.default.location")));
        UserConfiguration userConfiguration;

        if(userConfigurationResult.isSuccess()) {
            userConfiguration = userConfigurationResult.getPayload();
            logger.info(userConfigurationResult.getMessages());
        } else {
            userConfiguration = new UserConfiguration();
            logger.warn(userConfigurationResult.getMessages());
        }

        ObservableList<ModProfile> modProfiles = FXCollections.observableList(userConfiguration.getModProfiles());
        ObservableList<SaveProfile> saveProfiles = FXCollections.observableList(userConfiguration.getSaveProfiles());

        //Initialize the list we use to store log messages shown to the user
        ObservableList<LogMessage> userLog = FXCollections.observableArrayList(logMessage ->
                new Observable[]{
                        logMessage.viewableLogMessageProperty(),
                        logMessage.messageTypeProperty()
                });

        UiService uiService = new UiService(logger, userLog);
        uiService.log(userConfigurationResult);

        //Manually inject our controllers into our FXML so we can reuse the FXML UI between ModProfiles and SaveProfiles.
        FXMLLoader modProfileCreateLoader = new FXMLLoader(getClass().getResource("/view/create-profile.fxml"));
        ModProfileCreateView modProfileCreateView = new ModProfileCreateView();
        modProfileCreateLoader.setController(modProfileCreateView);
        Parent modProfileCreateRoot = modProfileCreateLoader.load();
        modProfileCreateView.initController(modProfileCreateRoot);

        FXMLLoader modProfileLoader = new FXMLLoader(getClass().getResource("/view/profile-window.fxml"));
        ModProfileView modProfileView = new ModProfileView();
        modProfileLoader.setController(modProfileView);
        Parent modProfileRoot = modProfileLoader.load();
        modProfileView.initController(modProfiles, modProfileRoot, uiService, modProfileCreateView, properties);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-window.fxml"));
        Parent mainViewRoot = loader.load();
        MainWindowView mainWindowView = loader.getController();
        mainWindowView.initController(properties, logger, backendController, userConfiguration, stage, mainViewRoot, modProfiles, saveProfiles, modProfileView, uiService);

        //TODO: Add validation to services for input AND USE RESULT
        //TODO: When we launch the app for the first time, walk the user through first making a save profile, then naming a new mod profile, then IMMEDIATELY save to file.
    }
}
