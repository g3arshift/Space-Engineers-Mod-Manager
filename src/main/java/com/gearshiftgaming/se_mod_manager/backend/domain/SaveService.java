package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.SaveRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SaveService {

	private final SaveRepository saveRepository;

	private final SandboxService sandboxService;

	public SaveService(SaveRepository saveRepository, SandboxService sandboxService) {
		this.saveRepository = saveRepository;
		this.sandboxService = sandboxService;
	}

	public Result<SaveProfile> copySaveFiles(SaveProfile sourceSaveProfile) throws IOException {
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

		saveRepository.copySave(sourceSavePath, destinationSavePath);

		//Copies our source directory to a new location.
		Result<SaveProfile> result = new Result<>();
		SaveProfile copiedSaveProfile = new SaveProfile(sourceSaveProfile);

		//Check that our copy performed correctly.
		if (Files.exists(Path.of(destinationSavePath))) {
			result.addMessage("Save directory successfully copied.", ResultType.SUCCESS);
			copiedSaveProfile.setSavePath(destinationSavePath + "\\Sandbox_config.sbc");
		} else {
			result.addMessage("Failed to copy save directory.", ResultType.FAILED);
		}

		//Change the name in our copied save's Sandbox_config and Sandbox files to match the save name.
		Result<String> sandboxConfigResult = sandboxService.getSandboxFromFile(new File(destinationSavePath + "\\Sandbox_config.sbc"));
		if (sandboxConfigResult.isSuccess()) {
			String sandboxConfig = sandboxConfigResult.getPayload();

			copiedSaveProfile.setSaveName(getSessionName(sandboxConfig, destinationSavePath) + "_" + copyIndex);
			copiedSaveProfile.setProfileName(copiedSaveProfile.getProfileName() + "_" + copyIndex);

			//Change the name in our copied save's Sandbox_config file to match the save name.
			Result<Boolean> sandboxConfigNameChangeResult = changeSandboxConfigSessionName(sandboxConfig, copiedSaveProfile);

			//Change the name in our copied save's Sandbox file to match the save name.
			if (sandboxConfigNameChangeResult.isSuccess()) {
				Result<String> sandboxResult = sandboxService.getSandboxFromFile(new File(destinationSavePath + "\\Sandbox.sbc"));
				String sandbox = sandboxResult.getPayload();

				Result<Boolean> sandboxNameChangeResult = changeSandboxSessionName(sandbox, copiedSaveProfile);
				if(sandboxConfigNameChangeResult.isSuccess()) {
					result.addMessage("Successfully copied profile.", ResultType.SUCCESS);
					result.setPayload(copiedSaveProfile);
				} else {
					result.addMessage(sandboxNameChangeResult);
				}
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

	private Result<Boolean> changeSandboxConfigSessionName(String sandboxConfig, SaveProfile copiedSaveProfile) throws IOException {
		Result<Boolean> result = new Result<>();

		int[] sessionNameIndexPositions = getSessionNameIndexPositions(sandboxConfig);

		//Change the name in our copied save's Sandbox_config file to match the save name.
		if (sessionNameIndexPositions[0] != -1 && sessionNameIndexPositions[1] != -1) {
			result.addMessage(sandboxService.changeConfigSessionName(sandboxConfig, copiedSaveProfile, sessionNameIndexPositions));
		} else {
			result.addMessage("Save does not contain a SessionName tag,", ResultType.FAILED);
		}

		return result;
	}

	private Result<Boolean> changeSandboxSessionName(String sandbox, SaveProfile copiedSaveProfile) throws IOException {
		Result<Boolean> result = new Result<>();

		int[] sessionNameIndexPositions = getSessionNameIndexPositions(sandbox);

		if (sessionNameIndexPositions[0] != -1 && sessionNameIndexPositions[1] != -1) {
			result.addMessage(sandboxService.changeSandboxSessionName(sandbox, copiedSaveProfile, sessionNameIndexPositions));
		} else {
			result.addMessage("Save does not contain a SessionName tag,", ResultType.FAILED);
		}

		return result;
	}
}

