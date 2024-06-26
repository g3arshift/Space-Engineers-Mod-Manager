package com.gearshiftgaming.se_mod_manager.ui;

import com.gearshiftgaming.se_mod_manager.domain.ModService;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.FileChooserAndOption;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.ResultType;

import javax.swing.*;
import java.util.List;

public class ModController {

    private List<Mod> modList;
    private ModView modView;

    ModService modService;

    private FileChooserAndOption fileChooserAndOption;

    private final String DESKTOP_PATH;
    private final String APP_DATA_PATH;

    public ModController(List<Mod> modList, ModView modView, ModService modService, FileChooserAndOption fileChooserAndOption, String desktopPath, String appDataPath) {
        this.modList = modList;
        this.modView = modView;
        this.modService= modService;
        this.fileChooserAndOption = fileChooserAndOption;
        this.DESKTOP_PATH = desktopPath;
        this.APP_DATA_PATH = appDataPath;
    }

    public void injectModList() {

        Result modFileResult = new Result(ResultType.NOT_INITIALIZED);

        //TODO: get this shit passed into the view properly
        do {
            fileChooserAndOption = modView.getModListFile(DESKTOP_PATH);
            if (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION) {
                modFileResult = modService.getModListFile(fileChooserAndOption.getFc());

                if (!modFileResult.isSuccess()) {
                    modView.displayResult(modFileResult);
                }
            } else
                modView.displayCancellation();
        } while (fileChooserAndOption.getOption() == JFileChooser.APPROVE_OPTION && !modFileResult.isSuccess());

        //TODO: Get mod list
        //TODO: Select Sandbox_config.sbc to inject modlist into
        //TODO: Generate new Sandbox_config.sbc based on existing file
        //TODO: Save file. Ask user if they want to overwrite the existing file, or save to a new location
    }
}
