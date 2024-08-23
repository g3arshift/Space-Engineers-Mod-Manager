package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.SaveFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SaveService;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

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
public class SaveServiceTest {

	//TODO: Write integration tests.

	@TempDir
	private File tempDir;

	SaveService saveService;

	SaveFileRepository saveFileRepository;

	SandboxService sandboxService;

	Path testDir;


	@BeforeEach
	void setup() throws IOException {
		saveFileRepository = mock(SaveFileRepository.class);
		sandboxService = mock(SandboxService.class);
		saveService = new SaveService(saveFileRepository, sandboxService);

		testDir = Files.createDirectory(tempDir.toPath().resolve("test_copy_directory"));
		Files.createDirectory(tempDir.toPath().resolve(testDir + "/Backup"));
		Files.copy(Path.of("src/test/resources/SaveServiceTest/Good_Sandbox_config.sbc"), Path.of(testDir + "/Sandbox_config.sbc"));
		Files.copy(Path.of("src/test/resources/SaveServiceTest/Good_Sandbox.sbc"), Path.of(testDir + "/Sandbox.sbc"));
	}

	@Test
	void shouldGetInvalidSandboxConfig() throws IOException {
		Result<String> badResult = new Result<>();
		SaveProfile saveProfile = createTestSaveProfile();
		badResult.addMessage("This is a bad result.", ResultType.FAILED);
		when(sandboxService.getSandboxFromFile(any(File.class))).thenReturn(badResult);

		Result<SaveProfile> result = saveService.copySaveFiles(saveProfile);
		assertFalse(result.isSuccess());
		assertEquals("This is a bad result.", result.getCurrentMessage());
		assertNull(result.getPayload());
	}

	@Test
	void shouldGetMissingSessionNameTag() {

	}

	@Test
	void shouldFailToCopySourceSave() {

	}

	@Test
	void shouldFailToChangeSandboxConfigSaveName() {

	}

	@Test
	void shouldFailToChangeSandboxSaveName() {

	}

	@Test
	void shouldSuccessfullyCopyAndRenameCopiedSave() {

	}

	@Test
	void shouldSucceedAndAppendNewPostfixToDuplicateSavePath() {

	}

	@Test
	void shouldGetSessionNameThatIsFolderName() {

	}

	@Test
	void shouldGetSessionName() {

	}

	private SaveProfile createTestSaveProfile () {
		ModProfile testModProfile = new ModProfile();

		SaveProfile testSaveProfile = new SaveProfile();
		testSaveProfile.setSaveName("Test Save");
		testSaveProfile.setSavePath(testDir.toString());
		testSaveProfile.setLastAppliedModProfileId(testModProfile.getId());

		return testSaveProfile;
	}
}
