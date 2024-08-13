package com.gearshiftgaming.se_mod_manager.frontend.domain;

import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.controller.BackendController;
import javafx.collections.ObservableList;
import lombok.Getter;
import org.apache.logging.log4j.Logger;

@Getter
public class UiService {
    //TODO: Is this a controller, or a service? It's kind of performing the duties of a controller.

    private final Logger logger;

    private final ObservableList<LogMessage> userLog;

    private final ObservableList<ModProfile> modProfiles;

    private final ObservableList<SaveProfile> saveProfiles;

    private final BackendController backendController;
    public UiService(Logger logger, ObservableList<LogMessage> userLog,
                     ObservableList<ModProfile> modProfiles, ObservableList<SaveProfile> saveProfiles,
                     BackendController backendController) {

        this.logger = logger;
        this.userLog = userLog;
        this.modProfiles = modProfiles;
        this.saveProfiles = saveProfiles;
        this.backendController = backendController;
    }

    //TODO: Run first time user setup

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

    public void firstTimeSetup() {

    }
}