package com.gearshiftgaming.se_mod_manager.domain;

import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.ResultType;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.io.File;

public class ModService {

    public Result getModListFile(JFileChooser fc) {

        Result modFileResult = new Result();

        File modListFile;

        modListFile = fc.getSelectedFile();
        if (!modListFile.exists()) {
            modFileResult.addMessage("File does not exist.", ResultType.INVALID);
        } else if (FilenameUtils.getExtension(modListFile.getName()).equals("txt") || FilenameUtils.getExtension(modListFile.getName()).equals("doc")) {
            modFileResult.addMessage(fc.getSelectedFile().getName() + " selected.", ResultType.SUCCESS);
            modFileResult.setPayload(modListFile);
        } else {
            modFileResult.addMessage("Incorrect file type selected. Please select a .txt or .doc file.", ResultType.INVALID);
        }
        return modFileResult;
    }
}
