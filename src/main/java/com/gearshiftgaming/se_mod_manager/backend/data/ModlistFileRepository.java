package com.gearshiftgaming.se_mod_manager.backend.data;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModlistFileRepository implements ModlistRepository {
	private final Pattern STEAM_WORKSHOP_ID_REGEX_PATTERN;

	public ModlistFileRepository() throws IOException {
		final Properties PROPERTIES = new Properties();
		try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
			PROPERTIES.load(input);
		}

		STEAM_WORKSHOP_ID_REGEX_PATTERN = Pattern.compile(PROPERTIES.getProperty("semm.steam.mod.id.pattern"));
	}

	@Override
	public List<String> getSteamModList(File modListFile) throws IOException {
		//We use a set to prevent duplicate lines from being added
		Set<String> modIds = new LinkedHashSet<>();
		try (BufferedReader br = new BufferedReader(new FileReader(modListFile))) {
			String modUrl;
			while ((modUrl = br.readLine()) != null) {
				modUrl = modUrl.trim();
				//Grab just the ID from the full URLs
				if(StringUtils.isNumeric(modUrl)) {
					modIds.add(modUrl);
				} else {
					String modId = STEAM_WORKSHOP_ID_REGEX_PATTERN.matcher(modUrl).results().map(MatchResult::group).collect(Collectors.joining(""));

					//Don't add blanks
					if(!modId.isBlank()) {
						modId = modId.substring(3);
						modIds.add(modId);
					}
				}
			}
		}
		return modIds.stream().toList();
	}

	@Override
	public List<String> getModIoModUrls(File modListFile) throws IOException {
		//We use a set to prevent duplicate lines from being added
		Set<String> modUrlSet = new LinkedHashSet<>();
		try (BufferedReader br = new BufferedReader(new FileReader(modListFile))) {
			String modUrl;
			while ((modUrl = br.readLine()) != null) {
				if(!modUrl.isBlank()) {
					modUrlSet.add(modUrl);
				}
			}
		}
		return new ArrayList<>(modUrlSet);
	}
}
