package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.data.ModFileRepository;
import com.gearshiftgaming.se_mod_manager.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.domain.ModService;
import com.gearshiftgaming.se_mod_manager.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.controller.ModManagerController;
import com.gearshiftgaming.se_mod_manager.ui.ModManagerView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SpaceEngineersModManager {

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

        final Logger log = LoggerFactory.getLogger(SpaceEngineersModManager.class);
        log.info("Started application...");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        List<Mod> modList = new ArrayList<>();
        ModManagerView modManagerView = new ModManagerView();
        ModService modService = new ModService(new ModFileRepository());
        SandboxService sandboxService = new SandboxService(new SandboxConfigFileRepository());

        final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
        final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

        ModManagerController modController = new ModManagerController(modList, modManagerView, modService, sandboxService, DESKTOP_PATH, APP_DATA_PATH);

        //Get the party started
        modController.injectModList();
        log.info("Application finished. Closing.");
    }
}