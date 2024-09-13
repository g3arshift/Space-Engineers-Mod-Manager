package com.gearshiftgaming.se_mod_manager.frontend.domain;

import atlantafx.base.controls.Message;
import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.controller.BackendController;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckMenuItem;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * All the UI logic passes through here, and is the endpoint that the UI uses to connect to the rest of the system.
 * It holds all the relevant variables that are actual logic, such as the observable lists for save and mod profiles, as well as the current profiles.
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
public class UiService {
    private final Logger logger;

    private final BackendController backendController;

    @Getter
    private final ObservableList<LogMessage> userLog;

    @Getter
    private final ObservableList<ModProfile> modProfiles;

    @Getter
    private final ObservableList<SaveProfile> saveProfiles;

    @Getter
    private final UserConfiguration userConfiguration;

    @Getter
    @Setter
    private SaveProfile currentSaveProfile;

    @Getter
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

    public void logPrivate(String message, MessageType messageType) {
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

    public Result<Void> saveUserData(UserConfiguration userConfiguration) {
        return backendController.saveUserData(userConfiguration);
    }

    public Result<Void> applyModlist(List<Mod> modList, String sandboxConfigPath) throws IOException {
        return backendController.applyModlist(modList, sandboxConfigPath);
    }

    public Result<SaveProfile> copySaveProfile(SaveProfile saveProfile) throws IOException {
        return backendController.copySaveProfile(saveProfile);
    }

    public Result<SaveProfile> getSaveProfile(File sandboxConfigFile) throws IOException {
        return backendController.getSaveProfile(sandboxConfigFile);
    }

    public void firstTimeSetup() {
        //TODO: Setup users first modlist and save, and also ask if they want to try and automatically find ALL saves they have and add them to SEMM.
    }

    //Sets the theme for our application based on the users preferred theme using reflection.
    //It expects to receive a list of CheckMenuItems that represent the UI dropdown list for all the available system themes in the MenuBar.
    public void setUserSavedApplicationTheme(List<CheckMenuItem> themeList) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (CheckMenuItem c : themeList) {
            String currentTheme = StringUtils.removeEnd(c.getId(), "Theme");
            String themeName = currentTheme.substring(0, 1).toUpperCase() + currentTheme.substring(1);
            if (themeName.equals(StringUtils.deleteWhitespace(userConfiguration.getUserTheme()))) {
                c.setSelected(true);
                Class<?> cls = Class.forName("atlantafx.base.theme." + StringUtils.deleteWhitespace(userConfiguration.getUserTheme()));
                Theme theme = (Theme) cls.getDeclaredConstructor().newInstance();
                Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
            }
        }
    }
}