package com.gearshiftgaming.se_mod_manager.backend.models;

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
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */

public class LogMessage {
    private final StringProperty VIEWABLE_LOG_MESSAGE = new SimpleStringProperty();
    private final StringProperty MESSAGE_TYPE = new SimpleStringProperty();
    public LogMessage(String message, MessageType MESSAGE_TYPE, Logger logger) {
        this.VIEWABLE_LOG_MESSAGE.setValue(new SimpleDateFormat("hh:mm:ss").format(new Date()) + " - " + message);
        this.MESSAGE_TYPE.setValue(MESSAGE_TYPE.toString());

        switch(MESSAGE_TYPE) {
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

    public String getVIEWABLE_LOG_MESSAGE() {
        return VIEWABLE_LOG_MESSAGE.get();
    }

    public StringProperty VIEWABLE_LOG_MESSAGEProperty() {
        return VIEWABLE_LOG_MESSAGE;
    }

    public MessageType getMESSAGE_TYPE() {
        return MessageType.valueOf(MESSAGE_TYPE.get());
    }

    public StringProperty MESSAGE_TYPEProperty() {
        return MESSAGE_TYPE;
    }
}
