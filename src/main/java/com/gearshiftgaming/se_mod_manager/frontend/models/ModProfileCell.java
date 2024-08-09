package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

public class ModProfileCell extends ListCell<ModProfile> {
    private final Label profileName = new Label();

    public ModProfileCell() {
        super();
        profileName.setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(ModProfile item, boolean empty) {
        super.updateItem(item, empty);

        if(empty || item == null) {
            setGraphic(null);
        } else {
            profileName.setText(item.getProfileName());
            //TODO: Not 100% happy with this, but it'll do for now.
            setStyle("-fx-border-color: transparent transparent -color-border-muted transparent; -fx-border-width: 1px; -fx-border-insets: 0 5 0 5;");
            setGraphic(profileName);
        }
    }
}
