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
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;


/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
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

	private SaveListInput saveListInputFirstStepView;

	private ProfileInputView saveListInputSecondStepView;

	private MenuBarView topBarView;

	public void initView(Parent root, UiService uiService,
						 SaveListInput saveListInputFirstStepView, ProfileInputView saveListInputSecondStepView,
						 Properties properties, MenuBarView topBarView) {

		Scene scene = new Scene(root);
		this.uiService = uiService;
		saveProfiles = uiService.getSaveProfiles();

		this.saveListInputFirstStepView = saveListInputFirstStepView;
		this.saveListInputSecondStepView = saveListInputSecondStepView;
		this.topBarView = topBarView;

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
		uiService.getLogger().info("Successfully initialized save manager.");
	}

	@FXML
	private void addSave() throws IOException {
		boolean duplicateSavePath = false;
		Result<SaveProfile> result;
		//Get our selected file from the user, check if its already being managed by SEMM by checking the save path, and then check if the save name already exists. If it does, append a number to the end of it.
		do {
			saveListInputFirstStepView.getStage().showAndWait();
			result = saveListInputFirstStepView.getSaveProfileResult();
			if (result.isSuccess()) {
				SaveProfile saveProfile = result.getPayload();
				duplicateSavePath = saveAlreadyExists(saveProfile.getSavePath());

				if (duplicateSavePath) {
					Popup.displaySimpleAlert("Save is already being managed!", stage, MessageType.WARN);
				} else {
					//Remove the default save profile that isn't actually a profile if it's all that we have in the list.
					//Technically a user could enter this by mistake, but it's extremely unlikely.
					boolean duplicateProfileName;
					do {
						saveListInputSecondStepView.getProfileNameInput().clear();
						saveListInputSecondStepView.getProfileNameInput().requestFocus();
						saveListInputSecondStepView.getStage().showAndWait();
						duplicateProfileName = profileNameAlreadyExists(saveListInputSecondStepView.getProfileNameInput().getText());

						if (duplicateProfileName) {
							Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
						} else if (!saveListInputSecondStepView.getProfileNameInput().getText().isBlank()) {
							saveProfile.setProfileName(saveListInputSecondStepView.getProfileNameInput().getText());
							if (saveProfiles.size() == 1 &&
									saveProfiles.getFirst().getSaveName().equals("None") &&
									saveProfiles.getFirst().getProfileName().equals("None") &&
									saveProfiles.getFirst().getSavePath() == null) {
								saveProfile.setSaveExists(true);
								saveProfiles.set(0, saveProfile);
								uiService.setCurrentSaveProfile(saveProfile);
							} else {
								saveProfiles.add(saveProfile);
								result.addMessage("Successfully added profile " + saveProfile.getSaveName() + " to save list.", ResultType.SUCCESS);
								uiService.log(result);

								saveListInputSecondStepView.getProfileNameInput().clear();
							}
						}
					} while (duplicateProfileName);
				}
			}
		} while (result.isSuccess() && duplicateSavePath);

		//Cleanup our UI actions.
		saveListInputFirstStepView.getSaveName().setText("No save selected.");
		saveListInputFirstStepView.setSelectedSave(null);
		saveListInputSecondStepView.getProfileNameInput().clear();
	}

	@FXML
	private void copySave() throws IOException {
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

	private Thread getCopyThread() {
		final Task<Result<SaveProfile>> task = new Task<>() {
			@Override
			protected Result<SaveProfile> call() throws Exception {
				return uiService.getBackendController().copySaveProfile(saveList.getSelectionModel().getSelectedItem());
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
		});

		Thread thread = new Thread(task);
		thread.setDaemon(true);
		return thread;
	}

	@FXML
	private void removeSave() {
		//TODO: Add active profile check and minimum size check.
		saveProfiles.remove(saveList.getSelectionModel().getSelectedIndex());
	}

	@FXML
	private void renameProfile() {
		//TODO: Implement
		//TODO: Add a button to rename save profiles, that will also rename them in the file structure! MAke sure it renames the session name in Sandbox.sbc and Sandbox_config.sbc, as well as the folder name.
		//TODO: Rename the profile in the list, then replicate to backend. If the backend rename fails, revert.
	}

	@FXML
	private void selectSave() {
		//TODO: Implement
	}

	@FXML
	private void closeSaveWindow() {
		stage.close();
	}

	private boolean saveAlreadyExists(String savePath) {
		return saveProfiles.stream()
				.anyMatch(saveProfile -> saveProfile.getSavePath() != null && saveProfile.getSavePath().equals(savePath));
	}

	private boolean saveNameAlreadyExists(String saveName) {
		return saveProfiles.stream()
				.anyMatch(saveProfile -> saveProfile.getSaveName().equals(saveName));
	}

	private boolean profileNameAlreadyExists(String profileName) {
		return saveProfiles.stream()
				.anyMatch(saveProfile -> saveProfile.getProfileName().equals(profileName));
	}
}
