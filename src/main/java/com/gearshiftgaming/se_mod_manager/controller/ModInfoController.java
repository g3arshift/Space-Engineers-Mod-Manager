package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModInfoService;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.ModType;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * Provides the core controls behind adding mods any way that they are allowed to be added.
 */
public class ModInfoController {

	private final ModInfoService MOD_INFO_SERVICE;

	public ModInfoController(ModlistRepository modlistRepository, Properties properties) {
		MOD_INFO_SERVICE = new ModInfoService(modlistRepository, properties);
	}

	//This is called in this roundabout manner because the UI can only be updated by a JFX thread, and the .get from futures is a blocking call.
	public Result<String[]> fillOutModInformation(Mod mod) throws IOException {
		return MOD_INFO_SERVICE.scrapeModInformation(mod);
	}

	public List<Result<String>> scrapeSteamModCollectionModList(String collectionId) throws IOException {
		return MOD_INFO_SERVICE.scrapeSteamCollectionModIds(collectionId);
	}

	public Result<String> getModIoIdFromUrlName(String modName) throws IOException {
		return MOD_INFO_SERVICE.getModIoIdFromName(modName);
	}

	public List<String> getModIdsFromFile(File modlistFile, ModType modType) throws IOException {
		return MOD_INFO_SERVICE.getModIdsFromFile(modlistFile, modType);
	}
}
