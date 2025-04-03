package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * <p>
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public class StatusBar {

	@FXML
	private Label lastInjected;

	@FXML
	private Label lastSaveModifiedName;

	@FXML
	private Label lastModlistAppliedName;

	@FXML
	@Getter
	private Label saveStatus;

	private String statusBaseStyling;

	private final UiService UI_SERVICE;

	/**
	 * Sets the initial values for the toolbar located at the bottom of the UI.
	 */

	public StatusBar(UiService UI_SERVICE) {
		this.UI_SERVICE = UI_SERVICE;
	}

	public void initView() {
		Optional<SaveProfile> lastUsedSaveProfile = findLastModifiedSaveProfileId();
		if (lastUsedSaveProfile.isPresent()) {
			this.statusBaseStyling = "-fx-border-width: 1px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 2px;";

			//Set the text for the last time this profile was saved
			if (UI_SERVICE.getCurrentSaveProfile().getLastSaved() == null || UI_SERVICE.getCurrentSaveProfile().getLastSaved().isEmpty()) {
				lastInjected.setText("Never");
			} else {
				lastInjected.setText(UI_SERVICE.getCurrentSaveProfile().getLastSaved());
			}

			lastSaveModifiedName.setText(lastUsedSaveProfile.get().getProfileName());
			Optional<ModListProfile> lastAppliedModlistProfile = UI_SERVICE.getMODLIST_PROFILE_IDS().stream()
							.filter(modlistProfile -> modlistProfile.getID().equals(lastUsedSaveProfile.get().getLastUsedModProfileId()))
									.findFirst();

			lastAppliedModlistProfile.ifPresentOrElse(modlistProfile -> lastModlistAppliedName.setText(modlistProfile.getProfileName()), () -> {
				lastModlistAppliedName.setText("None");
			});

			updateSaveStatus(UI_SERVICE.getCurrentSaveProfile());
		} else {
			this.statusBaseStyling = "-fx-border-width: 1px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 2px;";

			lastInjected.setText("Never");

			lastSaveModifiedName.setText("None");

			lastModlistAppliedName.setText("None");

			saveStatus.setText("None");
			saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-neutral-emphasis;");
		}

		UI_SERVICE.logPrivate("Successfully initialized status bar.", MessageType.INFO);
	}

	private void updateSaveStatus(SaveProfile saveProfile) {
		switch (saveProfile.getLastSaveStatus()) {
			case SAVED -> {
				saveStatus.setText("Modlist Applied");
				saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-success-emphasis; -fx-text-fill: -color-success-emphasis;");
			}
			case UNSAVED -> {
				saveStatus.setText("Unsaved Modlist Changes");
				saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-warning-emphasis; -fx-text-fill: -color-warning-emphasis;");
			}
			case FAILED -> {
				saveStatus.setText("Failed to Apply Modlist");
				saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-danger-emphasis; -fx-text-fill: -color-danger-emphasis;");
			}
			default -> {
				saveStatus.setText("N/A");
				saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-neutral-emphasis;");
			}
		}
	}

	private void updateLastInjected() {
		lastInjected.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d',' yyyy '@' h:mma")));
	}

	private Optional<SaveProfile> findLastModifiedSaveProfileId() {
		Optional<SaveProfile> lastUsedSaveProfile = Optional.empty();
		if (UI_SERVICE.getUSER_CONFIGURATION().getLastModifiedSaveProfileId() != null) {
			UUID lastUsedSaveProfileId = UI_SERVICE.getUSER_CONFIGURATION().getLastModifiedSaveProfileId();

			lastUsedSaveProfile = UI_SERVICE.getSAVE_PROFILES().stream()
					.filter(saveProfile -> saveProfile.getID().equals(lastUsedSaveProfileId))
					.findFirst();
		}
		return lastUsedSaveProfile;
	}

	public void update(SaveProfile saveProfile, ModListProfile modListProfile) {
		updateSaveStatus(saveProfile);
		updateLastInjected();
		lastModlistAppliedName.setText(modListProfile.getProfileName());
		lastSaveModifiedName.setText(saveProfile.getProfileName());
	}
}
