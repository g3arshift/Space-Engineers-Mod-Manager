package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.TextTruncationUtility;
import javafx.scene.control.Tooltip;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SaveProfileDropdownItemCell extends SaveProfileCell {


	public SaveProfileDropdownItemCell(UiService uiService) {
		super("", uiService);
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
			getPROFILE_NAME().setText(TextTruncationUtility.truncateWithEllipsis(item.getProfileName(), 240));

			if(!item.saveExists()) {
				getPROFILE_NAME().setStyle("-fx-fill: -color-danger-emphasis;");
				getPROFILE_NAME().setStrikethrough(true);
			}
			Tooltip.install(getREGION(), getSAVE_NAME());
			setStyle(getCellStyle());
			setGraphic(getLAYOUT());
		}
	}
}