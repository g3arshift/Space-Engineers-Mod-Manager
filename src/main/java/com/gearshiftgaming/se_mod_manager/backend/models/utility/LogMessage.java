package com.gearshiftgaming.se_mod_manager.backend.models.utility;

import com.gearshiftgaming.se_mod_manager.frontend.models.MessageType;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogMessage {
    private final StringProperty message = new SimpleStringProperty();
    private final StringProperty messageType = new SimpleStringProperty();
    public LogMessage(String message, MessageType messageType, Logger logger) {
        this.message.setValue(new SimpleDateFormat("hh:mm:ss").format(new Date()) + " - " + message);
        this.messageType.setValue(messageType.toString());

        switch(messageType) {
            case INFO -> {
                logger.info(message);
            }
            case WARN -> {
                logger.warn(message);
            }
            case ERROR -> {
                logger.error(message);
            }
            case UNKNOWN -> {
                logger.error("ERROR UNKNOWN - " +  message);
            }
        }
    }

    public String getMessage() {
        return message.get();
    }

    public StringProperty messageProperty() {
        return message;
    }

    public MessageType getMessageType() {
        return MessageType.valueOf(messageType.get());
    }

    public StringProperty messageTypeProperty() {
        return messageType;
    }
}
