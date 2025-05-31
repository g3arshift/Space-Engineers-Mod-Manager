package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lombok.Getter;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;

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
		loadStatusBarInfo();
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

	private void updateLastModifiedBy() {
		lastModlistAppliedName.setText(UI_SERVICE.getCurrentModListProfile().getProfileName());
	}

	public void update() {
		updateSaveStatus(UI_SERVICE.getCurrentSaveProfile());
		updateLastInjected();
		updateLastModifiedBy();
		lastModlistAppliedName.setText(UI_SERVICE.getCurrentModListProfile().getProfileName());
		lastSaveModifiedName.setText(UI_SERVICE.getCurrentSaveProfile().getProfileName());
	}

	//TODO: This isn't working right.
	// 1. When changing save profiles it always shows the CURRENT mod list profile as the last modlist applied.
	// 2. When changing save profiles it always shows the most recent applied mod list time as the one applied. It actually saves this to DB too.
	public void loadStatusBarInfo() {
		SaveProfile currentSaveProfile = UI_SERVICE.getCurrentSaveProfile();
		this.statusBaseStyling = "-fx-border-width: 1px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 2px;";
		if (currentSaveProfile.getLastSaveStatus() != SaveStatus.NONE) {
			updateSaveStatus(currentSaveProfile);
			updateLastModifiedBy();
			lastInjected.setText(currentSaveProfile.getLastSaved() != null ? currentSaveProfile.getLastSaved() : "Never");
		} else {
			lastInjected.setText("Never");
			lastSaveModifiedName.setText("None");
			lastModlistAppliedName.setText("None");
			saveStatus.setText("None");
			saveStatus.setStyle(statusBaseStyling += "-fx-border-color: -color-neutral-emphasis;");
		}
	}
}
