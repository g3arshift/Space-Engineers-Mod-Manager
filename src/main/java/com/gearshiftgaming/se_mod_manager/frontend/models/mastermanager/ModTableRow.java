package com.gearshiftgaming.se_mod_manager.frontend.models.mastermanager;

import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.ListCellUtility;
import javafx.concurrent.Task;
import javafx.scene.control.TableRow;
import lombok.Getter;
import lombok.Setter;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public class ModTableRow extends TableRow<Mod> {

	private final UiService uiService;

	@Setter
    @Getter
	private Task<Result<Void>> modDownloadTask;

	public ModTableRow(UiService uiService) {
		super();
		this.uiService = uiService;
	}

	@Override
	protected void updateItem(Mod item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {

			if(this.isSelected()) {
				setStyle("-color-cell-fg-selected: -color-fg-default;" +
						"-color-cell-fg-selected-focused: -color-fg-default;" +
						ListCellUtility.getSelectedCellColor(uiService.getUserConfiguration().getUserTheme()));
			} else {
				StringBuilder styleBuilder = new StringBuilder();
				if (getIndex() % 2 == 0) {
					styleBuilder.append("-fx-background-color: -color-cell-border, -color-cell-bg;");
				} else {
					styleBuilder.append("-fx-background-color: -color-cell-border, -color-cell-bg-odd;");
				}

				styleBuilder.append("-fx-background-insets: 0, 0 0 1 0;" +
						"-fx-padding: 0;" +
						"-fx-cell-size: 2.8em;");
				setStyle(styleBuilder.toString());
			}
		}
	}
}
