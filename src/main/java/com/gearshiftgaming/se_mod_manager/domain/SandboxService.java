package com.gearshiftgaming.se_mod_manager.domain;

import com.gearshiftgaming.se_mod_manager.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.ResultType;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class SandboxService {
    private final SandboxConfigRepository sandboxConfigFileRepository;

    public SandboxService(SandboxConfigFileRepository sandboxConfigRepository) {
        this.sandboxConfigFileRepository = sandboxConfigRepository;
    }

    public Result<File> getSandboxConfigFromFile(JFileChooser fc) {
        File sandboxConfig = fc.getSelectedFile();
        return sandboxConfigFileRepository.getAll(sandboxConfig);
    }

    public Result<Boolean> addModsToSandboxConfig(File sandboxConfig, List<Mod> modList) throws IOException {
        String modifiedSandboxConfig = injectModsIntoSandboxConfig(sandboxConfig, modList);
        //TODO: Implement saving properly
        //Result<Boolean> result = sandboxConfigFileRepository.saveSandboxConfig(sandboxConfig, modifiedSandboxConfig);


        //if()
        //TODO: Add a check for if the path the user wants to save the file to already has a file, and ask them if they want to overwrite
        //TODO: Inject the mods
        //TODO: Save the mods, and add the result here to the log, else log a success
        //TODO: Save the mods, and add the result here to the log, else log a success
        return result;
    }

    private String injectModsIntoSandboxConfig(File sandboxConfig, List<Mod> modList) throws IOException {
        String sandboxFileContent = Files.readString(sandboxConfig.toPath());
        StringBuilder sandboxContent = new StringBuilder();
        String[] preModSandboxContent = StringUtils.substringBefore(sandboxFileContent, "<Mods>").split(System.lineSeparator());
        String[] postModSandboxContent = StringUtils.substringAfter(sandboxFileContent, "</Mods>").split(System.lineSeparator());

        //Append the text in the Sandbox_config that comes before the mod section
        for (String s : preModSandboxContent) {
            sandboxContent.append(s);
            sandboxContent.append(System.lineSeparator());
        }

        //Remove extra newline
        if (!sandboxContent.isEmpty()) sandboxContent.setLength(sandboxContent.length() - 1);

        //Inject our new modlist
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
        for (String s : postModSandboxContent) {
            sandboxContent.append(s);
            sandboxContent.append(System.lineSeparator());
        }

        //Remove extra newline
        if (!sandboxContent.isEmpty()) sandboxContent.setLength(sandboxContent.length() - 1);
        return sandboxContent.toString();
    }
}
