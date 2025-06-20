package com.gearshiftgaming.se_mod_manager.frontend.domain;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.controller.ModInfoController;
import com.gearshiftgaming.se_mod_manager.controller.StorageController;
import com.gearshiftgaming.se_mod_manager.frontend.view.MasterManager;
import com.gearshiftgaming.se_mod_manager.frontend.view.helper.ModListManagerHelper;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * All the UI logic passes through here, and is the endpoint that the UI uses to connect to the rest of the system.
 * It holds all the relevant variables that are actual logic, such as the observable lists for save and mod profiles, as well as the current profiles.
 * <p>
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class UiService {
    private final Logger LOGGER;

    private final StorageController STORAGE_CONTROLLER;

    private final ModInfoController MOD_INFO_CONTROLLER;

    @Getter
    private final ObservableList<LogMessage> USER_LOG;

    private final int USER_LOG_MAX_SIZE;

    @Getter
    private final ObservableList<MutableTriple<UUID, String, SpaceEngineersVersion>> MOD_LIST_PROFILE_DETAILS;

    @Getter
    private final ObservableList<SaveProfile> SAVE_PROFILES;

    @Getter
    private final UserConfiguration USER_CONFIGURATION;

    @Getter
    private SaveProfile currentSaveProfile;

    @Getter
    private ModListProfile currentModListProfile;

    @Getter
    private ObservableList<Mod> currentModList;

    @Getter
    private final IntegerProperty activeModCount;

    @Setter
    private IntegerProperty modImportProgressNumerator;

    @Setter
    private IntegerProperty modImportProgressDenominator;

    @Setter
    private DoubleProperty modImportProgressPercentage;

    private final String MOD_DATE_FORMAT;

    @Getter
    final javafx.event.EventHandler<KeyEvent> KEYBOARD_BUTTON_NAVIGATION_DISABLER;

    //TODO: Really oughta redo most of this into a function so we can reset the user config without restarting the app
    // Shouldn't be hard. Just need to reset the user config to default settings, drop existing data, and persist the new data.
    public UiService(Logger LOGGER, @NotNull ObservableList<LogMessage> USER_LOG, int userLogMaxSize,
                     @NotNull ObservableList<MutableTriple<UUID, String, SpaceEngineersVersion>> modListProfileDetails, @NotNull ObservableList<SaveProfile> SAVE_PROFILES,
                     StorageController storageController, ModInfoController modInfoController, UserConfiguration USER_CONFIGURATION, @NotNull Properties properties) {

        this.LOGGER = LOGGER;
        this.MOD_INFO_CONTROLLER = modInfoController;
        this.USER_LOG = USER_LOG;
        this.USER_LOG_MAX_SIZE = userLogMaxSize;
        this.MOD_LIST_PROFILE_DETAILS = modListProfileDetails;
        this.SAVE_PROFILES = SAVE_PROFILES;
        this.STORAGE_CONTROLLER = storageController;
        this.USER_CONFIGURATION = USER_CONFIGURATION;

        this.MOD_DATE_FORMAT = properties.getProperty("semm.steam.mod.dateFormat");

        KEYBOARD_BUTTON_NAVIGATION_DISABLER = arrowKeyEvent -> {
            switch (arrowKeyEvent.getCode()) {
                case UP, DOWN, LEFT, RIGHT, TAB, ESCAPE:
                    arrowKeyEvent.consume();
                    break;
            }
        };

        //Load our last active mod list profile, or at least the first one.
        Result<ModListProfile> modListProfileResult = getLastActiveModlistProfile();
        if (!modListProfileResult.isSuccess()) {
            log("No previously chosen modlist detected.", MessageType.INFO);
            modListProfileResult = storageController.loadFirstModListProfile();
            if (!modListProfileResult.isSuccess()) {
                log(modListProfileResult);
                Popup.displaySimpleAlert(String.format("Fatal error!\n%s", modListProfileResult.getCurrentMessage()), MessageType.ERROR);
                throw new RuntimeException(modListProfileResult.getCurrentMessage());
            }
        }
        currentModListProfile = modListProfileResult.getPayload();
        if (!modListProfileResult.isSuccess()) {
            logPrivate(modListProfileResult);
            log(modListProfileResult.getCurrentMessage(), MessageType.ERROR);
            throw new MissingDefaultModListProfileException();
        } else {
            log(modListProfileResult.getCurrentMessage(), MessageType.INFO);
        }

        //Load our last active save profile
        getLastActiveSaveProfile().ifPresentOrElse(saveProfile -> currentSaveProfile = saveProfile, () -> {
            log("No previously chosen save profile detected.", MessageType.INFO);
            currentSaveProfile = SAVE_PROFILES.getFirst();
        });

        //A little bit of duplication, but the order of construction is a bit different from setCurrentModProfile
        currentModList = FXCollections.observableArrayList(currentModListProfile.getModList());
        activeModCount = new SimpleIntegerProperty((int) currentModList.stream().filter(Mod::isActive).count());
    }

    public void log(String message, MessageType messageType) {
        LogMessage logMessage = new LogMessage(message, messageType, LOGGER);
        USER_LOG.add(logMessage);

        //There's no observable queue in JavaFX so we're basically mimicking that here.
        if (USER_LOG.size() > USER_LOG_MAX_SIZE) {
            USER_LOG.removeFirst();
        }
    }

    public <T> void log(@NotNull Result<T> result) {
        MessageType messageType;
        switch (result.getType()) {
            case INVALID -> messageType = MessageType.WARN;
            case CANCELLED, NOT_INITIALIZED, FAILED -> messageType = MessageType.ERROR;
            default -> messageType = MessageType.INFO;
        }
        for (String message : result.getMESSAGES()) {
            log(message, messageType);
        }
    }

    public void log(Exception e) {
        log(String.valueOf(e), MessageType.ERROR);
    }

    public void logPrivate(String message, @NotNull MessageType messageType) {
        switch (messageType) {
            case INFO -> LOGGER.info(message);
            case WARN -> LOGGER.warn(message);
            case ERROR -> LOGGER.error(message);
            case DEBUG -> LOGGER.debug(message);
            case UNKNOWN -> LOGGER.error("ERROR UNKNOWN - {}", message);
        }
    }

    public <T> void logPrivate(@NotNull Result<T> result) {
        for (String message : result.getMESSAGES()) {
            switch (result.getType()) {
                case SUCCESS, CANCELLED -> LOGGER.info(message);
                case INVALID, WARN -> LOGGER.warn(message);
                case FAILED -> LOGGER.error(message);
                default -> LOGGER.error("ERROR UNKNOWN - {}", message);
            }
        }
    }

    public Result<Void> deleteModListProfile(UUID modListProfileId) {
        return STORAGE_CONTROLLER.deleteModListProfile(modListProfileId);
    }

    public Result<Void> saveUserConfiguration() {
        return STORAGE_CONTROLLER.saveUserConfiguration(USER_CONFIGURATION);
    }

    public Result<Void> updateModListLoadPriority() {
        return STORAGE_CONTROLLER.updateModListLoadPriority(currentModListProfile.getID(), currentModList);
    }

    public Result<ModListProfile> loadModListProfileById(UUID modListProfileId) {
        return STORAGE_CONTROLLER.loadModListProfileById(modListProfileId);
    }

    public Result<Void> updateModListActiveMods() {
        return STORAGE_CONTROLLER.updateModListActiveMods(currentModListProfile.getID(), currentModList);
    }

    public Result<Void> saveModListProfileDetails(Triple<UUID, String, SpaceEngineersVersion> modListProfileDetails) {
        return STORAGE_CONTROLLER.saveModListProfileDetails(modListProfileDetails);
    }

    public Result<Void> saveModListProfile(ModListProfile modListProfile) {
        return STORAGE_CONTROLLER.saveModListProfile(modListProfile);
    }

    public Result<Void> saveCurrentModListProfile() {
        return STORAGE_CONTROLLER.saveModListProfile(currentModListProfile);
    }

    public Result<Void> updateModInformation(List<Mod> modList) {
        return STORAGE_CONTROLLER.updateModInformation(modList);
    }

    public Result<Void> deleteSaveProfile(SaveProfile saveProfile) {
        return STORAGE_CONTROLLER.deleteSaveProfile(saveProfile);
    }

    public Result<Void> updateModListProfileModList() {
        return STORAGE_CONTROLLER.updateModListProfileModList(currentModListProfile.getID(), currentModList);
    }

    public Result<Void> saveSaveProfile(SaveProfile saveProfile) {
        return STORAGE_CONTROLLER.saveSaveProfile(saveProfile);
    }

    public Result<Void> resetData() {
        return STORAGE_CONTROLLER.resetData();
    }

    public Result<Void> applyModlist(List<Mod> modList, SaveProfile saveProfile) throws IOException {
        return STORAGE_CONTROLLER.applyModlist(modList, saveProfile);
    }

    public Result<SaveProfile> copySaveProfile(SaveProfile saveProfile, List<SaveProfile> saveProfileList) throws IOException {
        return STORAGE_CONTROLLER.copySaveProfile(saveProfile, saveProfileList);
    }

    public Result<SaveProfile> getSaveProfile(File sandboxConfigFile) throws IOException {
        return STORAGE_CONTROLLER.getSpaceEngineersOneSaveProfile(sandboxConfigFile);
    }

    public Result<String> getSaveName(File sandboxConfigFile) throws IOException {
        return STORAGE_CONTROLLER.getSaveName(sandboxConfigFile);
    }

    /**
     * Sets the theme for our application based on the users preferred theme using reflection.
     * It expects to receive a list of CheckMenuItems that represent the UI dropdown list for all the available system themes in the MenuBar. Not the *best* way to do this, but it works.
     */
    public void setUserSavedApplicationTheme(@NotNull List<CheckMenuItem> themeList) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (CheckMenuItem c : themeList) {
            String currentTheme = StringUtils.removeEnd(c.getId(), "Theme");
            String themeName = currentTheme.substring(0, 1).toUpperCase() + currentTheme.substring(1);
            if (themeName.equals(StringUtils.deleteWhitespace(USER_CONFIGURATION.getUserTheme()))) {
                c.setSelected(true);
                Class<?> cls = Class.forName("atlantafx.base.theme." + StringUtils.deleteWhitespace(USER_CONFIGURATION.getUserTheme()));
                Theme theme = (Theme) cls.getDeclaredConstructor().newInstance();
                Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
            }
        }
    }

    public Result<Void> setCurrentModListProfile(UUID modListProfileId) {
        Result<Void> setResult = new Result<>();
        Result<ModListProfile> newCurrentModListProfileResult = STORAGE_CONTROLLER.loadModListProfileById(modListProfileId);
        if (!newCurrentModListProfileResult.isSuccess()) {
            log(newCurrentModListProfileResult);
            setResult.addAllMessages(newCurrentModListProfileResult);
            return setResult;
        }

        log(newCurrentModListProfileResult.getCurrentMessage(), MessageType.INFO);
        ModListProfile modListProfile = newCurrentModListProfileResult.getPayload();
        currentModListProfile = modListProfile;
        currentModList = FXCollections.observableArrayList(currentModListProfile.getModList());
        activeModCount.set((int) currentModList.stream().filter(Mod::isActive).count());
        setResult.addAllMessages(setLastActiveModlistProfile(modListProfile.getID()));
        return setResult;
    }

    public Result<Void> setCurrentSaveProfile(SaveProfile newCurrentSaveProfile) {
        this.currentSaveProfile = newCurrentSaveProfile;
        return setLastActiveSaveProfile(newCurrentSaveProfile.getID());
    }

    public void modifyActiveModCount(@NotNull Mod mod) {
        if (mod.isActive()) {
            activeModCount.set(activeModCount.get() + 1);
        } else {
            activeModCount.set(activeModCount.get() - 1);
        }
    }

    public void modifyActiveModCount(int numMods) {
        activeModCount.set(activeModCount.get() + numMods);
    }

    //This isn't down in the ModlistService because we need to actually update the numerator on each and every single completed get call for the UI progress
    // bars to work properly.
    public List<Result<String>> scrapeSteamModCollectionModList(String collectionId) throws IOException {

        List<Result<String>> steamCollectionModIds = MOD_INFO_CONTROLLER.scrapeSteamModCollectionModList(collectionId);

        //Process the returned ID's and check for duplicates in our current mod list.
        for (Result<String> modIdResult : steamCollectionModIds) {
            if (modIdResult.isSuccess()) {
                SteamMod duplicateSteamMod = ModListManagerHelper.findDuplicateSteamMod(modIdResult.getPayload(), currentModList);
                if (duplicateSteamMod != null) {
                    modIdResult.addMessage(String.format("\"%s\" is already in the modlist!", duplicateSteamMod.getFriendlyName()), ResultType.INVALID);
                }
            } else {
                log(modIdResult);
            }
        }

        return steamCollectionModIds;
    }

    public Result<List<Mod>> getModlistFromSave(File sandboxConfigFile) throws IOException {
        Result<List<Mod>> modListResult = STORAGE_CONTROLLER.getModlistFromSave(sandboxConfigFile);

        if (modListResult.isSuccess()) {
            int initialModlistSize = modListResult.getPayload().size();
            ModListManagerHelper.removeDuplicateMods(modListResult.getPayload(), currentModList);

            if (modListResult.getPayload().size() != initialModlistSize) {
                if (modListResult.getPayload().isEmpty()) {
                    modListResult.addMessage("Every mod in the save is already in the modlist!", ResultType.INVALID);
                } else
                    modListResult.addMessage(String.format("%d mods were found. %d are already in the modlist.", initialModlistSize, (initialModlistSize - modListResult.getPayload().size())), ResultType.SUCCESS);
            }
        }

        return modListResult;
    }

    public Result<Mod> fillOutModInformation(Mod mod) throws IOException, InterruptedException {
        Result<String[]> scrapeResult = MOD_INFO_CONTROLLER.fillOutModInformation(mod);
        Result<Mod> infoFilloutResult = new Result<>();

        if (scrapeResult.isSuccess()) {
            String[] modInfo = scrapeResult.getPayload();

            mod.setFriendlyName(modInfo[0]);

            List<String> modTags = List.of(modInfo[1].split(","));
            mod.setCategories(modTags);

            mod.setDescription(modInfo[2]);

            infoFilloutResult.setPayload(mod);

            String duplicateModMessage = ModListManagerHelper.findDuplicateMod(mod, currentModList);

            DateTimeFormatter formatter;
            try {
                if (mod instanceof SteamMod) {
                    formatter = new DateTimeFormatterBuilder()
                            .parseCaseInsensitive()
                            .appendPattern(MOD_DATE_FORMAT)
                            .toFormatter(Locale.ENGLISH);
                    ((SteamMod) mod).setLastUpdated(LocalDateTime.parse(modInfo[3], formatter));
                    infoFilloutResult.addMessage("Successfully parsed last updated datetime for \"" + mod.getFriendlyName() + "\".", ResultType.SUCCESS);
                } else {
                    ((ModIoMod) mod).setLastUpdatedYear(Year.parse(modInfo[3]));

                    if (modInfo[4] != null) {
                        ((ModIoMod) mod).setLastUpdatedMonthDay(MonthDay.parse(modInfo[4]));
                    }

                    if (modInfo[5] != null) {
                        ((ModIoMod) mod).setLastUpdatedHour(LocalTime.parse(modInfo[5]));
                    }

                    infoFilloutResult.addMessage("Successfully parsed last updated datetime for \"" + mod.getFriendlyName() + "\".", ResultType.SUCCESS);
                }
            } catch (DateTimeParseException e) {
                infoFilloutResult.addMessage(getStackTrace(e), ResultType.FAILED);
                infoFilloutResult.addMessage("Failed to parse last updated datetime for mod.", ResultType.FAILED);
            }

            if (!duplicateModMessage.isBlank()) {
                infoFilloutResult.addMessage(duplicateModMessage, ResultType.REQUIRES_ADJUDICATION);
            } else {
                if (infoFilloutResult.isSuccess())
                    infoFilloutResult.addMessage("Mod \"" + mod.getFriendlyName() + "\" has been successfully scraped.", ResultType.SUCCESS);
            }
        } else {
            infoFilloutResult.addAllMessages(scrapeResult);
        }

        Platform.runLater(() -> {
            modImportProgressNumerator.setValue(modImportProgressNumerator.get() + 1);
            modImportProgressPercentage.setValue((double) modImportProgressNumerator.get() / (double) modImportProgressDenominator.get());
        });
        return infoFilloutResult;
    }

    public IntegerProperty getModImportProgressNumeratorProperty() {
        if (this.modImportProgressNumerator == null) {
            this.modImportProgressNumerator = new SimpleIntegerProperty(0);
        }
        return this.modImportProgressNumerator;
    }

    public int getModImportProgressNumerator() {
        return modImportProgressNumerator.get();
    }

    public IntegerProperty getModImportProgressDenominatorProperty() {
        if (this.modImportProgressDenominator == null) {
            this.modImportProgressDenominator = new SimpleIntegerProperty(0);
        }
        return this.modImportProgressDenominator;
    }

    public int getModImportProgressDenominator() {
        return modImportProgressDenominator.get();
    }

    public DoubleProperty getModImportProgressPercentageProperty() {
        if (modImportProgressPercentage == null) {
            modImportProgressPercentage = new SimpleDoubleProperty(0d);
        }
        return modImportProgressPercentage;
    }

    public double getModImportProgressPercentage() {
        return modImportProgressPercentage.get();
    }

    public Task<List<Result<Mod>>> importModsFromList(List<Mod> modList) {
        List<Result<Mod>> modInfoFillOutResults = new ArrayList<>();
        return new Task<>() {
            @Override
            protected List<Result<Mod>> call() throws ExecutionException, InterruptedException {
                Platform.runLater(() -> modImportProgressDenominator.setValue(modList.size()));
                List<Future<Result<Mod>>> futures = new ArrayList<>(modList.size());
                try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                    Random random = new Random();
                    for (Mod m : modList) {
                        // Submit the task without waiting for it to finish
                        Future<Result<Mod>> future = executorService.submit(() -> {
                            try {
                                if (m instanceof ModIoMod && modList.size() > 1) {
                                    Thread.sleep(random.nextInt(200, 600));
                                }
                                return fillOutModInformation(m);
                            } catch (IOException e) {
                                Result<Mod> failedResult = new Result<>();
                                if (e.toString().equals("java.net.UnknownHostException: steamcommunity.com")) {
                                    failedResult.addMessage("Unable to reach the Steam Workshop. Please check your internet connection.", ResultType.FAILED);
                                } else if (e.toString().equals("java.net.UnknownHostException: mod.io")) {
                                    failedResult.addMessage("Unable to reach Mod.io. Please check your internet connection.", ResultType.FAILED);
                                } else {
                                    failedResult.addMessage(getStackTrace(e), ResultType.FAILED);
                                }
                                return failedResult;
                            }
                        });
                        futures.add(future);
                    }
                    try {
                        for (Future<Result<Mod>> f : futures) {
                            modInfoFillOutResults.add(f.get());
                        }
                    } catch (RuntimeException e) {
                        Result<Mod> failedResult = new Result<>();
                        failedResult.addMessage(getStackTrace(e), ResultType.FAILED);
                        modInfoFillOutResults.add(failedResult);
                    }
                }
                return modInfoFillOutResults;
            }
        };
    }

    public Task<List<Result<String>>> importSteamCollection(String collectionId) {
        return new Task<>() {
            @Override
            protected List<Result<String>> call() {
                try {
                    return scrapeSteamModCollectionModList(collectionId);
                } catch (IOException e) {
                    List<Result<String>> failedResults = new ArrayList<>();
                    Result<String> failedResult = new Result<>();
                    if (e.toString().equals("java.net.UnknownHostException: steamcommunity.com")) {
                        failedResult.addMessage("Unable to reach the Steam Workshop. Please check your internet connection.", ResultType.FAILED);
                    } else {
                        failedResult.addMessage(getStackTrace(e), ResultType.FAILED);
                    }
                    failedResults.add(failedResult);
                    return failedResults;
                }
            }
        };
    }


    /**
     * Converts a list of Mod IO urls to their respective ID's.
     *
     * @param modUrls List of Mod.io url's
     * @return The task to perform the conversion.
     */
    public Task<List<Result<String>>> convertModIoUrlListToIds(List<String> modUrls) {
        List<Result<String>> modIdResults = new ArrayList<>();
        return new Task<>() {
            @Override
            protected List<Result<String>> call() throws ExecutionException, InterruptedException {
                List<Future<Result<String>>> futures = new ArrayList<>(modUrls.size());
                try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                    for (String modUrl : modUrls) {

                        Future<Result<String>> future = executorService.submit(() -> {
                            Result<String> idResult = new Result<>();
                            if (StringUtils.isNumeric(modUrl)) {
                                idResult.addMessage(String.format("%s is already in Mod ID format.", modUrl), ResultType.SUCCESS);
                                idResult.setPayload(modUrl);
                            } else {
                                idResult = getModIoModIdFromUrlName(modUrl);
                            }

                            if (idResult.isSuccess()) {
                                ModIoMod duplicateModIoMod = ModListManagerHelper.findDuplicateModIoMod(idResult.getPayload(), currentModList);
                                if (duplicateModIoMod != null) {
                                    idResult.addMessage(String.format("Mod \"%s\" is already in the mod list!", idResult.getPayload()), ResultType.FAILED);
                                }
                            }
                            return idResult;
                        });
                        futures.add(future);
                    }
                    try {
                        for (Future<Result<String>> f : futures) {
                            modIdResults.add(f.get());
                        }
                    } catch (RuntimeException e) {
                        Result<String> failedResult = new Result<>();
                        failedResult.addMessage(getStackTrace(e), ResultType.FAILED);
                        modIdResults.add(failedResult);
                    }
                }
                return modIdResults;
            }
        };
    }

    /**
     * Converts a single Mod.io URL to its respective ID.
     *
     * @param modUrl The URL to convert.
     * @return The task to perform the conversion.
     */
    public Task<Result<String>> convertModIoUrlToId(String modUrl) {
        return new Task<>() {
            @Override
            protected Result<String> call() {
                Result<String> urltoIdConversionResult = getModIoModIdFromUrlName(modUrl);
                if (urltoIdConversionResult.isSuccess()) {
                    ModIoMod duplicateModIoMod = ModListManagerHelper.findDuplicateModIoMod(urltoIdConversionResult.getPayload(), currentModList);
                    if (duplicateModIoMod != null) {
                        urltoIdConversionResult.addMessage(String.format("Mod \"%s\" is already in the mod list!", duplicateModIoMod.getFriendlyName()), ResultType.FAILED);
                    }
                }
                return urltoIdConversionResult;
            }
        };
    }

    /**
     * Converts a Mod.io URL to its respective ID.
     *
     * @param modUrl The url to convert
     * @return The ID associated with the Mod.io URL.
     */
    public Result<String> getModIoModIdFromUrlName(String modUrl) {
        try {
            return MOD_INFO_CONTROLLER.getModIoIdFromUrlName(modUrl);
        } catch (IOException e) {
            Result<String> failedResult = new Result<>();
            if (e.toString().equals("java.net.UnknownHostException: mod.io")) {
                failedResult.addMessage("Unable to reach Mod.io. Please check your internet connection.", ResultType.FAILED);
            } else {
                failedResult.addMessage(getStackTrace(e), ResultType.FAILED);
            }
            return failedResult;
        }
    }

    public List<String> getModlistFromFile(File modlistFile, ModType modType) throws IOException {
        return MOD_INFO_CONTROLLER.getModIdsFromFile(modlistFile, modType);
    }

    public Result<Void> exportModListProfile(ModListProfile modListProfile, File exportLocation) {
        return STORAGE_CONTROLLER.exportModListProfile(modListProfile, exportLocation);
    }

    //Once the file is read, we want to add its details to the detail list
    public Result<Void> importModlistProfile(File saveLocation) {
        Result<Void> importResult = new Result<>();
        Result<ModListProfile> modlistProfileResult = STORAGE_CONTROLLER.importModListProfile(saveLocation);
        if (modlistProfileResult.isSuccess()) {
            ModListProfile importModListProfile = modlistProfileResult.getPayload();
            boolean duplicateProfileExists = MOD_LIST_PROFILE_DETAILS
                    .stream()
                    .anyMatch(modlistProfile -> modlistProfile.getMiddle().toLowerCase().trim().equals(importModListProfile.getProfileName().toLowerCase().trim()));
            if (!duplicateProfileExists) {
                for (int i = 0; i < importModListProfile.getModList().size(); i++) {
                    importModListProfile.getModList().get(i).setLoadPriority(i + 1);
                }
                updateModInformation(importModListProfile.getModList());
                Result<Void> saveModListResult = saveModListProfile(importModListProfile);
                if(!saveModListResult.isSuccess()) {
                    log(saveModListResult);
                    return saveModListResult;
                }

                MOD_LIST_PROFILE_DETAILS.add(MutableTriple.of(importModListProfile.getID(), importModListProfile.getProfileName(), importModListProfile.getSPACE_ENGINEERS_VERSION()));
                importResult.addAllMessages(setCurrentModListProfile(importModListProfile.getID()));
                if (importResult.isSuccess()) {
                    importResult.addMessage(String.format("Successfully imported mod list profile \"%s\".", importModListProfile.getProfileName()), ResultType.SUCCESS);
                    logPrivate(importResult);
                } else {
                    log(importResult);
                }
            } else
                importResult.addMessage(String.format("Mod profile \"%s\" already exists!", modlistProfileResult.getPayload().getProfileName()), ResultType.INVALID);
        }
        return importResult;
    }

    public Result<Void> setLastActiveModlistProfile(UUID modlistProfileId) {
        USER_CONFIGURATION.setLastActiveModProfileId(modlistProfileId);
        return saveUserConfiguration();
    }

    public Result<Void> setLastActiveSaveProfile(UUID saveProfileId) {
        USER_CONFIGURATION.setLastActiveSaveProfileId(saveProfileId);
        return saveUserConfiguration();
    }

    public Result<ModListProfile> getLastActiveModlistProfile() {
        return loadModListProfileById(USER_CONFIGURATION.getLastActiveModProfileId());
    }

    public Optional<SaveProfile> getLastActiveSaveProfile() {
        return SAVE_PROFILES.stream()
                .filter(saveProfile -> saveProfile.getID().equals(USER_CONFIGURATION.getLastActiveSaveProfileId()))
                .findFirst();
    }

    public void setSaveProfileInformationAfterSuccessfullyApplyingModlist() {
        currentSaveProfile.setLastUsedModListProfileId(currentModListProfile.getID());
        currentSaveProfile.setLastSaveStatus(SaveStatus.SAVED);
        USER_CONFIGURATION.setLastModifiedSaveProfileId(currentSaveProfile.getID());
        Result<Void> saveResult = saveSaveProfile(currentSaveProfile);
        if (!saveResult.isSuccess()) {
            log(saveResult);
            Popup.displaySimpleAlert(saveResult);
            return;
        }

        saveResult = saveUserConfiguration();
        if (!saveResult.isSuccess()) {
            log(saveResult);
            Popup.displaySimpleAlert(saveResult);
        }
    }

    //TODO: Add a dialog option to choose whether you have space engineers 1, 2, or both.
    // Make it a part of UserConfiguration, and add menu options to toggle SEMM support for either game. Reference it when creating mod + save lists.
    public void displayTutorial(Stage stage, MasterManager masterManager) {
        log("Starting tutorial...", MessageType.INFO);

        stage.setResizable(false);
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, KEYBOARD_BUTTON_NAVIGATION_DISABLER);

        List<String> tutorialMessages = new ArrayList<>();
        tutorialMessages.add("Welcome to Space Engineers Mod Manager (SEMM). " +
                "This tutorial will guide you through managing the mods of an existing save, how to create a mod list, how to add mods to a mod list, and how to apply a mod list to a save.");
        tutorialMessages.add("Let's start by creating a new mod list by pressing the \"Manage Mod Profiles\" button.");
        Popup.displayNavigationDialog(tutorialMessages, stage, MessageType.INFO, "Welcome to SEMM!");

        masterManager.runTutorialModListManagementStep();
    }

    /**
     * This returns an array of panes to be used for highlighting parts of the UI.
     * The panes are returned in top, right, bottom, left order
     */
    public Pane[] getHighlightPanes() {
        Pane[] panes = new Pane[4];
        Pane topPane = new Pane();
        Pane rightPane = new Pane();
        Pane bottomPane = new Pane();
        Pane leftPane = new Pane();
        panes[0] = topPane;
        panes[1] = rightPane;
        panes[2] = bottomPane;
        panes[3] = leftPane;

        for (Pane p : panes) {
            p.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        }

        return panes;
    }
}
