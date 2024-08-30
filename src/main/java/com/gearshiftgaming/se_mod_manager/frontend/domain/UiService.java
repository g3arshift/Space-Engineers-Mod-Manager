package com.gearshiftgaming.se_mod_manager.frontend.domain;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.controller.BackendController;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
@Getter
public class UiService {
    //TODO: Is this a controller, or a service? It's kind of performing the duties of a controller as it doesn't have too much of its own logic.

    private final Logger logger;

    private final ObservableList<LogMessage> userLog;

    private final ObservableList<ModProfile> modProfiles;

    private final ObservableList<SaveProfile> saveProfiles;

    private final BackendController backendController;

    private final UserConfiguration userConfiguration;

    @Setter
    private SaveProfile currentSaveProfile;
    @Setter
    private ModProfile currentModProfile;

    public UiService(Logger logger, ObservableList<LogMessage> userLog,
                     ObservableList<ModProfile> modProfiles, ObservableList<SaveProfile> saveProfiles,
                     BackendController backendController, UserConfiguration userConfiguration) {

        this.logger = logger;
        this.userLog = userLog;
        this.modProfiles = modProfiles;
        this.saveProfiles = saveProfiles;
        this.backendController = backendController;
        this.userConfiguration = userConfiguration;
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
        log(result.getCurrentMessage(), messageType);
    }

    public Result<Void> saveUserData(UserConfiguration userConfiguration) {
        return backendController.saveUserData(userConfiguration);
    }

    public Result<Void> applyModlist(List<Mod> modList, String sandboxConfigPath) throws IOException {
        return backendController.applyModlist(modList, sandboxConfigPath);
    }

    public void firstTimeSetup() {
        //TODO: Setup users first modlist and save, and also ask if they want to try and automatically find ALL saves they have and add them to SEMM.
    }
}