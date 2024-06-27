package com.gearshiftgaming.se_mod_manager.ui;

import com.gearshiftgaming.se_mod_manager.domain.ModService;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.FileChooserAndOption;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.ResultType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ModController {
    private ModView modView;
    static final Logger log = LoggerFactory.getLogger(ModController.class);

    ModService modService;

    private FileChooserAndOption fileChooserAndOption;

    private final String DESKTOP_PATH;
    private final String APP_DATA_PATH;

    List<Mod> modList;

    public ModController(List<Mod> modList, ModView modView, ModService modService, FileChooserAndOption fileChooserAndOption, String desktopPath, String appDataPath) {
        this.modList = modList;
        this.modView = modView;
        this.modService = modService;
        this.fileChooserAndOption = fileChooserAndOption;
        this.DESKTOP_PATH = desktopPath;
        this.APP_DATA_PATH = appDataPath;
    }

    public void injectModList() {

        Result modFileResult = new Result(ResultType.NOT_INITIALIZED);

        modView.displayWelcome();

        //TODO: get this shit passed into the view properly
        do {
            fileChooserAndOption = modView.getModListFile(DESKTOP_PATH);
            if (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION) {
                modFileResult = modService.getModListFile(fileChooserAndOption.getFc());

                if (!modFileResult.isSuccess()) {
                    log.warn(modFileResult.getMessages().getLast().toString());
                    modView.displayResult(modFileResult);
                }
            } else {
                modView.displayCancellation();
                log.info("Program closed by user.");
            }
        } while (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION && !modFileResult.isSuccess());

        //Grab the list of mod ID's from our file
        if (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION) {
            modList = modService.generateModListIds((File) modFileResult.getPayload());
            log.info("Number of mods in " + ((File) modFileResult.getPayload()).getName() + ": " + modList.size());

            List<Future<String>> futures = new ArrayList<>(modList.size());

            //Create multiple virtual threads to efficiently scrape the page. We're using virtual ones here since this is IO intensive, not CPU
            try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {

                for (Mod m : modList) {
                    futures.add(executorService.submit(modService.scrapeModFriendlyName(m)));
                }
            }

        }


        //TODO: Select Sandbox_config.sbc to inject modlist into
        //TODO: Generate new Sandbox_config.sbc based on existing file
        //TODO: Save file. Ask user if they want to overwrite the existing file, or save to a new location
    }
}
