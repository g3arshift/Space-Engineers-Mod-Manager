package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SpaceEngineersVersion;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.ListCellUtility;
import javafx.scene.control.ListCell;
import javafx.scene.text.Text;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Triple;

import java.util.UUID;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */

@Getter
public abstract class ModListCell extends ListCell<Triple<UUID, String, SpaceEngineersVersion>> {
    private final Text PROFILE_NAME = new Text();

    private final String cellStyle;

    private final UiService UI_SERVICE;

    public ModListCell(String cellStyle, UiService uiService) {
        super();
        this.cellStyle = cellStyle;
        this.UI_SERVICE = uiService;
    }

    @Override
    protected void updateItem(Triple<UUID, String, SpaceEngineersVersion> item, boolean empty) {
        super.updateItem(item, empty);
        if(empty || item == null) {
            setGraphic(null);
            setStyle(null);
        } else {
            PROFILE_NAME.setText(item.getMiddle());
            setGraphic(PROFILE_NAME);

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
