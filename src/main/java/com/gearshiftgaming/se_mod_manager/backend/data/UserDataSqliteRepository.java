package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager;
import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.core.SQLiteDatabase;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class UserDataSqliteRepository implements UserDataRepository{

    //TODO: We need to make sure we enable foreign keys every single time we connect.
    //TODO: Indexes! FK's aren't default indexed.
    //https://medium.com/destinationaarhus-techblog/integrate-liquibase-with-the-pipeline-using-gradle-2ad24f691009

    private final Jdbi SQLITE_DB;
    private final String databasePath;
    private final String changelogPath;
    public UserDataSqliteRepository (String databasePath, String changelogPath) {
        SQLITE_DB = Jdbi.create("jdbc:sqlite:" + databasePath);
        this.databasePath = databasePath;
        this.changelogPath = changelogPath;
    }
    @Override
    public Result<UserConfiguration> loadUserData() {
        Result<UserConfiguration> userConfigurationResult = new Result<>();
        if(Files.notExists(Path.of(databasePath))) {
            userConfigurationResult.addMessage("User data was not found. Defaulting to new user configuration.", ResultType.FAILED);
            userConfigurationResult.setPayload(new UserConfiguration());

        } else {
            //TODO: Need to fillout the query.
        }
        return userConfigurationResult;
    }

    @Override
    public boolean saveUserData(UserConfiguration userConfiguration) {
        if(Files.notExists(Path.of(databasePath))) {
            Database database = new SQLiteDatabase();
            Liquibase liquibase = new Liquibase(changelogPath, new ClassLoaderResourceAccessor(), database);
            try {
                liquibase.update();
                SQLITE_DB.useHandle(handle -> handle.execute("PRAGMA journal_mode=WAL;"));
                //TODO: Write the user config to the DB.
                return true;
            } catch (LiquibaseException e) {
                throw new RuntimeException(e);
            }
        } else {
            //TODO: Need to fillout the query.
        }
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
