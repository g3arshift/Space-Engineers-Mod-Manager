package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;

public class LogCell extends ListCell<LogMessage> {
    private Image messageTypeImage;
    private final Label message = new Label();

    private final HBox layout = new HBox(message);

    //TODO: The cells need styled.
    public LogCell() {
        super();
    }

    @Override
    protected void updateItem(LogMessage item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            message.setText(null);
        } else {
            //TODO: Get some icons for the message type images so we can define them.
//            switch(item.getMessageType()) {
//                case INFO -> {
//                    messageTypeImage = new Image("");
//                }
//                case WARN -> {
//                    messageTypeImage = new Image("");
//                }
//                case ERROR -> {
//                    messageTypeImage = new Image("");
//                }
//                case UNKNOWN -> {
//                    messageTypeImage = new Image("");
//                }
            message.setText(item.getMessage());
            setGraphic(layout);
        }
    }
}
