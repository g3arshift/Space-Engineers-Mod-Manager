package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.UserDataService;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
public class UserDataServiceTest {


	UserDataService userDataService;
	UserDataFileRepository userDataFileRepository;

	@BeforeEach
	void setup() {
		userDataFileRepository = mock(UserDataFileRepository.class);
		userDataService = new UserDataService(userDataFileRepository);
	}

	@Test
	void shouldGetNewConfigFromNonExistentUSerDataFile() throws JAXBException {
		Result<UserConfiguration> result = userDataService.getUserData(new File("src/this/file/does/not/exist"));

		UserConfiguration userData = result.getPayload();
		assertEquals(ResultType.FAILED, result.getType());
		assertEquals("Could not load user data. Defaulting to new user configuration.", result.getMESSAGES().getFirst());
		assertEquals("Primer Light", userData.getUserTheme());
		assertNull(userData.getLastUsedSaveProfileId());
		assertEquals(1, userData.getModlistProfiles().size());
		assertEquals("Default", userData.getModlistProfiles().getFirst().getProfileName());
		assertEquals("None", userData.getSaveProfiles().getFirst().getProfileName());
	}

	@Test
	void shouldGetGoodUserData() throws JAXBException {
		File goodUserDataFile = new File("src/test/resources/TestUserData/SEMM_TEST_Data.xml");

		UserConfiguration goodUserConfig = new UserConfiguration();
		goodUserConfig.setUserTheme("Primer Dark");

		when(userDataFileRepository.loadUserData(goodUserDataFile)).thenReturn(goodUserConfig);

		Result<UserConfiguration> result = userDataService.getUserData(goodUserDataFile);

		UserConfiguration userConfiguration = result.getPayload();

		assertEquals("Successfully loaded user data.", result.getMESSAGES().getFirst());

		assertEquals("Primer Dark", userConfiguration.getUserTheme());
		assertNull(userConfiguration.getLastUsedSaveProfileId());
		assertEquals(1, userConfiguration.getModlistProfiles().size());
		assertEquals("Default", userConfiguration.getModlistProfiles().getFirst().getProfileName());
		assertEquals("None", userConfiguration.getSaveProfiles().getFirst().getProfileName());
	}

	@Test
	void shouldSaveUserData() {
		File goodUserDataFile = new File("src/test/resources/TestUserData/SEMM_TEST_Data.xml");
		UserConfiguration mockUserConfig = mock(UserConfiguration.class);

		when(userDataFileRepository.saveUserData(mockUserConfig, goodUserDataFile)).thenReturn(true);

		Result<Void> result = userDataService.saveUserData(mockUserConfig, goodUserDataFile);

		assertEquals(ResultType.SUCCESS, result.getType());
		assertEquals("Successfully saved user data.", result.getMESSAGES().getFirst());
	}

	@Test
	void shouldNotSaveUserData() {
		File badUserDataFile = new File("src/test/resources/TestUserData/SEMM_BAD_TEST_Data.xml");
		UserConfiguration mockUserConfig = mock(UserConfiguration.class);

		when(userDataFileRepository.saveUserData(mockUserConfig, badUserDataFile)).thenReturn(false);

		Result<Void> result = userDataService.saveUserData(mockUserConfig, badUserDataFile);
		assertEquals(ResultType.FAILED, result.getType());
		assertNull(result.getPayload());
		assertEquals("Failed to save user data.", result.getMESSAGES().getFirst());
	}
}
