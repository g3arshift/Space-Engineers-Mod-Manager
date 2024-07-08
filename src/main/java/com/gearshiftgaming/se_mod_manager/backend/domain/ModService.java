package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

public class ModService {

    private final Logger logger;

    private final String STEAM_WORKSHOP_URL = "https://steamcommunity.com/sharedfiles/filedetails/?id=";
    private final ModRepository modFileRepository;
    private final String MOD_SCRAPING_SELECTOR;

    @Setter
    @Getter
    private boolean workshopConnectionActive;

    public ModService(ModRepository modFileRepository, Logger logger) {
        this.modFileRepository = modFileRepository;
        this.logger = logger;
        Properties properties = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
            properties.load(input);
        } catch (IOException e) {
            logger.error("Could not load SEMM.properties.");
        }

        this.MOD_SCRAPING_SELECTOR = properties.getProperty("semm.steam.modScraper.workshop.type.cssSelector");
    }

    public Result<List<Mod>> getInjectableModListFromFile(String modFilePath) {
        return modFileRepository.getModList(modFilePath);
    }

    //Take in our list of mod ID's and fill out the rest of their fields.
    public void generateModListSteam(List<Mod> modList) throws ExecutionException, InterruptedException {
        List<Future<String>> futures = new ArrayList<>(modList.size());

        //Check if our workshop connection is active.
        if (isWorkshopConnectionActive()) {
            logger.info("Retrieving mod information from Steam Workshop...");
            //Create multiple virtual threads to efficiently scrape the page. We're using virtual ones here since this is IO intensive, not CPU
            try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                for (Mod m : modList) {
                    futures.add(executorService.submit(scrapeModInformationSteam(m)));
                }
            }

            for (int i = 0; i < modList.size(); i++) {
                String[] modInfo = futures.get(i).get().split(" Workshop::");
                if(modInfo[0].contains("_NOT_A_MOD")) {
                    modList.get(i).setFriendlyName(modInfo[0]);
                } else {
                    modList.get(i).setPublishedServiceName(modInfo[0]);
                    modList.get(i).setFriendlyName(modInfo[1]);
                }
            }
        } else logger.warn("Not retrieving mod information from Steam Workshop.");
    }

    //Scrape the Steam Workshop HTML pages for their titles, which are our friendly names
    private Callable<String> scrapeModInformationSteam(Mod mod) {
        return () -> Jsoup.connect(STEAM_WORKSHOP_URL + mod.getModId()).get().title() + (checkIfModIsMod(mod.getModId()) ? "" : "_NOT_A_MOD");
    }

    //Check if the mod we're scraping is actually a workshop mod.
    private Boolean checkIfModIsMod(String modId) throws IOException {
        return (Jsoup.connect(STEAM_WORKSHOP_URL + modId).get().select(MOD_SCRAPING_SELECTOR).toString().contains("Mod"));
    }
}

