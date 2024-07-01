package com.gearshiftgaming.se_mod_manager.domain;

import com.gearshiftgaming.se_mod_manager.data.ModFileRepository;
import com.gearshiftgaming.se_mod_manager.data.ModRepository;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ModService {

    private final String STEAM_WORKSHOP_URL = "https://steamcommunity.com/sharedfiles/filedetails/?id=";

    private final ModRepository modFileRepository;

    @Setter
    @Getter
    private boolean workshopConnectionActive;

    public ModService(ModFileRepository modFileRepository) {
        this.modFileRepository = modFileRepository;
    }

    //TODO: This is too tightly coupled to the structure of the original mod file. Modify it so it returns a list of mod objects.
    public Result<List<Mod>> getInjectableModListFromFile(String modFilePath) {
        return modFileRepository.getModList(modFilePath);
    }

    public void generateModListSteam(List<Mod> modList) throws ExecutionException, InterruptedException {
        List<Future<String>> futures = new ArrayList<>(modList.size());

        //Check if our workshop connection is active.
        if (isWorkshopConnectionActive()) {
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
        }
    }

    //Scrape the Steam Workshop HTML pages for their titles, which are our friendly names
    private Callable<String> scrapeModInformationSteam(Mod mod) {
        return () -> Jsoup.connect(STEAM_WORKSHOP_URL + mod.getModId()).get().title();
    }
}
