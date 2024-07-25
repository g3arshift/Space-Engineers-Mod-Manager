package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class SandboxService {
    private final SandboxConfigRepository sandboxConfigFileRepository;
    Logger logger;

    public SandboxService(SandboxConfigRepository sandboxConfigRepository, Logger logger) {
        this.sandboxConfigFileRepository = sandboxConfigRepository;
        this.logger = logger;
    }

    public Result<File> getSandboxConfigFromFile(String sandboxConfigPath) {
        return sandboxConfigFileRepository.getSandboxConfig(sandboxConfigPath);
    }

    public String addModsToSandboxConfigFile(File sandboxConfig, List<Mod> modList) throws IOException {
        return injectModsIntoSandboxConfig(sandboxConfig, modList);
    }

    public Result<Boolean> saveSandboxConfig(String savePath, String sandboxConfig) throws IOException {
        return sandboxConfigFileRepository.saveSandboxConfig(savePath, sandboxConfig);
    }

    private String injectModsIntoSandboxConfig(File sandboxConfig, List<Mod> modList) throws IOException {
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
            logger.error("No valid mod section found for " + sandboxConfig.getName() + ".");
            return "INVALID_SANDBOX_CONFIG";
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

        return sandboxContent.toString();
    }

    private void generateModifiedSandboxConfig(String[] sandboxContent, StringBuilder modifiedSandboxContent) {
        for (int i = 0; i < sandboxContent.length; i++) {
            if (!sandboxContent[i].isBlank()) {
                modifiedSandboxContent.append(sandboxContent[i]);
                if (i + 1 < sandboxContent.length) modifiedSandboxContent.append(System.lineSeparator());
            }
        }
    }
}
