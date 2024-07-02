package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.domain.ModService;
import com.gearshiftgaming.se_mod_manager.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.SteamWorkshopConnection;
import com.gearshiftgaming.se_mod_manager.ui.ModManagerView;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ModManagerController {
    List<Mod> modList;
    private final ModManagerView modManagerView;
    private final ModService modService;
    private final SandboxService sandboxService;
    private final String DESKTOP_PATH;
    private final String APP_DATA_PATH;
    private final Logger logger;
    private final String CONNECTION_CHECK_URL;
    private final String CONNECTION_CHECK_TITLE;
    private final SteamWorkshopConnection steamWorkshopConnection = new SteamWorkshopConnection();


    public ModManagerController(Logger logger, List<Mod> modList, ModManagerView modManagerView, ModService modService, SandboxService sandboxService, String desktopPath, String appDataPath, String urlToUseForConnectionCheck, String titleToUseForConnectionCheck) {
        this.logger = logger;
        this.modList = modList;
        this.modManagerView = modManagerView;
        this.modService = modService;
        this.sandboxService = sandboxService;
        this.DESKTOP_PATH = desktopPath;
        this.APP_DATA_PATH = appDataPath;
        this.CONNECTION_CHECK_URL = urlToUseForConnectionCheck;
        this.CONNECTION_CHECK_TITLE = titleToUseForConnectionCheck;
    }

    public void injectModList() throws ExecutionException, InterruptedException, IOException {
        checkSteamWorkshopConnection();

        //Give the user the choice to continue anyway if there is no active workshop connection
        if (!steamWorkshopConnection.isSteamWorkshopConnectionActive()) {
            logger.error("Failed to connect to Steam Workshop.");
            if (modManagerView.getConnectionErrorOption() != JFileChooser.APPROVE_OPTION) {
                logger.error("User opted to not continue with no Steam Workshop connection. Exiting.");
                return;
            } else {
                logger.warn("Continuing with no active workshop connection.");
                modService.setWorkshopConnectionActive(false);
            }
        } else {
            logger.info("Successfully connected to Steam Workshop.");
            modService.setWorkshopConnectionActive(true);
        }

        modManagerView.displayWelcomeDialog();

        //Present the user with a file chooser dialog to select the mod list to load
        Result<List<Mod>> modListResult = getModList();

        //Get our Sandbox_config file that we want to modify from the user, then write the new mod list to it
        if (modListResult.isSuccess()) {
            modList = modListResult.getPayload();
            modService.generateModListSteam(modList);

            //Check and make sure all our mods are valid entries
            int numBadMods = 0;
            int badModListOverride = 0;
            for (Mod m : modList) {
                if (m.getFriendlyName().contains("_NOT_A_MOD")) {
                    numBadMods++;
                }
            }
            if (numBadMods != 0) {
                badModListOverride = modManagerView.getBadModListOverrideDialog();
            }

            if (badModListOverride != JOptionPane.NO_OPTION && badModListOverride != JOptionPane.DEFAULT_OPTION) {
                if(badModListOverride == 2) {
                    modList.removeIf(m -> m.getFriendlyName().contains("_NOT_A_MOD"));
                }
                logger.info("Number of mods to inject is " + modListResult.getPayload().size());

                Result<File> sandboxFileResult = getSandboxFileResult();

                //Get the location the user wants to save the modified Sandbox_config.sbc file and then save it there
                if (sandboxFileResult.isSuccess()) {
                    modManagerView.displaySaveLocationDialog();
                    Result<Boolean> sandboxInjectionResult = injectModsIntoSandboxConfig(sandboxFileResult.getPayload());

                    switch (sandboxInjectionResult.getType()) {
                        case SUCCESS -> {
                            logger.info("Successfully injected mod list into save.");
                            modManagerView.displayResult(sandboxInjectionResult);
                        }
                        case FAILED -> {
                            logger.info(sandboxInjectionResult.getMessages().getLast());
                            modManagerView.displayResult(sandboxInjectionResult);
                        }
                    }
                }
            } else modManagerView.displayCancellationDialog();
        }
    }

    public void checkSteamWorkshopConnection() {
        logger.info("Checking Steam Workshop connection...");
        int attempt = 0;
        boolean connectivityCheckSuccess = false;
        final int MAX_RETRIES = 3;

        //Check if we have a valid connection to the steam workshop
        while (attempt < MAX_RETRIES && !connectivityCheckSuccess) {
            try {
                steamWorkshopConnection.checkWorkshopConnectivity(CONNECTION_CHECK_URL, CONNECTION_CHECK_TITLE);
                if (!steamWorkshopConnection.isSteamWorkshopConnectionActive()) {
                    logger.warn("Attempt " + (attempt + 1) + ": Failed to connect to Steam Workshop. Retrying...");
                    attempt++;
                } else connectivityCheckSuccess = true;
            } catch (Exception e) {
                attempt++;
                logger.warn(e.getMessage());
            }
        }
    }

    private Result<List<Mod>> getModList() {
        String modListPath;
        Result<List<Mod>> modListResult = new Result<>();
        do {
            modListPath = modManagerView.getModListFromFile(DESKTOP_PATH);
            if (!modListPath.equals(String.valueOf(JOptionPane.NO_OPTION))) {
                logger.info("Grabbing mods from " + (modListPath));
                modListResult = modService.getInjectableModListFromFile(modListPath);

                if (!modListResult.isSuccess()) {
                    logger.warn(modListResult.getMessages().getLast());
                    modManagerView.displayResult(modListResult);
                }
            } else {
                modManagerView.displayCancellationDialog();
                logger.info("Program closed by user.");
            }
        } while (!modListPath.equals(String.valueOf(JOptionPane.NO_OPTION)) && !modListResult.isSuccess());
        return modListResult;
    }

    private Result<File> getSandboxFileResult() throws ExecutionException, InterruptedException {
        String sandboxConfigPath;

        Result<File> sandboxFileResult = new Result<>();

        modManagerView.displaySandboxInjectDialog();

        do {
            sandboxConfigPath = modManagerView.getSandboxConfigFromFile(APP_DATA_PATH);

            if (!sandboxConfigPath.equals(String.valueOf(JOptionPane.NO_OPTION))) {
                sandboxFileResult = sandboxService.getSandboxConfigFromFile(sandboxConfigPath);

                if (!sandboxFileResult.isSuccess()) {
                    logger.warn(sandboxFileResult.getMessages().getLast());
                    modManagerView.displayResult(sandboxFileResult);
                }
                logger.info("Injecting mods into " + (sandboxFileResult.getPayload()).getPath());
            } else {
                modManagerView.displayCancellationDialog();
                logger.info("Program closed by user.");
            }
        } while (!sandboxConfigPath.equals(String.valueOf(JOptionPane.NO_OPTION)) && !sandboxFileResult.isSuccess());
        return sandboxFileResult;
    }

    private Result<Boolean> injectModsIntoSandboxConfig(File sandboxFile) throws IOException {
        Result<Boolean> sandboxInjectionResult = new Result<>();
        String savePath;
        do {
            savePath = modManagerView.getSavePath(DESKTOP_PATH);

            if (!savePath.equals(String.valueOf(JOptionPane.NO_OPTION))) {

                //Check if the file exists and let the user choose if they want to overwrite it
                if (new File(savePath).exists()) {
                    int overwriteChoice = modManagerView.getOverwriteOption();
                    if (overwriteChoice == JFileChooser.APPROVE_OPTION) {
                        sandboxInjectionResult = sandboxService.addModsToSandboxConfigFile(sandboxFile, savePath, modList);
                    } else if (overwriteChoice == JOptionPane.CANCEL_OPTION) {
                        modManagerView.displayCancellationDialog();
                        savePath = "1";
                    } else modManagerView.displayOverwriteAbortDialog();
                } else
                    sandboxInjectionResult = sandboxService.addModsToSandboxConfigFile(sandboxFile, savePath, modList);
            }
        } while (!savePath.equals(String.valueOf(JOptionPane.NO_OPTION)) && !sandboxInjectionResult.isSuccess());
        return sandboxInjectionResult;
    }
}
