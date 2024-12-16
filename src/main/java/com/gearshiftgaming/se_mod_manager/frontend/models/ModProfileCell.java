package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.ModlistProfile;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import lombok.Getter;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */

@Getter
public abstract class ModProfileCell extends ListCell<ModlistProfile> {
    private final Label PROFILE_NAME = new Label();

    private final String cellStyle;

    public ModProfileCell(String cellStyle) {
        super();
        PROFILE_NAME.setAlignment(Pos.CENTER_LEFT);
        this.cellStyle = cellStyle;
    }

    @Override
    protected void updateItem(ModlistProfile item, boolean empty) {
        super.updateItem(item, empty);
        if(empty || item == null) {
            setGraphic(null);
            setStyle(null);
        } else {
            PROFILE_NAME.setText(item.getProfileName());
            setStyle(cellStyle);
            setGraphic(PROFILE_NAME);
        }
    }
}
