package backend.data;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
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

	private UserDataFileRepository userDataFileRepository;

	@TempDir
	private File tempDir;

	@BeforeEach
	void setup() throws IOException {
		userDataFileRepository = new UserDataFileRepository();
	}

	@Test
	void shouldGetValidConfig() throws JAXBException {
		Result<UserConfiguration> userConfigurationResult = userDataFileRepository.loadUserData(new File("src/test/resources/TestUserData/SEMM_TEST_Data.xml"));
		assertTrue(userConfigurationResult.isSuccess());
		UserConfiguration validUserConfig = userConfigurationResult.getPayload();
		assertEquals("Primer Dark", validUserConfig.getUserTheme());
		assertNull(validUserConfig.getLastModifiedSaveProfileId());
		assertEquals(1, validUserConfig.getModLists().size());
		assertEquals("Default", validUserConfig.getModLists().getFirst().getProfileName());
		assertEquals("None", validUserConfig.getSaveProfiles().getFirst().getProfileName());
	}

	@Test
	void shouldFailOnInvalidUserConfig() throws JAXBException {
		Result<UserConfiguration> userConfigurationResult = (userDataFileRepository.loadUserData(new File("src/test/resources/TestUserData/SEMM_BAD_TEST_Data.xml")));
		UserConfiguration badUserConfiguration = new UserConfiguration();
		badUserConfiguration.setUserTheme("Primer Light");
		userConfigurationResult.setPayload(badUserConfiguration);
		assertFalse(userConfigurationResult.isSuccess());
		UserConfiguration badUserData = userConfigurationResult.getPayload();
		assertEquals("Primer Light", badUserData.getUserTheme());
		assertNull(badUserData.getLastModifiedSaveProfileId());
		assertEquals(1, badUserData.getModLists().size());
		assertEquals("Default", badUserData.getModLists().getFirst().getProfileName());
		assertEquals("None", badUserData.getSaveProfiles().getFirst().getProfileName());
	}

	@Test
	void shouldSaveUserData() throws IOException {
		UserConfiguration freshUserConfig = new UserConfiguration();
		Path tempFile = Files.createFile(tempDir.toPath().resolve("test_user_data.xml"));
		assertTrue(userDataFileRepository.saveUserData(freshUserConfig, new File(String.valueOf(tempFile))));
	}
}
