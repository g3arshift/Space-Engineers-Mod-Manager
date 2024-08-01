package com.gearshiftgaming.se_mod_manager.frontend.view;

import atlantafx.base.theme.*;
import com.gearshiftgaming.se_mod_manager.backend.data.ModlistFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveStatus;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import com.gearshiftgaming.se_mod_manager.controller.BackendController;
import com.gearshiftgaming.se_mod_manager.controller.BackendFileController;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.LogCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModCell;
import jakarta.xml.bind.JAXBException;
import javafx.application.Application;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.javafx.Icon;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Responsible for the main window of the application, and all the visual elements of it. Business logic has been delegated to service classes.
 */
@Getter
public class MainWindowView {

    //FXML Items
    @FXML
    private MenuItem settings;

    @FXML
    private MenuItem saveModlistAs;

    @FXML
    private CheckMenuItem logToggle;

    @FXML
    private CheckMenuItem modDescriptionToggle;

    @FXML
    private MenuItem themes;

    @FXML
    private MenuItem close;

    @FXML
    private MenuItem about;

    @FXML
    private MenuItem guide;

    @FXML
    private MenuItem faq;

    @FXML
    private ComboBox<String> modProfileDropdown;

    @FXML
    private ComboBox<String> selectedSaveDropdown;

    @FXML
    private Text activeModCount;

    @FXML
    private Text modConflicts;

    @FXML
    private ComboBox<String> modImportDropdown;

    @FXML
    private Button importModlist;

    @FXML
    private Button exportModlist;

    @FXML
    private Button saveModlist;

    @FXML
    private Button resetModlist;

    @FXML
    private Button injectModlist;

    @FXML
    private Button launchSpaceEngineers;

    //TODO: Set the text
    @FXML
    private Label lastInjected;

    //TODO: Set the color of this with the AtlantaFX global colors
    //TODO: Set the text
    @FXML
    private Label saveStatus;

    //TODO: Set the color of this with the AtlantaFX global colors
    //TODO: Set the text
    @FXML
    private Label lastModifiedBy;

    private String statusBaseStyling;

    @FXML
    private SplitPane mainViewSplit;

    //TODO: Low priority: Fix the alignment of the table headers to be centered, not center left.
    @FXML
    private TableView<ModCell> modTable; //TODO: Create a new object for the mod items that contains a check box and all the context menu stuff

    //TODO: The modcell needs properties for each of these https://stackoverflow.com/questions/53751455/how-to-create-a-javafx-tableview-without-warnings
    @FXML
    private TableColumn<ModCell, String> modName;

    @FXML
    private TableColumn<ModCell, String> modType;

    @FXML
    private TableColumn<ModCell, String> modVersion;

    @FXML
    private TableColumn<ModCell, String> modLastUpdated;

    @FXML
    private TableColumn<ModCell, Integer> loadPriority;

    @FXML
    private TableColumn<ModCell, String> modSource;

    @FXML
    private TableColumn<ModCell, String> modCategory;

    @FXML
    private TextField modTableSearchBox;

    @FXML
    private TabPane informationPane;

    @FXML
    private Tab logTab;

    @FXML
    private Tab modDescriptionTab;

    @FXML
    private ListView<LogMessage> viewableLog;

    @FXML
    private Button clearSearchBox;

    //TODO: We need to replace the window control bar for the window.
    private ObservableList<LogMessage> userLog;

    //TODO: Might need to go into UiService
    private SaveProfile currentSaveProfile;
    private ModProfile currentModProfile;

    private Logger logger;

    private Properties properties;

    private BackendController backendFileController;

    private UiService uiService;

    private Stage stage;

    private Scene scene;

    private boolean mainViewSplitDividerVisible = true;

    private UserConfiguration userConfiguration;

    //Initializes our controller while maintaining the empty constructor JavaFX expects
    public void initController(Properties properties, Logger logger, Stage stage, Parent root) throws IOException, JAXBException, XmlPullParserException {
        this.logger = logger;
        this.backendFileController = new BackendFileController(new SandboxConfigFileRepository(),
                new ModlistFileRepository(), properties,
                new UserDataFileRepository());
        this.scene = new Scene(root);
        this.stage = stage;
        this.properties = properties;

        //Get the users configurations
        Result<UserConfiguration> userConfigurationResult = backendFileController.getUserData(new File(this.properties.getProperty("semm.userData.default.location")));
        if(userConfigurationResult.isSuccess()) {
            userConfiguration = userConfigurationResult.getPayload();
        } else {
            userConfiguration = new UserConfiguration();
        }

        //Prepare the UI
        setupWindow(root);
        setupInformationBar();
        setupMainViewItems();

        //Do this last so the constructor for the service runs after the application has been visually prepared
        //Initialize the list we use to store log messages shown to the user
        this.uiService = new UiService(logger, userLog);
        uiService.log(userConfigurationResult);
    }

    //TODO: If our mod profile is null but we make a save, popup mod profile UI too. And vice versa for save profile.

    /**
     * Sets the basic properties of the window for the application, including the title bar, minimum resolutions, and listeners.
     */
    private void setupWindow(Parent root) throws IOException, XmlPullParserException {
        //Set the theme for our application based on the users preferred theme.
        //This makes it clunky to add any new themes in the future, but for the moment it's works and it's a straightforwards approach.
        switch (userConfiguration.getUserTheme()) {
            case "Primer Dark" -> Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            case "Nord Light" -> Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());
            case "Nord Dark" -> Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
            case "Cupertino Light" -> Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
            case "Cupertino Dark" -> Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
            case "Dracula" -> Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
            default -> Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }

        //Prepare the scene
        int minWidth = Integer.parseInt(properties.getProperty("semm.mainView.resolution.minWidth"));
        int minHeight = Integer.parseInt(properties.getProperty("semm.mainView.resolution.minHeight"));

        //Prepare the stage
        stage.setScene(scene);
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);

        //Add title and icon to the stage
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("pom.xml"));
        stage.setTitle("SEMM v" + model.getVersion());
        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

        //Add a listener to make the slider on the split pane stay at the bottom of our window when resizing it when it shouldn't be visible
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!this.isMainViewSplitDividerVisible()) {
                this.getMainViewSplit().setDividerPosition(0, 1);
            }
        });
    }

    /**
     * Sets the initial values for the toolbar located at the bottom of the UI.
     */
    private void setupInformationBar() {
        Optional<SaveProfile> lastUsedSaveProfile = findLastUsedSaveProfile();
        if (lastUsedSaveProfile.isPresent()) {
            this.currentSaveProfile = lastUsedSaveProfile.get();
            this.statusBaseStyling = "-fx-border-width: 1px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 2px;";
            ;

            updateSaveStatus(currentSaveProfile);
            updateLastModifiedBy(currentSaveProfile);

            //Set the text for the last time this profile was saved
            if (currentSaveProfile.getLastSaved().isEmpty()) {
                lastInjected.setText("Never");
            } else {
                lastInjected.setText(currentSaveProfile.getLastSaved());
            }
        } else {
            this.statusBaseStyling = "-fx-border-width: 1px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 2px;";
            ;

            saveStatus.setText("None");
            saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-neutral-emphasis;");

            lastModifiedBy.setText("None");
            lastModifiedBy.setStyle(statusBaseStyling += "-fx-border-color: -color-neutral-emphasis;");

            lastInjected.setText("Never");
        }
    }

    //TODO: Make it so that when we change the modlist and save it, but don't inject it, the status becomes "Modified since last injection"
    public void setupMainViewItems() {
        //Initialize the list we use to store log messages shown to the user
        userLog = FXCollections.observableArrayList(logMessage ->
                new Observable[]{
                        logMessage.viewableLogMessageProperty(),
                        logMessage.messageTypeProperty()
                });

        viewableLog.setItems(userLog);
        viewableLog.setCellFactory(param -> new LogCell());

        modImportDropdown.getItems().addAll("Add mods by Steam Workshop ID", "Add mods from Steam Collection", "Add mods from Mod.io", "Add mods from modlist file");

        selectedSaveDropdown.getItems().add("Manage...");
        modProfileDropdown.getItems().add("Manage...");

        //TODO: Much of this needs to happen down in the service layer
        //TODO: Setup a function in ModList service to track conflicts.
        //TODO: Populate mod table
    }
    //TODO: Hookup all the buttons to everything

    private Optional<SaveProfile> findLastUsedSaveProfile() {
        Optional<SaveProfile> lastUsedSaveProfile = Optional.empty();
        if (userConfiguration.getLastUsedSaveProfileId() != null) {
            UUID lastUsedSaveProfileId = userConfiguration.getLastUsedSaveProfileId();

            lastUsedSaveProfile = userConfiguration.getSaveProfiles().stream()
                    .filter(saveProfile -> saveProfile.getId().equals(lastUsedSaveProfileId))
                    .findFirst();
        }
        return lastUsedSaveProfile;
    }

    @FXML
    private void clearSearchBox() {
        modTableSearchBox.clear();
    }

    @FXML
    private void closeLogTab() {
        logToggle.setSelected(false);
        if (informationPane.getTabs().isEmpty()) {
            disableSplitPaneDivider();
        }
    }

    @FXML
    private void toggleLog() {
        if (!logToggle.isSelected()) {
            informationPane.getTabs().remove(logTab);
        } else informationPane.getTabs().add(logTab);

        if (informationPane.getTabs().isEmpty()) {
            disableSplitPaneDivider();
        } else if (!mainViewSplitDividerVisible) {
            enableSplitPaneDivider();
        }
    }

    @FXML
    private void closeModDescriptionTab() {
        modDescriptionToggle.setSelected(false);
        if (informationPane.getTabs().isEmpty()) {
            disableSplitPaneDivider();
        }
    }

    @FXML
    private void toggleModDescription() {
        if (!modDescriptionToggle.isSelected()) {
            informationPane.getTabs().remove(modDescriptionTab);
        } else informationPane.getTabs().add(modDescriptionTab);

        if (informationPane.getTabs().isEmpty()) {
            disableSplitPaneDivider();
        } else if (!mainViewSplitDividerVisible) {
            enableSplitPaneDivider();
        }
    }

    @FXML
    private void launchSpaceEngineers() throws URISyntaxException, IOException {
        //TODO: Check this works on systems with no previous steam url association
        Desktop.getDesktop().browse(new URI("steam://rungameid/244850"));
    }

    //Apply the modlist the user is currently viewing to the save profile they're currently viewing.
    @FXML
    private void applyModlist() throws IOException {
        //This should only return null when the SEMM has been run for the first time and the user hasn't made and modlists or save profiles.
        if (currentSaveProfile != null && currentModProfile != null && currentSaveProfile.getSavePath() != null) {
            Result<Boolean> modlistResult = backendFileController.applyModlist(currentModProfile.getModList(), currentSaveProfile.getSavePath());
            uiService.log(modlistResult);
            if (!modlistResult.isSuccess()) {
                //TODO: Popup some error window
                currentSaveProfile.setLastSaveStatus(SaveStatus.FAILED);
            } else {
                currentSaveProfile.setLastSaveStatus(SaveStatus.SAVED);
                currentSaveProfile.setLastAppliedModProfileId(currentModProfile.getId());

                int modProfileIndex = userConfiguration.getModProfiles().indexOf(currentModProfile);
                userConfiguration.getModProfiles().set(modProfileIndex, currentModProfile);

                int saveProfileIndex = userConfiguration.getSaveProfiles().indexOf(currentSaveProfile);
                userConfiguration.getSaveProfiles().set(saveProfileIndex, currentSaveProfile);

                backendFileController.saveUserData(userConfiguration, new File(properties.getProperty("semm.userData.default.location=./Storage/SEMM_Data.xml")));
            }
            updateInfoBar(currentSaveProfile);
        } else {
            String errorMessage = "Save or Mod profile not setup yet.";
            uiService.log(errorMessage, MessageType.ERROR);
            Alert.display(errorMessage, stage, MessageType.ERROR);
            //TODO: Overload the update infobar function
        }
    }

    private void disableSplitPaneDivider() {
        for (Node node : mainViewSplit.lookupAll(".split-pane-divider")) {
            node.setVisible(false);
            mainViewSplitDividerVisible = false;
        }
        mainViewSplit.setDividerPosition(0, 1);
    }

    private void enableSplitPaneDivider() {
        for (Node node : mainViewSplit.lookupAll(".split-pane-divider")) {
            node.setVisible(true);
            mainViewSplitDividerVisible = true;
        }
        mainViewSplit.setDividerPosition(0, 0.7);
    }

    private void updateInfoBar(SaveProfile saveProfile) {
        updateSaveStatus(saveProfile);
        updateLastModifiedBy(saveProfile);
        updateLastInjected();
    }

    private void updateSaveStatus(SaveProfile saveProfile) {
        switch (saveProfile.getLastSaveStatus()) {
            case SAVED -> {
                saveStatus.setText("Saved");
                saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-success-emphasis; -fx-text-fill: -color-success-emphasis;");
            }
            case UNSAVED -> {
                saveStatus.setText("Unsaved");
                saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-warning-emphasis; -fx-text-fill: -color-warning-emphasis;");
            }
            case FAILED -> {
                saveStatus.setText("Failed to save");
                saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-danger-emphasis; -fx-text-fill: -color-danger-emphasis;");
            }
            default -> {
                saveStatus.setText("N/A");
                saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-neutral-emphasis;");
            }
        }
    }

    //TODO: The visual part should be handled here, the logic should go into the service layers
    private void updateLastModifiedBy(SaveProfile saveProfile) {
        switch (saveProfile.getLastModifiedBy()) {
            case SEMM -> {
                lastModifiedBy.setText("SEMM");
                lastModifiedBy.setStyle(statusBaseStyling += "-fx-border-color: -color-success-emphasis; -fx-text-fill: -color-success-emphasis;");
            }
            case SPACE_ENGINEERS_IN_GAME -> {
                lastModifiedBy.setText("In-game Mod Manager");
                lastModifiedBy.setStyle(statusBaseStyling += "-fx-border-color: -color-warning-emphasis; -fx-text-fill: -color-warning-emphasis;");
            }
            default -> {
                lastModifiedBy.setText("None");
                lastModifiedBy.setStyle(statusBaseStyling += "-fx-border-color: -color-neutral-emphasis;");
            }
        }
    }

    private void updateLastInjected() {
        lastInjected.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm a")));
    }
}
