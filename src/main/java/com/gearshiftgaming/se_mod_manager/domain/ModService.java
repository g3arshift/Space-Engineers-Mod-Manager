package com.gearshiftgaming.se_mod_manager.domain;

import com.gearshiftgaming.se_mod_manager.data.ModRepository;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ModService {

    private final Logger logger;
    private final String modScrapingCssSelector;
    private final ModRepository modFileRepository;

    @Setter
    @Getter
    private boolean workshopConnectionActive;

    public ModService(ModRepository modFileRepository, Logger logger, String modScrapingCssSelector) {
        this.modFileRepository = modFileRepository;
        this.logger = logger;
        this.modScrapingCssSelector = modScrapingCssSelector;
    }

    public Result<List<Mod>> getInjectableModListFromFile(String modFilePath) {
        return modFileRepository.getModList(modFilePath);
    }

    //TODO: This is running slowly again. Check that when we refactored the controller this is being called properly.
    public void generateModListSteam(List<Mod> modList) throws ExecutionException, InterruptedException, IOException {
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
                modList.get(i).setPublishedServiceName(modInfo[0]);
                modList.get(i).setFriendlyName(modInfo[1]);
            }
        } else logger.warn("Not retrieving mod information from Steam Workshop.");
    }

    //Scrape the Steam Workshop HTML pages for their titles, which are our friendly names
    private Callable<String> scrapeModInformationSteam(Mod mod) throws IOException {
        final String STEAM_WORKSHOP_URL = "https://steamcommunity.com/sharedfiles/filedetails/?id=";
        Document doc = Jsoup.connect(STEAM_WORKSHOP_URL + mod.getModId()).get();

        if (doc.select(modScrapingCssSelector).toString().contains("Mod")) {
            return doc::title;
        } else {
            logger.error("Mod " + mod.getModId() + " is not a workshop mod item.");
            return () -> doc.title() + "_NOT_A_MOD";
        }
    }
}
