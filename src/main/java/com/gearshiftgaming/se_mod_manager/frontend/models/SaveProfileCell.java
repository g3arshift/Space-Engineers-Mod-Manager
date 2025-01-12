package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.ListCellUtility;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import lombok.Getter;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */

@Getter
public abstract class SaveProfileCell extends ListCell<SaveProfile> {

	private final Text PROFILE_NAME = new Text();

	private final Tooltip SAVE_NAME = new Tooltip();

	private final Region REGION = new Region();

	private final StackPane STACK_PANE = new StackPane(PROFILE_NAME, REGION);

	private final HBox LAYOUT = new HBox(STACK_PANE);

	private final String cellStyle;

	private final UiService UI_SERVICE;

	public SaveProfileCell(String cellStyle, UiService uiService) {
		super();
		this.UI_SERVICE = uiService;
		HBox.setHgrow(STACK_PANE, Priority.ALWAYS);
		STACK_PANE.setAlignment(Pos.CENTER_LEFT);
		this.cellStyle = cellStyle;
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
			getPROFILE_NAME().setText(item.getProfileName());

			if (!item.isSaveExists()) {
				getPROFILE_NAME().setStyle("-fx-fill: -color-danger-emphasis;");
				getPROFILE_NAME().setStrikethrough(true);
			}

			Tooltip.install(getREGION(), getSAVE_NAME());
			setGraphic(getLAYOUT());

			StringBuilder styleBuilder = new StringBuilder(cellStyle);
			if (this.isSelected()) {
				styleBuilder.append("-color-cell-fg-selected: -color-fg-default;")
						.append("-color-cell-fg-selected-focused: -color-fg-default;")
						.append(ListCellUtility.getSelectedCellColor(UI_SERVICE.getUSER_CONFIGURATION().getUserTheme()));
			}
			setStyle(styleBuilder.toString());
		}
	}
}
