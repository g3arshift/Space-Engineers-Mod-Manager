package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.SpaceEngineersModManager;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.core.SQLiteDatabase;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.transaction.TransactionException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class UserDataSqliteRepository implements UserDataRepository {

    //TODO: We need to make sure we enable foreign keys every single time we connect.
    //TODO: Indexes! FK's aren't default indexed.
    //https://medium.com/destinationaarhus-techblog/integrate-liquibase-with-the-pipeline-using-gradle-2ad24f691009

    private final Jdbi SQLITE_DB;
    private final String databasePath;
    private final String changelogPath;

    public UserDataSqliteRepository(String databasePath, String changelogPath) {
        SQLITE_DB = Jdbi.create("jdbc:sqlite:" + databasePath);
        this.databasePath = databasePath;
        this.changelogPath = changelogPath;
    }

    @Override
    public Result<UserConfiguration> loadUserData() {
        Result<UserConfiguration> userConfigurationResult = new Result<>();
        if (Files.notExists(Path.of(databasePath))) {
            userConfigurationResult.addMessage("User data was not found. Defaulting to new user configuration.", ResultType.FAILED);
            userConfigurationResult.setPayload(new UserConfiguration());
        } else {
            //TODO: Need to fillout the query.
        }
        return userConfigurationResult;
    }

    @Override
    public Result<Void> saveUserData(UserConfiguration userConfiguration) {
        Result<Void> saveResult = new Result<>();
        if (Files.notExists(Path.of(databasePath))) {
            Database database = new SQLiteDatabase();
            Liquibase liquibase = new Liquibase(changelogPath, new ClassLoaderResourceAccessor(), database);
            try {
                liquibase.update();
                SQLITE_DB.useHandle(handle -> handle.execute("PRAGMA journal_mode=WAL;"));
            } catch (LiquibaseException e) {
                throw new RuntimeException(e);
            }
        }

        //TODO: Write the user config to the DB.
        // First insert into ID 1 if exists, on duplicate key update. https://sqlite.org/lang_upsert.html. We actually want to do this for basically everything
        // Second create all the parent objects. That should also be on duplicate key update. Everything should. first table should be mod profiles
        // Third insert into user_configuration_mod_list_profile
        // Fourth insert save profiles
        // Fifth insert into user_configuration_save_profile
        // Sixth go back through our modprofiles. Insert an appropriate entry into the mod table for each mod, as well as their sub-tables. Like steam mods, categories, mod_list_conflicts, etc
        //      After this. then also an entry into the modlist profile.
        // Seventh do the same for save profiles and their bridge table.
        try {
            SQLITE_DB.useTransaction(handle -> {
                //Upsert our user_configuration row. We should only ever have one, so we set the row to the first one.
                handle.createUpdate("""
                                INSERT INTO user_configuration (
                                        id,
                                        user_theme,
                                        last_modified_save_profile_id,
                                        last_active_mod_profile_id,
                                        last_active_save_profile_id,
                                        run_first_time_setup)
                                    VALUES (
                                        1,
                                        :userTheme,
                                        :lastModifiedSaveProfileId,
                                        :lastActiveModProfileId,
                                        :lastActiveSaveProfileId,
                                        :runFirstTimeSetup)
                                    ON CONFLICT(id) DO UPDATE SET
                                        user_theme = excluded.user_theme,
                                        last_modified_save_profile_id = excluded.last_modified_save_profile_id,
                                        last_active_mod_profile_id = excluded.last_active_mod_profile_id,
                                        last_active_save_profile_id = excluded.last_active_save_profile_id,
                                        run_first_time_setup = excluded.run_first_time_setup""")
                        .bindBean(userConfiguration)
                        .execute();
                saveResult.addMessage("Successfully updated user_configuration table.", ResultType.SUCCESS);

                //Upsert our save_profile table, but only update information that's changed.
                PreparedBatch saveProfilesBatch = handle.prepareBatch("""
                                INSERT INTO save_profile (
                                        save_profile_id,
                                        profile_name,
                                        save_name,
                                        save_path,
                                        last_used_mod_list_profile_id,
                                        last_save_status,
                                        last_saved,
                                        save_exists,
                                        space_engineers_version)
                                     VALUES (
                                        :id,
                                        :profileName,
                                        :saveName,
                                        :savePath,
                                        :lastUsedModProfileId,
                                        :lastSaveStatus,
                                        :lastSaved,
                                        :saveExists)
                                     ON CONFLICT (save_profile_id) DO UPDATE SET
                                        profile_name = CASE WHEN save_profile.profile_name != excluded.profile_name THEN excluded.profile_name ELSE save_profile.profile_name END,
                                        last_used_mod_list_profile_id = CASE WHEN save_profile.last_used_mod_list_profile_id  != excluded.last_used_mod_list_profile_id THEN excluded.last_used_mod_list_profile_id  ELSE save_profile.last_used_mod_list_profile_id  END,
                                        last_save_status = CASE WHEN save_profile.last_save_status != excluded.last_save_status THEN excluded.last_save_status ELSE save_profile.last_save_status END,
                                        last_saved = CASE WHEN save_profile.last_saved != excluded.last_saved THEN excluded.last_saved ELSE save_profile.last_saved END,
                                        save_exists = CASE WHEN save_profile.save_exists != excluded.save_exists THEN excluded.save_exists ELSE save_profile.save_exists END""");
                //Update our bridge table to connect save profiles to the user config. Ignore duplicates.
                PreparedBatch saveProfilesUserConfigurationBatch = handle.prepareBatch("""
                                     INSERT OR IGNORE INTO user_configuration_save_profile (user_configuration_id, save_profile_id)
                                     VALUES (1, :saveProfileId)""");

                //Actually attach a value to our variables in our batch.
                for(SaveProfile saveProfile : userConfiguration.getSaveProfiles()) {
                    saveProfilesBatch.bind("id", saveProfile.getID())
                            .bind("profileName", saveProfile.getProfileName())
                            .bind("saveName", saveProfile.getSaveName())
                            .bind("savePath", saveProfile.getSavePath())
                            .bind("lastUsedModProfileId", saveProfile.getLastUsedModProfileId())
                            .bind("lastSaveStatus", saveProfile.getLastSaveStatus())
                            .bind("lastSaved", saveProfile.getLastSaved())
                            .bind("saveExists", saveProfile.isSaveExists())
                            .add();

                    saveProfilesUserConfigurationBatch.bind("saveProfileId", saveProfile.getID());
                }
                int[] saveProfileRowsUpdate = saveProfilesBatch.execute();
                saveResult.addMessage(saveProfileRowsUpdate.length + " save profiles saved to database. Expected " + userConfiguration.getSaveProfiles().size(), ResultType.SUCCESS);
                int[] saveProfilesUserConfigurationRowsUpdated = saveProfilesUserConfigurationBatch.execute();
                saveResult.addMessage(saveProfilesUserConfigurationRowsUpdated.length + " save profiles saved to user config bridge table. Expected " + userConfiguration.getSaveProfiles().size(), ResultType.SUCCESS);

                //Delete the removed save profiles
                int numSaveProfilesDeleted = handle.createUpdate("DELETE FROM save_profile WHERE save_profile_id NOT IN (<ids>)")
                        .bindList("ids", userConfiguration.getSaveProfiles())
                        .execute();
                saveResult.addMessage(numSaveProfilesDeleted + " save profiles deleted.", ResultType.SUCCESS);
                //TODO: Follow this same process for mod_list_profile and mods and their bridge tables and all that shit
                //TODO: Oh god the testing. We need to really test the conditionals for updates in particular
            });
        } catch (TransactionException e) {
            saveResult.addMessage(e.toString(), ResultType.FAILED);
            saveResult.addMessage("Failed to save user data.", ResultType.FAILED);
        }
        return saveResult;
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
