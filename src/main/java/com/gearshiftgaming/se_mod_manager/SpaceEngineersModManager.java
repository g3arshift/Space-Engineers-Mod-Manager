package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.controller.ModManagerController;
import com.gearshiftgaming.se_mod_manager.data.ModFileRepository;
import com.gearshiftgaming.se_mod_manager.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.domain.ModService;
import com.gearshiftgaming.se_mod_manager.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.ui.ModManagerView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class SpaceEngineersModManager {

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

        final Logger logger = LogManager.getLogger(SpaceEngineersModManager.class);
        logger.info("Started application...");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        List<Mod> modList = new ArrayList<>();
        ModManagerView modManagerView = new ModManagerView();
        SandboxService sandboxService = new SandboxService(new SandboxConfigFileRepository());

        final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
        final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

        if(!Files.isDirectory(Path.of(APP_DATA_PATH))) {
            logger.warn("Space Engineers save location not found.");
        }

        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("src/main/resources/SEMM.properties")) {
            properties.load(input);
        } catch (IOException e) {
            logger.error("Could not load SEMM properties file.");
        }

        final String CONNECTION_CHECK_URL = properties.getProperty("semm.connectionCheck.steam.url");
        final String CONNECTION_CHECK_TITLE = properties.getProperty("semm.connectionCheck.steam.title");
        final String MOD_SCRAPING_SELECTOR = properties.getProperty("semm.steam.modScraper.workshop.type.cssSelector");

        ModService modService = new ModService(new ModFileRepository(), logger, MOD_SCRAPING_SELECTOR);


        ModManagerController modManagerController = new ModManagerController(logger, modList, modManagerView, modService, sandboxService, DESKTOP_PATH, APP_DATA_PATH, CONNECTION_CHECK_URL, CONNECTION_CHECK_TITLE);

        //Get the party started
        modManagerController.injectModList();
        logger.info("Application finished. Closing.");
    }
}