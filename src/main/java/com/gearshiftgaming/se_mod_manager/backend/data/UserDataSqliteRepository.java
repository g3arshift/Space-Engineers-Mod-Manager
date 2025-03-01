package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import org.jdbi.v3.core.Jdbi;

import java.io.File;

public class UserDataSqliteRepository implements UserDataRepository{

    //TODO: We need to make sure we enable foreign keys every single time we connect.
    //TODO: Indexes! FK's aren't default indexed.
    //TODO: Enable WAL mode, it'll run like crap otherwise.
    //TODO: We need to enforce type checking. Use Strict tables. https://sqlite.org/stricttables.html
    //https://medium.com/destinationaarhus-techblog/integrate-liquibase-with-the-pipeline-using-gradle-2ad24f691009
    private final Jdbi SQLITE_DB;
    public UserDataSqliteRepository (String sqliteLocation) {
        SQLITE_DB = Jdbi.create("jdbc:sqlite:" + sqliteLocation);
    }
    @Override
    public Result<UserConfiguration> loadUserData() {
        return null;
    }

    @Override
    public boolean saveUserData(UserConfiguration userConfiguration) {
        return false;
    }

    @Override
    public Result<Void> exportModlist(ModListProfile modListProfile, File modlistLocation) {
        return null;
    }

    @Override
    public Result<ModListProfile> importModlist(File modlistLocation) {
        return null;
    }

    //TODO: Reset and re-create the DB.
    @Override
    public Result<Void> resetUserConfiguration() {
        return null;
    }
}
