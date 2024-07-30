package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

public class ModlistService {
    private final Properties properties;

    private final String STEAM_WORKSHOP_URL = "https://steamcommunity.com/sharedfiles/filedetails/?id=";

    //TODO: Implement other repositories for the expected data options
    private final ModlistRepository modFileRepository;
    private final String MOD_SCRAPING_SELECTOR;

    @Setter
    @Getter
    //TODO: Move this to master controller, and decide and log there.
    //"Retrieving mod information from Steam Workshop..."
    //"Not retrieving mod information from Steam Workshop."
    private boolean workshopConnectionActive;

    public ModlistService(ModlistRepository modFileRepository, Properties properties) {
        this.modFileRepository = modFileRepository;
        this.properties = properties;
        this.MOD_SCRAPING_SELECTOR = properties.getProperty("semm.steam.modScraper.workshop.type.cssSelector");
    }

    public Result<List<Mod>> getModListFromFile(String modFilePath) throws IOException {
        File modlistFile = new File(modFilePath);
        Result<List<Mod>> result = new Result<>();
        if (!modlistFile.exists()) {
            result.addMessage("File does not exist.", ResultType.INVALID);
        } else if (FilenameUtils.getExtension(modlistFile.getName()).equals("txt") || FilenameUtils.getExtension(modlistFile.getName()).equals("doc")) {
            result.addMessage(modlistFile.getName() + " selected.", ResultType.SUCCESS);
            result.setPayload(modFileRepository.getModList(modlistFile));
        } else {
            result.addMessage("Incorrect file type selected. Please select a .txt or .doc file.", ResultType.INVALID);
        }
        return result;
    }

    //TODO: Do this with the concurrency API
    //Take in our list of mod ID's and fill out the rest of their fields.
    public void generateModListSteam(List<Mod> modList) throws ExecutionException, InterruptedException {
        List<Future<String>> futures = new ArrayList<>(modList.size());

        //Create multiple virtual threads to efficiently scrape the page. We're using virtual ones here since this is IO intensive, not CPU
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Mod m : modList) {
                futures.add(executorService.submit(scrapeModInformationSteam(m)));
            }
        }

        for (int i = 0; i < modList.size(); i++) {
            String[] modInfo = futures.get(i).get().split(" Workshop::");
            if (modInfo[0].contains("_NOT_A_MOD")) {
                modList.get(i).setFriendlyName(modInfo[0]);
            } else {
                modList.get(i).setPublishedServiceName(modInfo[0]);
                modList.get(i).setFriendlyName(modInfo[1]);
            }
        }
    }

    //Scrape the Steam Workshop HTML pages for their titles, which are our friendly names
    private Callable<String> scrapeModInformationSteam(Mod mod) {
        return () -> Jsoup.connect(STEAM_WORKSHOP_URL + mod.getId()).get().title() + (checkIfModIsMod(mod.getId()) ? "" : "_NOT_A_MOD");
    }

    //Check if the mod we're scraping is actually a workshop mod.
    private Boolean checkIfModIsMod(String modId) throws IOException {
        return (Jsoup.connect(STEAM_WORKSHOP_URL + modId).get().select(MOD_SCRAPING_SELECTOR).toString().contains("Mod"));
    }
}

