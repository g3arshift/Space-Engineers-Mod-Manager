package com.gearshiftgaming.se_mod_manager.frontend.view;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.modlist.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.ResultType;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.SpaceEngineersVersion;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.contextbar.ModListDropdownButtonCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.contextbar.ModListDropdownItemCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.contextbar.SaveProfileDropdownButtonCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.contextbar.SaveProfileDropdownItemCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.TextTruncationUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.TwoButtonChoice;
import com.gearshiftgaming.se_mod_manager.frontend.view.window.WindowTitleBarColorUtility;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.MutableTriple;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModTableContextBar {

    //FXML Items
    @FXML
    private VBox modTableContextBarRoot;

    @FXML
    @Getter
    private CheckMenuItem logToggle;

    @FXML
    @Getter
    private CheckMenuItem modDescriptionToggle;

    @FXML
    @Getter
    private CheckMenuItem conflictsToggle;

    @FXML
    private MenuItem themes;

    @FXML
    private MenuItem close;

    @FXML
    private MenuItem updateMods;

    @FXML
    private MenuItem resetConfig;

    @FXML
    private MenuItem about;

    @FXML
    private MenuItem faq;

    @FXML
    private MenuItem reportBug;

    @FXML
    private MenuItem runTutorial;

    @FXML
    @Getter
    private ComboBox<MutableTriple<UUID, String, SpaceEngineersVersion>> modProfileDropdown;

    @FXML
    @Getter
    private ComboBox<SaveProfile> saveProfileDropdown;

    @FXML
    private Rectangle activeModCountBox;

    @FXML
    @Getter
    private Label activeModCount;

    @FXML
    private Rectangle modConflictBox;

    @FXML
    @Getter
    private Label modConflicts;

    @FXML
    @Getter
    private TextField modTableSearchField;

    @FXML
    private Label modTableSearchFieldPromptText;

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

    private final MasterManager masterManagerView;

    private final StatusBar statusBarView;

    private final List<CheckMenuItem> themeList = new ArrayList<>();

    private final UiService uiService;

    private final Stage stage;

    public ModTableContextBar(UiService uiService, MasterManager masterManager, StatusBar statusBar, Stage stage) {
        this.uiService = uiService;
        this.masterManagerView = masterManager;
        this.statusBarView = statusBar;
        this.stage = stage;
    }

    public void initView() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        themeList.add(primerLightTheme);
        themeList.add(primerDarkTheme);
        themeList.add(nordLightTheme);
        themeList.add(nordDarkTheme);
        themeList.add(cupertinoLightTheme);
        themeList.add(cupertinoDarkTheme);
        themeList.add(draculaTheme);

        saveProfileDropdown.setItems(uiService.getSaveProfiles());
        Optional<SaveProfile> lastActiveSaveProfile = uiService.getLastActiveSaveProfile();
        if (lastActiveSaveProfile.isPresent())
            saveProfileDropdown.getSelectionModel().select(lastActiveSaveProfile.get());
        else
            saveProfileDropdown.getSelectionModel().selectFirst();

        saveProfileDropdown.setCellFactory(param -> new SaveProfileDropdownItemCell(uiService));
        saveProfileDropdown.setButtonCell(new SaveProfileDropdownButtonCell(uiService));
        saveProfileDropdown.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> saveProfileDropdown.setButtonCell(new SaveProfileDropdownButtonCell(uiService)));

        ChangeListener<Number> saveProfileButtonCellWidthListener = (observable, oldValue, newValue) -> {
            String profileName = uiService.getCurrentSaveProfile().getProfileName();
            double cellWidth = saveProfileDropdown.getButtonCell().getWidth() - 5;
            ((SaveProfileDropdownButtonCell) saveProfileDropdown.getButtonCell()).getPROFILE_NAME().setText(TextTruncationUtility.truncateWithEllipsisWithRealWidth(profileName, cellWidth));
        };

        ChangeListener<Number> modlistProfileButtonCellWidthListener = (observable, oldValue, newValue) -> {
            String profileName = uiService.getCurrentModListProfile().getProfileName();
            double cellWidth = modProfileDropdown.getButtonCell().getWidth() - 5;
            ((ModListDropdownButtonCell) modProfileDropdown.getButtonCell()).getPROFILE_NAME().setText(TextTruncationUtility.truncateWithEllipsisWithRealWidth(profileName, cellWidth));
        };

        stage.widthProperty().addListener(saveProfileButtonCellWidthListener);
        stage.widthProperty().addListener(modlistProfileButtonCellWidthListener);

        modProfileDropdown.setItems(uiService.getModListProfileDetails());
        Result<ModListProfile> lastActiveModlistProfile = uiService.getLastActiveModlistProfile();
        if (lastActiveModlistProfile.isSuccess())
            for (MutableTriple<UUID, String, SpaceEngineersVersion> details : modProfileDropdown.getItems()) {
                if (details.getLeft().equals(uiService.getUserConfiguration().getLastActiveModProfileId())) {
                    modProfileDropdown.getSelectionModel().select(details);
                    break;
                }
            }
        else
            modProfileDropdown.getSelectionModel().selectFirst();


        modProfileDropdown.setCellFactory(param -> new ModListDropdownItemCell(uiService));
        modProfileDropdown.setButtonCell(new ModListDropdownButtonCell(uiService));
        modProfileDropdown.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> modProfileDropdown.setButtonCell(new ModListDropdownButtonCell(uiService)));

        uiService.setUserSavedApplicationTheme(themeList);

        //Makes it so the combo boxes will properly return strings in their menus instead of the objects
        saveProfileDropdown.setConverter(new StringConverter<>() {
            @Override
            public String toString(SaveProfile saveProfile) {
                return saveProfile.getProfileName();
            }

            @Override
            public SaveProfile fromString(String s) {
                return null;
            }
        });
        modProfileDropdown.setConverter(new StringConverter<>() {
            @Override
            public String toString(MutableTriple<UUID, String, SpaceEngineersVersion> modListProfileDetails) {
                return modListProfileDetails.getMiddle();
            }

            @Override
            public MutableTriple<UUID, String, SpaceEngineersVersion> fromString(String s) {
                return null;
            }
        });

        modTableSearchField.textProperty().addListener(observable -> {
            String filter = modTableSearchField.getText();
            if (filter == null || filter.isBlank()) {
                masterManagerView.getFilteredModList().setPredicate(mod -> true);
            } else {
                masterManagerView.getFilteredModList().setPredicate(mod -> mod.getFriendlyName().toLowerCase().contains(filter.toLowerCase())); // Case-insensitive check
            }
        });

        activeModCount.textProperty().bind(uiService.getActiveModCount().asString());

        activeModCountBox.setStroke(getThemeBoxColor());
        modConflictBox.setStroke(getThemeBoxColor());

        modTableSearchField.setOnMouseClicked(mouseEvent -> modTableSearchFieldPromptText.setVisible(false));
        modTableSearchField.focusedProperty().addListener((observableValue, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(!newValue) && modTableSearchField.getText().isBlank()) {
                modTableSearchFieldPromptText.setVisible(true);
            }
        });

        uiService.logPrivate("Successfully initialized context bar.", MessageType.INFO);
    }

    @FXML
    private void toggleLog() {
        TabPane informationPane = masterManagerView.getInformationPane();
        Tab logTab = masterManagerView.getLogTab();

        if (!logToggle.isSelected())
            informationPane.getTabs().remove(logTab);
        else informationPane.getTabs().add(logTab);

        if (informationPane.getTabs().isEmpty())
            masterManagerView.disableSplitPaneDivider();
        else if (!masterManagerView.isMainViewSplitDividerVisible())
            masterManagerView.enableSplitPaneDivider();

    }

    @FXML
    private void toggleModDescription() {
        TabPane informationPane = masterManagerView.getInformationPane();
        Tab modDescriptionTab = masterManagerView.getModDescriptionTab();

        if (!modDescriptionToggle.isSelected())
            informationPane.getTabs().remove(modDescriptionTab);
        else informationPane.getTabs().add(modDescriptionTab);

        if (informationPane.getTabs().isEmpty())
            masterManagerView.disableSplitPaneDivider();
        else if (!masterManagerView.isMainViewSplitDividerVisible())
            masterManagerView.enableSplitPaneDivider();

    }

    @FXML
    private void toggleConflicts() {
        TabPane informationPane = masterManagerView.getInformationPane();
        Tab conflictsTab = masterManagerView.getConflictsTab();

        if (!conflictsTab.isClosable())
            informationPane.getTabs().remove(conflictsTab);
        else informationPane.getTabs().add(conflictsTab);

        if (informationPane.getTabs().isEmpty())
            masterManagerView.disableSplitPaneDivider();
        else if (!masterManagerView.isMainViewSplitDividerVisible())
            masterManagerView.enableSplitPaneDivider();

    }

    /**
     * Using reflection, set the theme of the application and check/uncheck the appropriate menu boxes for themes.
     */
    @FXML
    private void setTheme(ActionEvent event) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        //Remove the "Theme" end tag from our caller and capitalize the first letter
        final CheckMenuItem SOURCE = (CheckMenuItem) event.getSource();
        String caller = Strings.CS.removeEnd(SOURCE.getId(), "Theme");
        String selectedTheme = caller.substring(0, 1).toUpperCase() + caller.substring(1);

        for (CheckMenuItem c : themeList) {
            String currentTheme = Strings.CS.removeEnd(c.getId(), "Theme");
            String themeName = currentTheme.substring(0, 1).toUpperCase() + currentTheme.substring(1);
            if (!themeName.equals(selectedTheme)) {
                c.setSelected(false);
            } else {
                //Use reflection to get a theme class from our string
                c.setSelected(true);
                Class<?> cls = Class.forName("atlantafx.base.theme." + selectedTheme);
                Theme theme = (Theme) cls.getDeclaredConstructor().newInstance();
                Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
                uiService.getUserConfiguration().setUserTheme(selectedTheme);
                activeModCountBox.setStroke(getThemeBoxColor());
                modConflictBox.setStroke(getThemeBoxColor());

                String activeThemeName = StringUtils.substringAfter(Application.getUserAgentStylesheet(), "theme/");
                masterManagerView.getModDescription().getEngine().setUserStyleSheetLocation(Objects.requireNonNull(getClass().getResource("/styles/mod-description_" + activeThemeName)).toString());

                WindowTitleBarColorUtility.setWindowsTitleBar(stage);
            }
        }

        //TODO: Replace with just the user config save.
        Result<Void> savedUserTheme = uiService.saveUserConfiguration();
        //This fixes the selected row being the wrong color until we change selection
        masterManagerView.getModTable().refresh();
        if (savedUserTheme.isSuccess()) {
            uiService.log("Successfully set user theme to " + selectedTheme + ".", MessageType.INFO);
        } else {
            savedUserTheme.addMessage("Failed to save theme to user configuration.", ResultType.FAILED);
            uiService.log(savedUserTheme);
        }
    }

    @FXML
    private void selectModProfile() {
        clearSearchBox();

        uiService.setCurrentModListProfile(modProfileDropdown.getSelectionModel().getSelectedItem().getLeft());
        masterManagerView.updateModTableContents();
    }

    @FXML
    private void selectSaveProfile() {
        Result<Void> saveSelectionResult = uiService.setCurrentSaveProfile(saveProfileDropdown.getSelectionModel().getSelectedItem());
        if (saveSelectionResult.isFailure()) {
            uiService.log(saveSelectionResult);
            Popup.displaySimpleAlert("Failed to select save profile. See log for more details.", MessageType.ERROR);
        } else
            statusBarView.loadStatusBarInfo();
    }

    @FXML
    private void clearSearchBox() {
        modTableSearchField.clear();
        modTableSearchFieldPromptText.setVisible(true);
    }

    @FXML
    private void exit() {
        Platform.exit();
    }

    @FXML
    private void updateModInformation() {
        updateMods(uiService.getCurrentModList()).start();
    }

    @FXML
    private void resetConfig() {
        TwoButtonChoice resetChoice = Popup.displayYesNoDialog("Do you want to reset your SEMM configuration?", stage, MessageType.INFO);

        if (resetChoice == TwoButtonChoice.YES) {
            resetChoice = Popup.displayYesNoDialog("Are you REALLY sure you want to reset it? This will remove all save configs (but not delete them from your saves folder), mod lists, and everything else. Are you CERTAIN you want to delete it?", stage, MessageType.WARN);
            if (resetChoice == TwoButtonChoice.YES) {
                Result<Void> configResetResult = uiService.resetData();
                if (configResetResult.isSuccess()) {
                    Popup.displaySimpleAlert("SEMM configuration successfully reset. The application will now close, and will be free of any configuration when you launch it next.", stage, MessageType.INFO);
                    Platform.exit();
                } else {
                    Popup.displaySimpleAlert(configResetResult, stage);
                }
            }
        }
    }

    @FXML
    private void reportBug() throws URISyntaxException, IOException {
        Desktop.getDesktop().browse(new URI("https://bugreport.spaceengineersmodmanager.com"));
    }

    @FXML
    private void runTutorial() {
        TwoButtonChoice runTutorialChoice = Popup.displayYesNoDialog("Are you sure you want to run the tutorial?", stage, MessageType.INFO);
        if (runTutorialChoice == TwoButtonChoice.YES) {
            uiService.getUserConfiguration().setRunFirstTimeSetup(true);
            uiService.displayTutorial(stage, masterManagerView);
        }
    }

    private Thread updateMods(List<Mod> initialModList) {
        final Task<Void> TASK;
        List<Mod> modList = new ArrayList<>(initialModList);

        TASK = new Task<>() {
            @Override
            protected Void call() {
                uiService.getCurrentModList().clear();
                masterManagerView.importModsFromList(modList).start();
                return null;
            }
        };

        Thread thread = Thread.ofVirtual().unstarted(TASK);
        thread.setDaemon(true);
        return thread;
    }

    private Color getThemeBoxColor() {
        return switch (uiService.getUserConfiguration().getUserTheme()) {
            case "PrimerLight", "NordLight", "CupertinoLight":
                yield Color.web("#000000");
            case "PrimerDark", "CupertinoDark":
                yield Color.web("#748393");
            case "NordDark":
                yield Color.web("#5e6675");
            default:
                yield Color.web("#685ab3");
        };
    }
}
