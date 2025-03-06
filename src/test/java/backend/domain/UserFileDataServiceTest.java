package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.UserDataService;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

//TODO: We need more
public class UserFileDataServiceTest {
	UserDataService userDataService;
	UserDataFileRepository userDataFileRepository;

	@BeforeEach
	void setup() {
		userDataFileRepository = mock(UserDataFileRepository.class);
		userDataService = new UserDataService(userDataFileRepository);
	}

	@Test
	void shouldGetNewConfigFromNonExistentUserDataFile() throws JAXBException {
		Result<UserConfiguration> badResult = new Result<>();
		badResult.addMessage("User data was not found. Defaulting to new user configuration.", ResultType.FAILED);
		badResult.setPayload(new UserConfiguration());
		when(userDataFileRepository.loadUserData()).thenReturn(badResult);
		Result<UserConfiguration> result = userDataService.getUserData();

		UserConfiguration userData = result.getPayload();
		assertEquals(ResultType.FAILED, result.getType());
		assertEquals("User data was not found. Defaulting to new user configuration.", result.getMESSAGES().getFirst());
		assertEquals("PrimerLight", userData.getUserTheme());
		assertNull(userData.getLastModifiedSaveProfileId());
		assertEquals(1, userData.getModListProfiles().size());
		assertEquals("Default", userData.getModListProfiles().getFirst().getProfileName());
		assertEquals("None", userData.getSaveProfiles().getFirst().getProfileName());
	}

	@Test
	void shouldGetGoodUserData() throws JAXBException {
		File goodUserDataFile = new File("src/test/resources/TestUserData/SEMM_TEST_Data.xml");

		UserConfiguration goodUserConfig = new UserConfiguration();
		goodUserConfig.setUserTheme("Primer Dark");
		Result<UserConfiguration> goodResult = new Result<>();
		goodResult.addMessage("Successfully loaded user data.", ResultType.SUCCESS);
		goodResult.setPayload(goodUserConfig);

		when(userDataFileRepository.loadUserData()).thenReturn(goodResult);

		Result<UserConfiguration> result = userDataService.getUserData();

		UserConfiguration userConfiguration = result.getPayload();

		assertEquals("Successfully loaded user data.", result.getMESSAGES().getFirst());

		assertEquals("Primer Dark", userConfiguration.getUserTheme());
		assertNull(userConfiguration.getLastModifiedSaveProfileId());
		assertEquals(1, userConfiguration.getModListProfiles().size());
		assertEquals("Default", userConfiguration.getModListProfiles().getFirst().getProfileName());
		assertEquals("None", userConfiguration.getSaveProfiles().getFirst().getProfileName());
	}

	@Test
	void shouldSaveUserData() throws IOException {
		File goodUserDataFile = new File("src/test/resources/TestUserData/SEMM_TEST_Data.xml");
		UserConfiguration mockUserConfig = mock(UserConfiguration.class);

		when(userDataFileRepository.saveUserData(mockUserConfig)).thenReturn(true);

		Result<Void> result = userDataService.saveUserData(mockUserConfig);

		assertEquals(ResultType.SUCCESS, result.getType());
		assertEquals("Successfully saved user data.", result.getMESSAGES().getFirst());
	}

	@Test
	void shouldNotSaveUserData() throws IOException {
		File badUserDataFile = new File("src/test/resources/TestUserData/SEMM_BAD_TEST_Data.xml");
		UserConfiguration mockUserConfig = mock(UserConfiguration.class);

		when(userDataFileRepository.saveUserData(mockUserConfig)).thenReturn(false);

		Result<Void> result = userDataService.saveUserData(mockUserConfig);
		assertEquals(ResultType.FAILED, result.getType());
		assertNull(result.getPayload());
		assertEquals("Failed to save user data.", result.getMESSAGES().getFirst());
	}
}
