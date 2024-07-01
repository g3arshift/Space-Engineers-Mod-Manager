package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.domain.ModService;
import com.gearshiftgaming.se_mod_manager.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.ui.ModManagerView;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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


    public ModManagerController(Logger logger, List<Mod> modList, ModManagerView modManagerView, ModService modService, SandboxService sandboxService, String desktopPath, String appDataPath) {
        this.logger = logger;
        this.modList = modList;
        this.modManagerView = modManagerView;
        this.modService = modService;
        this.sandboxService = sandboxService;
        this.DESKTOP_PATH = desktopPath;
        this.APP_DATA_PATH = appDataPath;
    }

    public void injectModList() throws ExecutionException, InterruptedException, IOException {

        if (!checkWorkshopConnectivity()) {
            if (modManagerView.getConnectionErrorOption() != JFileChooser.APPROVE_OPTION) {
                logger.error("User opted to not continue with no Steam Workshop connection. Exiting.");
                return;
            } else modService.setWorkshopConnectionActive(false);
        }

        Result<List<Mod>> modListResult = new Result<>();

        modManagerView.displayWelcomeDialog();

        //TODO: Rewrite to work with strings?
        String modListPath;

        //Grab the list of mod ID's from our file, then scrape the friendly name and the service we retrieved it from
        do {
            modListPath = modManagerView.getModListFromFile(DESKTOP_PATH);
            if (!modListPath.equals(String.valueOf(JOptionPane.NO_OPTION))) {
                logger.info("Grabbing mods from " + (modListPath));
                modListResult = modService.getInjectableModListFromFile(modListPath);

                if (!modListResult.isSuccess()) {
                    logger.warn(modListResult.getMessages().getLast());
                    modManagerView.displayResult(modListResult);
                } else modList = modListResult.getPayload();
            } else {
                modManagerView.displayCancellationDialog();
                logger.info("Program closed by user.");
            }
        } while (!modListPath.equals(String.valueOf(JOptionPane.NO_OPTION)) && !modListResult.isSuccess());


        //TODO: Adapt the below to using paths, not files
        String sandboxConfigPath;
        //Get our Sandbox_config file that we want to modify from the user, then write the new mod list to it
        if (!modListPath.equals(String.valueOf(JOptionPane.NO_OPTION))) {
            logger.info("Number of mods to inject is " + modListResult.getPayload().size());
            modService.generateModListSteam(modList);

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


            //Get the location the user wants to save the modified Sandbox_config.sbc file and then save it there
            if (!sandboxConfigPath.equals(String.valueOf(JOptionPane.NO_OPTION))) {
                String savePath;
                Result<Boolean> sandboxInjectionResult = new Result<>();

                modManagerView.displaySaveLocationDialog();
                do {

                    savePath = modManagerView.getSavePath(DESKTOP_PATH);

                    if (!savePath.equals(String.valueOf(JOptionPane.NO_OPTION))) {

                        //Check if the file exists and let the user choose if they want to overwrite it
                        if (new File(savePath).exists()) {
                            int overwriteChoice = modManagerView.getOverwriteOption();
                            if (overwriteChoice == JFileChooser.APPROVE_OPTION) {
                                sandboxInjectionResult = sandboxService.addModsToSandboxConfigFile(sandboxFileResult.getPayload(), savePath, modList);
                            } else if (overwriteChoice == JOptionPane.CANCEL_OPTION) {
                                modManagerView.displayCancellationDialog();
                                savePath = "1";
                            } else modManagerView.displayOverwriteAbortDialog();
                        } else
                            sandboxInjectionResult = sandboxService.addModsToSandboxConfigFile(sandboxFileResult.getPayload(), savePath, modList);
                    }
                } while (!savePath.equals(String.valueOf(JOptionPane.NO_OPTION)) && !sandboxInjectionResult.isSuccess());

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
        }
    }

    //Query if a known workshop item is reachable and if it isn't then we can assume the workshop is not reachable.
    private boolean checkWorkshopConnectivity() {
        int attempt = 0;
        boolean success = false;
        int MAX_RETRIES = 3;
        while (attempt < MAX_RETRIES && !success) {
            try {
                Document doc = Jsoup.connect("https://steamcommunity.com/sharedfiles/filedetails/?id=2135416557")
                        .timeout(5000)
                        .get();
                success = doc.title().equals("Steam Workshop::Halo Mod - Weapons");
                if (!success) {
                    attempt++;
                }
            } catch (Exception e) {
                attempt++;
                logger.warn("Attempt " + attempt + ": Failed to connect to Steam Workshop. Retrying...");
            }
        }
        if (!success) {
            logger.error("Failed to connect to Steam Workshop.");
        } else logger.info("Successfully connected to Steam Workshop.");
        modService.setWorkshopConnectionActive(true);
        return success;
    }
}
