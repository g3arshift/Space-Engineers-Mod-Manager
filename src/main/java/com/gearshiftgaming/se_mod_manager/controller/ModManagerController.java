package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.domain.ModService;
import com.gearshiftgaming.se_mod_manager.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.FileChooserAndOption;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.ui.ModManagerView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

public class ModManagerController {
    List<Mod> modList;
    private final ModManagerView modManagerView;
    private final ModService modService;
    private final SandboxService sandboxService;
    private final String DESKTOP_PATH;
    private final String APP_DATA_PATH;
    static final Logger log = LoggerFactory.getLogger(ModManagerController.class);


    public ModManagerController(List<Mod> modList, ModManagerView modManagerView, ModService modService, SandboxService sandboxService, String desktopPath, String appDataPath) {
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
                log.error("User opted to not continue with no Steam Workshop connection. Exiting.");
                return;
            } else modService.setWorkshopConnectionActive(false);
        }

        Result<List<Mod>> modListResult = new Result<>();

        modManagerView.displayWelcomeDialog();

        FileChooserAndOption fileChooserAndOption;

        //Grab the list of mod ID's from our file, then scrape the friendly name and the service we retrieved it from
        do {
            fileChooserAndOption = modManagerView.getModListFromFile(DESKTOP_PATH);
            if (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION) {
                log.info("Grabbing mods from " + (fileChooserAndOption.getFc().getSelectedFile()));
                modListResult = modService.getInjectableModListFromFile(fileChooserAndOption.getFc().getSelectedFile());

                if (!modListResult.isSuccess()) {
                    log.warn(modListResult.getMessages().getLast());
                    modManagerView.displayResult(modListResult);
                } else modList = modListResult.getPayload();
            } else {
                modManagerView.displayCancellationDialog();
                log.info("Program closed by user.");
            }
        } while (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION && !modListResult.isSuccess());


        //Get our Sandbox_config file that we want to modify from the user, then write the new mod list to it
        if (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION) {
            log.info("Number of mods to inject is " + modListResult.getPayload().size());
            modService.generateModListSteam(modList);

            Result<File> sandboxFileResult = new Result<>();

            modManagerView.displaySandboxInjectDialog();

            do {
                fileChooserAndOption = modManagerView.getSandboxConfigFromFile(APP_DATA_PATH);

                if (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION) {
                    sandboxFileResult = sandboxService.getSandboxConfigFromFile(fileChooserAndOption.getFc());

                    if (!sandboxFileResult.isSuccess()) {
                        log.warn(sandboxFileResult.getMessages().getLast());
                        modManagerView.displayResult(sandboxFileResult);
                    }
                    log.info("Injecting mods into " + (sandboxFileResult.getPayload()).getPath());
                } else {
                    modManagerView.displayCancellationDialog();
                    log.info("Program closed by user.");
                }
            } while (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION && !sandboxFileResult.isSuccess());

            //Get the location the user wants to save the modified Sandbox_config.sbc file and then save it there
            if (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION) {
                Path savePath;
                Result<Boolean> sandboxInjectionResult = new Result<>();

                modManagerView.displaySaveLocationDialog();
                do {

                    fileChooserAndOption = modManagerView.getSavePath(DESKTOP_PATH);

                    if (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION) {
                        savePath = fileChooserAndOption.getFc().getSelectedFile().toPath();

                        //Check if the file exists and let the user choose if they want to overwrite it
                        if (new File(savePath.toString()).exists()) {
                            int option;
                            option = modManagerView.getOverwriteOption();

                            if (option == JFileChooser.APPROVE_OPTION) {
                                sandboxInjectionResult = sandboxService.addModsToSandboxConfig(sandboxFileResult.getPayload(), savePath, modList);
                            } else if (option == JOptionPane.CANCEL_OPTION) {
                                modManagerView.displayCancellationDialog();
                                fileChooserAndOption.setOption(option);
                            } else modManagerView.displayCancellationDialog();
                        } else
                            sandboxInjectionResult = sandboxService.addModsToSandboxConfig(sandboxFileResult.getPayload(), savePath, modList);
                    }
                } while (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION && !sandboxInjectionResult.isSuccess());

                switch (sandboxInjectionResult.getType()) {
                    case SUCCESS -> {
                        log.info("Successfully injected mod list into save.");
                        modManagerView.displayResult(sandboxInjectionResult);
                    }
                    case FAILED -> {
                        log.info(sandboxInjectionResult.getMessages().getLast());
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
                log.warn("Attempt " + attempt + ": Failed to connect to Steam Workshop. Retrying...");
            }
        }
        if (!success) {
            log.error("Failed to connect to Steam Workshop.");
        } else log.info("Successfully connected to Steam Workshop.");
        modService.setWorkshopConnectionActive(true);
        return success;
    }
}
