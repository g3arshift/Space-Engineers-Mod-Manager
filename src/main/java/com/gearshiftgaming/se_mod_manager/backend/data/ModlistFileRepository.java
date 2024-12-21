package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.SteamMod;

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

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public class ModlistFileRepository implements ModlistRepository {
    private final Pattern STEAM_WORKSHOP_ID_REGEX_PATTERN;

    public ModlistFileRepository() {
        STEAM_WORKSHOP_ID_REGEX_PATTERN = Pattern.compile("([0-9])\\d*");
    }

    //TODO: Add modio functionality
    @Override
    public List<Mod> getSteamModList(File modListFile) throws IOException {
        //We use a set to prevent duplicate lines from being added
        //TODO: Check to make sure the workshop url is in the correct form. This includes its variants.
        Set<Mod> modSet = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(modListFile))) {
            String modUrl;
            while ((modUrl = br.readLine()) != null) {
                //Grab just the ID from the full URLs
                SteamMod mod = new SteamMod(STEAM_WORKSHOP_ID_REGEX_PATTERN.matcher(modUrl).results().map(MatchResult::group).collect(Collectors.joining("")));

                //Don't add blank lines
                if (!mod.getId().isBlank()) {
                    modSet.add(mod);
                }
            }
        }
        return new ArrayList<>(modSet);
    }
}
