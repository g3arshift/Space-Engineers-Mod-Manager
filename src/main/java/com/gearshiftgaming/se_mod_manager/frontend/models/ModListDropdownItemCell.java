package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.SpaceEngineersVersion;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.TextTruncationUtility;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.apache.commons.lang3.tuple.MutableTriple;

import java.util.UUID;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModListDropdownItemCell extends ModListCell {

	public ModListDropdownItemCell(UiService uiService) {
		super("", uiService);
	}

	@Override
	protected void updateItem(MutableTriple<UUID, String, SpaceEngineersVersion> item, boolean empty) {
		super.updateItem(item, empty);
		if(empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			//TODO: Minor bug where this gets scrollbars on the dropdown when the button cell truncates.
			// They go away after you change profile names though and don't come back.
			final Region REGION = new Region();
			final Tooltip EXTENDED_PROFILE_NAME = new Tooltip();
			StackPane STACK_PANE = new StackPane(new Text(TextTruncationUtility.truncateWithEllipsis(item.getMiddle(), 240)), REGION);
			HBox LAYOUT = new HBox(STACK_PANE);
			HBox.setHgrow(STACK_PANE, Priority.ALWAYS);
			STACK_PANE.setAlignment(Pos.CENTER_LEFT);

			EXTENDED_PROFILE_NAME.setText(TextTruncationUtility.truncateWithEllipsis("Profile Name: " + item.getMiddle(), 600));
			Tooltip.install(REGION, EXTENDED_PROFILE_NAME);

			setStyle(getCellStyle());
			setGraphic(LAYOUT);
		}
	}
}
