package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.SQLiteConnectionFactory;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataSqliteRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.mappers.ModListProfileMapper;
import com.gearshiftgaming.se_mod_manager.backend.data.mappers.SaveProfileMapper;
import com.gearshiftgaming.se_mod_manager.backend.data.mappers.UserConfigurationMapper;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserDataSqliteRepositoryTest {
    private UserDataSqliteRepository userDataSqliteRepository;
    Jdbi sqliteDb;
    private final String DATABASE_PATH = "src/test/resources/DataStorage/TestData.db";

    @BeforeEach
    void setup() throws IOException {
        sqliteDb = Jdbi.create(new SQLiteConnectionFactory("jdbc:sqlite:" + DATABASE_PATH));
        userDataSqliteRepository = new UserDataSqliteRepository(DATABASE_PATH);
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.delete(Path.of(DATABASE_PATH));
    }

    @Test
    void shouldCreateDatabaseSchema() {
        List<String> tableNames = sqliteDb.withHandle(handle -> handle.createQuery("SELECT name FROM sqlite_master WHERE type='table';")
                .mapTo(String.class)
                .list());
        tableNames.remove("sqlite_sequence");
        assertEquals(11, tableNames.size());
        assertTrue(tableNames.contains("mod"));
        assertTrue(tableNames.contains("mod_category"));
        assertTrue(tableNames.contains("mod_list_profile"));
        assertTrue(tableNames.contains("mod_list_profile_mod"));
        assertTrue(tableNames.contains("mod_modified_path"));
        assertTrue(tableNames.contains("modio_mod"));
        assertTrue(tableNames.contains("save_profile"));
        assertTrue(tableNames.contains("steam_mod"));
        assertTrue(tableNames.contains("user_configuration"));
        assertTrue(tableNames.contains("user_configuration_mod_list_profile"));
        assertTrue(tableNames.contains("user_configuration_save_profile"));
    }

    @Test
    void shouldCreateDefaultData() {
        UserConfiguration userConfiguration = sqliteDb.withHandle(handle -> handle.createQuery("SELECT * FROM user_configuration WHERE id = 1;")
                .map(new UserConfigurationMapper())
                .first());
        assertEquals("PrimerLight", userConfiguration.getUserTheme());
        assertNull(userConfiguration.getLastModifiedSaveProfileId());
        assertNull(userConfiguration.getLastActiveSaveProfileId());
        assertTrue(userConfiguration.isRunFirstTimeSetup());
        Handle handle = sqliteDb.open();
        List<ModListProfile> modListProfiles = handle.createQuery("SELECT * FROM mod_list_profile")
                .map(new ModListProfileMapper())
                .list();
        handle.close();
        assertEquals(1, modListProfiles.size());
        assertEquals(userConfiguration.getLastActiveModProfileId(), modListProfiles.getFirst().getId());
        assertEquals("Default", modListProfiles.getFirst().getProfileName());

        handle = sqliteDb.open();
        List<SaveProfile> saveProfiles = handle.createQuery("SELECT * FROM save_profile")
                .map(new SaveProfileMapper())
                .list();
        handle.close();
        assertEquals(1, saveProfiles.size());
        assertEquals("None", saveProfiles.getFirst().getProfileName());
        assertEquals("None", saveProfiles.getFirst().getSaveName());
        assertFalse(saveProfiles.getFirst().isSaveExists());
        assertEquals(SaveStatus.NONE, saveProfiles.getFirst().getLastSaveStatus());
    }

    @Test
    void shouldLoadStartupData() {
        Result<UserConfiguration> result = userDataSqliteRepository.loadStartupData();
        assertTrue(result.isSuccess());
        assertNotNull(result.getPayload());
    }

    @Test
    void shouldFailToLoadStartupDataWithNoUserConfiguration() {
        sqliteDb.useHandle(handle -> handle.createUpdate("DELETE FROM user_configuration where id = 1;").execute());
        Result<UserConfiguration> result = userDataSqliteRepository.loadStartupData();
        assertFalse(result.isSuccess());
        assertNull(result.getPayload());
        assertEquals("Failed to load user configuration.", result.getCurrentMessage());
    }

    @Test
    void shouldFailToLoadStartupDataWithNoModListProfiles() {
        sqliteDb.useHandle(handle -> handle.createUpdate("DELETE FROM mod_list_profile;").execute());
        Result<UserConfiguration> result = userDataSqliteRepository.loadStartupData();
        assertFalse(result.isSuccess());
        assertNull(result.getPayload());
        assertEquals("Failed to load mod list profile ID's.", result.getCurrentMessage());
    }

    @Test
    void shouldFailToLoadStartupDataWithNoSaveProfiles() {
        sqliteDb.useHandle(handle -> handle.createUpdate("DELETE FROM save_profile;").execute());
        Result<UserConfiguration> result = userDataSqliteRepository.loadStartupData();
        assertFalse(result.isSuccess());
        assertNull(result.getPayload());
        assertEquals("Failed to load save profiles.", result.getCurrentMessage());
    }

    @Test
    void shouldSuccessfullySaveUserConfiguration() {
        Result<UserConfiguration> loadResult = userDataSqliteRepository.loadStartupData();
        assertTrue(loadResult.isSuccess());
        assertNotNull(loadResult.getPayload());

        loadResult.getPayload().setUserTheme("PrimerDark");

        Result<Void> saveResult = userDataSqliteRepository.saveUserConfiguration(loadResult.getPayload());
        assertTrue(saveResult.isSuccess());

        loadResult = userDataSqliteRepository.loadStartupData();
        assertTrue(loadResult.isSuccess());
        assertNotNull(loadResult.getPayload());
        assertEquals("PrimerDark", loadResult.getPayload().getUserTheme());
    }

    @Test
    void shouldFailToSaveUserConfigurationWithNonExistentLastActiveSaveProfile() {
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setLastActiveSaveProfileId(UUID.randomUUID());
        Result<Void> result = userDataSqliteRepository.saveUserConfiguration(userConfiguration);
        assertFalse(result.isSuccess());
        assertEquals("Failed to save user configuration.", result.getCurrentMessage());
    }

    @Test
    void shouldFailToSaveUserConfigurationWithNonExistentLastActiveModListProfile() {
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setLastActiveModProfileId(UUID.randomUUID());
        Result<Void> result = userDataSqliteRepository.saveUserConfiguration(userConfiguration);
        assertFalse(result.isSuccess());
        assertEquals("Failed to save user configuration.", result.getCurrentMessage());
    }

    @Test
    void shouldUpdateUserConfiguration() {
        //Need to get the rowID of our item with "Select rowid" to make sure we are updating and not deleting/remaking the row
        int rowID = -1;
        try (Handle handle = sqliteDb.open()) {
            rowID = handle.createQuery("SELECT ROWID FROM user_configuration;")
                    .mapTo(Integer.class)
                    .first();
        }
        assertNotEquals(-1, rowID);
        assertEquals(1, rowID);

        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setUserTheme("Test Value");
        userDataSqliteRepository.saveUserConfiguration(userConfiguration);
        try (Handle handle = sqliteDb.open()) {
            rowID = handle.createQuery("SELECT ROWID FROM user_configuration;")
                    .mapTo(Integer.class)
                    .first();
        }
        assertNotEquals(-1, rowID);
        assertEquals(1, rowID);

        Result<UserConfiguration> result = userDataSqliteRepository.loadStartupData();
        assertTrue(result.isSuccess());
        assertNotNull(result.getPayload());
        assertEquals("Test Value", result.getPayload().getUserTheme());
    }

    @Test
    void shouldSaveNewModListProfile() {
        ModListProfile modListProfile = new ModListProfile("Test Profile", SpaceEngineersVersion.SPACE_ENGINEERS_TWO);
        Result<Void> saveResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(saveResult.isSuccess());
        assertEquals(String.format("Successfully saved mod list profile \"%s\".", modListProfile.getProfileName()), saveResult.getCurrentMessage());

        ModListProfile foundModListProfile;
        try (Handle handle = sqliteDb.open()) {
            foundModListProfile = handle.createQuery("SELECT * FROM mod_list_profile WHERE mod_list_profile_id = :id")
                    .bind("id", modListProfile.getId())
                    .map(new ModListProfileMapper())
                    .one();
        }
        assertNotNull(foundModListProfile);
        assertEquals(modListProfile, foundModListProfile);
    }

    @Test
    void shouldLoadModListProfileByName() {
        Result<ModListProfile> modListProfileResult = userDataSqliteRepository.loadModListProfileByName("Default");
        assertTrue(modListProfileResult.isSuccess());
        assertNotNull(modListProfileResult.getPayload());
        ModListProfile modListProfile = modListProfileResult.getPayload();
        assertEquals(modListProfile, modListProfileResult.getPayload());
    }

    @Test
    void shouldNotLoadNonExistentModListProfileByName() {
        Result<ModListProfile> modListProfileResult = userDataSqliteRepository.loadModListProfileByName("Doesn't exist");
        assertFalse(modListProfileResult.isSuccess());
        assertNull(modListProfileResult.getPayload());
        assertEquals(String.format("Failed to find mod list profile \"%s\".", "Doesn't exist"), modListProfileResult.getCurrentMessage());
    }

    @Test
    void shouldLoadModListProfileById() {
        ModListProfile modListProfile = new ModListProfile();
        Result<Void> saveResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(saveResult.isSuccess());

        Result<ModListProfile> modListProfileResult = userDataSqliteRepository.loadModListProfileById(modListProfile.getId());
        assertTrue(modListProfileResult.isSuccess());
        assertNotNull(modListProfileResult.getPayload());
        assertEquals(modListProfile, modListProfileResult.getPayload());
    }

    @Test
    void shouldNotLoadNonExistentModListProfileById() {
        UUID uuid = UUID.randomUUID();
        Result<ModListProfile> result = userDataSqliteRepository.loadModListProfileById(uuid);
        assertFalse(result.isSuccess());
        assertNull(result.getPayload());
        assertEquals(String.format("Failed to find mod list profile \"%s\".", uuid), result.getCurrentMessage());
    }

    @Test
    void shouldSaveModListProfileDetails() {
        ModListProfile modListProfile = new ModListProfile("Test profile", SpaceEngineersVersion.SPACE_ENGINEERS_TWO);
        Result<Void> saveDetailsResult = userDataSqliteRepository.saveModListProfileDetails(modListProfile.getId(), modListProfile.getProfileName(), modListProfile.getSpaceEngineersVersion());
        assertTrue(saveDetailsResult.isSuccess());

        Result<ModListProfile> profileLoadResult = userDataSqliteRepository.loadModListProfileById(modListProfile.getId());
        assertTrue(profileLoadResult.isSuccess());
        assertEquals(modListProfile, profileLoadResult.getPayload());
    }

    @Test
    void shouldLoadFirstModListProfile() {
        ModListProfile modListProfile = new ModListProfile("Test Profile", SpaceEngineersVersion.SPACE_ENGINEERS_ONE);
        sqliteDb.useHandle(handle -> handle.createUpdate("DELETE FROM mod_list_profile;").execute());
        Result<Void> saveResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(saveResult.isSuccess());

        Result<ModListProfile> loadResult = userDataSqliteRepository.loadFirstModListProfile();
        assertTrue(loadResult.isSuccess());
        assertNotNull(loadResult.getPayload());
        assertEquals(modListProfile, loadResult.getPayload());
    }

    @Test
    void shouldUpdateExistingModListProfile() {
        //Need to get the rowID of our item with "Select rowid" to make sure we are updating and not deleting/remaking the row
        Result<ModListProfile> defaultProfileLoadResult = userDataSqliteRepository.loadFirstModListProfile();
        assertTrue(defaultProfileLoadResult.isSuccess());
        assertNotNull(defaultProfileLoadResult.getPayload());
        ModListProfile defaultProfile = defaultProfileLoadResult.getPayload();

        defaultProfile.setProfileName("Not the default profile name");
        userDataSqliteRepository.saveModListProfile(defaultProfile);
        int rowID = -1;
        try (Handle handle = sqliteDb.open()) {
            rowID = handle.createQuery("SELECT ROWID FROM mod_list_profile WHERE mod_list_profile_id = :id;")
                    .bind("id", defaultProfile.getId())
                    .mapTo(Integer.class)
                    .first();
        }
        assertNotEquals(-1, rowID);
        assertEquals(1, rowID);

        Result<ModListProfile> modifiedProfileResult = userDataSqliteRepository.loadModListProfileById(defaultProfile.getId());
        assertTrue(modifiedProfileResult.isSuccess());
        assertNotNull(modifiedProfileResult.getPayload());
        assertEquals(defaultProfile, modifiedProfileResult.getPayload());
    }

    @Test
    void shouldUpdateActiveModListProfileMods() {
        SteamMod modOne = new SteamMod("1234567890");
        SteamMod modTwo = new SteamMod("0987654321");
        Result<Void> modUpdateResult = userDataSqliteRepository.updateModInformation(List.of(modOne, modTwo));
        assertTrue(modUpdateResult.isSuccess());

        ModListProfile modListProfile = new ModListProfile();
        modListProfile.getModList().add(modOne);
        modListProfile.getModList().add(modTwo);
        Result<Void> saveResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(saveResult.isSuccess());

        modListProfile.getModList().getFirst().setActive(true);
        Result<Void> updateResult = userDataSqliteRepository.updateModListActiveMods(modListProfile.getId(), modListProfile.getModList());
        assertTrue(updateResult.isSuccess());

        boolean modIsActive;
        try (Handle handle = sqliteDb.open()) {
            modIsActive = handle.createQuery("""
                            SELECT active
                            FROM mod_list_profile_mod
                            WHERE mod_list_profile_id = :profileId AND mod_id = :modId;""")
                    .bind("profileId", modListProfile.getId())
                    .bind("modId", modOne.getId())
                    .mapTo(Boolean.class)
                    .one();
        }
        assertTrue(modIsActive);
    }

    @Test
    void shouldUpdateLoadPriorityForModListProfile() {
        SteamMod modOne = new SteamMod("1234567890");
        SteamMod modTwo = new SteamMod("0987654321");
        modOne.setLoadPriority(1);
        modTwo.setLoadPriority(2);
        Result<Void> modUpdateResult = userDataSqliteRepository.updateModInformation(List.of(modOne, modTwo));
        assertTrue(modUpdateResult.isSuccess());

        ModListProfile modListProfile = new ModListProfile();
        modListProfile.getModList().add(modOne);
        modListProfile.getModList().add(modTwo);
        Result<Void> saveResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(saveResult.isSuccess());

        modListProfile.getModList().getFirst().setLoadPriority(5);
        Result<Void> updateResult = userDataSqliteRepository.updateModListLoadPriority(modListProfile.getId(), modListProfile.getModList());
        assertTrue(updateResult.isSuccess());

        int modLoadPriority;
        try (Handle handle = sqliteDb.open()) {
            modLoadPriority = handle.createQuery("""
                            SELECT load_priority
                            FROM mod_list_profile_mod
                            WHERE mod_list_profile_id = :profileId AND mod_id = :modId;""")
                    .bind("profileId", modListProfile.getId())
                    .bind("modId", modOne.getId())
                    .mapTo(Integer.class)
                    .one();
        }
        assertEquals(5, modLoadPriority);
    }

    @Test
    void shouldAddNewModInformation() {
        SteamMod modOne = new SteamMod("1234567890");
        SteamMod modTwo = new SteamMod("0987654321");
        Result<Void> modUpdateResult = userDataSqliteRepository.updateModInformation(List.of(modOne, modTwo));
        assertTrue(modUpdateResult.isSuccess());
        int numberOfModsInDatabase;
        try (Handle handle = sqliteDb.open()) {
            numberOfModsInDatabase = handle.createQuery("SELECT COUNT (*) FROM mod;")
                    .mapTo(Integer.class)
                    .first();
        }
        assertEquals(2, numberOfModsInDatabase);
    }

    @Test
    void shouldUpdateModListProfileModList() {
        SteamMod modOne = new SteamMod("1234567890");
        SteamMod modTwo = new SteamMod("0987654321");
        Result<Void> modUpdateResult = userDataSqliteRepository.updateModInformation(List.of(modOne, modTwo));
        assertTrue(modUpdateResult.isSuccess());

        ModListProfile modListProfile = new ModListProfile();
        modListProfile.getModList().add(modOne);
        modListProfile.getModList().add(modTwo);
        Result<Void> saveResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(saveResult.isSuccess());

        ModIoMod modIoMod = new ModIoMod("abcdefg");
        modUpdateResult = userDataSqliteRepository.updateModInformation(List.of(modIoMod));
        assertTrue(modUpdateResult.isSuccess());

        Result<Void> modListUpdateResult = userDataSqliteRepository.updateModListProfileModList(modListProfile.getId(), List.of(modOne, modTwo, modIoMod));
        assertTrue(modListUpdateResult.isSuccess());

        Result<ModListProfile> loadResult = userDataSqliteRepository.loadModListProfileById(modListProfile.getId());
        assertTrue(loadResult.isSuccess());
        assertNotNull(loadResult.getPayload());
        assertEquals(3, loadResult.getPayload().getModList().size());
    }

    @Test
    void shouldUpdateExistingModInformation() {
        //Need to get the rowID of our item with "Select rowid" to make sure we are updating and not deleting/remaking the row
        SteamMod modOne = new SteamMod("1234567890");
        Result<Void> modUpdateResult = userDataSqliteRepository.updateModInformation(List.of(modOne));
        assertTrue(modUpdateResult.isSuccess());

        ModListProfile modListProfile = new ModListProfile();
        modListProfile.getModList().add(modOne);
        Result<Void> saveResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(saveResult.isSuccess());

        int rowID = -1;
        try (Handle handle = sqliteDb.open()) {
            rowID = handle.createQuery("SELECT ROWID FROM mod WHERE mod_id = :id;")
                    .bind("id", modOne.getId())
                    .mapTo(Integer.class)
                    .first();
        }
        assertNotEquals(-1, rowID);
        assertEquals(1, rowID);

        modOne.setFriendlyName("Test mod");
        modUpdateResult = userDataSqliteRepository.updateModInformation(List.of(modOne));
        assertTrue(modUpdateResult.isSuccess());

        try (Handle handle = sqliteDb.open()) {
            rowID = handle.createQuery("SELECT ROWID FROM mod WHERE mod_id = :id;")
                    .bind("id", modOne.getId())
                    .mapTo(Integer.class)
                    .first();
            assertEquals(1, rowID);
        }
        modListProfile = userDataSqliteRepository.loadModListProfileById(modListProfile.getId()).getPayload();
        assertEquals(1, modListProfile.getModList().size());
        Mod retrievedMod = modListProfile.getModList().getFirst();
        assertNotNull(retrievedMod);
        assertEquals("Test mod", retrievedMod.getFriendlyName());
        assertEquals(retrievedMod, modOne);
    }

    @Test
    void shouldDeleteRemovedModsFromModProfile() {
        SteamMod modOne = new SteamMod("1234567890");
        SteamMod modTwo = new SteamMod("0987654321");
        Result<Void> modUpdateResult = userDataSqliteRepository.updateModInformation(List.of(modOne, modTwo));
        assertTrue(modUpdateResult.isSuccess());

        ModListProfile modListProfile = new ModListProfile();
        modListProfile.getModList().add(modOne);
        modListProfile.getModList().add(modTwo);
        Result<Void> saveResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(saveResult.isSuccess());

        modListProfile.getModList().removeLast();
        modUpdateResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(modUpdateResult.isSuccess());

        Result<ModListProfile> loadResult = userDataSqliteRepository.loadModListProfileById(modListProfile.getId());
        assertTrue(loadResult.isSuccess());
        assertEquals(1, loadResult.getPayload().getModList().size());
    }

    @Test
    void shouldDeleteOrphanMods() {
        SteamMod modOne = new SteamMod("1234567890");
        SteamMod modTwo = new SteamMod("0987654321");
        Result<Void> modUpdateResult = userDataSqliteRepository.updateModInformation(List.of(modOne, modTwo));
        assertTrue(modUpdateResult.isSuccess());

        ModListProfile modListProfile = new ModListProfile();
        modListProfile.getModList().add(modOne);
        modListProfile.getModList().add(modTwo);
        Result<Void> saveResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(saveResult.isSuccess());

        modListProfile.getModList().removeLast();
        modUpdateResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(modUpdateResult.isSuccess());

        try (Handle handle = sqliteDb.open()) {
            int numberOfModsInDatabase = handle.createQuery("SELECT COUNT (*) FROM mod;")
                    .mapTo(Integer.class)
                    .first();
            assertEquals(1, numberOfModsInDatabase);
        }
    }

    //TODO: We need checks for save type in all of these.
    @Test
    void shouldSaveNewSaveProfile() {
        SaveProfile saveProfile = new SaveProfile("Test Profile", "/this/does/not/exist", "BadSave", SpaceEngineersVersion.SPACE_ENGINEERS_TWO, SaveType.GAME);
        Result<Void> saveResult = userDataSqliteRepository.saveSaveProfile(saveProfile);
        assertTrue(saveResult.isSuccess());
        assertEquals(String.format("Successfully saved save profile \"%s\".", saveProfile.getProfileName()), saveResult.getCurrentMessage());

        SaveProfile foundSaveProfile;
        try (Handle handle = sqliteDb.open()) {
            foundSaveProfile = handle.createQuery("SELECT * FROM save_profile WHERE save_profile_id = :id")
                    .bind("id", saveProfile.getId())
                    .map(new SaveProfileMapper())
                    .one();
        }
        assertNotNull(foundSaveProfile);
        assertEquals(saveProfile, foundSaveProfile);
    }

    @Test
    void shouldUpdateExistingSaveProfile() {
        //Need to get the rowID of our item with "Select rowid" to make sure we are updating and not deleting/remaking the row
        SaveProfile saveProfile = new SaveProfile("Test Profile", "/this/does/not/exist", "BadSave", SpaceEngineersVersion.SPACE_ENGINEERS_TWO, SaveType.GAME);
        Result<Void> saveResult = userDataSqliteRepository.saveSaveProfile(saveProfile);
        assertTrue(saveResult.isSuccess());
        int rowID = -1;
        try (Handle handle = sqliteDb.open()) {
            rowID = handle.createQuery("SELECT ROWID FROM save_profile WHERE save_profile_id = :id;")
                    .bind("id", saveProfile.getId())
                    .mapTo(Integer.class)
                    .first();
        }
        assertNotEquals(-1, rowID);
        assertEquals(2, rowID);

        SaveProfile foundProfile;
        try (Handle handle = sqliteDb.open()) {
            foundProfile = handle.createQuery("SELECT * FROM save_profile WHERE save_profile_id = :id")
                    .bind("id", saveProfile.getId())
                    .map(new SaveProfileMapper())
                    .first();
        }
        assertNotNull(foundProfile);
        assertEquals(foundProfile, saveProfile);

        saveProfile.setProfileName("New Profile Name");
        saveResult = userDataSqliteRepository.saveSaveProfile(saveProfile);
        assertTrue(saveResult.isSuccess());

        try (Handle handle = sqliteDb.open()) {
            foundProfile = handle.createQuery("SELECT * FROM save_profile WHERE save_profile_id = :id")
                    .bind("id", saveProfile.getId())
                    .map(new SaveProfileMapper())
                    .first();
        }
        assertNotNull(foundProfile);
        assertEquals("New Profile Name", foundProfile.getProfileName());
        assertEquals(foundProfile, saveProfile);

        try (Handle handle = sqliteDb.open()) {
            rowID = handle.createQuery("SELECT ROWID FROM save_profile WHERE save_profile_id = :id;")
                    .bind("id", saveProfile.getId())
                    .mapTo(Integer.class)
                    .first();
        }
        assertNotEquals(-1, rowID);
        assertEquals(2, rowID);
    }

    //We combine these two tests for two reasons.
    //1. Either is useless without the other
    //2. To test one, we need to use the other
    @Test
    void shouldExportAndImportModListProfile() throws IOException {
        ModListProfile originalModListProfile = new ModListProfile("Test Profile", SpaceEngineersVersion.SPACE_ENGINEERS_TWO);
        SteamMod modOne = new SteamMod("1234567890");
        SteamMod modTwo = new SteamMod("0987654321");
        originalModListProfile.getModList().add(modOne);
        originalModListProfile.getModList().add(modTwo);
        String testFileLocation = "src/test/resources/DataStorage/TestProfile.SEMM";
        Path testFilePath = Path.of(testFileLocation);
        userDataSqliteRepository.exportModListProfile(originalModListProfile, new File(testFileLocation));
        assertTrue(Files.exists(testFilePath));

        Result<ModListProfile> loadResult = userDataSqliteRepository.importModListProfile(new File(testFileLocation));
        assertTrue(loadResult.isSuccess());
        assertNotNull(loadResult.getPayload());

        ModListProfile foundProfile = loadResult.getPayload();
        assertEquals(originalModListProfile.getProfileName(), foundProfile.getProfileName());
        assertEquals(originalModListProfile.getModList().size(), foundProfile.getModList().size());
        List<Mod> originalModListProfileModList = originalModListProfile.getModList();
        List<Mod> foundModListProfileModList = foundProfile.getModList();
        for (int i = 0; i < originalModListProfileModList.size(); i++) {
            assertEquals(originalModListProfileModList.get(i), foundModListProfileModList.get(i));
        }
        assertEquals(originalModListProfile.getSpaceEngineersVersion(), foundProfile.getSpaceEngineersVersion());

        Files.delete(testFilePath);
    }

    //TODO: This is not a great test, but the base functionality isn't great either.
    @Test
    void shouldResetDatabase() {
        assertTrue(userDataSqliteRepository.resetData().isSuccess());
    }

    @Test
    void shouldDeleteModListProfile() {
        ModListProfile modListProfile = new ModListProfile("Test Profile", SpaceEngineersVersion.SPACE_ENGINEERS_TWO);
        Result<Void> saveResult = userDataSqliteRepository.saveModListProfile(modListProfile);
        assertTrue(saveResult.isSuccess());
        assertEquals(String.format("Successfully saved mod list profile \"%s\".", modListProfile.getProfileName()), saveResult.getCurrentMessage());

        Result<Void> deleteResult = userDataSqliteRepository.deleteModListProfile(modListProfile.getId());
        assertTrue(deleteResult.isSuccess());

        Result<ModListProfile> searchResult = userDataSqliteRepository.loadModListProfileById(modListProfile.getId());
        assertFalse(searchResult.isSuccess());
        assertNull(searchResult.getPayload());
    }

    @Test
    void shouldNotDeleteNonExistentModListProfile() {
        Result<Void> deleteResult = userDataSqliteRepository.deleteModListProfile(UUID.randomUUID());
        assertFalse(deleteResult.isSuccess());
    }

    @Test
    void shouldDeleteSaveProfile() {
        SaveProfile saveProfile = new SaveProfile("Test Profile", "/this/does/not/exist", "BadSave", SpaceEngineersVersion.SPACE_ENGINEERS_TWO, SaveType.GAME);
        Result<Void> saveResult = userDataSqliteRepository.saveSaveProfile(saveProfile);
        assertTrue(saveResult.isSuccess());
        assertEquals(String.format("Successfully saved save profile \"%s\".", saveProfile.getProfileName()), saveResult.getCurrentMessage());

        Result<Void> deleteResult = userDataSqliteRepository.deleteSaveProfile(saveProfile);
        assertTrue(deleteResult.isSuccess());

        SaveProfile foundSaveProfile;
        try (Handle handle = sqliteDb.open()) {
            foundSaveProfile = handle.createQuery("SELECT * FROM save_profile WHERE save_profile_id = :id")
                    .bind("id", saveProfile.getId())
                    .map(new SaveProfileMapper())
                    .findFirst().orElse(null);
        }
        assertNull(foundSaveProfile);
    }

    @Test
    void shouldNotDeleteNonExistentSaveProfile() {
        Result<Void> deleteResult = userDataSqliteRepository.deleteSaveProfile(new SaveProfile());
        assertFalse(deleteResult.isSuccess());
    }
}
