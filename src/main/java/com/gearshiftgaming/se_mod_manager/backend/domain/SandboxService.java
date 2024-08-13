package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SandboxService {
    private final SandboxConfigRepository sandboxConfigFileRepository;

    public SandboxService(SandboxConfigRepository sandboxConfigRepository) {
        this.sandboxConfigFileRepository = sandboxConfigRepository;
    }

    public Result<String> getSandboxConfigFromFile(File sandboxConfigFile) throws IOException {
        Result<String> result = new Result<>();
        if (!sandboxConfigFile.exists()) {
            result.addMessage("File does not exist.", ResultType.INVALID);
        } else if (FilenameUtils.getExtension(sandboxConfigFile.getName()).equals("sbc")) {
            result.addMessage(sandboxConfigFile.getName() + " selected.", ResultType.SUCCESS);
            result.setPayload(sandboxConfigFileRepository.getSandboxConfig(sandboxConfigFile));
        } else {
            result.addMessage("Incorrect file type selected. Please select a .sbc file.", ResultType.INVALID);
        }
        return result;
    }

    public Result<Boolean> saveSandboxConfig(String savePath, String sandboxConfig) throws IOException {
        Result<Boolean> result = new Result<>();
        if (!FilenameUtils.getExtension(savePath).equals("sbc")) {
            result.addMessage("File extension ." + FilenameUtils.getExtension(savePath) + " not permitted. Changing to .sbc.", ResultType.SUCCESS);
            savePath = FilenameUtils.removeExtension(savePath);
            savePath = savePath + ".sbc";
        }

        if (validateFilePath(savePath)) {
            result.addMessage("Save path or name contains invalid characters.", ResultType.FAILED);
            result.setPayload(false);
        } else {
            File sandboxFile = new File(savePath);
            sandboxConfigFileRepository.saveSandboxConfig(sandboxFile, sandboxConfig);
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
        } else if(StringUtils.contains(sandboxFileContent, "<Mods>")){
            preModSandboxContent = StringUtils.substringBefore(sandboxFileContent, "<Mods>").split(System.lineSeparator());
            postModSandboxContent = StringUtils.substringAfter(sandboxFileContent, "</Mods>").split(System.lineSeparator());
        } else{
            result.addMessage("No valid mod section found for " + sandboxConfig.getName() + ".", ResultType.FAILED);
            return  result;
        }

        //Append the text in the Sandbox_config that comes before the mod section
        generateModifiedSandboxConfig(preModSandboxContent, sandboxContent);

        //Inject our new mod list
        sandboxContent.append("  <Mods>");
        sandboxContent.append(System.lineSeparator());
        for (Mod m : modList) {
            String modItem = "    <ModItem FriendlyName=\"%s\">%n" +
                    "      <Name>%s.sbm</Name>%n" +
                    "      <PublishedFileId>%s</PublishedFileId>%n" +
                    "      <PublishedServiceName>%s</PublishedServiceName>%n" +
                    "    </ModItem>%n";

            sandboxContent.append(String.format(modItem, m.getFriendlyName(), m.getId(), m.getId(), m.getPublishedServiceName()));
        }

        //Append the text in the Sandbox_config that comes after the mod section
        sandboxContent.append("  </Mods>");

        //TODO: It's having the issue again, but perhaps only when not saving back to OG file? Needs testing.
        if (!sandboxContent.toString().endsWith("\n")) {
            sandboxContent.append(System.lineSeparator());
        }

        generateModifiedSandboxConfig(postModSandboxContent, sandboxContent);

        result.setPayload(sandboxContent.toString());
        result.addMessage("Successfully injected mods into save.", ResultType.SUCCESS);
        return result;
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
