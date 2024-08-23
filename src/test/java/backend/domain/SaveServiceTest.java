package backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.SaveFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
public class SaveServiceTest {

	SaveService service;
	@BeforeEach
	void setup() {
		service = new SaveService(mock(SaveFileRepository.class), mock(SandboxService.class));
	}

	@Test
	void shouldGetInvalidSandboxConfig() {

	}

	@Test
	void shouldGetMissingSessionNameTag() {

	}

	@Test
	void shouldFailToCopySourceSave(){

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
}
