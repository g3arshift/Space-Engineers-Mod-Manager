package backend.data;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Copyright GearShiftGaming 2024
 *
 * @author Gear Shift
 */
public class UserDataFileRepositoryTest {

	UserDataFileRepository userDataFileRepository;

	@TempDir
	private File tempDir;

	@Test
	void shouldGetValidConfig() {
		userDataFileRepository = new UserDataFileRepository((new File("src/test/resources/TestUserData/SEMM_TEST_Data.xml")));
		Result<UserConfiguration> userConfigurationResult = userDataFileRepository.loadUserData();
		assertTrue(userConfigurationResult.isSuccess());
		UserConfiguration validUserConfig = userConfigurationResult.getPayload();
		assertEquals("Primer Dark", validUserConfig.getUserTheme());
		assertNull(validUserConfig.getLastModifiedSaveProfileId());
		assertEquals(1, validUserConfig.getModListProfiles().size());
		assertEquals("Default", validUserConfig.getModListProfiles().getFirst().getProfileName());
		assertEquals("None", validUserConfig.getSaveProfiles().getFirst().getProfileName());
	}

	@Test
	void shouldFailOnInvalidUserConfig() {
		userDataFileRepository = new UserDataFileRepository((new File("src/test/resources/TestUserData/SEMM_BAD_TEST_Data.xml")));
		Result<UserConfiguration> userConfigurationResult = (userDataFileRepository.loadUserData());
		UserConfiguration badUserConfiguration = new UserConfiguration();
		badUserConfiguration.setUserTheme("Primer Light");
		userConfigurationResult.setPayload(badUserConfiguration);
		assertFalse(userConfigurationResult.isSuccess());
		UserConfiguration badUserData = userConfigurationResult.getPayload();
		assertEquals("Primer Light", badUserData.getUserTheme());
		assertNull(badUserData.getLastModifiedSaveProfileId());
		assertEquals(1, badUserData.getModListProfiles().size());
		assertEquals("Default", badUserData.getModListProfiles().getFirst().getProfileName());
		assertEquals("None", badUserData.getSaveProfiles().getFirst().getProfileName());
	}

	@Test
	void shouldSaveUserData() throws IOException {
		UserConfiguration freshUserConfig = new UserConfiguration();
		Path tempFile = Files.createFile(tempDir.toPath().resolve("test_user_data.xml"));
		userDataFileRepository = new UserDataFileRepository(tempFile.toFile());
		assertTrue(userDataFileRepository.saveUserData(freshUserConfig).isSuccess());
	}

	@Test
	void shouldExportAndImportModList() {
		ModListProfile modListProfile = new ModListProfile();
		modListProfile.setProfileName("Test Profile");
		modListProfile.getModList().add(new SteamMod("12345"));
		modListProfile.getModList().add(new ModIoMod("56789"));

		File testFile = new File(tempDir.getPath() + "/test_dir.semm");

		assertFalse(Files.exists(testFile.toPath()));
		userDataFileRepository = new UserDataFileRepository(new File("null"));
		Result<Void> result = userDataFileRepository.exportModlist(modListProfile, testFile);
		assertTrue(result.isSuccess());
		assertEquals("Successfully exported modlist.", result.getCurrentMessage());
		assertTrue(Files.exists(testFile.toPath()));

		Result<ModListProfile> exportContents = userDataFileRepository.importModlist(testFile);
		assertTrue(exportContents.isSuccess());
		ModListProfile testModListProfile = exportContents.getPayload();
		assertEquals(2, testModListProfile.getModList().size());
		assertEquals("Test Profile", testModListProfile.getProfileName());
	}

	@Test
	void shouldResetUserData() throws IOException {
		File testFile = new File(tempDir.getPath() + "/SEMM_TEST_Data.xml");
		Files.copy(Path.of("src/test/resources/TestUserData/SEMM_TEST_Data.xml"), testFile.toPath());
		userDataFileRepository = new UserDataFileRepository(testFile);
		assertTrue(Files.exists(testFile.toPath()));
		userDataFileRepository.resetUserConfiguration();
		assertFalse(Files.exists(testFile.toPath()));
	}
}
