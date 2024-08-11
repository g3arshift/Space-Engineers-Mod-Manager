package com.gearshiftgaming.se_mod_manager.frontend.view;

import atlantafx.base.theme.*;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.controller.BackendController;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.LogCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModCell;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

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
    private MenuItem manageModProfiles;

    @FXML
    private MenuItem manageSaveProfiles;

    @FXML
    @Getter
    private ComboBox<ModProfile> modProfileDropdown;

    @FXML
    private ComboBox<SaveProfile> saveProfileDropdown;

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

    @FXML
    private Label lastInjected;

    @FXML
    private Label saveStatus;

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

    //TODO: Implement
    @FXML
    private Button clearSearchBox;

    @FXML
    private CheckMenuItem primerLightTheme;

    @FXML
    private CheckMenuItem primerDarkTheme;

    @FXML
    private CheckMenuItem nordLightTheme;

    @FXML
    private CheckMenuItem nordDarkTheme;

    @FXML
    private CheckMenuItem cupertinoLightTheme;

    @FXML
    private CheckMenuItem cupertinoDarkTheme;

    @FXML
    private CheckMenuItem draculaTheme;

    //TODO: We need to replace the window control bar for the window.
    private ObservableList<LogMessage> userLog;

    @Setter
    private SaveProfile currentSaveProfile;
    @Setter
    private ModProfile currentModProfile;

    private Logger logger;

    private Properties properties;

    private BackendController backendController;

    private UiService uiService;

    private Stage stage;

    private Scene scene;

    private boolean mainViewSplitDividerVisible = true;

    private UserConfiguration userConfiguration;

    //This is just a wrapper for the userConfiguration modProfiles list. Any changes made to this will propagate back to it, but not the other way around.
    private ObservableList<ModProfile> modProfiles;

    //This is just a wrapper for the userConfiguration saveProfiles list. Any changes made to this will propagate back to it, but not the other way around.
    private ObservableList<SaveProfile> saveProfiles;

    private ModProfileManagerView modProfileManagerView;

    private SaveManagerView saveManagerView;

    private final List<CheckMenuItem> themeList = new ArrayList<>();

    //TODO: This class is GIANT. Consider splitting it up into multiple views and stitch it together.
    //Initializes our controller while maintaining the empty constructor JavaFX expects
    public void initView(Properties properties, Logger logger,
                         UserConfiguration userConfiguration,
                         Stage stage, Parent root,
                         ModProfileManagerView modProfileManagerView, SaveManagerView saveManagerView,
                         UiService uiService) throws IOException, XmlPullParserException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        this.logger = logger;
        backendController = uiService.getBackendController();
        this.scene = new Scene(root);
        this.stage = stage;
        this.properties = properties;
        this.userConfiguration = userConfiguration;
        this.uiService = uiService;
        this.userLog = this.uiService.getUserLog();

        modProfiles = uiService.getModProfiles();
        saveProfiles = uiService.getSaveProfiles();

        this.modProfileManagerView = modProfileManagerView;
        this.saveManagerView = saveManagerView;

        themeList.add(primerLightTheme);
        themeList.add(primerDarkTheme);
        themeList.add(nordLightTheme);
        themeList.add(nordDarkTheme);
        themeList.add(cupertinoLightTheme);
        themeList.add(cupertinoDarkTheme);
        themeList.add(draculaTheme);

        //Prepare the UI
        setupWindow();
        setupMainViewItems();
        setupInformationBar();
    }

    //TODO: If our mod profile is null but we make a save, popup mod profile UI too. And vice versa for save profile.

    /**
     * Sets the basic properties of the window for the application, including the title bar, minimum resolutions, and listeners.
     */
    private void setupWindow() throws IOException, XmlPullParserException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        //Set the theme for our application based on the users preferred theme using reflection.
        for(CheckMenuItem c : themeList) {
            String currentTheme = StringUtils.removeEnd(c.getId(), "Theme");
            String themeName = currentTheme.substring(0, 1).toUpperCase() + currentTheme.substring(1);
            if(themeName.equals(StringUtils.deleteWhitespace(userConfiguration.getUserTheme()))) {
                c.setSelected(true);
                Class<?> cls = Class.forName("atlantafx.base.theme." + StringUtils.deleteWhitespace(userConfiguration.getUserTheme()));
                Theme theme = (Theme) cls.getDeclaredConstructor().newInstance();
                Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
            }
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

            updateSaveStatus(currentSaveProfile);
            updateLastModifiedBy(currentSaveProfile);

            //Set the text for the last time this profile was saved
            if (currentSaveProfile.getLastSaved() == null || currentSaveProfile.getLastSaved().isEmpty()) {
                lastInjected.setText("Never");
            } else {
                lastInjected.setText(currentSaveProfile.getLastSaved());
            }
        } else {
            this.statusBaseStyling = "-fx-border-width: 1px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 2px;";

            saveStatus.setText("None");
            saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-neutral-emphasis;");

            lastModifiedBy.setText("None");
            lastModifiedBy.setStyle(statusBaseStyling += "-fx-border-color: -color-neutral-emphasis;");

            lastInjected.setText("Never");
        }
    }

    //TODO: Make it so that when we change the modlist and save it, but don't inject it, the status becomes "Modified since last injection"
    public void setupMainViewItems() {
        viewableLog.setItems(userLog);
        viewableLog.setCellFactory(param -> new LogCell());
        //Disable selecting rows in the log.
        viewableLog.setSelectionModel(null);

        modImportDropdown.getItems().addAll("Add mods by Steam Workshop ID", "Add mods from Steam Collection", "Add mods from Mod.io", "Add mods from modlist file");

        //Makes it so the combo boxes will properly return strings in their menus instead of the objects
        saveProfileDropdown.setConverter(new StringConverter<>() {
            @Override
            public String toString(SaveProfile saveProfile) {
                return saveProfile.getSaveName();
            }

            @Override
            public SaveProfile fromString(String s) {
                return null;
            }
        });
        modProfileDropdown.setConverter(new StringConverter<>() {
            @Override
            public String toString(ModProfile modProfile) {
                return modProfile.getProfileName();
            }

            @Override
            public ModProfile fromString(String s) {
                return null;
            }
        });

        saveProfileDropdown.setItems(saveProfiles);
        saveProfileDropdown.getSelectionModel().selectFirst();
        currentSaveProfile = saveProfileDropdown.getSelectionModel().getSelectedItem();


        modProfileDropdown.setItems(modProfiles);
        modProfileDropdown.getSelectionModel().selectFirst();
        currentModProfile = modProfileDropdown.getSelectionModel().getSelectedItem();

        //TODO: Much of this needs to happen down in the service layer
        //TODO: Setup a function in ModList service to track conflicts.
        //TODO: Populate mod table
    }
    //TODO: Hookup all the buttons to everything

    private Optional<SaveProfile> findLastUsedSaveProfile() {
        Optional<SaveProfile> lastUsedSaveProfile = Optional.empty();
        if (userConfiguration.getLastUsedSaveProfileId() != null) {
            UUID lastUsedSaveProfileId = userConfiguration.getLastUsedSaveProfileId();

            lastUsedSaveProfile = saveProfiles.stream()
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
    private void manageModProfiles() {
        modProfileManagerView.getStage().showAndWait();
        uiService.log(backendController.saveUserData(userConfiguration));
    }

    @FXML
    private void manageSaveProfiles() {

    }

    @FXML
    private void selectModProfile() throws IOException {
        currentModProfile = modProfileDropdown.getSelectionModel().getSelectedItem();


//        Optional<ModProfile> selectedProfile = modProfiles.stream().
//                filter(o -> o.getProfileName().equals(modProfileDropdown.getValue().getProfileName()))
//                .findFirst();
//        selectedProfile.ifPresent(modProfile -> currentModProfile = modProfile);
        //TODO: Update the mod table. Wrap the modlist in the profile with an observable list!
    }

    private void importModlist() {
        //TODO: Implement. Allow importing modlists from either sandbox file or exported list.
    }

    @FXML
    private void exportModlist() {
        //TODO: Implement. Export in our own format (use XML).
    }

    @FXML
    private void saveModlist() {
        //TODO: Implement
    }

    @FXML
    private void resetModlist() {
        //TODO: Implement by setting the current modprofile modlist to whatever it is in our user configuration.
        // Make a popup asking if they're sure they want to reset the modlist.
        // Also make a popup if the list was never saved and can't be found in the user configuration.
    }

    //Apply the modlist the user is currently using to the save profile they're currently using.
    @FXML
    private void applyModlist() throws IOException {
        //This should only return null when the SEMM has been run for the first time and the user hasn't made and modlists or save profiles.
        if (currentSaveProfile != null && currentModProfile != null && currentSaveProfile.getSavePath() != null) {
            Result<Boolean> modlistResult = backendController.applyModlist(currentModProfile.getModList(), currentSaveProfile.getSavePath());
            uiService.log(modlistResult);
            if (!modlistResult.isSuccess()) {
                currentSaveProfile.setLastSaveStatus(SaveStatus.FAILED);
            } else {
                currentSaveProfile.setLastAppliedModProfileId(currentModProfile.getId());

                int modProfileIndex = modProfiles.indexOf(currentModProfile);
                modProfiles.set(modProfileIndex, currentModProfile);

                int saveProfileIndex = saveProfiles.indexOf(currentSaveProfile);
                saveProfiles.set(saveProfileIndex, currentSaveProfile);

                uiService.log(backendController.saveUserData(userConfiguration));
                currentSaveProfile.setLastSaveStatus(SaveStatus.SAVED);
            }
            updateInfoBar(currentSaveProfile);
        } else {
            //Save or Mod profile not setup yet. Create both a Save and Mod profile to be able to apply a modlist.
            String errorMessage = "Save profile not setup yet. Create a save profile to apply a modlist.";
            uiService.log(errorMessage, MessageType.ERROR);
            Popup.displaySimpleAlert(errorMessage, stage, MessageType.ERROR);
        }
    }

    @FXML
    private void launchSpaceEngineers() throws URISyntaxException, IOException {
        //TODO: Check this works on systems with no previous steam url association
        Desktop.getDesktop().browse(new URI("steam://rungameid/244850"));
    }


    /**
     * Using reflection, set the theme of the application and check/uncheck the appropriate menu boxes for themes.
     */
    @FXML
    private void setTheme(ActionEvent event) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        //Remove the "Theme" end tag from our caller and capitalize the first letter
        final CheckMenuItem source = (CheckMenuItem) event.getSource();
        String caller = StringUtils.removeEnd(source.getId(), "Theme");
        String selectedTheme = caller.substring(0, 1).toUpperCase() + caller.substring(1);

        for(CheckMenuItem c : themeList) {
            String currentTheme = StringUtils.removeEnd(c.getId(), "Theme");
            String themeName = currentTheme.substring(0, 1).toUpperCase() + currentTheme.substring(1);
            if(!themeName.equals(selectedTheme)) {
                c.setSelected(false);
            } else {
                //Use reflection to get a theme class from our string
                c.setSelected(true);
                Class<?> cls = Class.forName("atlantafx.base.theme." + selectedTheme);
                Theme theme = (Theme) cls.getDeclaredConstructor().newInstance();
                Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
                userConfiguration.setUserTheme(selectedTheme);
            }
        }

        backendController.saveUserData(userConfiguration);
        Result<Boolean> userConfigurationResult = backendController.saveUserData(userConfiguration);
        if (userConfigurationResult.isSuccess()) {
            uiService.log("Successfully set user theme to " + selectedTheme + ".", MessageType.INFO);
        } else {
            uiService.log("Failed to save theme to user configuration.", MessageType.ERROR);
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
