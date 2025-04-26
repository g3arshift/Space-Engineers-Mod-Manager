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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        assertEquals(userConfiguration.getLastActiveModProfileId(), modListProfiles.getFirst().getID());
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

    }

    @Test
    void shouldFailToSaveUserConfigurationWithNonExistentSaveProfile() {

    }

    @Test
    void shouldFailToSaveUserConfigurationWithNonExistentModListProfile() {

    }

    @Test
    void shouldUpdateUserConfiguration() {
        //Need to get the rowID of our item with "Select rowid" to make sure we are updating and not deleting/remaking the row
    }

    @Test
    void shouldSaveModListProfileDetails() {

    }

    @Test
    void shouldDeleteRemovedModsFromModProfile() {

    }

    @Test
    void shouldUpdateModListProfileModList() {
        //Need to get the rowID of our item with "Select rowid" to make sure we are updating and not deleting/remaking the row
    }

    @Test
    void shouldSaveNewModListProfile() {

    }

    @Test
    void shouldUpdateExistingModListProfile() {
        //Need to get the rowID of our item with "Select rowid" to make sure we are updating and not deleting/remaking the row
    }

    @Test
    void shouldSaveNewSaveProfile() {

    }

    @Test
    void shouldUpdateExistingSaveProfile() {
        //Need to get the rowID of our item with "Select rowid" to make sure we are updating and not deleting/remaking the row
    }

    @Test
    void shouldAddNewModInformation() {
    }

    @Test
    void shouldUpdateExistingModInformation() {
        //Need to get the rowID of our item with "Select rowid" to make sure we are updating and not deleting/remaking the row
    }

    @Test
    void shouldDeleteOrphanMods() {

    }

    @Test
    void shouldExportModListProfile() {

    }

    @Test
    void shouldImportModListProfile() {

    }

    @Test
    void shouldResetDatabase() {

    }

    @Test
    void shouldLoadModListProfileByName() {

    }

    @Test
    void shouldNotLoadNonExistentModListProfileByName() {

    }

    @Test
    void shouldLoadModListProfileById() {

    }

    @Test
    void shouldNotLoadNonExistentModListProfileById() {

    }

    @Test
    void shouldLoadFirstModListProfile() {
        //We can do this by deleting all mod list profiles and then adding a new one.
    }

    @Test
    void shouldDeleteModListProfile() {

    }

    @Test
    void shouldNotDeleteNonExistentModListProfile() {

    }

    @Test
    void shouldUpdateActiveModListProfileMods() {
        //Need to get the rowID of our item with "Select rowid" to make sure we are updating and not deleting/remaking the row
    }

    @Test
    void shouldUpdateLoadPriorityForModListProfile() {
        //Need to get the rowID of our item with "Select rowid" to make sure we are updating and not deleting/remaking the row
    }

    @Test
    void shouldDeleteSaveProfile() {

    }

    @Test
    void shouldNotDeleteNonExistentSaveProfile() {

    }
}
