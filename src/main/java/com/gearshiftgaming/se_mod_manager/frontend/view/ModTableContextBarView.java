package com.gearshiftgaming.se_mod_manager.frontend.view;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.SaveProfileCell;
import com.gearshiftgaming.se_mod_manager.frontend.view.helper.TitleBarHelper;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public class ModTableContextBarView {

	//FXML Items
	@FXML
	private VBox modTableContextBarRoot;

	@FXML
	private MenuItem saveModlistAs;

	@FXML
	@Getter
	private CheckMenuItem logToggle;

	@FXML
	@Getter
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
	@Getter
	private ComboBox<ModProfile> modProfileDropdown;

	@FXML
	@Getter
	private ComboBox<SaveProfile> saveProfileDropdown;

	@FXML
	@Getter
	private Label activeModCount;

	@FXML
	@Getter
	private Label modConflicts;

	@FXML
	private TextField modTableSearchField;

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

	private final ModlistManagerView MODLIST_MANAGER_VIEW;

	private final List<CheckMenuItem> THEME_LIST = new ArrayList<>();

	private final UiService UI_SERVICE;

	private final Stage STAGE;

	//TODO: On dropdown select, change active profile

	public ModTableContextBarView(UiService uiService, ModlistManagerView modlistManagerView, Stage stage) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		//FIXME: For some reason a tiny section of the saveProfileDropdown isn't highlighted blue when selected. It's an issue with the button cell.
		// - For some reason, calling:
		//		ListCell<SaveProfile> buttonCellFix = new SaveProfileCell();
		//		buttonCellFix.setItem(saveProfile);
		//		buttonCellFix.setText(saveProfile.getProfileName());
		//		topBarView.getSaveProfileDropdown().setButtonCell(buttonCellFix);
		//	in saveManagerView fixes it?! WHY?!
		this.UI_SERVICE = uiService;
		this.MODLIST_MANAGER_VIEW = modlistManagerView;
		this.STAGE = stage;
	}

	public void initView() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

		THEME_LIST.add(primerLightTheme);
		THEME_LIST.add(primerDarkTheme);
		THEME_LIST.add(nordLightTheme);
		THEME_LIST.add(nordDarkTheme);
		THEME_LIST.add(cupertinoLightTheme);
		THEME_LIST.add(cupertinoDarkTheme);
		THEME_LIST.add(draculaTheme);

		saveProfileDropdown.setItems(UI_SERVICE.getSAVE_PROFILES());
		saveProfileDropdown.getSelectionModel().selectFirst();

		saveProfileDropdown.setCellFactory(param -> new SaveProfileCell(""));
		saveProfileDropdown.setButtonCell(new SaveProfileCell(""));

		modProfileDropdown.setItems(UI_SERVICE.getMOD_PROFILES());
		modProfileDropdown.getSelectionModel().selectFirst();

		UI_SERVICE.setUserSavedApplicationTheme(THEME_LIST);

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

		activeModCount.textProperty().bind(UI_SERVICE.getActiveModCount().asString());

		UI_SERVICE.logPrivate("Successfully initialized menu bar.", MessageType.INFO);
	}

	@FXML
	private void toggleLog() {
		TabPane informationPane = MODLIST_MANAGER_VIEW.getInformationPane();
		Tab logTab = MODLIST_MANAGER_VIEW.getLogTab();

		if (!logToggle.isSelected()) {
			informationPane.getTabs().remove(logTab);
		} else informationPane.getTabs().add(logTab);

		if (informationPane.getTabs().isEmpty()) {
			MODLIST_MANAGER_VIEW.disableSplitPaneDivider();
		} else if (!MODLIST_MANAGER_VIEW.isMainViewSplitDividerVisible()) {
			MODLIST_MANAGER_VIEW.enableSplitPaneDivider();
		}
	}

	@FXML
	private void toggleModDescription() {
		TabPane informationPane = MODLIST_MANAGER_VIEW.getInformationPane();
		Tab modDescriptionTab = MODLIST_MANAGER_VIEW.getModDescriptionTab();

		if (!modDescriptionToggle.isSelected()) {
			informationPane.getTabs().remove(modDescriptionTab);
		} else informationPane.getTabs().add(modDescriptionTab);

		if (informationPane.getTabs().isEmpty()) {
			MODLIST_MANAGER_VIEW.disableSplitPaneDivider();
		} else if (!MODLIST_MANAGER_VIEW.isMainViewSplitDividerVisible()) {
			MODLIST_MANAGER_VIEW.enableSplitPaneDivider();
		}
	}

	/**
	 * Using reflection, set the theme of the application and check/uncheck the appropriate menu boxes for themes.
	 */
	@FXML
	private void setTheme(ActionEvent event) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

		//Remove the "Theme" end tag from our caller and capitalize the first letter
		final CheckMenuItem SOURCE = (CheckMenuItem) event.getSource();
		String caller = StringUtils.removeEnd(SOURCE.getId(), "Theme");
		String selectedTheme = caller.substring(0, 1).toUpperCase() + caller.substring(1);

		for (CheckMenuItem c : THEME_LIST) {
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
				UI_SERVICE.getUSER_CONFIGURATION().setUserTheme(selectedTheme);
				TitleBarHelper.SetTitleBar(STAGE, selectedTheme);
			}
		}

		Result<Void> savedUserTheme = UI_SERVICE.saveUserData();
		//This fixes the selected row being the wrong color until we change selection
		MODLIST_MANAGER_VIEW.getModTable().refresh();
		if (savedUserTheme.isSuccess()) {
			UI_SERVICE.log("Successfully set user theme to " + selectedTheme + ".", MessageType.INFO);
		} else {
			UI_SERVICE.log("Failed to save theme to user configuration.", MessageType.ERROR);
		}
	}

	@FXML
	private void selectModProfile() {
		ModProfile modProfile = modProfileDropdown.getSelectionModel().getSelectedItem();

		UI_SERVICE.setCurrentModProfile(modProfile);
		MODLIST_MANAGER_VIEW.getModTable().setItems(UI_SERVICE.getCurrentModList());
		//TODO: Update the mod table. Wrap the modlist in the profile with an observable list!
	}

	@FXML
	private void selectSaveProfile() {
		UI_SERVICE.setCurrentSaveProfile(saveProfileDropdown.getSelectionModel().getSelectedItem());
		//TODO: Update the mod table. Wrap the modlist in the profile with an observable list!
	}

	@FXML
	private void clearSearchBox() {
		modTableSearchField.clear();
	}
}
