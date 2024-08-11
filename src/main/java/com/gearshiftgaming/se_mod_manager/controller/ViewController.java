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
import com.gearshiftgaming.se_mod_manager.frontend.view.*;
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
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * Used to prepare basic environment setup for the application.
 */
public class ViewController {


    private final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
    private final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

    public ViewController(Stage stage) throws IOException, XmlPullParserException, JAXBException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
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
                new UserDataFileRepository(),
                new File(properties.getProperty("semm.userData.default.location")));

        Result<UserConfiguration> userConfigurationResult = backendController.getUserData();
        UserConfiguration userConfiguration;

        if(userConfigurationResult.isSuccess()) {
            userConfiguration = userConfigurationResult.getPayload();
        } else {
            userConfiguration = new UserConfiguration();
            backendController.saveUserData(userConfiguration);
        }

        ObservableList<ModProfile> modProfiles = FXCollections.observableList(userConfiguration.getModProfiles());
        ObservableList<SaveProfile> saveProfiles = FXCollections.observableList(userConfiguration.getSaveProfiles());

        //Initialize the list we use to store log messages shown to the user
        ObservableList<LogMessage> userLog = FXCollections.observableArrayList(logMessage ->
                new Observable[]{
                        logMessage.viewableLogMessageProperty(),
                        logMessage.messageTypeProperty()
                });

        UiService uiService = new UiService(logger, userLog, modProfiles, saveProfiles, backendController);
        uiService.log(userConfigurationResult);

        //Manually inject our controllers into our FXML so we can reuse the FXML for the profile creation elsewhere, and have greater flexibility in controller injection.
        //View for adding a new Save Profile
        FXMLLoader saveListInputLoader = new FXMLLoader(getClass().getResource("/view/save-list-input.fxml"));
        SaveListInputView saveListInputView = new SaveListInputView();
        saveListInputLoader.setController(saveListInputView);
        Parent saveListInputRoot = saveListInputLoader.load();
        saveListInputView.initView(saveListInputRoot);

        //View for adding a new Mod Profile
        FXMLLoader modProfileManagerLoader = new FXMLLoader(getClass().getResource("/view/profile-input.fxml"));
        ModProfileInputView modProfileInputView = new ModProfileInputView();
        modProfileManagerLoader.setController(modProfileInputView);
        Parent modProfileCreateRoot = modProfileManagerLoader.load();
        modProfileInputView.initView(modProfileCreateRoot);

        //View for managing Save Profiles
        FXMLLoader saveManagerLoader = new FXMLLoader(getClass().getResource("/view/save-profile-manager.fxml"));
        SaveManagerView saveManagerView = new SaveManagerView();
        saveManagerLoader.setController(saveManagerView);
        Parent saveManagerRoot = saveManagerLoader.load();

        //View for managing Mod Profiles
        FXMLLoader modProfilerManagerLoader = new FXMLLoader(getClass().getResource("/view/mod-profile-manager.fxml"));
        ModProfileManagerView modProfileManagerView = new ModProfileManagerView();
        modProfilerManagerLoader.setController(modProfileManagerView);
        Parent modProfileRoot = modProfilerManagerLoader.load();

        //View for the primary application window
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-window.fxml"));
        Parent mainViewRoot = loader.load();
        MainWindowView mainWindowView = loader.getController();
        mainWindowView.initView(properties, logger,
                userConfiguration, stage,
                mainViewRoot, modProfileManagerView,
                saveManagerView, uiService);

        modProfileManagerView.initView(modProfileRoot, uiService, modProfileInputView, properties, mainWindowView);
        saveManagerView.initView(saveManagerRoot, uiService, saveListInputView, properties, mainWindowView);

        //TODO: Actually implement this. Function is empty at the moment.
        if(!userConfigurationResult.isSuccess()) {
            uiService.firstTimeSetup();
        }

        //TODO: When we launch the app for the first time, walk the user through first making a save profile, then renaming the default mod profile, then IMMEDIATELY save to file.
    }
}
