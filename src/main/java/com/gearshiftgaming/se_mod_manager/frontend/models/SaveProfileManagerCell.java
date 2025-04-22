package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.TextTruncationUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.ListCellUtility;
import javafx.scene.control.Tooltip;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SaveProfileManagerCell extends SaveProfileCell {

	private final UiService UI_SERVICE;

	public SaveProfileManagerCell(final UiService UI_SERVICE) {
		super("-fx-border-color: transparent transparent -color-border-muted transparent; -fx-border-width: 1px; -fx-border-insets: 0 5 0 5;", UI_SERVICE);
		this.UI_SERVICE = UI_SERVICE;
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
			getPROFILE_NAME().setText(TextTruncationUtility.truncateWithEllipsisWithRealWidth(item.getProfileName(), 600));

			if (!item.isSaveExists()) {
				getPROFILE_NAME().setStyle("-fx-fill: -color-danger-emphasis;");
				getPROFILE_NAME().setStrikethrough(true);
			}

			Tooltip.install(getREGION(), getSAVE_NAME());
			setGraphic(getLAYOUT());

			StringBuilder styleBuilder = new StringBuilder(getCellStyle());
			if(item.equals(UI_SERVICE.getCurrentSaveProfile())) {
				styleBuilder.append("-fx-border-color: -color-accent-emphasis;")
						.append("-fx-border-width: 2px 0px 2px 0px;");
			}

			if (this.isSelected()) {
				styleBuilder.append("-color-cell-fg-selected: -color-fg-default;")
						.append("-color-cell-fg-selected-focused: -color-fg-default;")
						.append(ListCellUtility.getSelectedCellColor(UI_SERVICE.getUSER_CONFIGURATION().getUserTheme()));
			}
			setStyle(styleBuilder.toString());
		}
	}
}
