package com.gearshiftgaming.se_mod_manager.frontend.view;

import atlantafx.base.controls.RingProgressIndicator;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.SaveProfileManagerCell;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.TitleBarUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.Properties;


/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */

public class SaveManagerView {
	@FXML
	private ListView<SaveProfile> saveList;

	@FXML
	private Button addSave;

	@FXML
	private Button copySave;

	@FXML
	private Button removeSave;

	@FXML
	private Button renameProfile;

	@FXML
	private Button selectSave;

	@FXML
	private Button closeSaveWindow;

	@FXML
	private Pane operationInProgressDimmer;

	@FXML
	private ProgressIndicator progressIndicator;

	private Stage stage;

	private final ObservableList<SaveProfile> SAVE_PROFILES;

	private final UiService UI_SERVICE;

	private final SaveInputView SAVE_INPUT_VIEW;

	private final SimpleInputView PROFILE_INPUT_VIEW;

	private ModTableContextBarView modTableContextBarView;

	public SaveManagerView(UiService UI_SERVICE, SaveInputView saveInputViewFirstStepView, SimpleInputView simpleInputView) {
		this.UI_SERVICE = UI_SERVICE;
		SAVE_PROFILES = UI_SERVICE.getSAVE_PROFILES();
		this.SAVE_INPUT_VIEW = saveInputViewFirstStepView;
		this.PROFILE_INPUT_VIEW = simpleInputView;
	}

	public void initView(Parent root, Properties properties, ModTableContextBarView modTableContextBarView) {
		this.modTableContextBarView = modTableContextBarView;
		Scene scene = new Scene(root);

		stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);

		stage.setTitle("Save Manager");
		stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

		stage.setMinWidth(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minWidth")));
		stage.setMinHeight(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minHeight")));

		saveList.setItems(SAVE_PROFILES);
		saveList.setCellFactory(param -> new SaveProfileManagerCell());

		saveList.setStyle("-fx-background-color: -color-bg-default;");

		stage.setScene(scene);
		UI_SERVICE.logPrivate("Successfully initialized save manager.", MessageType.INFO);
	}

	@FXML
	private void addSave() {
		boolean duplicateSavePath = false;
		Result<SaveProfile> result;
		//Get our selected file from the user, check if its already being managed by SEMM by checking the save path, and then check if the save name already exists. If it does, append a number to the end of it.
		do {
			SAVE_INPUT_VIEW.show();
			result = SAVE_INPUT_VIEW.getSaveProfileResult();
			if (result.isSuccess()) {
				SaveProfile saveProfile = result.getPayload();
				duplicateSavePath = saveAlreadyExists(saveProfile.getSavePath());

				if (duplicateSavePath) {
					Popup.displaySimpleAlert("Save is already being managed!", stage, MessageType.WARN);
				} else {
					//Remove the default save profile that isn't actually a profile if it's all that we have in the list.
					boolean duplicateProfileName;
					do {
						PROFILE_INPUT_VIEW.getInput().clear();
						PROFILE_INPUT_VIEW.getInput().requestFocus();
						PROFILE_INPUT_VIEW.show();
						duplicateProfileName = profileNameAlreadyExists(PROFILE_INPUT_VIEW.getInput().getText());

						if (duplicateProfileName) {
							Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
						} else if (!PROFILE_INPUT_VIEW.getInput().getText().isBlank()) {
							saveProfile.setProfileName(PROFILE_INPUT_VIEW.getInput().getText());
							if (SAVE_PROFILES.size() == 1 && SAVE_PROFILES.getFirst().getSaveName().equals("None") && SAVE_PROFILES.getFirst().getProfileName().equals("None") && SAVE_PROFILES.getFirst().getSavePath() == null) {
								saveProfile.setSaveExists(true);
								SAVE_PROFILES.set(0, saveProfile);
								UI_SERVICE.setCurrentSaveProfile(saveProfile);

								saveList.refresh();
							} else {
								SAVE_PROFILES.add(saveProfile);
								result.addMessage("Successfully added profile " + saveProfile.getSaveName() + " to save list.", ResultType.SUCCESS);
								UI_SERVICE.log(result);

								PROFILE_INPUT_VIEW.getInput().clear();
							}
							UI_SERVICE.saveUserData();
						}
					} while (duplicateProfileName);
				}
			}
		} while (result.isSuccess() && duplicateSavePath);

		//Cleanup our UI actions.
		SAVE_INPUT_VIEW.getSaveName().setText("No save selected.");
		SAVE_INPUT_VIEW.setSelectedSave(null);
		PROFILE_INPUT_VIEW.getInput().clear();
	}

	@FXML
	private void copySave() {
		if (saveList.getSelectionModel().getSelectedItem() != null) {
			if (saveList.getSelectionModel().getSelectedItem().isSaveExists()) {

				operationInProgressDimmer.setVisible(true);
				progressIndicator.setVisible(true);
				saveList.setMouseTransparent(true);
				Thread copyThread = getCopyThread();
				copyThread.start();
			} else {
				Popup.displaySimpleAlert("You cannot copy a profile that is missing its save!", stage, MessageType.ERROR);
			}
		} else {
			Popup.displaySimpleAlert("You have to select a profile first!", stage, MessageType.ERROR);
		}
	}

	//Create a new thread that runs the code for copying profiles
	private Thread getCopyThread() {
		final Task<Result<SaveProfile>> TASK = new Task<>() {
			@Override
			protected Result<SaveProfile> call() throws Exception {
				return UI_SERVICE.copySaveProfile(saveList.getSelectionModel().getSelectedItem());
			}
		};

		TASK.setOnSucceeded(event -> Platform.runLater(() -> {
			Result<SaveProfile> profileCopyResult = TASK.getValue();

			if (profileCopyResult.isSuccess()) {
				SAVE_PROFILES.add(profileCopyResult.getPayload());
			} else {
				Popup.displaySimpleAlert(profileCopyResult, stage);
			}

			UI_SERVICE.log(profileCopyResult);
			operationInProgressDimmer.setVisible(false);
			progressIndicator.setVisible(false);
			saveList.setMouseTransparent(false);
			UI_SERVICE.saveUserData();
		}));

		Thread thread = Thread.ofVirtual().unstarted(TASK);
		thread.setDaemon(true);
		return thread;
	}

	@FXML
	private void removeSave() {
		if (UI_SERVICE.getCurrentSaveProfile().equals(saveList.getSelectionModel().getSelectedItem())) {
			Popup.displaySimpleAlert("You cannot remove the active profile.", stage, MessageType.WARN);
		} else {
			int choice = Popup.displayYesNoDialog("Are you sure you want to delete this profile? It will not delete the save itself from the saves folder, ONLY the profile used by SEMM.", stage, MessageType.WARN);
			if (choice == 1) {
				int profileIndex = saveList.getSelectionModel().getSelectedIndex();
				SAVE_PROFILES.remove(profileIndex);
				if (profileIndex > SAVE_PROFILES.size()) {
					saveList.getSelectionModel().select(profileIndex - 1);
				} else {
					saveList.getSelectionModel().select(profileIndex);
				}
				UI_SERVICE.saveUserData();
			}
		}
	}

	@FXML
	private void renameProfile() {
		PROFILE_INPUT_VIEW.getInput().clear();
		PROFILE_INPUT_VIEW.getInput().requestFocus();
		if (saveList.getSelectionModel().getSelectedItem() != null) {


			PROFILE_INPUT_VIEW.show();

			String newProfileName = PROFILE_INPUT_VIEW.getInput().getText();
			if (profileNameAlreadyExists(newProfileName)) {
				Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
			} else if (!newProfileName.isBlank()) {
				saveList.getSelectionModel().getSelectedItem().setProfileName(newProfileName);
				saveList.refresh();
				UI_SERVICE.saveUserData();
			}
		}
	}

	@FXML
	private void selectSave() {
		UI_SERVICE.setCurrentSaveProfile(saveList.getSelectionModel().getSelectedItem());
		modTableContextBarView.getSaveProfileDropdown().getSelectionModel().select(saveList.getSelectionModel().getSelectedItem());
	}

	@FXML
	private void closeSaveWindow() {
		stage.close();
		saveList.getSelectionModel().clearSelection();
		Platform.exitNestedEventLoop(stage, null);
	}

	private boolean saveAlreadyExists(String savePath) {
		return SAVE_PROFILES.stream()
				.anyMatch(saveProfile -> saveProfile.getSavePath() != null && saveProfile.getSavePath().equals(savePath));
	}

	private boolean profileNameAlreadyExists(String profileName) {
		return SAVE_PROFILES.stream()
				.anyMatch(saveProfile -> saveProfile.getProfileName().equals(profileName));
	}

	public void show() {
		stage.show();
		TitleBarUtility.SetTitleBar(stage);
		Platform.enterNestedEventLoop(stage);
	}
}
