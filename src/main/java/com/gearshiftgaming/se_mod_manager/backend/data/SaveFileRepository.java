package com.gearshiftgaming.se_mod_manager.backend.data;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

public class SaveFileRepository implements SaveRepository{
    //TODO: Do we need to implement locks?
    @Override
    public void copySave(String savePathSource, String savePathDestination) {
        try{
            //Copy everything except for the Backup folder
            FileFilter fileFilter = FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter("Backup"));
            FileUtils.copyDirectory(new File(savePathSource), new File(savePathDestination), fileFilter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
