package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.SaveRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 *
 * @author Gear Shift
 */
public class SaveService {

	private final SaveRepository SAVE_REPOSITORY;

	private final SandboxService SANDBOX_SERVICE;

	public SaveService(SaveRepository SAVE_REPOSITORY, SandboxService SANDBOX_SERVICE) {
		this.SAVE_REPOSITORY = SAVE_REPOSITORY;
		this.SANDBOX_SERVICE = SANDBOX_SERVICE;
	}

	public Result<SaveProfile> copySaveFiles(SaveProfile sourceSaveProfile) throws IOException {
		Result<SaveProfile> result = new Result<>();

		//Gets the path without Sandbox_config.sbc at the end
		String sourceSavePath = sourceSaveProfile.getSavePath().substring(0, sourceSaveProfile.getSavePath().length() - 19);
		String destinationSavePath;

		//Checks if our intended save path already exists, and if it does, create a new name.
		boolean pathHasDuplicate;
		int copyIndex = 1;
		do {
			destinationSavePath = sourceSavePath + "_" + copyIndex;
			pathHasDuplicate = Files.exists(Path.of(destinationSavePath));
			copyIndex++;
		} while (pathHasDuplicate);

		Result<String> sandboxConfigResult = SANDBOX_SERVICE.getSandboxFromFile(new File(sourceSavePath + "\\Sandbox_config.sbc"));

		//Check that we got a config before we do anything else
		if (sandboxConfigResult.isSuccess()) {
			String sandboxConfig = sandboxConfigResult.getPayload();
			int[] sessionNameIndexPositions = getSessionNameIndexPositions(sandboxConfig);

			//Check if the sandbox_config actually contains a <SessionName> tag.
			if (sessionNameIndexPositions[0] != -1 && sessionNameIndexPositions[1] != -1) {
				//Copies our source directory to a new location.
				SAVE_REPOSITORY.copySave(sourceSavePath, destinationSavePath);
				SaveProfile copiedSaveProfile = new SaveProfile(sourceSaveProfile);

				//Check that our copy performed correctly.
				if (Files.exists(Path.of(destinationSavePath))) {
					result.addMessage("Save directory successfully copied.", ResultType.SUCCESS);
					copiedSaveProfile.setSavePath(destinationSavePath + "\\Sandbox_config.sbc");

					//Change the name in our copied save's Sandbox_config and Sandbox files to match the save name.
					copiedSaveProfile.setSaveName(getSessionName(sandboxConfig, destinationSavePath) + "_" + (copyIndex - 1));
					copiedSaveProfile.setProfileName(copiedSaveProfile.getProfileName() + "_" + (copyIndex - 1));

					//Change the name in our copied save's Sandbox_config file to match the save name.
					Result<Void> sandboxConfigNameChangeResult = changeSandboxConfigSessionName(sandboxConfig, copiedSaveProfile);

					//Change the name in our copied save's Sandbox file to match the save name.
					if (sandboxConfigNameChangeResult.isSuccess()) {
						Result<String> sandboxResult = SANDBOX_SERVICE.getSandboxFromFile(new File(destinationSavePath + "\\Sandbox.sbc"));
						String sandbox = sandboxResult.getPayload();

						//Change the name in our copied save's Sandbox file to match the save name. This is NOT THE SAME AS the previous step.
						Result<Void> sandboxNameChangeResult = changeSandboxSessionName(sandbox, copiedSaveProfile);
						if (sandboxNameChangeResult.isSuccess()) {
							result.addMessage("Successfully copied profile.", ResultType.SUCCESS);
							result.setPayload(copiedSaveProfile);
						} else {
							result.addMessage(sandboxNameChangeResult);

							//Cleanup the copied save since our rename failed
							FileUtils.deleteDirectory(new File(destinationSavePath));
						}
					} else {
						result.addMessage(sandboxConfigNameChangeResult);

						//Cleanup the copied save since our rename failed
						FileUtils.deleteDirectory(new File(destinationSavePath));
					}
				} else {
					result.addMessage("Failed to copy save directory.", ResultType.FAILED);
				}
			} else {
				result.addMessage("Save does not contain a <SessionName> tag, and cannot be copied.", ResultType.FAILED);
			}
		} else {
			result.addMessage(sandboxConfigResult);
		}
		return result;
	}

	public String getSessionName(String sandboxConfig, String saveDestinationPath) {
		String saveName;

		int[] sessionNameIndexPositions = getSessionNameIndexPositions(sandboxConfig);

		if (sessionNameIndexPositions[0] != -1 && sessionNameIndexPositions[1] != -1) {
			saveName = sandboxConfig.substring(sessionNameIndexPositions[0], sessionNameIndexPositions[1]);
		} else {
			//If our file does not contain a session name for whatever reason set the name to the folder name the save is contained within.
			String[] pathSections = StringUtils.split(saveDestinationPath, "\\");
			int i;
			if (FilenameUtils.getExtension(saveDestinationPath).equals("sbc")) {
				i = 2;
			} else i = 1;

			saveName = pathSections[pathSections.length - i];
		}
		return saveName;
	}

	private int[] getSessionNameIndexPositions(String sandboxConfig) {
		int saveNameStartIndex = StringUtils.indexOf(sandboxConfig, "<SessionName>");
		final int SESSION_NAME_LENGTH = 13;

		if (saveNameStartIndex == -1) {
			return new int[]{-1, -1};
		}

		saveNameStartIndex += SESSION_NAME_LENGTH; //This is how long <SessionName> is.
		int saveNameEndIndex = saveNameStartIndex;
		boolean foundName = false;
		do {
			if (sandboxConfig.charAt(saveNameEndIndex) != '<') {
				saveNameEndIndex++;
			} else {
				foundName = true;
			}
		} while (!foundName && saveNameEndIndex < sandboxConfig.length());

		return new int[]{saveNameStartIndex, saveNameEndIndex};
	}

	private Result<Void> changeSandboxConfigSessionName(String sandboxConfig, SaveProfile copiedSaveProfile) throws IOException {
		Result<Void> result = new Result<>();

		int[] sessionNameIndexPositions = getSessionNameIndexPositions(sandboxConfig);

		//Change the name in our copied save's Sandbox_config file to match the save name.
		result.addMessage(SANDBOX_SERVICE.changeConfigSessionName(sandboxConfig, copiedSaveProfile, sessionNameIndexPositions));

		return result;
	}

	private Result<Void> changeSandboxSessionName(String sandbox, SaveProfile copiedSaveProfile) throws IOException {
		Result<Void> result = new Result<>();

		int[] sessionNameIndexPositions = getSessionNameIndexPositions(sandbox);

		if (sessionNameIndexPositions[0] != -1 && sessionNameIndexPositions[1] != -1) {
			result.addMessage(SANDBOX_SERVICE.changeSandboxSessionName(sandbox, copiedSaveProfile, sessionNameIndexPositions));
		} else {
			result.addMessage("Save does not contain a SessionName tag,", ResultType.FAILED);
		}

		return result;
	}
}

