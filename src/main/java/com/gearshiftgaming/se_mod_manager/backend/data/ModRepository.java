package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;

import java.io.File;
import java.util.List;

public interface ModRepository {

    Result<List<Mod>> getModList(String modFilePath);

    List<Mod> getModListModIds(File modListFile);
}
