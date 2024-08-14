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

	public Result<SaveProfile> copySaveFiles(String sourceSavePath) throws IOException {
		String destinationSavePath;
		boolean pathHasDuplicate;
		int copyIndex = 1;
		do {
			destinationSavePath = sourceSavePath + "_" + copyIndex;
			pathHasDuplicate = Files.exists(Path.of(destinationSavePath));
			copyIndex++;
		} while (pathHasDuplicate);

		saveRepository.copySave(sourceSavePath, destinationSavePath);

		Result<SaveProfile> result = new Result<>();
		SaveProfile copiedSaveProfile = new SaveProfile();
		if (Files.exists(Path.of(destinationSavePath))) {
			result.addMessage("Save directory successfully copied.", ResultType.SUCCESS);
			copiedSaveProfile.setSavePath(destinationSavePath + "\\Sandbox_config.sbc");
		} else {
			result.addMessage("Failed to copy save directory.", ResultType.FAILED);
		}

		Result<String> sandboxConfigResult = sandboxService.getSandboxConfigFromFile(new File(destinationSavePath + "\\Sandbox_config.sbc"));
		if (result.isSuccess()) {
			String sandboxConfig = sandboxConfigResult.getPayload();
			copiedSaveProfile.setSaveName(getSessionName(sandboxConfig, destinationSavePath) + "_" + copyIndex);
		} else {
			result.addMessage(sandboxConfigResult.getMessages().getLast(), sandboxConfigResult.getType());
		}

        copiedSaveProfile.setProfileName(copiedSaveProfile.getProfileName() + "_" + copyIndex);
		//TODO: Modify sandbox.sbc and sandbox_config.sbc with sandboxService.
		return result;
	}

    //TODO: Implement
	private Result<String> renameSave(SaveProfile saveProfile) {
		return null;
	}

	public String getSessionName(String sandboxConfig, String saveDestinationPath) {
		final int SESSION_NAME_LENGTH = 13;
		String saveName;

		int saveNameStartIndex = StringUtils.indexOf(sandboxConfig, "<SessionName>");

		if (saveNameStartIndex != -1) {
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

			saveName = sandboxConfig.substring(saveNameStartIndex, saveNameEndIndex);
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
}

