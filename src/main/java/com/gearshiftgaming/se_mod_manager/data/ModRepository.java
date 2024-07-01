package com.gearshiftgaming.se_mod_manager.data;

import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;

import java.io.File;
import java.util.List;

public interface ModRepository {

    Result<List<Mod>> getModList(String modFilePath);

    List<Mod> getModListModIds(File modListFile);
}
