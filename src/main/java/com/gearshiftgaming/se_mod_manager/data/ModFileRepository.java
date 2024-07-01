package com.gearshiftgaming.se_mod_manager.data;

import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.ResultType;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModFileRepository implements ModRepository {
    private final Pattern STEAM_WORKSHOP_ID_REGEX_PATTERN;

    public ModFileRepository() {
        STEAM_WORKSHOP_ID_REGEX_PATTERN = Pattern.compile("([0-9])\\d*");
    }

    @Override
    public Result<List<Mod>> getModList(String modFilePath) {
        File modFile = new File(modFilePath);
        Result<List<Mod>> result = new Result<>();
        if (!modFile.exists()) {
            result.addMessage("File does not exist.", ResultType.INVALID);
        } else if (FilenameUtils.getExtension(modFile.getName()).equals("txt") || FilenameUtils.getExtension(modFile.getName()).equals("doc")) {
            result.addMessage(modFile.getName() + " selected.", ResultType.SUCCESS);
            result.setPayload(getModListModIds(modFile));
        } else {
            result.addMessage("Incorrect file type selected. Please select a .txt or .doc file.", ResultType.INVALID);
        }
        return result;
    }

    @Override
    public List<Mod> getModListModIds(File modListFile) {
        //We use a set to prevent duplicate lines from being added
        //TODO: Check for correct pattern of the workshop url, and its variants. Only add if it matches.
        Set<Mod> modSet = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(modListFile))) {
            String modUrl;
            while ((modUrl = br.readLine()) != null) {
                //Grab just the ID from the full URLs
                Mod mod = new Mod(STEAM_WORKSHOP_ID_REGEX_PATTERN.matcher(modUrl).results().map(MatchResult::group).collect(Collectors.joining("")));

                //Don't add blank lines
                if (!mod.getModId().isBlank()) {
                    modSet.add(mod);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>(modSet);
    }
}
