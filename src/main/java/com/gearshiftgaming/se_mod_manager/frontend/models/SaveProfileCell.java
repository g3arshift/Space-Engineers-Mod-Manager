package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class SaveProfileCell extends ListCell<SaveProfile> {
	private final Label profileName = new Label();

	private final Tooltip saveName = new Tooltip();

	private final Region region = new Region();

	private final StackPane stackPane = new StackPane(profileName, region);

	private final HBox layout = new HBox(stackPane);

	public SaveProfileCell() {
		super();
		profileName.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(stackPane, Priority.ALWAYS);
		stackPane.setAlignment(Pos.CENTER_LEFT);
	}

	@Override
	protected void updateItem(SaveProfile item, boolean empty) {

		super.updateItem(item, empty);
		if (empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			//This lets a region span the entire width of the cell, and allows the tooltip to be visible even in the "empty" space.
			saveName.setText("Save name: " + item.getSaveName());
			profileName.setText(item.getProfileName());
			Tooltip.install(region, saveName);
			setStyle("-fx-border-color: transparent transparent -color-border-muted transparent; -fx-border-width: 1px; -fx-border-insets: 0 5 0 5;");
			setGraphic(layout);
		}
	}
}
