package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * <p>
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
public class StatusBarView {

	@FXML
	private Label lastInjected;

	@FXML
	private Label saveStatus;

	@FXML
	private Label lastModifiedBy;

	private String statusBaseStyling;

	private UiService uiService;

	private MainWindowView mainWindowView;

	/**
	 * Sets the initial values for the toolbar located at the bottom of the UI.
	 */
	public void initView(MainWindowView mainWindowView, UiService uiService) {
		this.uiService = uiService;
		this.mainWindowView = mainWindowView;

		Optional<SaveProfile> lastUsedSaveProfile = findLastUsedSaveProfile();
		if (lastUsedSaveProfile.isPresent()) {
			uiService.setCurrentSaveProfile(lastUsedSaveProfile.get());
			this.statusBaseStyling = "-fx-border-width: 1px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 2px;";

			updateSaveStatus(uiService.getCurrentSaveProfile());
			updateLastModifiedBy(uiService.getCurrentSaveProfile());

			//Set the text for the last time this profile was saved
			if (uiService.getCurrentSaveProfile().getLastSaved() == null || uiService.getCurrentSaveProfile().getLastSaved().isEmpty()) {
				lastInjected.setText("Never");
			} else {
				lastInjected.setText(uiService.getCurrentSaveProfile().getLastSaved());
			}
		} else {
			this.statusBaseStyling = "-fx-border-width: 1px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 2px;";

			saveStatus.setText("None");
			saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-neutral-emphasis;");

			lastModifiedBy.setText("None");
			lastModifiedBy.setStyle(statusBaseStyling += "-fx-border-color: -color-neutral-emphasis;");

			lastInjected.setText("Never");
		}

		uiService.getLogger().info("Successfully initialized status bar.");
	}

	private void updateInfoBar(SaveProfile saveProfile) {
		updateSaveStatus(saveProfile);
		updateLastModifiedBy(saveProfile);
		updateLastInjected();
	}

	//TODO: MAybe make the graphic label also colored?
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

	private Optional<SaveProfile> findLastUsedSaveProfile() {
		Optional<SaveProfile> lastUsedSaveProfile = Optional.empty();
		if (uiService.getUserConfiguration().getLastUsedSaveProfileId() != null) {
			UUID lastUsedSaveProfileId = uiService.getUserConfiguration().getLastUsedSaveProfileId();

			lastUsedSaveProfile = uiService.getSaveProfiles().stream()
					.filter(saveProfile -> saveProfile.getId().equals(lastUsedSaveProfileId))
					.findFirst();
		}
		return lastUsedSaveProfile;
	}

	public void update(SaveProfile saveProfile) {

	}
}
