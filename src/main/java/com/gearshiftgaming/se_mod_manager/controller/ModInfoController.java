package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.ModType;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;

import java.io.IOException;
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

	private final ModlistService MODLIST_SERVICE;

	public ModInfoController(ModlistRepository modlistRepository, Properties properties) {
		MODLIST_SERVICE = new ModlistService(modlistRepository, properties);
	}

	public Result<Mod> addModBySteamId(String modId) throws IOException {
		Mod mod = new Mod(modId, ModType.MOD_IO);
		Result<Void> modResult = MODLIST_SERVICE.getModInfoById(mod);
		//TODO: REMOVE
		return null;
	}
}
