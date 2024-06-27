package com.gearshiftgaming.se_mod_manager.domain;

import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.ResultType;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModService {

    private final Pattern STEAM_WORKSHOP_ID_REGEX_PATTERN = Pattern.compile("([0-9])\\d*");
    private final String STEAM_WORKSHOP_URL = "https://steamcommunity.com/sharedfiles/filedetails/?id=";

    //TODO: Move to repository
    public Result getModListFile(JFileChooser fc) {

        Result modFileResult = new Result();

        File modListFile;

        modListFile = fc.getSelectedFile();
        if (!modListFile.exists()) {
            modFileResult.addMessage("File does not exist.", ResultType.INVALID);
        } else if (FilenameUtils.getExtension(modListFile.getName()).equals("txt") || FilenameUtils.getExtension(modListFile.getName()).equals("doc")) {
            modFileResult.addMessage(fc.getSelectedFile().getName() + " selected.", ResultType.SUCCESS);
            modFileResult.setPayload(modListFile);
        } else {
            modFileResult.addMessage("Incorrect file type selected. Please select a .txt or .doc file.", ResultType.INVALID);
        }
        return modFileResult;
    }

    public List<Mod> generateModListIds(File modListFile) {
        //TODO: checkInternetConnectivity()
        List<Mod> modList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(modListFile))) {
            String modUrl;
            //TODO: Check for blank lines and skip them
            while ((modUrl = br.readLine()) != null) {
                //Grab just the ID from the full URLs
                Mod mod = new Mod(STEAM_WORKSHOP_ID_REGEX_PATTERN.matcher(modUrl).results().map(MatchResult::group).collect(Collectors.joining("")));
                modList.add(mod);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return modList;
    }

    //TODO: We need to make this sometimes not return true. Probably do it with the internet check?
    //Scrape the workshop HTML pages for their titles, which are our friendly names
    public Callable<String> scrapeModFriendlyName(Mod mod) {
        return () -> Jsoup.connect(STEAM_WORKSHOP_URL + mod.getModId()).get().title();
    }
}
