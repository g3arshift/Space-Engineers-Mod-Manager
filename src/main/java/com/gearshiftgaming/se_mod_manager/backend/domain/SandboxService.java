package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SandboxService {
	private final SandboxConfigRepository SANDBOX_CONFIG_FILE_REPOSITORY;

	public SandboxService(SandboxConfigRepository sandboxConfigRepository) {
		this.SANDBOX_CONFIG_FILE_REPOSITORY = sandboxConfigRepository;
	}

	//This will work to retrieve both a Sandbox_config file and a Sandbox file.
	public Result<String> getSandboxFromFile(File sandboxConfigFile) throws IOException {
		Result<String> result = new Result<>();
		if (!sandboxConfigFile.exists()) {
			result.addMessage("File does not exist.", ResultType.INVALID);
		} else if (FilenameUtils.getExtension(sandboxConfigFile.getName()).equals("sbc")) {
			result.addMessage(sandboxConfigFile.getName() + " selected.", ResultType.SUCCESS);
			result.setPayload(SANDBOX_CONFIG_FILE_REPOSITORY.getSandboxInfo(sandboxConfigFile));
		} else {
			result.addMessage("Incorrect file type selected. Please select a .sbc file.", ResultType.INVALID);
		}
		return result;
	}

	public Result<List<Mod>> getModListFromSandboxConfig(File sandboxConfigFile) {
		Result<String> sandboxConfigResult = new Result<>();
		try {
			sandboxConfigResult = getSandboxFromFile(sandboxConfigFile);
		} catch (IOException e) {
			sandboxConfigResult.addMessage(getStackTrace(e), ResultType.FAILED);
		}

		Result<List<Mod>> modListResult = new Result<>();
		if (sandboxConfigResult.isSuccess()) {
			List<Mod> modList = new ArrayList<>();
			String sandboxFileContent = sandboxConfigResult.getPayload();
			String modContent;
			if (StringUtils.contains(sandboxFileContent, "<Mods>")) {
				modContent = StringUtils.substringBetween(sandboxFileContent, "<Mods>", "</Mods>");
				String[] modConfig = modContent.split("</ModItem>");
				for (String s : modConfig) {
					s = s.trim();
					if (!s.isEmpty()) {
						String[] splitModConfig = s.split("\r?\n");
						String modId = StringUtils.substringBetween(splitModConfig[2], "<PublishedFileId>", "</PublishedFileId>");
						Mod mod;
						if (StringUtils.substringBetween(splitModConfig[3], "<PublishedServiceName>", "</PublishedServiceName>").equals("Steam")) {
							mod = new SteamMod(modId);
						} else {
							mod = new ModIoMod(modId);
						}
						modList.add(mod);
					}
				}
				modListResult.addMessage("Retrieved " + modList.size() + " mods.", ResultType.SUCCESS);
				modListResult.setPayload(modList);
			} else if (StringUtils.contains(sandboxFileContent, "<Mods />")) {
				modListResult.addMessage("There are no mods in this save!", ResultType.INVALID);
			} else {
				modListResult.addMessage("No valid mod section found.", ResultType.FAILED);
			}
		}

		return modListResult;
	}

	public Result<Void> saveSandboxConfigToFile(String savePath, String sandboxConfig) throws IOException {
		Result<Void> result = new Result<>();
		if (!FilenameUtils.getExtension(savePath).equals("sbc")) {
			result.addMessage("File extension ." + FilenameUtils.getExtension(savePath) + " not permitted. Changing to .sbc.", ResultType.SUCCESS);
			savePath = FilenameUtils.removeExtension(savePath);
			savePath = savePath + ".sbc";
		}

		if (validateFilePath(savePath)) {
			result.addMessage("Save path or name contains invalid characters.", ResultType.FAILED);
		} else {
			File sandboxFile = new File(savePath);
			SANDBOX_CONFIG_FILE_REPOSITORY.saveSandboxInfo(sandboxFile, sandboxConfig);
			result.addMessage("Successfully saved sandbox config.", ResultType.SUCCESS);
		}
		return result;
	}

	public Result<String> injectModsIntoSandboxConfig(File sandboxConfig, List<Mod> modList) throws IOException {
		Result<String> result = new Result<>();
		String sandboxFileContent = Files.readString(sandboxConfig.toPath());
		StringBuilder sandboxContent = new StringBuilder();

		String[] preModSandboxContent;
		String[] postModSandboxContent;

		if (StringUtils.contains(sandboxFileContent, "<Mods />")) {
			preModSandboxContent = StringUtils.substringBefore(sandboxFileContent, "<Mods />").split(System.lineSeparator());
			postModSandboxContent = StringUtils.substringAfter(sandboxFileContent, "<Mods />").split(System.lineSeparator());
		} else if (StringUtils.contains(sandboxFileContent, "<Mods>")) {
			preModSandboxContent = StringUtils.substringBefore(sandboxFileContent, "<Mods>").split(System.lineSeparator());
			postModSandboxContent = StringUtils.substringAfter(sandboxFileContent, "</Mods>").split(System.lineSeparator());
		} else {
			result.addMessage("No valid mod section found for " + sandboxConfig.getName() + ".", ResultType.FAILED);
			return result;
		}

		//Append the text in the Sandbox_config that comes before the mod section
		generateModifiedSandboxConfig(preModSandboxContent, sandboxContent);

		//Inject our new mod list
		if (!modList.isEmpty()) {
			sandboxContent.append("  <Mods>");
			sandboxContent.append(System.lineSeparator());
			for (Mod m : modList) {
				String sanitizedFriendlyName = m.getFriendlyName()
						.replace("&", "&amp;")
						.replace("\"", "&quot;")
						.replace("'", "&apos;")
						.replace("<", "&lt;")
						.replace(">", "&gt;");
				String modItem = "    <ModItem FriendlyName=\"%s\">%n" +
						"      <Name>%s.sbm</Name>%n" +
						"      <PublishedFileId>%s</PublishedFileId>%n" +
						"      <PublishedServiceName>%s</PublishedServiceName>%n" +
						"    </ModItem>%n";

				sandboxContent.append(String.format(modItem, sanitizedFriendlyName, m.getId(), m.getId(), m.getPublishedServiceName()));
			}

			//Append the text in the Sandbox_config that comes after the mod section
			sandboxContent.append("  </Mods>");
		} else {
			sandboxContent.append("  <Mods />");
		}


		if (!sandboxContent.toString().endsWith("\n")) {
			sandboxContent.append(System.lineSeparator());
		}

		generateModifiedSandboxConfig(postModSandboxContent, sandboxContent);

		result.setPayload(sandboxContent.toString());
		result.addMessage("Successfully injected mods into save.", ResultType.SUCCESS);
		return result;
	}

	public Result<Void> changeConfigSessionName(String sandbox, SaveProfile saveProfile, int[] sessionNameIndexPositions) throws IOException {
		StringBuilder renamedSandboxConfig = new StringBuilder(sandbox);
		renamedSandboxConfig.replace(sessionNameIndexPositions[0], sessionNameIndexPositions[1], saveProfile.getSaveName());

		return saveSandboxConfigToFile(saveProfile.getSavePath(), renamedSandboxConfig.toString());
	}

	public Result<Void> changeSandboxSessionName(String sandbox, SaveProfile saveProfile, int[] sessionNameIndexPositions) throws IOException {
		StringBuilder renamedSandboxConfig = new StringBuilder(sandbox);
		renamedSandboxConfig.replace(sessionNameIndexPositions[0], sessionNameIndexPositions[1], saveProfile.getSaveName());

		String savePath = saveProfile.getSavePath().substring(0, saveProfile.getSavePath().length() - 19) + "\\Sandbox.sbc";

		return saveSandboxConfigToFile(savePath, renamedSandboxConfig.toString());
	}

	private void generateModifiedSandboxConfig(String[] sandboxContent, StringBuilder modifiedSandboxContent) {
		for (int i = 0; i < sandboxContent.length; i++) {
			if (!sandboxContent[i].isBlank()) {
				modifiedSandboxContent.append(sandboxContent[i]);
				if (i + 1 < sandboxContent.length) modifiedSandboxContent.append(System.lineSeparator());
			}
		}
	}

	private boolean validateFilePath(String toExamine) {
		Pattern pattern = Pattern.compile("[#@*+%{}<>\\[\\]|\"^]");
		Matcher matcher = pattern.matcher(toExamine);
		return matcher.find();
	}
}
