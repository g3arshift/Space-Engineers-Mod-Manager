package com.gearshiftgaming.se_mod_manager.frontend.view;

import atlantafx.base.controls.RingProgressIndicator;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.SaveProfileCell;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;

import java.util.Objects;
import java.util.Properties;


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
	private RingProgressIndicator progressIndicator;

	@Getter
	private Stage stage;

	private ObservableList<SaveProfile> saveProfiles;

	private UiService uiService;

	private SaveInputView saveInputViewView;

	private ProfileInputView profileInputView;

	private MenuBarView menuBarView;

	public void initView(Parent root, UiService uiService,
						 SaveInputView saveInputViewFirstStepView, ProfileInputView saveListInputSecondStepView,
						 Properties properties, MenuBarView menuBarView) {

		Scene scene = new Scene(root);
		this.uiService = uiService;
		saveProfiles = uiService.getSaveProfiles();

		this.saveInputViewView = saveInputViewFirstStepView;
		this.profileInputView = saveListInputSecondStepView;
		this.menuBarView = menuBarView;

		stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);

		stage.setTitle("Save Manager");
		stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

		stage.setMinWidth(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minWidth")));
		stage.setMinHeight(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minHeight")));

		saveList.setItems(saveProfiles);
		saveList.setCellFactory(param -> new SaveProfileCell());

		saveList.setStyle("-fx-background-color: -color-bg-default;");

		stage.setScene(scene);
		uiService.logPrivate("Successfully initialized save manager.", MessageType.INFO);
	}

	@FXML
	private void addSave() {
		boolean duplicateSavePath = false;
		Result<SaveProfile> result;
		//Get our selected file from the user, check if its already being managed by SEMM by checking the save path, and then check if the save name already exists. If it does, append a number to the end of it.
		do {
			saveInputViewView.getStage().showAndWait();
			result = saveInputViewView.getSaveProfileResult();
			if (result.isSuccess()) {
				SaveProfile saveProfile = result.getPayload();
				duplicateSavePath = saveAlreadyExists(saveProfile.getSavePath());

				if (duplicateSavePath) {
					Popup.displaySimpleAlert("Save is already being managed!", stage, MessageType.WARN);
				} else {
					//Remove the default save profile that isn't actually a profile if it's all that we have in the list.
					boolean duplicateProfileName;
					do {
						profileInputView.getProfileNameInput().clear();
						profileInputView.getProfileNameInput().requestFocus();
						profileInputView.getStage().showAndWait();
						duplicateProfileName = profileNameAlreadyExists(profileInputView.getProfileNameInput().getText());

						if (duplicateProfileName) {
							Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
						} else if (!profileInputView.getProfileNameInput().getText().isBlank()) {
							saveProfile.setProfileName(profileInputView.getProfileNameInput().getText());
							if (saveProfiles.size() == 1 && saveProfiles.getFirst().getSaveName().equals("None") && saveProfiles.getFirst().getProfileName().equals("None") && saveProfiles.getFirst().getSavePath() == null) {
								saveProfile.setSaveExists(true);
								saveProfiles.set(0, saveProfile);
								uiService.setCurrentSaveProfile(saveProfile);

								//TODO: This only partially works. It fixes the styling, but leaves the text as "None" for the button cell.
								ListCell<SaveProfile> buttonCellFix = new SaveProfileCell();
								buttonCellFix.setItem(saveProfile);
								buttonCellFix.setText(saveProfile.getProfileName());
								menuBarView.getSaveProfileDropdown().setButtonCell(buttonCellFix);
								saveList.refresh();
							} else {
								saveProfiles.add(saveProfile);
								result.addMessage("Successfully added profile " + saveProfile.getSaveName() + " to save list.", ResultType.SUCCESS);
								uiService.log(result);

								profileInputView.getProfileNameInput().clear();
							}
							uiService.saveUserData();
						}
					} while (duplicateProfileName);
				}
			}
		} while (result.isSuccess() && duplicateSavePath);

		//Cleanup our UI actions.
		saveInputViewView.getSaveName().setText("No save selected.");
		saveInputViewView.setSelectedSave(null);
		profileInputView.getProfileNameInput().clear();
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
		final Task<Result<SaveProfile>> task = new Task<>() {
			@Override
			protected Result<SaveProfile> call() throws Exception {
				return uiService.copySaveProfile(saveList.getSelectionModel().getSelectedItem());
			}
		};

		task.setOnSucceeded(event -> {
			Result<SaveProfile> profileCopyResult = task.getValue();

			if (profileCopyResult.isSuccess()) {
				saveProfiles.add(profileCopyResult.getPayload());
			} else {
				Popup.displaySimpleAlert(profileCopyResult, stage);
			}

			uiService.log(profileCopyResult);
			operationInProgressDimmer.setVisible(false);
			progressIndicator.setVisible(false);
			saveList.setMouseTransparent(false);
			uiService.saveUserData();
		});

		Thread thread = new Thread(task);
		thread.setDaemon(true);
		return thread;
	}

	@FXML
	private void removeSave() {
		if (uiService.getCurrentSaveProfile().equals(saveList.getSelectionModel().getSelectedItem())) {
			Popup.displaySimpleAlert("You cannot remove the active profile.", stage, MessageType.WARN);
		} else {
			int choice = Popup.displayYesNoDialog("Are you sure you want to delete this profile? It will not delete the save itself from the saves folder, ONLY the profile used by SEMM.", stage, MessageType.WARN);
			if (choice == 1) {
				int profileIndex = saveList.getSelectionModel().getSelectedIndex();
				saveProfiles.remove(profileIndex);
				if(profileIndex > saveProfiles.size()) {
					saveList.getSelectionModel().select(profileIndex - 1);
				} else {
					saveList.getSelectionModel().select(profileIndex);
				}
				uiService.saveUserData();
			}
		}
	}

	@FXML
	private void renameProfile() {
		profileInputView.getProfileNameInput().clear();
		profileInputView.getProfileNameInput().requestFocus();
		profileInputView.getStage().showAndWait();

		String newProfileName = profileInputView.getProfileNameInput().getText();
		if(profileNameAlreadyExists(newProfileName)) {
			Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
		} else if (!newProfileName.isBlank()){
			saveList.getSelectionModel().getSelectedItem().setProfileName(newProfileName);
			saveList.refresh();
			uiService.saveUserData();
		}
	}

	@FXML
	private void selectSave() {
		uiService.setCurrentSaveProfile(saveList.getSelectionModel().getSelectedItem());
		menuBarView.getSaveProfileDropdown().getSelectionModel().select(saveList.getSelectionModel().getSelectedItem());
	}

	@FXML
	private void closeSaveWindow() {
		stage.close();
	}

	private boolean saveAlreadyExists(String savePath) {
		return saveProfiles.stream()
				.anyMatch(saveProfile -> saveProfile.getSavePath() != null && saveProfile.getSavePath().equals(savePath));
	}

	private boolean profileNameAlreadyExists(String profileName) {
		return saveProfiles.stream()
				.anyMatch(saveProfile -> saveProfile.getProfileName().equals(profileName));
	}
}
