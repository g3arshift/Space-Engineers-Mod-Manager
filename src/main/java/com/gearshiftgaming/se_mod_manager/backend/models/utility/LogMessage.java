package com.gearshiftgaming.se_mod_manager.backend.models.utility;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Functionally a wrapper for Log4j logging, but simplifies keeping logging synchronized between the system log and the log in the UI while also providing finer control on what log messages are displayed to the user.
 * The message in the system log can be of any length, but the UI will only display the first two lines of a LogMessage. It is thus preferred to instead
 * <p>
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */

public class LogMessage {
    private final StringProperty viewableLogMessage = new SimpleStringProperty();
    private final StringProperty messageType = new SimpleStringProperty();
    public LogMessage(String message, MessageType messageType, Logger logger) {
        this.viewableLogMessage.setValue(new SimpleDateFormat("hh:mm:ss").format(new Date()) + " - " + message);
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
                logger.error("ERROR UNKNOWN - " + message);
            }
        }
    }

    public String getViewableLogMessage() {
        return viewableLogMessage.get();
    }

    public StringProperty viewableLogMessageProperty() {
        return viewableLogMessage;
    }

    public MessageType getMessageType() {
        return MessageType.valueOf(messageType.get());
    }

    public StringProperty messageTypeProperty() {
        return messageType;
    }
}
