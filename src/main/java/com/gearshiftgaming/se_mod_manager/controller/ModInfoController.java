package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

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

	//This is called in this roundabout manner because the UI can only be updated by a JFX thread, and the .get from futures is a blocking call.
	public Result<Void> fillOutModInformation(Mod mod) throws IOException, ExecutionException, InterruptedException {
		//TODO:
		// When we are implementing this, here is how we want it to flow. Total rewrite of this class.
		// future.get is a blocking call. It will pause code execution until it's done.
		// 1. Create a virtual thread executor in the controller
		// 2. For each mod we want to get info for, create a function that will call all the normal code we want to get info and return a fully assembled mod object in a Result,
		//     and run executor.submit and call .submit on that function.
		// 3. Once every mod has had it submitted to the thread executor, return the list of futures up to the view controller
		// 4. In the view controller, call Platform.runLater on the list of futures. We will also want to, in the forEach loop we have in this list of future call, update some UI progress bar.
		//     4a. Additionally, we need to actually have the ModListService do the assembling of proper mod objects from the futures result.
		// 5. Once we have finally gotten our list of Result<Mod> from the fully finished list of futures, all with their full information, iterate this list and add the successful mods
		//     and print an error that summarizes the failures.
		// 6. Call executor.shutdown() on the executor.
		// We HAVE to do it this way cause you can only update the UI on the JFX thread, which you access through Platform.runLater, and the service layer shouldn't need access to the UI down here.

		return MODLIST_SERVICE.fillOutModInfoById(mod);
	}
}
