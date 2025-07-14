package com.gearshiftgaming.se_mod_manager.frontend.models.modlistmanager;

import com.gearshiftgaming.se_mod_manager.backend.models.shared.SpaceEngineersVersion;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.shared.ModListCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.TextTruncationUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.ListCellUtility;
import org.apache.commons.lang3.tuple.MutableTriple;

import java.util.UUID;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModListManagerCell extends ModListCell {

	private final UiService UI_SERVICE;

	public ModListManagerCell(final UiService UI_SERVICE) {
		super("-fx-border-color: transparent transparent -color-border-muted transparent; -fx-border-width: 1px; -fx-border-insets: 0 5 0 5;", UI_SERVICE);
		this.UI_SERVICE = UI_SERVICE;
	}

	@Override
	protected void updateItem(MutableTriple<UUID, String , SpaceEngineersVersion> item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			getPROFILE_NAME().setText(TextTruncationUtility.truncateWithEllipsisWithRealWidth(item.getMiddle(), 600));
			setGraphic(getPROFILE_NAME());

			StringBuilder styleBuilder = new StringBuilder(getCellStyle());
			if(item.getLeft().equals(UI_SERVICE.getCurrentModListProfile().getId())) {
				styleBuilder.append("-fx-border-color: -color-accent-emphasis;")
						.append("-fx-border-width: 2px 0px 2px 0px;");
			}

			if (this.isSelected()) {
				styleBuilder.append("-color-cell-fg-selected: -color-fg-default;")
						.append("-color-cell-fg-selected-focused: -color-fg-default;")
						.append(ListCellUtility.getSelectedCellColor(UI_SERVICE.getUserConfiguration().getUserTheme()));
			}
			setStyle(styleBuilder.toString());
		}
	}
}