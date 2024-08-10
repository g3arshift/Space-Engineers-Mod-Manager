package com.gearshiftgaming.se_mod_manager.frontend.domain;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import javafx.collections.ObservableList;
import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@Getter
public class UiService {

    private final ObservableList<LogMessage> userLog;

    private final Logger logger;

    //TODO: Pass the objects to the UI instead and parse to observable lists
    //private final List<ModProfile> modProfiles;
    //private final List<SaveProfile> saveProfiles;

    public UiService(Logger logger, ObservableList<LogMessage> userLog) throws IOException {
        this.logger = logger;
        this.userLog = userLog;

        //TODO: Run first time user setup
    }

    public void log(String message, MessageType messageType) {
        LogMessage logMessage = new LogMessage(message, messageType, logger);
        userLog.add(logMessage);
    }

    public <T> void log(Result<T> result) {
        MessageType messageType;
        switch (result.getType()) {
            case INVALID -> messageType = MessageType.WARN;
            case CANCELLED, NOT_INITIALIZED, FAILED -> messageType = MessageType.ERROR;
            default -> messageType = MessageType.INFO;
        }
        log(result.getMessages().getLast(), messageType);

    }
}