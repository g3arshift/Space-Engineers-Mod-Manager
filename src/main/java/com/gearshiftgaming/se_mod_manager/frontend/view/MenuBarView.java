package com.gearshiftgaming.se_mod_manager.frontend.view;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.SaveProfileCell;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 *
 * @author Gear Shift
 */

@Getter
public class MenuBarView {

	//FXML Items
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
	private ComboBox<ModProfile> modProfileDropdown;

	@FXML
	private ComboBox<SaveProfile> saveProfileDropdown;

	@FXML
	private Text activeModCount;

	@FXML
	private Text modConflicts;

	@FXML
	private TextField modTableSearchBox;

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

	private MainWindowViewExperimental mainWindowView;

	private final List<CheckMenuItem> themeList = new ArrayList<>();

	private UiService uiService;

	private ObservableList<ModProfile> modProfiles;

	private ObservableList<SaveProfile> saveProfiles;

	private UserConfiguration userConfiguration;

	private ModProfileManagerView modProfileManagerView;

	private SaveManagerView saveManagerView;

	public void initView(MainWindowViewExperimental mainWindowView, UiService uiService,
						 ModProfileManagerView modProfileManagerView, SaveManagerView saveManagerView) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		//FIXME: For some reason a tiny section of the saveProfileDropdown isn't highlighted blue when selected.
		// - There's something on top of it. Look at it in dracula theme.
		this.mainWindowView = mainWindowView;
		this.uiService = uiService;
		this.modProfileManagerView = modProfileManagerView;
		this.saveManagerView = saveManagerView;

		modProfiles = uiService.getModProfiles();
		saveProfiles = uiService.getSaveProfiles();

		themeList.add(primerLightTheme);
		themeList.add(primerDarkTheme);
		themeList.add(nordLightTheme);
		themeList.add(nordDarkTheme);
		themeList.add(cupertinoLightTheme);
		themeList.add(cupertinoDarkTheme);
		themeList.add(draculaTheme);

		saveProfileDropdown.setItems(uiService.getSaveProfiles());
		saveProfileDropdown.getSelectionModel().selectFirst();

		saveProfileDropdown.setCellFactory(param -> new SaveProfileCell());
		saveProfileDropdown.setButtonCell(new SaveProfileCell());

		modProfileDropdown.setItems(modProfiles);
		modProfileDropdown.getSelectionModel().selectFirst();

		//TODO: Set the current save and mod profile equal to whatever was used last.
		uiService.setCurrentSaveProfile(saveProfileDropdown.getSelectionModel().getSelectedItem());
		uiService.setCurrentModProfile(modProfileDropdown.getSelectionModel().getSelectedItem());

		userConfiguration = uiService.getUserConfiguration();

		//Set the theme for our application based on the users preferred theme using reflection.
		for (CheckMenuItem c : themeList) {
			String currentTheme = StringUtils.removeEnd(c.getId(), "Theme");
			String themeName = currentTheme.substring(0, 1).toUpperCase() + currentTheme.substring(1);
			if (themeName.equals(StringUtils.deleteWhitespace(userConfiguration.getUserTheme()))) {
				c.setSelected(true);
				Class<?> cls = Class.forName("atlantafx.base.theme." + StringUtils.deleteWhitespace(userConfiguration.getUserTheme()));
				Theme theme = (Theme) cls.getDeclaredConstructor().newInstance();
				Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
			}
		}

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
			public String toString(ModProfile modProfile) {
				return modProfile.getProfileName();
			}

			@Override
			public ModProfile fromString(String s) {
				return null;
			}
		});
	}

	@FXML
	private void toggleLog() {
		TabPane informationPane = mainWindowView.getInformationPane();
		Tab logTab = mainWindowView.getLogTab();

		if (!logToggle.isSelected()) {
			informationPane.getTabs().remove(logTab);
		} else informationPane.getTabs().add(logTab);

		if (informationPane.getTabs().isEmpty()) {
			mainWindowView.disableSplitPaneDivider();
		} else if (!mainWindowView.isMainViewSplitDividerVisible()) {
			mainWindowView.enableSplitPaneDivider();
		}
	}

	@FXML
	private void toggleModDescription() {
		TabPane informationPane = mainWindowView.getInformationPane();
		Tab modDescriptionTab = mainWindowView.getModDescriptionTab();
		if (!modDescriptionToggle.isSelected()) {
			informationPane.getTabs().remove(modDescriptionTab);
		} else informationPane.getTabs().add(modDescriptionTab);

		if (informationPane.getTabs().isEmpty()) {
			mainWindowView.disableSplitPaneDivider();
		} else if (!mainWindowView.isMainViewSplitDividerVisible()) {
			mainWindowView.enableSplitPaneDivider();
		}
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

		for (CheckMenuItem c : themeList) {
			String currentTheme = StringUtils.removeEnd(c.getId(), "Theme");
			String themeName = currentTheme.substring(0, 1).toUpperCase() + currentTheme.substring(1);
			if (!themeName.equals(selectedTheme)) {
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

		uiService.saveUserData(userConfiguration);
		Result<Void> userConfigurationResult = uiService.saveUserData(userConfiguration);
		if (userConfigurationResult.isSuccess()) {
			uiService.log("Successfully set user theme to " + selectedTheme + ".", MessageType.INFO);
		} else {
			uiService.log("Failed to save theme to user configuration.", MessageType.ERROR);
		}
	}

	@FXML
	private void manageModProfiles() {
		modProfileManagerView.getStage().showAndWait();
		uiService.log(uiService.saveUserData(userConfiguration));
	}

	//TODO: Add check for if the file can't be found for a save profile.
	@FXML
	private void manageSaveProfiles() {
		saveManagerView.getStage().showAndWait();
		uiService.log(uiService.saveUserData(userConfiguration));
	}

	@FXML
	private void selectModProfile() throws IOException {
		uiService.setCurrentModProfile(modProfileDropdown.getSelectionModel().getSelectedItem());
		//TODO: Update the mod table. Wrap the modlist in the profile with an observable list!
	}

	@FXML
	private void clearSearchBox() {
		modTableSearchBox.clear();
	}
}
