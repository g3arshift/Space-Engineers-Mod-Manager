package com.gearshiftgaming.se_mod_manager.frontend.models.utility;

import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.SimpleInputView;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModImportUtility {

	public static Mod addModScrapeResultsToModlist(final UiService UI_SERVICE, Stage stage, List<Result<Mod>> modInfoFillOutResults, int importedModlistSize) {
		int successfulScrapes = 0;
		int failedScrapes = 0;

		for (Result<Mod> currentModInfoFillOutResult : modInfoFillOutResults) {
			boolean shouldAddMod = false;
			if (currentModInfoFillOutResult.isSuccess() || currentModInfoFillOutResult.getType() == ResultType.REQUIRES_ADJUDICATION) {
				if (currentModInfoFillOutResult.getType() == ResultType.REQUIRES_ADJUDICATION) {
					int response = Popup.displayYesNoDialog(currentModInfoFillOutResult.getCurrentMessage(), stage, MessageType.WARN);
					if (response == 1) {
						shouldAddMod = true;
					}
				} else {
					shouldAddMod = true;
				}

				if (shouldAddMod) {
					Mod mod = currentModInfoFillOutResult.getPayload();
					currentModInfoFillOutResult.addMessage("Mod \"" + mod.getFriendlyName() + "\" has been successfully added.", ResultType.SUCCESS);
					mod.setLoadPriority(UI_SERVICE.getCurrentModList().size() + 1);
					UI_SERVICE.getCurrentModList().add(mod);
					successfulScrapes++;
					UI_SERVICE.logPrivate(currentModInfoFillOutResult);
				}
			} else {
				failedScrapes++;
				UI_SERVICE.log(currentModInfoFillOutResult);
			}
		}

		if (importedModlistSize == 1) {
			if (modInfoFillOutResults.getFirst().getType() != ResultType.REQUIRES_ADJUDICATION) {
				Popup.displaySimpleAlert(modInfoFillOutResults.getFirst(), stage);
			}
		} else {
			String modFillOutResultMessage = String.format("%d mods were successfully added. %d failed to be added.%s",
					successfulScrapes, failedScrapes, failedScrapes > 0 ? " Check the log for more information for each specific mod." : "");
			Popup.displaySimpleAlert(modFillOutResultMessage, MessageType.INFO);
			UI_SERVICE.log(modFillOutResultMessage, MessageType.INFO);
		}

		Mod topMostMod = null;
		for (Result<Mod> modResult : modInfoFillOutResults) {
			if (modResult.isSuccess()) {
				topMostMod = modResult.getPayload();
			}
		}
		return topMostMod;
	}


	public static Result<List<Mod>> getModlistFromSandboxConfig(final UiService UI_SERVICE, final File selectedSave, final Stage STAGE) {
		Result<List<Mod>> existingModlistResult = new Result<>();
		try {
			existingModlistResult = UI_SERVICE.getModlistFromSave(selectedSave);
		} catch (IOException e) {
			existingModlistResult.addMessage(e.toString(), ResultType.FAILED);
		}

		Popup.displaySimpleAlert(existingModlistResult, STAGE);

		return existingModlistResult;
	}

	public static String createNewModProfile(final UiService UI_SERVICE, final Stage STAGE, final SimpleInputView PROFILE_INPUT_VIEW) {
		boolean duplicateProfileName;
		String newProfileName = "";

		do {
			PROFILE_INPUT_VIEW.getInput().clear();
			PROFILE_INPUT_VIEW.getInput().requestFocus();
			PROFILE_INPUT_VIEW.show();
			ModlistProfile newModlistProfile = new ModlistProfile(PROFILE_INPUT_VIEW.getInput().getText());
			duplicateProfileName = profileNameExists(PROFILE_INPUT_VIEW.getInput().getText().toLowerCase().trim(), UI_SERVICE);

			if (duplicateProfileName) {
				Popup.displaySimpleAlert("Profile name already exists!", STAGE, MessageType.WARN);
			} else if (!PROFILE_INPUT_VIEW.getInput().getText().isBlank()) {
				UI_SERVICE.getMODLIST_PROFILES().add(newModlistProfile);
				UI_SERVICE.setCurrentModlistProfile(newModlistProfile);
				UI_SERVICE.log("Successfully created profile " + PROFILE_INPUT_VIEW.getInput().getText(), MessageType.INFO);
				newProfileName = PROFILE_INPUT_VIEW.getInput().getText();
				PROFILE_INPUT_VIEW.getInput().clear();
				UI_SERVICE.saveUserData();
			}
		} while (duplicateProfileName);

		return newProfileName;
	}

	private static boolean profileNameExists(String profileName, final UiService UI_SERVICE) {
		return UI_SERVICE.getMODLIST_PROFILES().stream()
				.anyMatch(modProfile -> modProfile.getProfileName().toLowerCase().trim().equals(profileName));
	}
}
