package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
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
            setStyle(null);
        } else {
            profileName.setText(item.getProfileName());
            setStyle("-fx-border-color: transparent transparent -color-border-muted transparent; -fx-border-width: 1px; -fx-border-insets: 0 5 0 5;");
            setGraphic(profileName);
        }
    }
}
