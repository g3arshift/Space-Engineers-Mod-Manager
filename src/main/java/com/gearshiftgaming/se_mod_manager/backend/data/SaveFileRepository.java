package com.gearshiftgaming.se_mod_manager.backend.data;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
//TODO: Add a lock so that only this application can work on the files. Allow it to access as much as it wants, but prevent outside stuff from writing to it.
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
