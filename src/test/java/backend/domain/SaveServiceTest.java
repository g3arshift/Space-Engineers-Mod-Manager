package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SaveFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SaveService;
import com.gearshiftgaming.se_mod_manager.backend.models.ModlistProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 *
 * @author Gear Shift
 */

//TODO: Make sure these are still valid
public class SaveServiceTest {

	//TODO: Write integration tests.
	//FIXME: Naming scheme for functions is bad.

	@TempDir
	private File tempDir;

	SaveService saveService;

	SaveFileRepository saveFileRepository;

	SandboxService sandboxService;

	Path testDir;

	SaveProfile saveProfile;


	@BeforeEach
	void setup() throws IOException {
		saveFileRepository = mock(SaveFileRepository.class);
		sandboxService = mock(SandboxService.class);
		saveService = new SaveService(saveFileRepository, sandboxService);

		testDir = Files.createDirectory(tempDir.toPath().resolve("test_copy_directory"));
		Files.createDirectory(tempDir.toPath().resolve(testDir + "/Backup"));
		Files.copy(Path.of("src/test/resources/SaveServiceTest/Good_Sandbox_config.sbc"), Path.of(testDir + "/Sandbox_config.sbc"));
		Files.copy(Path.of("src/test/resources/SaveServiceTest/Good_Sandbox.sbc"), Path.of(testDir + "/Sandbox.sbc"));
		saveProfile = createTestSaveProfile();
	}

	@Test
	void shouldGetInvalidSandboxConfig() throws IOException {
		Result<String> badResult = new Result<>();
		badResult.addMessage("This is a bad result.", ResultType.FAILED);
		when(sandboxService.getSandboxFromFile(any(File.class))).thenReturn(badResult);

		Result<SaveProfile> result = saveService.copySaveFiles(saveProfile);
		assertFalse(result.isSuccess());
		assertEquals(ResultType.FAILED, result.getType());
		assertEquals("This is a bad result.", result.getCurrentMessage());
		assertNull(result.getPayload());
	}

	@Test
	void shouldGetMissingSessionNameTag() throws IOException {
		Result<String> goodResult = new Result<>();
		goodResult.addMessage("This is a good result.", ResultType.SUCCESS);

		when(sandboxService.getSandboxFromFile(any(File.class))).thenReturn(goodResult);
		Result<SaveProfile> result = saveService.copySaveFiles(saveProfile);

		assertFalse(result.isSuccess());
		assertEquals(ResultType.FAILED, result.getType());
		assertEquals("Save does not contain a <SessionName> tag, and cannot be copied.", result.getCurrentMessage());
		assertNull(result.getPayload());
	}

	@Test
	void shouldFailToCopySourceSave() throws IOException {
		Result<String> goodResult = new Result<>();
		goodResult.addMessage("This is a good result.", ResultType.SUCCESS);
		goodResult.setPayload(Files.readString(Path.of("src/test/resources/SaveServiceTest/Good_Sandbox_config.sbc")));

		when(sandboxService.getSandboxFromFile(any(File.class))).thenReturn(goodResult);

		Result<SaveProfile> result = saveService.copySaveFiles(saveProfile);

		assertFalse(result.isSuccess());
		assertEquals(ResultType.FAILED, result.getType());
		assertEquals("Failed to copy save directory.", result.getCurrentMessage());
		assertNull(result.getPayload());
	}

	//This is technically an integration test, but it's the best way to implement this.
	@Test
	void shouldFailToChangeSandboxConfigSaveName() throws IOException {
		Result<String> goodResult = new Result<>();
		goodResult.addMessage("This is a good result.", ResultType.SUCCESS);
		goodResult.setPayload(Files.readString(Path.of("src/test/resources/SaveServiceTest/Good_Sandbox_config.sbc")));

		when(sandboxService.getSandboxFromFile(any(File.class))).thenReturn(goodResult);
		doCallRealMethod().when(saveFileRepository).copySave(anyString(), anyString());

		Result<Void> badResult = new Result<>();
		badResult.addMessage("This is a bad result.", ResultType.FAILED);
		when(sandboxService.changeConfigSessionName(anyString(), any(SaveProfile.class), any(int[].class))).thenReturn(badResult);

		Result<SaveProfile> result = saveService.copySaveFiles(saveProfile);

		assertFalse(result.isSuccess());
		assertEquals(ResultType.FAILED, result.getType());
		assertEquals("This is a bad result.", result.getCurrentMessage());
		assertNull(result.getPayload());
		assertTrue(Files.notExists(Path.of(testDir + "_1")));
		assertTrue(Files.exists(testDir));
	}

	@Test
	void shouldFailToChangeSandboxSaveName() throws IOException {
		Result<String> goodSandboxConfigResult = new Result<>();
		goodSandboxConfigResult.addMessage("This is a good Sandbox_config result.", ResultType.SUCCESS);
		goodSandboxConfigResult.setPayload(Files.readString(Path.of("src/test/resources/SaveServiceTest/Good_Sandbox_config.sbc")));

		when(sandboxService.getSandboxFromFile(any(File.class))).thenReturn(goodSandboxConfigResult);
		doCallRealMethod().when(saveFileRepository).copySave(anyString(), anyString());

		Result<Void> goodConfigNameChangeResult = new Result<>();
		goodConfigNameChangeResult.addMessage("This is a test for changing config save name.", ResultType.SUCCESS);

		when(sandboxService.changeConfigSessionName(anyString(), any(SaveProfile.class), any(int[].class))).thenReturn(goodConfigNameChangeResult);

		Result<String> goodSandboxResult = new Result<>();
		goodSandboxResult.addMessage("This is a good Sandbox result.", ResultType.SUCCESS);
		goodSandboxResult.setPayload(Files.readString(Path.of("src/test/resources/SaveServiceTest/Good_Sandbox.sbc")));

		when(sandboxService.getSandboxFromFile(new File(saveProfile.getSavePath() + "_1\\Sandbox.sbc"))).thenReturn(goodSandboxResult);

		Result<Void> badResult = new Result<>();
		badResult.addMessage("This is a bad result.", ResultType.FAILED);
		when(sandboxService.changeSandboxSessionName(any(), any(SaveProfile.class), any(int[].class))).thenReturn(badResult);

		Result<SaveProfile> result = saveService.copySaveFiles(saveProfile);

		assertFalse(result.isSuccess());
		assertEquals(ResultType.FAILED, result.getType());
		assertEquals("This is a bad result.", result.getCurrentMessage());
		assertNull(result.getPayload());
		assertTrue(Files.notExists(Path.of(testDir + "_1")));
		assertTrue(Files.exists(testDir));
	}

	//This is an integration test, but it's the best way to actually test this
	@Test
	void shouldSuccessfullyCopyAndRenameCopiedSave() throws IOException {
		SaveService realSaveService = new SaveService(new SaveFileRepository(), new SandboxService(new SandboxConfigFileRepository()));

		Result<SaveProfile> result = realSaveService.copySaveFiles(saveProfile);
		assertNotNull(result.getPayload());
		SaveProfile finalSaveProfile = result.getPayload();

		assertTrue(Files.exists(Path.of(testDir + "_1")));
		assertEquals(saveProfile.getProfileName() + "_1", finalSaveProfile.getProfileName());
		assertEquals(saveProfile.getSaveName() + "_1", finalSaveProfile.getSaveName());
		assertEquals( testDir + "_1\\Sandbox_config.sbc", finalSaveProfile.getSavePath());

		assertEquals("Save directory successfully copied.", result.getMESSAGES().get(0));
		assertEquals("Successfully copied profile.", result.getMESSAGES().get(1));

		//Check the changes were written to the actual sandbox and sandbox_config file
		assertNotEquals(-1, StringUtils.indexOf(Files.readString(Path.of(testDir + "_1\\Sandbox_config.sbc")), "Test Save_1"));
		assertNotEquals(-1, StringUtils.indexOf(Files.readString(Path.of(testDir + "_1\\Sandbox.sbc")), "Test Save_1"));
	}


	@Test
	void shouldGetSessionNameThatIsFolderName() {
		String sessionNameWithSandbox = saveService.getSessionName("This does not contain a session name", "src\\test\\resources\\SaveServiceTest\\Good_Sandbox_config.sbc");
		assertEquals("SaveServiceTest", sessionNameWithSandbox);

		String sessionNameWithoutSandbox = saveService.getSessionName("This does not contain a session name", "src\\test\\resources\\SaveServiceTest");
		assertEquals("SaveServiceTest", sessionNameWithoutSandbox);
	}

	@Test
	void shouldGetSessionName() throws IOException {
		String sandboxConfig = Files.readString(Path.of("src/test/resources/SaveServiceTest/Good_Sandbox_config.sbc"));
		String sessionName = saveService.getSessionName(sandboxConfig, "path/doesn't/matter/here");
		assertEquals("Test Save", sessionName);

		String sandbox = Files.readString(Path.of("src/test/resources/SaveServiceTest/Good_Sandbox.sbc"));
		String sandboxSessionName  = saveService.getSessionName(sandbox, "path/doesn't/matter/here");
		assertEquals("Test Save", sandboxSessionName);
	}

	private SaveProfile createTestSaveProfile () {
		ModlistProfile testModlistProfile = new ModlistProfile();

		SaveProfile testSaveProfile = new SaveProfile();
		testSaveProfile.setSaveName("Test Save");
		testSaveProfile.setSavePath(testDir.toString() + "\\Sandbox_config.sbc");
		testSaveProfile.setLastUsedModProfileId(testModlistProfile.getID());

		return testSaveProfile;
	}
}
