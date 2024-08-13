package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

public class SaveProfileCell extends ListCell<SaveProfile> {
    private final Label saveName = new Label();

    public SaveProfileCell() {
        super();
        saveName.setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(SaveProfile item, boolean empty) {
        super.updateItem(item, empty);
        if(empty || item == null) {
            setGraphic(null);
            setStyle(null);
        } else {
            saveName.setText(item.getSaveName());
            setStyle("-fx-border-color: transparent transparent -color-border-muted transparent; -fx-border-width: 1px; -fx-border-insets: 0 5 0 5;");
            setGraphic(saveName);
        }
    }
}
