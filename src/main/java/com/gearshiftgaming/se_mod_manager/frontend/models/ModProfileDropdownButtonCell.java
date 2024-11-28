package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.DropdownLabelUtility;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModProfileDropdownButtonCell extends ModProfileCell {

	public ModProfileDropdownButtonCell() {
		super("");
	}

	@Override
	protected void updateItem(ModProfile item, boolean empty) {
		super.updateItem(item, empty);
		if(empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			getPROFILE_NAME().setText(DropdownLabelUtility.truncateWithEllipsisWithRealWidth(item.getProfileName(), 165));
			setStyle(getCellStyle());
			setGraphic(getPROFILE_NAME());
		}
	}
}
