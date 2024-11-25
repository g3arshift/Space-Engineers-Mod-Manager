package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import lombok.Getter;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */

//TODO: DO the same abstraction for the mod profile cell.
public abstract class SaveProfileCell extends ListCell<SaveProfile> {

	@Getter
	private final Text PROFILE_NAME = new Text();

	private final Tooltip SAVE_NAME = new Tooltip();

	private final Region REGION = new Region();

	private final StackPane STACK_PANE = new StackPane(PROFILE_NAME, REGION);

	@Getter
	private final HBox LAYOUT = new HBox(STACK_PANE);

	private final String style;

	public SaveProfileCell(String style) {
		super();
		HBox.setHgrow(STACK_PANE, Priority.ALWAYS);
		STACK_PANE.setAlignment(Pos.CENTER_LEFT);
		this.style = style;
	}

	@Override
	protected void updateItem(SaveProfile item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setStyle(null);
			setGraphic(null);
		} else {
			//This lets a region span the entire width of the cell, and allows the tooltip to be visible even in the "empty" space.
			SAVE_NAME.setText("Save name: " + item.getSaveName());
			PROFILE_NAME.setText(item.getProfileName());

			//TODO: Erroneously this is bleeding over to other items
			if(!item.isSaveExists()) {
				PROFILE_NAME.setStyle("-fx-fill: -color-danger-emphasis;");
				PROFILE_NAME.setStrikethrough(true);
			}
			Tooltip.install(REGION, SAVE_NAME);
			setStyle(style);
			setGraphic(LAYOUT);
		}
	}
}
