package com.gearshiftgaming.se_mod_manager.domain;

import com.gearshiftgaming.se_mod_manager.data.ModFileRepository;
import com.gearshiftgaming.se_mod_manager.data.ModRepository;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import org.jsoup.Jsoup;
import org.slf4j.Logger;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ModService {

    private final String STEAM_WORKSHOP_URL = "https://steamcommunity.com/sharedfiles/filedetails/?id=";

    private final ModRepository modFileRepository;

    public ModService(ModFileRepository modFileRepository) {
        this.modFileRepository = modFileRepository;
    }

    public Result<File> getModListFromFile(JFileChooser fc) {
        return modFileRepository.getModFile(fc.getSelectedFile());
    }

    public List<Mod> generateModListSteam(File modListFile, Logger log) throws ExecutionException, InterruptedException {
        List<Mod> modList = modFileRepository.generateModListIds(modListFile);

        log.info("Number of mods in " + (modListFile.getName() + ": " + modList.size()));

        List<Future<String>> futures = new ArrayList<>(modList.size());

        //Create multiple virtual threads to efficiently scrape the page. We're using virtual ones here since this is IO intensive, not CPU
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Mod m : modList) {
                futures.add(executorService.submit(scrapeModInformationSteam(m)));
            }
        }

        for(int i = 0; i < modList.size(); i++) {
            String[] modInfo = futures.get(i).get().split(" Workshop::");
            modList.get(i).setPublishedServiceName(modInfo[0]);
            modList.get(i).setFriendlyName(modInfo[1]);
        }

        return modList;
    }

    //Scrape the Steam Workshop HTML pages for their titles, which are our friendly names
    private Callable<String> scrapeModInformationSteam(Mod mod) {
        return () -> Jsoup.connect(STEAM_WORKSHOP_URL + mod.getModId()).get().title();
    }
}
