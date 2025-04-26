package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.mappers.ModListProfileMapper;
import com.gearshiftgaming.se_mod_manager.backend.data.mappers.ModMapper;
import com.gearshiftgaming.se_mod_manager.backend.data.mappers.SaveProfileMapper;
import com.gearshiftgaming.se_mod_manager.backend.data.mappers.UserConfigurationMapper;
import com.gearshiftgaming.se_mod_manager.backend.data.utility.StringCodepressor;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.core.transaction.TransactionException;
import org.sqlite.SQLiteException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

//TODO: look into parallelizing this.
public class UserDataSqliteRepository extends ModListProfileJaxbSerializer implements UserDataRepository {

    private static final Logger log = LogManager.getLogger(UserDataSqliteRepository.class);
    private final Jdbi SQLITE_DB;
    private final String databasePath;

    public UserDataSqliteRepository(String databasePath) throws IOException {
        this.databasePath = databasePath;

        Path databaseLocation = Path.of(databasePath);
        if (Files.notExists(databaseLocation)) {
            log.info("Database not found. Creating new database...");
            if (Files.notExists(databaseLocation.getParent())) {
                log.info("Database storage folder not found. Creating folder...");
                Files.createDirectories(databaseLocation.getParent());
            }
            //We have to enable foreign keys on each connection for SQLite so we do it in our factory.
            SQLITE_DB = Jdbi.create(new SQLiteConnectionFactory("jdbc:sqlite:" + databasePath));
            createDatabase();
            initializeData();
        } else {
            SQLITE_DB = Jdbi.create(new SQLiteConnectionFactory("jdbc:sqlite:" + databasePath));
        }
    }

    private void createDatabase() {
        //If our DB doesn't exist, we create a new one. We want to enable WAL mode for performance, and setup a trigger for the mod_list_profile_mod table to cleanup unused mods.
        log.info("Creating database schema...");
        SQLITE_DB.useHandle(handle -> handle.execute("PRAGMA journal_mode=WAL;"));
        SQLITE_DB.useTransaction(handle -> {
            try (InputStream sqlStream = this.getClass().getClassLoader().getResourceAsStream("Database/semm_db_base.sql")) {
                if (sqlStream == null) {
                    log.error("Could not find database schema.");
                    throw new FileNotFoundException("Resource not found: " + "Database/semm_db_base.sql");
                }
                String sqlScript = new String(sqlStream.readAllBytes(), StandardCharsets.UTF_8);

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
        log.info("Finished creating database schema.");
    }

    public Result<Void> initializeData() {
        log.info("Initializing data...");
        UserConfiguration userConfiguration = new UserConfiguration();
        ModListProfile modListProfile = new ModListProfile("Default", SpaceEngineersVersion.SPACE_ENGINEERS_ONE);
        Result<Void> saveResult = saveCurrentData(userConfiguration, modListProfile, userConfiguration.getSaveProfiles().getFirst());
        userConfiguration.setLastActiveModProfileId(modListProfile.getID());
        saveResult.addAllMessages(saveUserConfiguration(userConfiguration));
        if (!saveResult.isSuccess()) {
            log.error("Failed to initialize database data!");
            log.error(saveResult.getCurrentMessage());
        } else {
            log.info("Finished initializing database data.");
        }
        return saveResult;
    }

    @Override
    public Result<Void> saveCurrentData(UserConfiguration userConfiguration, ModListProfile modListProfile, SaveProfile saveProfile) {
        Result<Void> saveResult = saveUserConfiguration(userConfiguration);
        if (!saveResult.isSuccess()) {
            return saveResult;
        }

        saveResult.addAllMessages(saveModListProfile(modListProfile));
        if (!saveResult.isSuccess()) {
            return saveResult;
        }

        saveResult.addAllMessages(saveSaveProfile(saveProfile));
        if (!saveResult.isSuccess()) {
            return saveResult;
        }

        saveResult.addAllMessages(updateModInformation(modListProfile.getModList()));
        if (!saveResult.isSuccess()) {
            return saveResult;
        }

        saveResult.addMessage("Successfully saved all user data!", ResultType.SUCCESS);
        return saveResult;
    }

    @Override
    public Result<UserConfiguration> loadStartupData() {
        Result<UserConfiguration> userProfileLoadResult = new Result<>();
        UserConfiguration userConfiguration;
        try (Handle handle = SQLITE_DB.open()) {
            userConfiguration = handle.createQuery("SELECT * FROM user_configuration WHERE id = 1;")
                    .map(new UserConfigurationMapper())
                    .findFirst().orElse(null);
            userProfileLoadResult.addMessage("Loaded user configuration from database.", ResultType.SUCCESS);
        }
        if (userConfiguration == null) {
            userProfileLoadResult.addMessage("Failed to load user configuration.", ResultType.FAILED);
            return userProfileLoadResult;
        }

        List<MutableTriple<UUID, String, SpaceEngineersVersion>> modListProfileIds = loadAllBasicModProfileInformation();
        if (modListProfileIds.isEmpty()) {
            userProfileLoadResult.addMessage("Failed to load mod list profile ID's.", ResultType.FAILED);
            return userProfileLoadResult;
        }
        userProfileLoadResult.addMessage("Loaded mod list profile ID's.", ResultType.SUCCESS);
        userConfiguration.setModListProfilesBasicInfo(modListProfileIds);

        List<SaveProfile> saveProfiles = loadSaveProfiles();
        if (saveProfiles.isEmpty()) {
            userProfileLoadResult.addMessage("Failed to load save profiles.", ResultType.FAILED);
            return userProfileLoadResult;
        }
        userProfileLoadResult.addMessage("Loaded save profiles.", ResultType.SUCCESS);
        userConfiguration.setSaveProfiles(saveProfiles);

        userProfileLoadResult.setPayload(userConfiguration);

        return userProfileLoadResult;
    }

    //Loads all the mod list profile ID's, names, and the SE version of those profiles into our user configuration.
    private List<MutableTriple<UUID, String, SpaceEngineersVersion>> loadAllBasicModProfileInformation() {
        List<MutableTriple<UUID, String, SpaceEngineersVersion>> modListProfileIds;
        try (Handle handle = SQLITE_DB.open()) {
            modListProfileIds = handle.createQuery("SELECT * from mod_list_profile;")
                    .map((rs, ctx) -> MutableTriple.of(UUID.fromString(rs.getString("mod_list_profile_id")),
                            rs.getString("profile_name"),
                            SpaceEngineersVersion.valueOf(rs.getString("space_engineers_version"))))
                    .list();
        }
        return modListProfileIds;
    }

    //Loads all the save profiles into the user configuration
    private List<SaveProfile> loadSaveProfiles() {
        List<SaveProfile> saveProfiles;
        try (Handle handle = SQLITE_DB.open()) {
            saveProfiles = handle.createQuery("SELECT * from save_profile;")
                    .map(new SaveProfileMapper())
                    .list();
        }
        return saveProfiles;
    }

    //TODO: OP#58
    @Override
    public Result<Void> saveUserConfiguration(UserConfiguration userConfiguration) {
        Result<Void> saveResult = new Result<>();
        //Upsert our user_configuration row. We should only ever have one, so we set the row to the first one.
        try {
            SQLITE_DB.useTransaction(handle -> handle.createUpdate("""
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
                    .execute());
            for (MutableTriple<UUID, String, SpaceEngineersVersion> details : userConfiguration.getModListProfilesBasicInfo()) {
                Result<Void> detailsSaveResult = saveModListProfileDetails(details.getLeft(), details.getMiddle(), details.getRight());
                if (!detailsSaveResult.isSuccess()) {
                    saveResult.addAllMessages(detailsSaveResult);
                    throw new TransactionException(detailsSaveResult.getCurrentMessage());
                }
            }
            saveResult.addMessage("Successfully saved user configuration.", ResultType.SUCCESS);
        } catch (TransactionException | UnableToExecuteStatementException e) {
            saveResult.addMessage(getStackTrace(e), ResultType.FAILED);
            saveResult.addMessage("Failed to save user configuration.", ResultType.FAILED);
        }
        return saveResult;
    }

    @Override
    public Result<ModListProfile> loadModListProfileByName(String profileName) {
        //Get our basic mod list profile information
        Result<ModListProfile> modListProfileResult = new Result<>();
        Optional<ModListProfile> foundModListProfile;
        try (Handle handle = SQLITE_DB.open()) {
            foundModListProfile = handle.createQuery("SELECT * FROM mod_list_profile WHERE profile_name = :profileName")
                    .bind("profileName", profileName)
                    .map(new ModListProfileMapper())
                    .findOne();
        }
        foundModListProfile.ifPresentOrElse(modListProfile1 -> modListProfileResult.addMessage("Found mod list profile.", ResultType.SUCCESS),
                () -> modListProfileResult.addMessage(String.format("Failed to find mod list profile \"%s\".", profileName), ResultType.FAILED));
        if (foundModListProfile.isEmpty()) {
            return modListProfileResult;
        }

        loadModList(foundModListProfile.get(), modListProfileResult);

        return modListProfileResult;
    }

    @Override
    public Result<ModListProfile> loadFirstModListProfile() {
        Result<ModListProfile> modListProfileResult = new Result<>();
        Optional<ModListProfile> foundModListProfile;
        try (Handle handle = SQLITE_DB.open()) {
            foundModListProfile = handle.createQuery("SELECT * FROM mod_list_profile LIMIT 1;")
                    .map(new ModListProfileMapper())
                    .findFirst();
        }
        foundModListProfile.ifPresentOrElse(modListProfile1 -> modListProfileResult.addMessage("Found mod list profile.", ResultType.SUCCESS),
                () -> modListProfileResult.addMessage("Failed to find first mod list profile.", ResultType.FAILED));
        if (foundModListProfile.isEmpty()) {
            return modListProfileResult;
        }
        loadModList(foundModListProfile.get(), modListProfileResult);
        return modListProfileResult;
    }

    @Override
    public Result<ModListProfile> loadModListProfileById(UUID modListProfileId) {
        Result<ModListProfile> modListProfileResult = new Result<>();
        Optional<ModListProfile> foundModListProfile;
        try (Handle handle = SQLITE_DB.open()) {
            foundModListProfile = handle.createQuery("SELECT * FROM mod_list_profile WHERE mod_list_profile_id = :profileId")
                    .bind("profileId", modListProfileId)
                    .map(new ModListProfileMapper())
                    .findOne();
        }
        foundModListProfile.ifPresentOrElse(modListProfile1 -> modListProfileResult.addMessage("Found mod list profile.", ResultType.SUCCESS),
                () -> modListProfileResult.addMessage(String.format("Failed to find mod list profile \"%s\".", modListProfileId), ResultType.FAILED));
        if (foundModListProfile.isEmpty()) {
            return modListProfileResult;
        }

        loadModList(foundModListProfile.get(), modListProfileResult);
        modListProfileResult.setPayload(foundModListProfile.get());
        return modListProfileResult;
    }

    private void loadModList(ModListProfile modListProfile, Result<ModListProfile> modListProfileResult) {
        Result<List<Mod>> modListResult = loadModListForProfile(modListProfile.getID());
        if (!modListResult.isSuccess()) {
            modListProfileResult.addAllMessages(modListResult);
        }

        modListProfile.setModList(modListResult.getPayload());
        modListProfile.generateConflictTable();
        modListProfileResult.addMessage(String.format("Successfully loaded mod profile \"%s\"", modListProfile.getProfileName()), ResultType.SUCCESS);
    }

    private Result<List<Mod>> loadModListForProfile(UUID modListProfileId) {
        Result<List<Mod>> modListLoadResult = new Result<>();
        try (Handle handle = SQLITE_DB.open()) {

            List<String> modListIds = handle.createQuery("SELECT mod_id FROM mod_list_profile_mod WHERE mod_list_profile_id = :id")
                    .bind("id", modListProfileId.toString())
                    .mapTo(String.class)
                    .list();

            if (modListIds.isEmpty()) {
                modListLoadResult.addMessage(String.format("No mods for mod list profile \"%s\"", modListProfileId), ResultType.SUCCESS);
                modListLoadResult.setPayload(new ArrayList<>());
                return modListLoadResult;
            }

            //Get all information for mods from the database for our mod list.
            // This will get us a map for all mod information where the modID is the key and the mod object the value.
            List<Mod> modList = handle.createQuery("""
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
                                mlpm.load_priority,
                                mlpm.active,
                                GROUP_CONCAT(DISTINCT mc.category) AS categories
                            FROM ModDetails mod_details
                                LEFT JOIN mod_category mc ON mod_details.mod_id = mc.mod_id
                                LEFT JOIN mod_list_profile_mod mlpm ON mod_details.mod_id = mlpm.mod_id
                            WHERE mod_details.mod_id IN (<modListIds>) AND mlpm.mod_list_profile_id = :modListProfileId
                            GROUP BY mod_details.mod_id, mlpm.load_priority, mlpm.active
                            ORDER BY mlpm.load_priority;""")
                    .bind("modListProfileId", modListProfileId)
                    .bindList("modListIds", modListIds)
                    .map(new ModMapper())
                    .list();

            //If there's no mods on the mod list we can just skip the next steps since they're all mod assembly steps.
            if (modList.isEmpty()) {
                modListLoadResult.addMessage("Could not load mods from database.", ResultType.FAILED);
                modListLoadResult.setPayload(new ArrayList<>());
                return modListLoadResult;
            }

            modListLoadResult.addMessage("Successfully grabbed all mod data. Fetching modified paths...", ResultType.SUCCESS);

            Map<String, List<String>> modifiedPathsMap = handle.createQuery("""
                            SELECT * from mod_modified_path WHERE mod_id IN (<modIds>);""")
                    .bindList("modIds", modListIds)
                    .reduceRows(new LinkedHashMap<>(), (map, row) -> {
                        String modId = row.getColumn("mod_id", String.class);
                        String modifiedPath = row.getColumn("modified_path", String.class);
                        map.computeIfAbsent(modId, k -> new ArrayList<>()).add(modifiedPath);
                        return map;
                    });

            if (modifiedPathsMap.isEmpty()) {
                modListLoadResult.addMessage("No modified paths found for mods!", ResultType.SUCCESS);
            } else {
                //Actually add the values of the modified paths to each mod
                for (Mod mod : modList) {
                    mod.setModifiedPaths(modifiedPathsMap.getOrDefault(mod.getId(), new ArrayList<>()));
                }
            }
            modListLoadResult.addMessage("Successfully grabbed all modified paths for mods.", ResultType.SUCCESS);

            modListLoadResult.addMessage(String.format("Successfully loaded mods for profile \"%s\"", modListProfileId), ResultType.SUCCESS);
            modListLoadResult.setPayload(modList);
            return modListLoadResult;
        }
    }

    //Saves a mod list profile, but ONLY the profile. Does not save the mod list.
    @Override
    public Result<Void> saveModListProfileDetails(UUID modListProfileId, String modListProfileName, SpaceEngineersVersion spaceEngineersVersion) {
        Result<Void> modListProfileSaveResult = new Result<>();
        try {
            SQLITE_DB.useTransaction(handle -> handle.createUpdate("""
                            INSERT INTO mod_list_profile (
                                mod_list_profile_id,
                                profile_name,
                                space_engineers_version)
                                VALUES (
                                    :id,
                                    :profileName,
                                    :spaceEngineersVersion)
                                ON CONFLICT (mod_list_profile_id) DO UPDATE SET
                                    profile_name = CASE WHEN mod_list_profile.profile_name != excluded.profile_name THEN excluded.profile_name ELSE mod_list_profile.profile_name END;""")
                    .bind("id", modListProfileId)
                    .bind("profileName", modListProfileName)
                    .bind("spaceEngineersVersion", spaceEngineersVersion)
                    .execute());
            //Update our user config bridge table to
            SQLITE_DB.useTransaction(handle -> handle.createUpdate("""
                            INSERT OR IGNORE INTO user_configuration_mod_list_profile (user_configuration_id, mod_list_profile_id)
                                values (1, :modListProfileId);""")
                    .bind("modListProfileId", modListProfileId)
                    .execute());
            modListProfileSaveResult.addMessage(String.format("Successfully saved mod list profile \"%s\"", modListProfileName), ResultType.SUCCESS);
        } catch (TransactionException | UnableToExecuteStatementException e) {
            modListProfileSaveResult.addMessage(getStackTrace(e), ResultType.FAILED);
            return modListProfileSaveResult;
        }
        return modListProfileSaveResult;
    }

    /**
     * Saves the mod list profile details AND the mod list for it.
     *
     * @param modListProfile The mod list profile we are going to save all the information for.
     * @return The status and message of the operation.
     */
    @Override
    public Result<Void> saveModListProfile(ModListProfile modListProfile) {
        Result<Void> modListProfileSaveResult = saveModListProfileDetails(modListProfile.getID(), modListProfile.getProfileName(), modListProfile.getSPACE_ENGINEERS_VERSION());
        if (!modListProfileSaveResult.isSuccess()) {
            return modListProfileSaveResult;
        }

        modListProfileSaveResult.addAllMessages(updateModListProfileModList(modListProfile.getID(), modListProfile.getModList()));

        return modListProfileSaveResult;
    }

    @Override
    public Result<Void> deleteModListProfile(UUID modListProfileId) {
        Result<Void> modListProfileDeleteResult = new Result<>();
        try {
            SQLITE_DB.useTransaction(handle -> {
                int numDeletedRows = handle.createUpdate("DELETE FROM mod_list_profile WHERE mod_list_profile_id = :id")
                        .bind("id", modListProfileId)
                        .execute();
                if (numDeletedRows != 1) {
                    modListProfileDeleteResult.addMessage(String.format("Error. Deleted %d rows, expected to delete 1 row.", numDeletedRows), ResultType.FAILED);
                } else {
                    modListProfileDeleteResult.addMessage(String.format("Successfully deleted mod list profile \"%s\".", modListProfileId), ResultType.SUCCESS);
                }
            });
        } catch (TransactionException | UnableToExecuteStatementException e) {
            modListProfileDeleteResult.addMessage(getStackTrace(e), ResultType.FAILED);
            return modListProfileDeleteResult;
        }
        return modListProfileDeleteResult;
    }

    //This updates the DB copy of the entire mod list for a mod list profile, but not the mods themselves.
    @Override
    public Result<Void> updateModListProfileModList(UUID modListProfileId, List<Mod> modList) {
        //Delete any removed mods first.
        Result<Void> modListUpdateResult = deleteModProfileRemovedMods(modListProfileId, modList);
        if (!modListUpdateResult.isSuccess()) {
            return modListUpdateResult;
        }

        try {
            SQLITE_DB.useTransaction(handle -> {
                //Update our bridge table to connect mod list profiles to mods. Ignore duplicates.
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
                for (Mod mod : modList) {
                    modListProfileModBatch.bind("id", mod.getId())
                            .bind("modListProfileId", modListProfileId)
                            .bind("loadPriority", mod.getLoadPriority())
                            .bind("active", mod.isActive())
                            .add();
                }
                int[] numUpdatedModListRows = modListProfileModBatch.execute();
                int numSuccessfullyUpdatedModListRows = 0;
                for (int num : numUpdatedModListRows) {
                    if (num == 1) {
                        numSuccessfullyUpdatedModListRows++;
                    }
                }

                modListUpdateResult.addMessage(String.format("Successfully updated %s mods in the modlist.", numSuccessfullyUpdatedModListRows), ResultType.SUCCESS);
            });
        } catch (TransactionException | UnableToExecuteStatementException e) {
            modListUpdateResult.addMessage(getStackTrace(e), ResultType.FAILED);
            return modListUpdateResult;
        }
        return modListUpdateResult;
    }

    private Result<Void> deleteModProfileRemovedMods(UUID modListProfileId, List<Mod> modProfileModList) {
        Result<Void> modDeletionResult = new Result<>();
        try {
            SQLITE_DB.useTransaction(handle -> {
                int deletedMods = handle.createUpdate("DELETE FROM mod_list_profile_mod WHERE mod_list_profile_id = :modListProfileId AND mod_id NOT IN (<currentModListIds>);")
                        .bind("modListProfileId", modListProfileId)
                        .bindList("currentModListIds", !modProfileModList.isEmpty() ? modProfileModList.stream().map(Mod::getId).toList() : List.of(-1))
                        .execute();
                modDeletionResult.addMessage(String.format("Successfully deleted %d mods from mod list profile \"%s\".", deletedMods, modListProfileId), ResultType.SUCCESS);
            });
        } catch (TransactionException | UnableToExecuteStatementException e) {
            modDeletionResult.addMessage(getStackTrace(e), ResultType.FAILED);
            return modDeletionResult;
        }
        return modDeletionResult;
    }

    @Override
    public Result<Void> updateModListActiveMods(UUID modListProfileId, List<Mod> modList) {
        Result<Void> modListUpdateResult = new Result<>();
        try {
            SQLITE_DB.useTransaction(handle -> {
                PreparedBatch updateActiveModsBatch = handle.prepareBatch("""
                        UPDATE mod_list_profile_mod
                        SET active = :active
                        WHERE mod_id = :modId
                        AND mod_list_profile_id = :modListProfileId
                        AND active != :active;""");

                for (Mod mod : modList) {
                    updateActiveModsBatch.bind("modId", mod.getId())
                            .bind("modListProfileId", modListProfileId)
                            .bind("active", mod.isActive())
                            .add();
                }
                int[] numModsUpdated = updateActiveModsBatch.execute();
                int numSuccessfullyUpdatedMods = 0;
                for (int num : numModsUpdated) {
                    if (num == 1) {
                        numSuccessfullyUpdatedMods++;
                    }
                }

                modListUpdateResult.addMessage(String.format("Successfully updated %s mods in the modlist.", numSuccessfullyUpdatedMods), ResultType.SUCCESS);
            });
        } catch (TransactionException | UnableToExecuteStatementException e) {
            modListUpdateResult.addMessage(getStackTrace(e), ResultType.FAILED);
            return modListUpdateResult;
        }

        return modListUpdateResult;
    }

    @Override
    public Result<Void> updateModListLoadPriority(UUID modListProfileId, List<Mod> modList) {
        Result<Void> modListUpdateResult = new Result<>();
        try {
            SQLITE_DB.useTransaction(handle -> {
                PreparedBatch updateModsLoadPriorityBatch = handle.prepareBatch("""
                        UPDATE mod_list_profile_mod
                        SET load_priority = :loadPriority
                        WHERE mod_id = :modId
                        AND mod_list_profile_id = :modListProfileId
                        AND load_priority != :loadPriority;""");
                for (Mod mod : modList) {
                    updateModsLoadPriorityBatch.bind("modId", mod.getId())
                            .bind("modListProfileId", modListProfileId)
                            .bind("loadPriority", mod.getLoadPriority())
                            .add();
                }

                int[] numModsUpdated = updateModsLoadPriorityBatch.execute();
                int numSuccessfullyUpdatedMods = 0;
                for (int num : numModsUpdated) {
                    if (num == 1) {
                        numSuccessfullyUpdatedMods++;
                    }
                }

                modListUpdateResult.addMessage(String.format("Successfully updated %s mods in the modlist.", numSuccessfullyUpdatedMods), ResultType.SUCCESS);
            });
        } catch (TransactionException | UnableToExecuteStatementException e) {
            modListUpdateResult.addMessage(getStackTrace(e), ResultType.FAILED);
            return modListUpdateResult;
        }
        return modListUpdateResult;
    }

    @Override
    public Result<Void> saveSaveProfile(SaveProfile saveProfile) {
        Result<Void> saveSaveProfileResult = new Result<>();
        try {
            SQLITE_DB.useTransaction(handle -> {
                handle.createUpdate("""
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
                                    :ID,
                                    :profileName,
                                    :saveName,
                                    :savePath,
                                    :lastUsedModProfileId,
                                    :lastSaveStatus,
                                    :lastSaved,
                                    :saveExists,
                                    :SPACE_ENGINEERS_VERSION)
                                ON CONFLICT (save_profile_id) DO UPDATE SET
                                    profile_name = CASE WHEN save_profile.profile_name != excluded.profile_name THEN excluded.profile_name ELSE save_profile.profile_name END,
                                    last_used_mod_list_profile_id = CASE WHEN save_profile.last_used_mod_list_profile_id  != excluded.last_used_mod_list_profile_id THEN excluded.last_used_mod_list_profile_id  ELSE save_profile.last_used_mod_list_profile_id  END,
                                    last_save_status = CASE WHEN save_profile.last_save_status != excluded.last_save_status THEN excluded.last_save_status ELSE save_profile.last_save_status END,
                                    last_saved = CASE WHEN save_profile.last_saved != excluded.last_saved THEN excluded.last_saved ELSE save_profile.last_saved END,
                                    save_exists = CASE WHEN save_profile.save_exists != excluded.save_exists THEN excluded.save_exists ELSE save_profile.save_exists END;""")
                        .bindBean(saveProfile)
                        .execute();
                SQLITE_DB.useTransaction(handle1 -> handle.createUpdate("""
                                INSERT OR IGNORE INTO user_configuration_save_profile (user_configuration_id, save_profile_id)
                                    VALUES (1, :saveProfileId);""")
                        .bind("saveProfileId", saveProfile.getID())
                        .execute());
                saveSaveProfileResult.addMessage(String.format("Successfully updated save profile \"%s\".", saveProfile.getProfileName()), ResultType.SUCCESS);
            });
        } catch (TransactionException | UnableToExecuteStatementException e) {
            saveSaveProfileResult.addMessage(getStackTrace(e), ResultType.FAILED);
            return saveSaveProfileResult;
        }
        return saveSaveProfileResult;
    }

    @Override
    public Result<Void> deleteSaveProfile(SaveProfile saveProfileId) {
        //Delete the removed save profiles
        Result<Void> saveResult = new Result<>();
        SQLITE_DB.useTransaction(handle -> {
            int countSaveProfilesDeleted = handle.execute("DELETE FROM save_profile WHERE save_profile_id NOT IN :id", saveProfileId.getID());
            if (countSaveProfilesDeleted != 1) {
                saveResult.addMessage(String.format("Failed to delete save profile \"%s\".", saveProfileId.getProfileName()), ResultType.FAILED);
            } else
                saveResult.addMessage(String.format("\"%s\" successfully deleted.", saveProfileId.getProfileName()), ResultType.SUCCESS);
        });
        return saveResult;
    }

    @Override
    public Result<Void> updateModInformation(List<Mod> modList) throws TransactionException {
        Result<Void> modUpdateResult = new Result<>();
        if (modList.isEmpty()) {
            modUpdateResult.addMessage("No mods to update.", ResultType.SUCCESS);
            return modUpdateResult;
        }

        try {
            SQLITE_DB.useTransaction(handle -> {
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

                int countExpectedSteamModsUpdated = 0, countExpectedModIoModsUpdated = 0;
                for (Mod mod : modList) {
                    modsBatch.bind("id", mod.getId())
                            .bind("friendlyName", mod.getFriendlyName())
                            .bind("publishedServiceName", mod.getPublishedServiceName())
                            .bind("description", StringCodepressor.compressandEncodeString(mod.getDescription()))
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
                }

                modsBatch.execute();
                modUpdateResult.addMessage("Mods updated.", ResultType.SUCCESS);

                modCategoriesBatch.execute();
                modUpdateResult.addMessage("Mod categories updated.", ResultType.SUCCESS);

                int[] affectedSteamModRows = steamModsBatch.execute();
                int numSteamModRowsActuallyAffected = 0;
                for (int i : affectedSteamModRows) {
                    if (i == 1) {
                        numSteamModRowsActuallyAffected++;
                    }
                }
                modUpdateResult.addMessage(numSteamModRowsActuallyAffected + " steam mods saved to database. Expected " + countExpectedSteamModsUpdated, (numSteamModRowsActuallyAffected == countExpectedSteamModsUpdated ? ResultType.SUCCESS : ResultType.WARN));

                int[] affectedModIoModRows = modIoModsBatch.execute();
                int numModIoModRowsActuallyAffected = 0;
                for (int i : affectedModIoModRows) {
                    if (i == 1) {
                        numModIoModRowsActuallyAffected++;
                    }
                }
                modUpdateResult.addMessage(numModIoModRowsActuallyAffected + " mod.io mods saved to database. Expected " + countExpectedModIoModsUpdated, (numModIoModRowsActuallyAffected == countExpectedModIoModsUpdated ? ResultType.SUCCESS : ResultType.WARN));

                modifiedPathsBatch.execute();
                modUpdateResult.addMessage("Mod modified paths updated.", ResultType.SUCCESS);

                modUpdateResult.addMessage("Mod list profiles updated with mods.", ResultType.SUCCESS);
                modUpdateResult.addMessage("Successfully updated mods.", ResultType.SUCCESS);
            });
        } catch (IOException e) {
            modUpdateResult.addMessage(getStackTrace(e), ResultType.FAILED);
            return modUpdateResult;
        }
        return modUpdateResult;
    }

    @Override
    public Result<Void> exportModListProfile(ModListProfile modListProfile, File modlistLocation) {
        return super.exportModlist(modListProfile, modlistLocation);
    }

    @Override
    public Result<ModListProfile> importModListProfile(File modlistLocation) {
        return super.importModlist(modlistLocation);
    }

    //TODO: REdo this to drop data instead of resetting everything.
    @Override
    public Result<Void> resetData() {
        Result<Void> resetResult = new Result<>();
        Path databaseLocation = Path.of(databasePath);
        if ((Files.exists(databaseLocation))) {
            try {
                Files.delete(databaseLocation);
                resetResult.addMessage("Deleted existing database.", ResultType.SUCCESS);
                createDatabase();
                resetResult.addMessage("Successfully deleted user data.", ResultType.SUCCESS);
            } catch (IOException e) {
                resetResult.addMessage(getStackTrace(e), ResultType.FAILED);
                return resetResult;
            }
        }
        return resetResult;
    }
}
