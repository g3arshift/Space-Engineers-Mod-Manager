package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class LogCell extends ListCell<LogMessage> {
    private final FontIcon messageIcon = new FontIcon();
    private final Label message = new Label();

    private final HBox layout = new HBox(messageIcon, message);

    public LogCell() {
        super();
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setSpacing(5d);
        message.setWrapText(true);
    }

    //TODO: We're getting a weird bug that's leaving empty cells sometimes when the message is especially large and the user clicks into the rows? Could be the cell resizing too large for the view maybe?
    @Override
    protected void updateItem(LogMessage item, boolean empty) {
        super.updateItem(item, empty);
        //If cells are empty or their contents null, remove them. Else, style them and add them.
        if (empty || item == null) {
            setGraphic(null);
        } else {
            switch (item.getMessageType()) {
                case INFO -> {
                    messageIcon.setStyle("-fx-icon-color: -color-accent-emphasis;");
                    messageIcon.setIconLiteral("ci-information-square");
                }
                case WARN -> {
                    messageIcon.setStyle("-fx-icon-color: -color-warning-emphasis;");
                    messageIcon.setIconLiteral("ci-warning-alt");
                }
                case ERROR -> {
                    messageIcon.setStyle("-fx-icon-color: -color-danger-emphasis;");
                    messageIcon.setIconLiteral("ci-warning-square");
                }
                default -> {
                    messageIcon.setStyle("-fx-icon-color: -color-neutral-emphasis;");
                    messageIcon.setIconLiteral("ci-unknown");
                }
            }
            //TODO: Reuse for the mod table!!!
            //Make every other row in the log a different color for visibility.
            message.setText(item.getViewableLogMessage());
            if (getIndex() % 2 == 0) {
                setStyle("-fx-background-color: -color-bg-subtle;");
            }
            setGraphic(layout);
        }
    }
}