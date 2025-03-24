package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.mappers.ModListProfileMapper;
import com.gearshiftgaming.se_mod_manager.backend.data.mappers.ModMapper;
import com.gearshiftgaming.se_mod_manager.backend.data.mappers.SaveProfileMapper;
import com.gearshiftgaming.se_mod_manager.backend.data.mappers.UserConfigurationMapper;
import com.gearshiftgaming.se_mod_manager.backend.data.utility.StringCryptpressor;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.transaction.TransactionException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

//TODO: look into parallelizing this.
//TODO: Our logging on numbers of edited rows has some problems, primarily because it returns a list of rows and if they're edited or not. I think. 1 for edited for that row (batch), 0 for not.
public class UserDataSqliteRepository extends ModListProfileJaxbSerializer implements UserDataRepository {

    //TODO: We need to re-engineer mod scraping code to check if a mod already exists in the mod table, and if it does, update it. Maybe? What about updating mods? Performance?
    //TODO: We need to modify the entire program in later versions so that we don't store everything in memory, only a list of the current mod/save profiles and load the information from the
    // DB on the fly.

    private final Jdbi SQLITE_DB;
    private final String databasePath;

    public UserDataSqliteRepository(String databasePath) {
        this.databasePath = databasePath;
        SQLITE_DB = Jdbi.create("jdbc:sqlite:" + databasePath);

        //If our DB doesn't exist, we create a new one. We want to enable WAL mode for performance, and setup a trigger for the mod_list_profile_mod table to cleanup unused mods.
        Path databaseLocation = Path.of(databasePath);
        if (Files.notExists(databaseLocation)) {
            try {
                if (Files.notExists(databaseLocation.getParent())) {
                    Files.createDirectories(databaseLocation.getParent());
                }
                createDatabase();
                saveUserData(new UserConfiguration());
            } catch (IOException e) {
                try {
                    Files.delete(databaseLocation.getParent());
                } catch (IOException ex) {
                    System.out.println("Failed to delete corrupt database. How?");
                    throw new RuntimeException(ex);
                }
            }
        }
        //We have to enable foreign keys on each connection for SQLite
        SQLITE_DB.useHandle(handle -> handle.execute("PRAGMA foreign_key=ON;"));
    }

    @Override
    public Result<UserConfiguration> loadUserData() {
        Result<UserConfiguration> userConfigurationResult = new Result<>();

        Result<UserConfiguration> userProfileLoadResult = loadUserProfile();
        if (!userProfileLoadResult.isSuccess()) {
            return userProfileLoadResult;
        }

        UserConfiguration userConfiguration = userProfileLoadResult.getPayload();

        Result<List<SaveProfile>> saveProfileLoadResult = loadSaveProfiles();
        if (!saveProfileLoadResult.isSuccess()) {
            for (String message : saveProfileLoadResult.getMESSAGES()) {
                userConfigurationResult.addMessage(message, ResultType.FAILED);
            }
            return userConfigurationResult;
        }

        userConfiguration.setSaveProfiles(saveProfileLoadResult.getPayload());

        Result<List<ModListProfile>> modListProfileLoadResult = loadModListProfiles();
        if (!modListProfileLoadResult.isSuccess()) {
            for (String message : modListProfileLoadResult.getMESSAGES()) {
                userConfigurationResult.addMessage(message, ResultType.FAILED);
            }
            return userConfigurationResult;
        }

        userConfiguration.setModListProfiles(modListProfileLoadResult.getPayload());
        userConfigurationResult.addMessage("Successfully loaded all user data.", ResultType.SUCCESS);
        userConfigurationResult.setPayload(userConfiguration);

        return userConfigurationResult;
    }

    private Result<UserConfiguration> loadUserProfile() {
        Result<UserConfiguration> userProfileLoadResult = new Result<>();
        UserConfiguration userConfiguration;
        try (Handle handle = SQLITE_DB.open()) {
            userConfiguration = handle.createQuery("SELECT * from user_configuration where id = 1;")
                    .map(new UserConfigurationMapper())
                    .first();
            userProfileLoadResult.addMessage("Loaded user configuration from database.", ResultType.SUCCESS);
        }
        if (userConfiguration == null) {
            userProfileLoadResult.addMessage("Failed to load user configuration.", ResultType.FAILED);
        } else {
            userProfileLoadResult.setPayload(userConfiguration);
        }
        return userProfileLoadResult;
    }

    private Result<List<SaveProfile>> loadSaveProfiles() {
        Result<List<SaveProfile>> saveProfileLoadResult = new Result<>();
        List<SaveProfile> saveProfiles;
        try (Handle handle = SQLITE_DB.open()) {
            saveProfiles = handle.createQuery("SELECT * from save_profile;")
                    .map(new SaveProfileMapper())
                    .list();
            saveProfileLoadResult.addMessage("Loaded save profiles from database.", ResultType.SUCCESS);
        }
        if (saveProfiles == null) {
            saveProfileLoadResult.addMessage("Failed to load user save profiles.", ResultType.FAILED);
        } else {
            saveProfileLoadResult.setPayload(saveProfiles);
        }
        return saveProfileLoadResult;
    }

    private Result<List<ModListProfile>> loadModListProfiles() {
        Result<List<ModListProfile>> modListProfileLoadResult = new Result<>();
        try (Handle handle = SQLITE_DB.open()) {
            //Grab all our mod list profiles.
            List<ModListProfile> modListProfiles = handle.createQuery("SELECT * from mod_list_profile;")
                    .map(new ModListProfileMapper())
                    .list();

            //If we don't get our profiles then anything else is a bit moot.
            if (modListProfiles == null || modListProfiles.isEmpty()) {
                modListProfileLoadResult.addMessage("Failed to load mod list profiles.", ResultType.FAILED);
                return modListProfileLoadResult;
            }

            modListProfileLoadResult.addMessage("Successfully grabbed initial mod list profile information. Fetching mods...", ResultType.SUCCESS);

            //TODO: Verify this doesn't introduce a thousand bugs through each list technically using the same list of mods? Probably actually just do an add here.
            //TODO: fffffuuuuuuuck. It is. It's shallow copies. We really need to reengineer the damn code to use the DB properly, not this "load and save the world" shit.
            //Get all information for mods from the database. This will get us a map for all mod information where the modID is the key and the mod object the value.
            Map<String, Mod> modMap = handle.createQuery("""
                            WITH ModDetails AS (
                                SELECT m.*,
                                    modio.last_updated_year,
                                    modio.last_updated_month_day,
                                    modio.last_updated_hour,
                                    steam.last_updated AS steam_mod_last_updated
                                FROM mod m
                                    LEFT JOIN modio_mod modio ON m.mod_id = modio.mod_id
                                    LEFT JOIN steam_mod steam ON m.mod_id = steam.mod_id)
                            SELECT mod_details.mod_id,
                                mod_details.friendly_name,
                                mod_details.published_service_name,
                                mod_details.description,
                                mod_details.last_updated_year,
                                mod_details.last_updated_month_day,
                                mod_details.last_updated_hour,
                                mod_details.steam_mod_last_updated,
                                GROUP_CONCAT(DISTINCT mc.category) AS categories
                            FROM ModDetails mod_details
                                LEFT JOIN main.mod_category mc ON mod_details.mod_id = mc.mod_id
                            GROUP BY mod_details.mod_id;""")
                    .map(new ModMapper())
                    .list()
                    .stream()
                    .collect(Collectors.toMap(Mod::getId, mod -> mod));

            modListProfileLoadResult.addMessage("Successfully grabbed all mod data. Fetching modified paths...", ResultType.SUCCESS);

            //If there's no mods on any mod list we can just skip the next steps since they're all mod assembly steps.
            if (modMap.isEmpty()) {
                modListProfileLoadResult.addMessage("No mods in the database!", ResultType.SUCCESS);
                modListProfileLoadResult.setPayload(modListProfiles);
                return modListProfileLoadResult;
            }

            //Finish constructing our mod objects with their modified paths.
            Map<String, List<String>> modifiedPathsMap = handle.createQuery("""
                            SELECT * from mod_modified_path;""")
                    .reduceRows(new LinkedHashMap<>(), (map, row) -> {
                        String modId = row.getColumn("mod_id", String.class);
                        String modifiedPath = row.getColumn("modified_path", String.class);
                        map.computeIfAbsent(modId, k -> new ArrayList<>()).add(modifiedPath);
                        return map;
                    });

            if (modifiedPathsMap.isEmpty()) {
                modListProfileLoadResult.addMessage("No modified paths found for mods!", ResultType.SUCCESS);
            } else {
                //Actually add the values of the modified paths to each mod
                for (Map.Entry<String, Mod> entry : modMap.entrySet()) {
                    entry.getValue().setModifiedPaths(modifiedPathsMap.getOrDefault(entry.getKey(),
                            entry.getValue().getModifiedPaths()));
                }
            }

            //Grab the list of mod ID's we expect to have in each mod list profile, then add them to the profile from our earlier hashtable.
            for (ModListProfile modListProfile : modListProfiles) {
                List<Triple<String, Integer, Boolean>> modIds = handle.createQuery("""
                                SELECT mod_id, load_priority, active
                                FROM mod_list_profile_mod
                                WHERE mod_list_profile_id = :modListProfileId
                                ORDER BY load_priority""")
                        .bind("modListProfileId", modListProfile.getID())
                        .map((rs, ctx) -> Triple.of(rs.getString("mod_id"), rs.getInt("load_priority"), rs.getInt("active") >= 1))
                        .list();
                //TODO: Need active field
                if (!modIds.isEmpty()) {
                    List<Mod> sortedMods = modIds.stream()
                            .map(entry -> {
                                Mod mod = modMap.get(entry.getLeft());
                                if (mod != null) {
                                    mod.setLoadPriority(entry.getMiddle());
                                    mod.setActive(entry.getRight());
                                }
                                return mod;
                            })
                            .filter(Objects::nonNull)
                            .toList();
                    modListProfile.setModList(sortedMods);
                    modListProfile.generateConflictTable();
                    modListProfileLoadResult.addMessage("Successfully loaded mods for Mod List Profile ID: " + modListProfile.getID(), ResultType.SUCCESS);
                }
            }
            modListProfileLoadResult.setPayload(modListProfiles);
        } catch (Exception e) {
            modListProfileLoadResult.addMessage(e.toString(), ResultType.FAILED);
            modListProfileLoadResult.addMessage("Error when loading data from database. Check the log for more information.", ResultType.FAILED);
            return modListProfileLoadResult;
        }
        if (!modListProfileLoadResult.isSuccess()) {
            modListProfileLoadResult.addMessage("Failed to load mod list profiles!", ResultType.FAILED);
        } else {
            modListProfileLoadResult.addMessage("Successfully loaded all mod list profiles.", ResultType.SUCCESS);
        }
        return modListProfileLoadResult;
    }

    //TODO: Oh god the testing. We need to really test the conditionals for updates in particular
    @Override
    public Result<Void> saveUserData(UserConfiguration userConfiguration) {
        Result<Void> saveResult = new Result<>();
        try {
            SQLITE_DB.useTransaction(handle -> {
                saveUserConfiguration(userConfiguration, handle, saveResult);
                saveSaveProfiles(userConfiguration.getSaveProfiles(), handle, saveResult);
                saveModListProfiles(userConfiguration.getModListProfiles(), handle, saveResult);
                saveMods(userConfiguration.getModListProfiles(), handle, saveResult);

                saveResult.addMessage("Successfully saved all user data!", ResultType.SUCCESS);
            });
        } catch (TransactionException | IOException e) {
            saveResult.addMessage(e.toString(), ResultType.FAILED);
            saveResult.addMessage("Failed to save user data.", ResultType.FAILED);
        }
        return saveResult;
    }

    private void saveUserConfiguration(UserConfiguration userConfiguration, Handle handle, Result<Void> saveResult) {
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
                                run_first_time_setup = excluded.run_first_time_setup;""")
                .bindBean(userConfiguration)
                .execute();
        saveResult.addMessage("Successfully updated user_configuration table.", ResultType.SUCCESS);
    }

    private void saveSaveProfiles(List<SaveProfile> saveProfiles, Handle handle, Result<Void> saveResult) {
        //Delete the removed save profiles
        PreparedBatch deleteBatch = handle.prepareBatch("""
                DELETE FROM save_profile
                WHERE save_profile_id NOT IN (<ids>);""");
        deleteBatch.bindList("ids", saveProfiles.isEmpty() ? List.of(-1) : saveProfiles.stream().map(SaveProfile::getID).toList()).add();

        int[] countSaveProfilesDeleted = deleteBatch.execute();
        saveResult.addMessage(countSaveProfilesDeleted.length + " save profiles deleted.", ResultType.SUCCESS);

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
                        :saveExists,
                        :spaceEngineersVersion)
                    ON CONFLICT (save_profile_id) DO UPDATE SET
                        profile_name = CASE WHEN save_profile.profile_name != excluded.profile_name THEN excluded.profile_name ELSE save_profile.profile_name END,
                        last_used_mod_list_profile_id = CASE WHEN save_profile.last_used_mod_list_profile_id  != excluded.last_used_mod_list_profile_id THEN excluded.last_used_mod_list_profile_id  ELSE save_profile.last_used_mod_list_profile_id  END,
                        last_save_status = CASE WHEN save_profile.last_save_status != excluded.last_save_status THEN excluded.last_save_status ELSE save_profile.last_save_status END,
                        last_saved = CASE WHEN save_profile.last_saved != excluded.last_saved THEN excluded.last_saved ELSE save_profile.last_saved END,
                        save_exists = CASE WHEN save_profile.save_exists != excluded.save_exists THEN excluded.save_exists ELSE save_profile.save_exists END;""");
        //Update our bridge table to connect save profiles to the user config. Ignore duplicates.
        PreparedBatch saveProfilesUserConfigurationBatch = handle.prepareBatch("""
                INSERT OR IGNORE INTO user_configuration_save_profile (user_configuration_id, save_profile_id)
                    VALUES (1, :saveProfileId);""");

        //Actually attach a value to our variables in our batch.
        for (SaveProfile saveProfile : saveProfiles) {
            saveProfilesBatch.bind("id", saveProfile.getID())
                    .bind("profileName", saveProfile.getProfileName())
                    .bind("saveName", saveProfile.getSaveName())
                    .bind("savePath", saveProfile.getSavePath())
                    .bind("lastUsedModProfileId", saveProfile.getLastUsedModProfileId())
                    .bind("lastSaveStatus", saveProfile.getLastSaveStatus())
                    .bind("lastSaved", saveProfile.getLastSaved())
                    .bind("saveExists", saveProfile.isSaveExists())
                    .bind("spaceEngineersVersion", saveProfile.getSPACE_ENGINEERS_VERSION())
                    .add();

            saveProfilesUserConfigurationBatch.bind("saveProfileId", saveProfile.getID()).add();
        }

        //Execute our batches
        int countExpectedSaveProfilesUpdate = saveProfiles.size();
        int[] saveProfileRowsUpdated = saveProfilesBatch.execute();
        saveResult.addMessage(saveProfileRowsUpdated.length + " save profiles saved to database. Expected " + countExpectedSaveProfilesUpdate, (saveProfileRowsUpdated.length == countExpectedSaveProfilesUpdate ? ResultType.SUCCESS : ResultType.WARN));
        int[] saveProfileUserConfigurationRowsUpdated = saveProfilesUserConfigurationBatch.execute();
        saveResult.addMessage(saveProfileUserConfigurationRowsUpdated.length + " save profiles saved to user config bridge table. Expected " + countExpectedSaveProfilesUpdate, (saveProfileRowsUpdated.length == countExpectedSaveProfilesUpdate ? ResultType.SUCCESS : ResultType.WARN));
        saveResult.addMessage("Successfully updated save profiles.", ResultType.SUCCESS);
    }

    private void saveModListProfiles(List<ModListProfile> modListProfiles, Handle handle, Result<Void> saveResult) {
        //Delete the removed mod list profiles
        PreparedBatch deleteBatch = handle.prepareBatch("""
                DELETE FROM mod_list_profile
                WHERE mod_list_profile_id NOT IN(<ids>);""");
        deleteBatch.bindList("ids", modListProfiles.isEmpty() ? List.of(-1) : modListProfiles.stream().map(ModListProfile::getID).toList()).add();
        int[] countModListProfilesDeleted = deleteBatch.execute();
        saveResult.addMessage(countModListProfilesDeleted.length + " mod list profiles deleted.", ResultType.SUCCESS);

        //Upsert the mod_list_profile table, but only for information that's changed
        PreparedBatch modListProfilesBatch = handle.prepareBatch("""
                INSERT INTO mod_list_profile (
                    mod_list_profile_id,
                    profile_name,
                    space_engineers_version)
                    VALUES (
                        :id,
                        :profileName,
                        :spaceEngineersVersion)
                    ON CONFLICT (mod_list_profile_id) DO UPDATE SET
                        profile_name = CASE WHEN mod_list_profile.profile_name != excluded.profile_name THEN excluded.profile_name ELSE mod_list_profile.profile_name END""");
        //Update our bridge table to connect mod list profiles to the user config. Ignore duplicates.
        PreparedBatch modListProfilesUserConfigurationBatch = handle.prepareBatch("""
                INSERT OR IGNORE INTO user_configuration_mod_list_profile (user_configuration_id, mod_list_profile_id)
                    values (1, :modListProfileId);""");
        for (ModListProfile modListProfile : modListProfiles) {
            modListProfilesBatch.bind("id", modListProfile.getID())
                    .bind("profileName", modListProfile.getProfileName())
                    .bind("spaceEngineersVersion", modListProfile.getSPACE_ENGINEERS_VERSION())
                    .add();
            modListProfilesUserConfigurationBatch.bind("modListProfileId", modListProfile.getID()).add();
        }

        //Execute our batches
        int countExpectedModListProfilesUpdated = modListProfiles.size();
        int[] modListProfileRowsUpdated = modListProfilesBatch.execute();
        saveResult.addMessage(modListProfileRowsUpdated.length + " mod list profiles saved to database. Expected " + countExpectedModListProfilesUpdated, (modListProfileRowsUpdated.length == countExpectedModListProfilesUpdated ? ResultType.SUCCESS : ResultType.WARN));
        int[] modListProfileUserConfigurationRowsUpdated = modListProfilesUserConfigurationBatch.execute();
        saveResult.addMessage(modListProfileUserConfigurationRowsUpdated.length + " mod list profiles saved to user config bridge table. Expected " + modListProfiles.size(), (modListProfileRowsUpdated.length == countExpectedModListProfilesUpdated ? ResultType.SUCCESS : ResultType.WARN));
        saveResult.addMessage("Successfully updated mod list profiles.", ResultType.SUCCESS);
    }

    private void saveMods(List<ModListProfile> modListProfiles, Handle handle, Result<Void> saveResult) throws IOException {
        //Delete mods from a profile that are no longer in it
        PreparedBatch deleteBatch = handle.prepareBatch("""
                DELETE FROM mod_list_profile_mod
                WHERE mod_list_profile_id = :profileId
                AND mod_id NOT IN (:ids);""");
        for (ModListProfile modListProfile : modListProfiles) {
            if (modListProfile.getModList().isEmpty()) {
                deleteBatch.bind("profileId", modListProfile.getID())
                        .bind("ids", "-1")
                        .add();
            } else {
                String ids = modListProfile.getModList().stream()
                        .map(Mod::getId)
                        .map(id -> "'" + id + "'")  // Ensure values are treated as strings
                        .collect(Collectors.joining(","));

                deleteBatch.bind("profileId", modListProfile.getID())
                        .bind("ids", ids)
                        .add();
            }
        }
        int[] countModsRemovedFromProfile = deleteBatch.execute();
        saveResult.addMessage(countModsRemovedFromProfile.length + " mods deleted from profile ", ResultType.SUCCESS);

        //Upsert the mod table but only for info that's changed
        PreparedBatch modsBatch = handle.prepareBatch("""
                INSERT INTO mod (
                    mod_id,
                    friendly_name,
                    published_service_name,
                    description)
                    VALUES (
                        :id,
                        :friendlyName,
                        :publishedServiceName,
                        :description)
                    ON CONFLICT (mod_id) DO UPDATE SET
                        friendly_name = CASE WHEN mod.friendly_name != excluded.friendly_name THEN excluded.friendly_name ELSE mod.friendly_name END,
                        description = CASE WHEN mod.description != excluded.description THEN excluded.description ELSE mod.description END;""");

        //Upsert the mod categories table but only for info that's changed
        PreparedBatch modCategoriesBatch = handle.prepareBatch("""
                INSERT INTO mod_category (
                    mod_id,
                    category)
                    VALUES (
                        :id,
                        :category)
                    ON CONFLICT (mod_id, category) DO UPDATE SET
                        category = CASE WHEN mod_category.category != excluded.category THEN excluded.category ELSE mod_category.category END;""");

        //Upsert the steam mods table but only for info that's changed
        PreparedBatch steamModsBatch = handle.prepareBatch("""
                INSERT INTO steam_mod (
                    mod_id,
                    last_updated)
                    VALUES (
                        :id,
                        :lastUpdated)
                    ON CONFLICT (mod_id) DO UPDATE SET
                        last_updated = CASE WHEN steam_mod.last_updated != excluded.last_updated THEN excluded.last_updated ELSE steam_mod.last_updated END;""");

        //Upsert the mod io mods table but only for info that's changed
        PreparedBatch modIoModsBatch = handle.prepareBatch("""
                INSERT INTO modio_mod (
                    mod_id,
                    last_updated_year,
                    last_updated_month_day,
                    last_updated_hour)
                    VALUES (
                        :id,
                        :lastUpdatedYear,
                        :lastUpdatedMonthDay,
                        :lastUpdatedHour)
                    ON CONFLICT (mod_id) DO UPDATE SET
                        last_updated_year = CASE WHEN modio_mod.last_updated_year != excluded.last_updated_year THEN excluded.last_updated_year ELSE modio_mod.last_updated_year END,
                        last_updated_month_day = CASE WHEN modio_mod.last_updated_month_day != excluded.last_updated_month_day THEN excluded.last_updated_month_day ELSE modio_mod.last_updated_month_day END,
                        last_updated_hour = CASE WHEN modio_mod.last_updated_hour != last_updated_hour THEN excluded.last_updated_hour ELSE modio_mod.last_updated_hour END;""");

        //Upsert the mod modified paths table but only for info that's changed
        PreparedBatch modifiedPathsBatch = handle.prepareBatch("""
                INSERT INTO mod_modified_path (
                    mod_id,
                    modified_path)
                    VALUES (
                        :id,
                        :modifiedPath)
                    ON CONFLICT (mod_id, modified_path) DO UPDATE SET
                        modified_path = CASE WHEN mod_modified_path.modified_path != excluded.modified_path THEN excluded.modified_path ELSE mod_modified_path.modified_path END;""");

        //Update our bridge table to connect mod list profiles to the user config. Ignore duplicates.
        PreparedBatch modListProfileModBatch = handle.prepareBatch("""
                INSERT INTO mod_list_profile_mod (
                    mod_id,
                    mod_list_profile_id,
                    load_priority,
                    active)
                    VALUES (
                        :id,
                        :modListProfileId,
                        :loadPriority,
                        :active)
                    ON CONFLICT (mod_id, mod_list_profile_id) DO UPDATE SET
                        load_priority = CASE WHEN mod_list_profile_mod.load_priority != excluded.load_priority THEN excluded.load_priority ELSE mod_list_profile_mod.load_priority END,
                        active = CASE WHEN mod_list_profile_mod.active != excluded.active THEN excluded.active ELSE mod_list_profile_mod.active END;""");

        //Holy mother of batching. This is the binding for all our batches for every mod profile table.
        int countExpectedModsUpdated = 0;
        int countExpectedSteamModsUpdated = 0;
        int countExpectedModIoModsUpdated = 0;
        for (ModListProfile modListProfile : modListProfiles) {
            for (Mod mod : modListProfile.getModList()) {
                modsBatch.bind("id", mod.getId())
                        .bind("friendlyName", mod.getFriendlyName())
                        .bind("publishedServiceName", mod.getPublishedServiceName())
                        .bind("description", StringCryptpressor.compressAndEncryptString(mod.getDescription()))
                        .add();

                for (String category : mod.getCategories()) {
                    modCategoriesBatch.bind("id", mod.getId())
                            .bind("category", category)
                            .add();
                }

                if (mod instanceof SteamMod steamMod) {
                    steamModsBatch.bind("id", steamMod.getId())
                            .bind("lastUpdated", steamMod.getLastUpdated())
                            .add();
                    countExpectedSteamModsUpdated++;
                } else {
                    ModIoMod modIoMod = (ModIoMod) mod;
                    modIoModsBatch.bind("id", modIoMod.getId())
                            .bind("lastUpdatedYear", modIoMod.getLastUpdatedYear() != null ? modIoMod.getLastUpdatedYear().toString() : null)
                            .bind("lastUpdatedMonthDay", modIoMod.getLastUpdatedMonthDay() != null ? modIoMod.getLastUpdatedMonthDay().toString() : null)
                            .bind("lastUpdatedHour", modIoMod.getLastUpdatedHour() != null ? modIoMod.getLastUpdatedHour().toString() : null)
                            .add();
                    countExpectedModIoModsUpdated++;
                }

                if (!mod.getModifiedPaths().isEmpty()) {
                    for (String modifiedPath : mod.getModifiedPaths()) {
                        modifiedPathsBatch.bind("id", mod.getId())
                                .bind("modifiedPath", modifiedPath)
                                .add();
                    }
                }
                modListProfileModBatch.bind("id", mod.getId())
                        .bind("modListProfileId", modListProfile.getID())
                        .bind("loadPriority", mod.getLoadPriority())
                        .bind("active", mod.isActive())
                        .add();
                countExpectedModsUpdated++;
            }
        }

        //TODO: Check if this is useful. It might ALWAYS return a different amount.
        int[] modRowsUpdated = modsBatch.execute();
        saveResult.addMessage(modRowsUpdated.length + " mods saved to database. Expected " + countExpectedModsUpdated, (modRowsUpdated.length == countExpectedModsUpdated ? ResultType.SUCCESS : ResultType.WARN));

        modCategoriesBatch.execute();
        saveResult.addMessage("Mod categories updated.", ResultType.SUCCESS);

        int[] steamModRowsUpdated = steamModsBatch.execute();
        saveResult.addMessage(steamModRowsUpdated.length + " steam mods saved to database. Expected " + countExpectedSteamModsUpdated, (steamModRowsUpdated.length == countExpectedSteamModsUpdated ? ResultType.SUCCESS : ResultType.WARN));

        int[] modIoModRowsUpdated = modIoModsBatch.execute();
        saveResult.addMessage(modIoModRowsUpdated.length + " steam mods saved to database. Expected " + countExpectedModIoModsUpdated, (modIoModRowsUpdated.length == countExpectedModIoModsUpdated ? ResultType.SUCCESS : ResultType.WARN));

        modifiedPathsBatch.execute();
        saveResult.addMessage("Mod modified paths updated.", ResultType.SUCCESS);

        modListProfileModBatch.execute();
        saveResult.addMessage("Mod list profiles updated with mods.", ResultType.SUCCESS);
        saveResult.addMessage("Successfully updated mods.", ResultType.SUCCESS);
    }

    @Override
    public Result<Void> exportModlist(ModListProfile modListProfile, File modlistLocation) {
        return super.exportModlist(modListProfile, modlistLocation);
    }

    @Override
    public Result<ModListProfile> importModlist(File modlistLocation) {
        return super.importModlist(modlistLocation);
    }

    @Override
    public Result<Void> resetUserConfiguration() {
        Result<Void> resetResult = new Result<>();
        Path databaseLocation = Path.of(databasePath);
        if ((Files.exists(databaseLocation))) {
            try {
                Files.delete(databaseLocation);
                resetResult.addMessage("Deleted existing database.", ResultType.SUCCESS);
                createDatabase();
                resetResult.addMessage("Successfully deleted user data.", ResultType.SUCCESS);
            } catch (IOException e) {
                resetResult.addMessage(e.toString(), ResultType.FAILED);
                return resetResult;
            }
        }
        return resetResult;
    }

    private void createDatabase() {
        SQLITE_DB.useHandle(handle -> handle.execute("PRAGMA journal_mode=WAL;"));
        SQLITE_DB.useTransaction(handle -> {
            try {
                String sqlScript = Files.readString(Path.of(
                        Objects.requireNonNull(
                                        this.getClass().getClassLoader().getResource("Database/semm_db_base.sql"))
                                .toURI()));

                String[] sqlOperations = sqlScript.split("\\r\\n\\r\\n");
                for (String sql : sqlOperations) {
                    handle.execute(sql);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        SQLITE_DB.useHandle(handle -> handle.execute("""
                CREATE TRIGGER delete_orphan_mods
                    AFTER DELETE ON mod_list_profile_mod
                    FOR EACH ROW
                    WHEN NOT EXISTS (SELECT 1 FROM mod_list_profile_mod WHERE mod_id = OLD.mod_id)
                    BEGIN
                        DELETE FROM mod WHERE mod_id = OLD.mod_id;
                    END;"""));
    }
}
