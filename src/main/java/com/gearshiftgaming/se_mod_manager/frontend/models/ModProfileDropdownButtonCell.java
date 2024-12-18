package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.ModlistProfile;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.TextTruncationUtility;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModProfileDropdownButtonCell extends ModProfileCell {

	public ModProfileDropdownButtonCell(String themeName) {
		super("", themeName);
	}

	@Override
	protected void updateItem(ModlistProfile item, boolean empty) {
		super.updateItem(item, empty);
		if(empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			getPROFILE_NAME().setText(TextTruncationUtility.truncateWithEllipsisWithRealWidth(item.getProfileName(), this.getWidth()));
			setStyle(getCellStyle());
			setGraphic(getPROFILE_NAME());
		}
	}
}
