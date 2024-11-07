package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public class LogCell extends ListCell<LogMessage> {
    private final FontIcon MESSAGE_ICON = new FontIcon();
    private final Label MESSAGE = new Label();

    private final HBox LAYOUT = new HBox(MESSAGE_ICON, MESSAGE);

    public LogCell() {
        super();
        LAYOUT.setAlignment(Pos.CENTER_LEFT);
        LAYOUT.setSpacing(5d);
        MESSAGE.setWrapText(true);
    }

    //TODO: We're getting a weird bug that's leaving empty cells sometimes when the message is especially large and the user clicks into the rows? Could be the cell resizing too large for the view maybe?
    @Override
    protected void updateItem(LogMessage item, boolean empty) {
        super.updateItem(item, empty);
        //If cells are empty or their contents null, remove them. Else, style them and add them.
        if (empty || item == null) {
            setGraphic(null);
            setStyle(null);
        } else {
            switch (item.getMESSAGE_TYPE()) {
                case INFO -> {
                    MESSAGE_ICON.setStyle("-fx-icon-color: -color-accent-emphasis;");
                    MESSAGE_ICON.setIconLiteral("ci-information-square");
                }
                case WARN -> {
                    MESSAGE_ICON.setStyle("-fx-icon-color: -color-warning-emphasis;");
                    MESSAGE_ICON.setIconLiteral("ci-warning-alt");
                }
                case ERROR -> {
                    MESSAGE_ICON.setStyle("-fx-icon-color: -color-danger-emphasis;");
                    MESSAGE_ICON.setIconLiteral("ci-warning-square");
                }
                default -> {
                    MESSAGE_ICON.setStyle("-fx-icon-color: -color-neutral-emphasis;");
                    MESSAGE_ICON.setIconLiteral("ci-unknown");
                }
            }
            //TODO: Reuse for the mod table!!!
            //Make every other row in the log a different color for visibility.
            MESSAGE.setText(item.getVIEWABLE_LOG_MESSAGE());
            if (getIndex() % 2 == 0) {
                setStyle("-fx-background-color: -color-bg-subtle;");
            }
            setGraphic(LAYOUT);
        }
    }
}