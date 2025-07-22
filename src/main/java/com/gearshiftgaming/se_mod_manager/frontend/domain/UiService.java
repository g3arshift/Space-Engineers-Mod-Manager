package com.gearshiftgaming.se_mod_manager.frontend.domain;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.ModIoMod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.ModType;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.SteamMod;
import com.gearshiftgaming.se_mod_manager.backend.models.modlist.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveStatus;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.*;
import com.gearshiftgaming.se_mod_manager.backend.models.user.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.controller.ModInfoController;
import com.gearshiftgaming.se_mod_manager.controller.StorageController;
import com.gearshiftgaming.se_mod_manager.frontend.view.MasterManager;
import com.gearshiftgaming.se_mod_manager.frontend.view.helper.ModListManagerHelper;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.Popup;
import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final Logger logger;

    private final StorageController storageController;

    private final ModInfoController modInfoController;

    @Getter
    private final ObservableList<LogMessage> userLog;

    private final int userLogMaxSize;

    @Getter
    private final ObservableList<MutableTriple<UUID, String, SpaceEngineersVersion>> modListProfileDetails;

    @Getter
    private final ObservableList<SaveProfile> saveProfiles;

    @Getter
    private final UserConfiguration userConfiguration;

    @Getter
    private SaveProfile currentSaveProfile;

    @Getter
    private ModListProfile currentModListProfile;

    @Getter
    private ObservableList<Mod> currentModList;

    @Getter
    private final IntegerProperty activeModCount;

    private final String modDateFormat;

    @Getter
    private final javafx.event.EventHandler<KeyEvent> keyboardButtonNavigationDisabler;

    //TODO: Really oughta redo most of this into a function so we can reset the user config without restarting the app
    // Shouldn't be hard. Just need to reset the user config to default settings, drop existing data, and persist the new data.
    public UiService(Logger logger, @NotNull ObservableList<LogMessage> userLog, int userLogMaxSize,
                     @NotNull ObservableList<MutableTriple<UUID, String, SpaceEngineersVersion>> modListProfileDetails, @NotNull ObservableList<SaveProfile> saveProfiles,
                     StorageController storageController, ModInfoController modInfoController, UserConfiguration userConfiguration, @NotNull Properties properties) {

        this.logger = logger;
        this.modInfoController = modInfoController;
        this.userLog = userLog;
        this.userLogMaxSize = userLogMaxSize;
        this.modListProfileDetails = modListProfileDetails;
        this.saveProfiles = saveProfiles;
        this.storageController = storageController;
        this.userConfiguration = userConfiguration;

        this.modDateFormat = properties.getProperty("semm.steam.mod.dateFormat");

        keyboardButtonNavigationDisabler = arrowKeyEvent -> {
            switch (arrowKeyEvent.getCode()) {
                case UP, DOWN, LEFT, RIGHT, TAB, ESCAPE:
                    arrowKeyEvent.consume();
                    break;
                default:
                    log("Failed to initialize UiService keyboard button navigation disabler.", MessageType.ERROR);
            }
        };

        //Load our last active mod list profile, or at least the first one.
        Result<ModListProfile> modListProfileResult = getLastActiveModlistProfile();
        if (modListProfileResult.isFailure()) {
            log("No previously chosen modlist detected.", MessageType.INFO);
            modListProfileResult = storageController.loadFirstModListProfile();
            if (modListProfileResult.isFailure()) {
                log(modListProfileResult);
                Popup.displaySimpleAlert(String.format("Fatal error!%n%s", modListProfileResult.getCurrentMessage()), MessageType.ERROR);
                throw new RuntimeException(modListProfileResult.getCurrentMessage());
            }
        }
        currentModListProfile = modListProfileResult.getPayload();
        if (modListProfileResult.isFailure()) {
            logPrivate(modListProfileResult);
            log(modListProfileResult.getCurrentMessage(), MessageType.ERROR);
            throw new MissingDefaultModListProfileException();
        } else {
            log(modListProfileResult.getCurrentMessage(), MessageType.INFO);
        }

        //Load our last active save profile
        getLastActiveSaveProfile().ifPresentOrElse(saveProfile -> currentSaveProfile = saveProfile, () -> {
            log("No previously chosen save profile detected.", MessageType.INFO);
            currentSaveProfile = saveProfiles.getFirst();
        });

        //A little bit of duplication, but the order of construction is a bit different from setCurrentModProfile
        currentModList = FXCollections.observableArrayList(currentModListProfile.getModList());
        activeModCount = new SimpleIntegerProperty((int) currentModList.stream().filter(Mod::isActive).count());
    }

    public void log(String message, MessageType messageType) {
        LogMessage logMessage = new LogMessage(message, messageType, logger);
        userLog.add(logMessage);

        //There's no observable queue in JavaFX so we're basically mimicking that here.
        if (userLog.size() > userLogMaxSize) {
            userLog.removeFirst();
        }
    }

    /**
     * Logs all messages in a result object.
     *
     * @param result is the object we will be logging.
     */
    public <T> void log(@NotNull Result<T> result) {
        MessageType messageType;
        switch (result.getType()) {
            case INVALID -> messageType = MessageType.WARN;
            case CANCELLED, NOT_INITIALIZED, FAILED -> messageType = MessageType.ERROR;
            default -> messageType = MessageType.INFO;
        }
        for (String message : result.getMessages()) {
            log(message, messageType);
        }
    }

    public void log(Exception e) {
        log(String.valueOf(e), MessageType.ERROR);
    }

    public void logPrivate(String message, @NotNull MessageType messageType) {
        switch (messageType) {
            case INFO -> logger.info(message);
            case WARN -> logger.warn(message);
            case ERROR -> logger.error(message);
            case DEBUG -> logger.debug(message);
            default -> logger.error("ERROR UNKNOWN - {}", message);
        }
    }

    public <T> void logPrivate(@NotNull Result<T> result) {
        for (String message : result.getMessages()) {
            switch (result.getType()) {
                case SUCCESS, CANCELLED -> logger.info(message);
                case INVALID, WARN -> logger.warn(message);
                case FAILED -> logger.error(message);
                default -> logger.error("ERROR UNKNOWN - {}", message);
            }
        }
    }

    public Result<Void> deleteModListProfile(UUID modListProfileId) {
        return storageController.deleteModListProfile(modListProfileId);
    }

    public Result<Void> saveUserConfiguration() {
        return storageController.saveUserConfiguration(userConfiguration);
    }

    public Result<Void> updateModListLoadPriority() {
        return storageController.updateModListLoadPriority(currentModListProfile.getId(), currentModList);
    }

    public Result<ModListProfile> loadModListProfileById(UUID modListProfileId) {
        return storageController.loadModListProfileById(modListProfileId);
    }

    public Result<Void> updateModListActiveMods() {
        return storageController.updateModListActiveMods(currentModListProfile.getId(), currentModList);
    }

    public Result<Void> saveModListProfileDetails(Triple<UUID, String, SpaceEngineersVersion> modListProfileDetails) {
        return storageController.saveModListProfileDetails(modListProfileDetails);
    }

    public Result<Void> saveModListProfile(ModListProfile modListProfile) {
        return storageController.saveModListProfile(modListProfile);
    }

    public Result<Void> saveCurrentModListProfile() {
        return storageController.saveModListProfile(currentModListProfile);
    }

    public Result<Void> updateModInformation(List<Mod> modList) {
        return storageController.updateModInformation(modList);
    }

    public Result<Void> deleteSaveProfile(SaveProfile saveProfile) {
        return storageController.deleteSaveProfile(saveProfile);
    }

    public Result<Void> updateModListProfileModList() {
        return storageController.updateModListProfileModList(currentModListProfile.getId(), currentModList);
    }

    public Result<Void> saveSaveProfile(SaveProfile saveProfile) {
        return storageController.saveSaveProfile(saveProfile);
    }

    public Result<Void> resetData() {
        return storageController.resetData();
    }

    public Result<Void> applyModlist(List<Mod> modList, SaveProfile saveProfile) throws IOException {
        return storageController.applyModlist(modList, saveProfile);
    }

    public Result<SaveProfile> copySaveProfile(SaveProfile saveProfile, List<SaveProfile> saveProfileList) throws IOException {
        return storageController.copySaveProfile(saveProfile, saveProfileList);
    }

    public Result<SaveProfile> getSaveProfile(File sandboxConfigFile) throws IOException {
        return storageController.getSpaceEngineersOneSaveProfile(sandboxConfigFile);
    }

    public Result<String> getSaveName(File sandboxConfigFile) throws IOException {
        return storageController.getSaveName(sandboxConfigFile);
    }

    /**
     * Sets the theme for our application based on the users preferred theme using reflection.
     * It expects to receive a list of CheckMenuItems that represent the UI dropdown list for all the available system themes in the MenuBar. Not the *best* way to do this, but it works.
     */
    public void setUserSavedApplicationTheme(@NotNull List<CheckMenuItem> themeList) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (CheckMenuItem c : themeList) {
            String currentTheme = Strings.CS.removeEnd(c.getId(), "Theme");
            String themeName = currentTheme.substring(0, 1).toUpperCase() + currentTheme.substring(1);
            if (themeName.equals(StringUtils.deleteWhitespace(userConfiguration.getUserTheme()))) {
                c.setSelected(true);
                Class<?> cls = Class.forName("atlantafx.base.theme." + StringUtils.deleteWhitespace(userConfiguration.getUserTheme()));
                Theme theme = (Theme) cls.getDeclaredConstructor().newInstance();
                Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
            }
        }
    }

    public Result<Void> setCurrentModListProfile(UUID modListProfileId) {
        Result<Void> setResult = new Result<>();
        Result<ModListProfile> newCurrentModListProfileResult = storageController.loadModListProfileById(modListProfileId);
        if (newCurrentModListProfileResult.isFailure()) {
            log(newCurrentModListProfileResult);
            setResult.addAllMessages(newCurrentModListProfileResult);
            return setResult;
        }

        log(newCurrentModListProfileResult.getCurrentMessage(), MessageType.INFO);
        ModListProfile modListProfile = newCurrentModListProfileResult.getPayload();
        currentModListProfile = modListProfile;
        currentModList = FXCollections.observableArrayList(currentModListProfile.getModList());
        activeModCount.set((int) currentModList.stream().filter(Mod::isActive).count());
        setResult.addAllMessages(setLastActiveModlistProfile(modListProfile.getId()));
        return setResult;
    }

    public Result<Void> setCurrentSaveProfile(SaveProfile newCurrentSaveProfile) {
        this.currentSaveProfile = newCurrentSaveProfile;
        return setLastActiveSaveProfile(newCurrentSaveProfile.getId());
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

    public Result<List<Mod>> getModlistFromSave(File sandboxConfigFile) {
        Result<List<Mod>> modListResult = storageController.getModlistFromSave(sandboxConfigFile);

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

    public Task<List<Result<Mod>>> importModsFromList(List<Mod> modList) {
        return new Task<>() {
            @Override
            protected List<Result<Mod>> call() {
                List<Result<Mod>> modInfoFillOutResults = new ArrayList<>();
                AtomicInteger completedMods = new AtomicInteger(0);
                int totalMods = modList.size();

                try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                    CompletionService<Result<Mod>> completionService = new ExecutorCompletionService<>(executorService);
                    Random random = new Random();
                    for (Mod m : modList) {
                        // Submit the task without waiting for it to finish
                        completionService.submit(() -> {
                            if (m instanceof ModIoMod && totalMods > 1) {
                                Thread.sleep(random.nextInt(200, 600));
                            }
                            return fillOutModInformation(m);
                        });
                    }
                    for (int i = 0; i < totalMods; i++) {
                        Future<Result<Mod>> completedFuture;
                        Result<Mod> result;
                        try {
                            completedFuture = completionService.take();
                            result = completedFuture.get();
                            modInfoFillOutResults.add(result);
                        } catch (InterruptedException | ExecutionException e) {
                            result = new Result<>();
                            result.addMessage(getStackTrace(e), ResultType.FAILED);
                            modInfoFillOutResults.add(result);
                        }

                        int done = completedMods.incrementAndGet();
                        updateProgress(done, totalMods);
                        updateMessage(String.format("Mods processed: %s/%s", done, totalMods));
                    }
                }
                return modInfoFillOutResults;
            }
        };
    }

    public Result<Mod> fillOutModInformation(Mod mod) throws InterruptedException {
        Result<String[]> scrapeResult = modInfoController.fillOutModInformation(mod);
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
                if (mod instanceof SteamMod steamMod) {
                    formatter = new DateTimeFormatterBuilder()
                            .parseCaseInsensitive()
                            .appendPattern(modDateFormat)
                            .toFormatter(Locale.ENGLISH);
                    steamMod.setLastUpdated(LocalDateTime.parse(modInfo[3], formatter));
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

        return infoFilloutResult;
    }

    public Task<List<Result<String>>> importSteamCollection(String collectionId) {
        return new Task<>() {
            @Override
            protected List<Result<String>> call() {
                try {
                    updateMessage("Processing Steam Collection...");
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

    //This isn't down in the ModlistService because we need to actually update the numerator on each and every single completed get call for the UI progress
    // bars to work properly.
    public List<Result<String>> scrapeSteamModCollectionModList(String collectionId) throws IOException {

        List<Result<String>> steamCollectionModIds = modInfoController.scrapeSteamModCollectionModList(collectionId);

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


    /**
     * Converts a list of Mod IO urls to their respective ID's.
     *
     * @param modUrls List of Mod.io url's
     * @return The task to perform the conversion.
     */
    //TODO: Should this use the single call just below it?
    public Task<List<Result<String>>> convertModIoUrlListToIds(List<String> modUrls) {
        return new Task<>() {
            @Override
            protected List<Result<String>> call() throws InterruptedException {
                List<Result<String>> modIdResults = new ArrayList<>();
                AtomicInteger completedUrls = new AtomicInteger(0);
                int totalUrls = modUrls.size();
                try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                    CompletionService<Result<String>> completionService = new ExecutorCompletionService<>(executorService);

                    for (String modUrl : modUrls) {
                        completionService.submit(() -> {
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
                    }
                    for(int i = 0; i < totalUrls; i++) {
                        Result<String> result;
                        try {
                            Future<Result<String>> completedFuture = completionService.take();
                             result = completedFuture.get();
                             modIdResults.add(result);
                        } catch (InterruptedException | ExecutionException e) {
                            result = new Result<>();
                            result.addMessage(getStackTrace(e), ResultType.FAILED);
                            modIdResults.add(result);
                        }

                        int done = completedUrls.incrementAndGet();
                        updateProgress(done, totalUrls);
                        updateMessage(String.format("URL's processed: %s/%s", done, totalUrls));
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
                updateMessage("Converting Mod.io URL to ID...");
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
            return modInfoController.getModIoIdFromUrlName(modUrl);
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
        return modInfoController.getModIdsFromFile(modlistFile, modType);
    }

    public Result<Void> exportModListProfile(ModListProfile modListProfile, File exportLocation) {
        return storageController.exportModListProfile(modListProfile, exportLocation);
    }

    //Once the file is read, we want to add its details to the detail list
    public Result<Void> importModlistProfile(File saveLocation) {
        Result<Void> importResult = new Result<>();
        Result<ModListProfile> modlistProfileResult = storageController.importModListProfile(saveLocation);
        if (modlistProfileResult.isSuccess()) {
            ModListProfile importModListProfile = modlistProfileResult.getPayload();
            boolean duplicateProfileExists = modListProfileDetails
                    .stream()
                    .anyMatch(modlistProfile -> modlistProfile.getMiddle().toLowerCase().trim().equals(importModListProfile.getProfileName().toLowerCase().trim()));
            if (!duplicateProfileExists) {
                for (int i = 0; i < importModListProfile.getModList().size(); i++) {
                    importModListProfile.getModList().get(i).setLoadPriority(i + 1);
                }
                updateModInformation(importModListProfile.getModList());
                Result<Void> saveModListResult = saveModListProfile(importModListProfile);
                if (saveModListResult.isFailure()) {
                    log(saveModListResult);
                    return saveModListResult;
                }

                modListProfileDetails.add(MutableTriple.of(importModListProfile.getId(), importModListProfile.getProfileName(), importModListProfile.getSpaceEngineersVersion()));
                importResult.addAllMessages(setCurrentModListProfile(importModListProfile.getId()));
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
        userConfiguration.setLastActiveModProfileId(modlistProfileId);
        return saveUserConfiguration();
    }

    public Result<Void> setLastActiveSaveProfile(UUID saveProfileId) {
        userConfiguration.setLastActiveSaveProfileId(saveProfileId);
        return saveUserConfiguration();
    }

    public Result<ModListProfile> getLastActiveModlistProfile() {
        return loadModListProfileById(userConfiguration.getLastActiveModProfileId());
    }

    public Optional<SaveProfile> getLastActiveSaveProfile() {
        return saveProfiles.stream()
                .filter(saveProfile -> saveProfile.getId().equals(userConfiguration.getLastActiveSaveProfileId()))
                .findFirst();
    }

    public void setSaveProfileInformationAfterSuccessfullyApplyingModlist() {
        currentSaveProfile.setLastUsedModListProfileId(currentModListProfile.getId());
        currentSaveProfile.setLastSaveStatus(SaveStatus.SAVED);
        userConfiguration.setLastModifiedSaveProfileId(currentSaveProfile.getId());
        Result<Void> saveResult = saveSaveProfile(currentSaveProfile);
        if (saveResult.isFailure()) {
            log(saveResult);
            Popup.displaySimpleAlert(saveResult);
            return;
        }

        saveResult = saveUserConfiguration();
        if (saveResult.isFailure()) {
            log(saveResult);
            Popup.displaySimpleAlert(saveResult);
        }
    }

    //TODO: Add a dialog option to choose whether you have space engineers 1, 2, or both.
    // Make it a part of UserConfiguration, and add menu options to toggle SEMM support for either game. Reference it when creating mod + save lists.
    public void displayTutorial(Stage stage, MasterManager masterManager) {
        log("Starting tutorial...", MessageType.INFO);

        stage.setResizable(false);
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, keyboardButtonNavigationDisabler);

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
