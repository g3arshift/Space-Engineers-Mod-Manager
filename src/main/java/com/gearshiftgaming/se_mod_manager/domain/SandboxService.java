package com.gearshiftgaming.se_mod_manager.domain;

import com.gearshiftgaming.se_mod_manager.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class SandboxService {
    private final SandboxConfigRepository sandboxConfigFileRepository;

    public SandboxService(SandboxConfigFileRepository sandboxConfigRepository) {
        this.sandboxConfigFileRepository = sandboxConfigRepository;
    }

    public Result<File> getSandboxConfigFromFile(String sandboxConfigPath) {
        return sandboxConfigFileRepository.getSandboxConfig(sandboxConfigPath);
    }

    public Result<Boolean> addModsToSandboxConfigFile(File sandboxConfig, String savePath, List<Mod> modList) throws IOException {
        String modifiedSandboxConfig = injectModsIntoSandboxConfig(sandboxConfig, modList);
        return sandboxConfigFileRepository.saveSandboxConfig(savePath, modifiedSandboxConfig);
    }

    private String injectModsIntoSandboxConfig(File sandboxConfig, List<Mod> modList) throws IOException {
        String sandboxFileContent = Files.readString(sandboxConfig.toPath());
        StringBuilder sandboxContent = new StringBuilder();

        String[] preModSandboxContent;
        String[] postModSandboxContent;

        if (StringUtils.contains(sandboxFileContent, "<Mods />")) {
            preModSandboxContent = StringUtils.substringBefore(sandboxFileContent, "<Mods />").split(System.lineSeparator());
            postModSandboxContent = StringUtils.substringAfter(sandboxFileContent, "<Mods />").split(System.lineSeparator());
        } else {
            preModSandboxContent = StringUtils.substringBefore(sandboxFileContent, "<Mods>").split(System.lineSeparator());
            postModSandboxContent = StringUtils.substringAfter(sandboxFileContent, "</Mods>").split(System.lineSeparator());
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

            sandboxContent.append(String.format(modItem, m.getFriendlyName(), m.getModId(), m.getModId(), m.getPublishedServiceName()));
        }

        //Append the text in the Sandbox_config that comes after the mod section
        sandboxContent.append("  </Mods>");
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
