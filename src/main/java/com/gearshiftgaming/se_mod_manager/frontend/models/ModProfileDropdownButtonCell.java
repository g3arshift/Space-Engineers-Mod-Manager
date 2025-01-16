package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.ModlistProfile;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.TextTruncationUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.ListCellUtility;
import javafx.beans.value.ChangeListener;
import org.checkerframework.checker.guieffect.qual.UI;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModProfileDropdownButtonCell extends ModProfileCell {

	private final UiService UI_SERVICE;

	public ModProfileDropdownButtonCell(final UiService UI_SERVICE) {
		super("", UI_SERVICE);
		this.UI_SERVICE = UI_SERVICE;
	}

	@Override
	protected void updateItem(ModlistProfile item, boolean empty) {
		super.updateItem(item, empty);
		if(empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			getPROFILE_NAME().setText(TextTruncationUtility.truncateWithEllipsisWithRealWidth(item.getProfileName(), this.getWidth()));
			setGraphic(getPROFILE_NAME());

			StringBuilder styleBuilder = new StringBuilder(getCellStyle());
			if (this.isSelected()) {
				styleBuilder.append("-color-cell-fg-selected: -color-fg-default;")
						.append("-color-cell-fg-selected-focused: -color-fg-default;")
						.append(ListCellUtility.getSelectedCellColor(UI_SERVICE.getUSER_CONFIGURATION().getUserTheme()));
			}
			setStyle(styleBuilder.toString());
		}
	}
}
