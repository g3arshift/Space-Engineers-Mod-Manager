package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.TextTruncationUtility;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModProfileDropdownItemCell extends ModProfileCell {

	public ModProfileDropdownItemCell() {
		super("");
	}

	@Override
	protected void updateItem(ModProfile item, boolean empty) {
		super.updateItem(item, empty);
		if(empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			final Region REGION = new Region();
			final Tooltip EXTENDED_PROFILE_NAME = new Tooltip();
			StackPane STACK_PANE = new StackPane(new Text(TextTruncationUtility.truncateWithEllipsis(item.getProfileName(), 240)), REGION);
			HBox LAYOUT = new HBox(STACK_PANE);
			HBox.setHgrow(STACK_PANE, Priority.ALWAYS);
			STACK_PANE.setAlignment(Pos.CENTER_LEFT);

			EXTENDED_PROFILE_NAME.setText(TextTruncationUtility.truncateWithEllipsis("Profile Name: " + item.getProfileName(), 600));
			Tooltip.install(REGION, EXTENDED_PROFILE_NAME);

			setStyle(getCellStyle());
			setGraphic(LAYOUT);
		}
	}
}
