package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.domain.ModService;
import com.gearshiftgaming.se_mod_manager.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.FileChooserAndOption;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.ui.ModView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

public class ModManagerController {
    List<Mod> modList;
    private final ModView modView;
    private final ModService modService;
    private final SandboxService sandboxService;
    private final String DESKTOP_PATH;
    private final String APP_DATA_PATH;
    static final Logger log = LoggerFactory.getLogger(ModManagerController.class);


    public ModManagerController(List<Mod> modList, ModView modView, ModService modService, SandboxService sandboxService, String desktopPath, String appDataPath) {
        this.modList = modList;
        this.modView = modView;
        this.modService = modService;
        this.sandboxService = sandboxService;
        this.DESKTOP_PATH = desktopPath;
        this.APP_DATA_PATH = appDataPath;
    }

    public void injectModList() throws ExecutionException, InterruptedException, IOException {

        if (checkWorkshopConnectivity()) {
            Result<File> modFileResult = new Result<>();

            modView.displayWelcome();

            FileChooserAndOption fileChooserAndOption;

            //Grab the list of mod ID's from our file and fill out the rest of the information it needs
            do {
                fileChooserAndOption = modView.getModListFromFile(DESKTOP_PATH);
                if (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION) {
                    modFileResult = modService.getModListFromFile(fileChooserAndOption.getFc());

                    if (!modFileResult.isSuccess()) {
                        log.warn(modFileResult.getMessages().getLast());
                        modView.displayResult(modFileResult);
                    }

                    log.info((modFileResult.getPayload()).getName());
                } else {
                    modView.displayCancellation();
                    log.info("Program closed by user.");
                }
            } while (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION && !modFileResult.isSuccess());


            //Get our Sandbox_config file from the user, then write the new modlist to it
            if (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION) {
                modList = modService.generateModListSteam(modFileResult.getPayload(), log);

                Result<File> sandboxFileResult = new Result<>();

                modView.displaySandboxDialog();

                do {
                    fileChooserAndOption = modView.getSandboxConfigFromFile(APP_DATA_PATH);

                    if (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION) {
                        sandboxFileResult = sandboxService.getSandboxConfigFromFile(fileChooserAndOption.getFc());

                        if (!sandboxFileResult.isSuccess()) {
                            log.warn(sandboxFileResult.getMessages().getLast());
                            modView.displayResult(sandboxFileResult);
                        }
                        log.info((sandboxFileResult.getPayload()).getName());
                    } else {
                        modView.displayCancellation();
                        log.info("Program closed by user.");
                    }
                } while (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION && !sandboxFileResult.isSuccess());

                //TODO: While we have a fail and the user isn't choosing to exit, run the below line
                Result<Boolean> sandboxInjectionResult = sandboxService.addModsToSandboxConfig(sandboxFileResult.getPayload(), modList);

                if (sandboxInjectionResult.isSuccess()) {
                    log.info("Successfully injected mod list into save.");
                }
            }

            //TODO: Generate new Sandbox_config.sbc based on existing file
            //TODO: Save file. Ask user if they want to overwrite the existing file, or save to a new location

            //TODO: Change this so it gives the user the option of continuing
        } else modView.displayConnectionError();
    }

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
                if(!success) {
                    attempt++;
                }
            } catch (Exception e) {
                attempt++;
                log.warn("Attempt " + attempt + ": Failed to connect to Steam Workshop. Retrying...");
            }
        }
        if(!success) {
            log.error("Failed to connect to Steam Workshop.");
        } else log.info("Successfully connected to Steam Workshop.");
        return success;
    }
}
