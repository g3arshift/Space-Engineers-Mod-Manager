package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
public class SaveProfileCell extends ListCell<SaveProfile> {
	private final Text profileName = new Text();

	private final Tooltip saveName = new Tooltip();

	private final Region region = new Region();

	private final StackPane stackPane = new StackPane(profileName, region);

	private final HBox layout = new HBox(stackPane);

	public SaveProfileCell() {
		super();
		HBox.setHgrow(stackPane, Priority.ALWAYS);
		stackPane.setAlignment(Pos.CENTER_LEFT);
	}

	@Override
	protected void updateItem(SaveProfile item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setStyle(null);
			setGraphic(null);
		} else {
			String style = "-fx-border-color: transparent transparent -color-border-muted transparent; -fx-border-width: 1px; -fx-border-insets: 0 5 0 5";
			//This lets a region span the entire width of the cell, and allows the tooltip to be visible even in the "empty" space.
			saveName.setText("Save name: " + item.getSaveName());
			profileName.setText(item.getProfileName());
			if(!item.isSaveExists()) {
				//TODO: Sometimes this throws an error about lookup for -fx-fill,but it doesn't seem to actually cause a problem.
				profileName.setStyle("-fx-fill: -color-danger-emphasis;");
				profileName.setStrikethrough(true);
			}
			Tooltip.install(region, saveName);
			setStyle(style);
			setGraphic(layout);
		}
	}
}
