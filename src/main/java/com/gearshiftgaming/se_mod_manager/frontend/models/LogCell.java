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

    //TODO: The cells need styled.
    public LogCell() {
        super();
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setSpacing(5d);
    }

    @Override
    protected void updateItem(LogMessage item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            message.setText(null);
        } else {
            //TODO: Get some icons for the message type images so we can define them.
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
            message.setText(item.getViewableLogMessage());
            if (getIndex() % 2 == 0) {
                setStyle("-fx-background-color: -color-bg-subtle");
            }
            setGraphic(layout);
        }
    }
}