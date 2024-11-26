package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.frontend.models.helper.DropdownLabelHelper;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SaveProfileDropdownButtonCell extends SaveProfileCell{

	private final int preferredWidth;

	public SaveProfileDropdownButtonCell() {
		super("");
		preferredWidth = 175;
		getLAYOUT().setMaxWidth(preferredWidth);
		getLAYOUT().setPrefWidth(preferredWidth);
		getLAYOUT().setMinWidth(preferredWidth);
	}

	@Override
	protected void updateItem(SaveProfile item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setStyle(null);
			setGraphic(null);
		} else {
			//This lets a region span the entire width of the cell, and allows the tooltip to be visible even in the "empty" space.
			getSAVE_NAME().setText("Save name: " + item.getSaveName());
			getPROFILE_NAME().setText(DropdownLabelHelper.truncateWithEllipsisWithRealWidth(item.getProfileName(), preferredWidth));

			if(!item.isSaveExists()) {
				getPROFILE_NAME().setStyle("-fx-fill: -color-danger-emphasis;");
				getPROFILE_NAME().setStrikethrough(true);
			}
			Tooltip.install(getREGION(), getSAVE_NAME());
			setStyle(getCellStyle());
			setGraphic(getLAYOUT());
		}
	}
}
