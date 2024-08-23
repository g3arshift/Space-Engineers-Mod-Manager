package backend.data;

import com.gearshiftgaming.se_mod_manager.backend.data.SaveFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
public class SaveFileRepositoryTest {

	@TempDir
	private File tempDir;

	SaveFileRepository saveFileRepository;

	@BeforeEach
	void setup() {
		saveFileRepository = new SaveFileRepository();
	}

	@Test
	void shouldCopySave() throws IOException {
		Path tempFile = Files.createDirectory(tempDir.toPath().resolve("test_copy_directory"));
		Files.createDirectory(tempDir.toPath().resolve(tempFile + "/Backup"));
		Files.createFile(tempDir.toPath().resolve(tempFile + "/Sandbox_config.sbc"));

		String destinationPath = tempDir.getPath() + "_1";

		saveFileRepository.copySave(tempFile.toString(), destinationPath);

		assertTrue(Files.exists(Path.of(destinationPath)));
		assertTrue(Files.exists(Path.of(destinationPath + "/Sandbox_config.sbc")));
		assertTrue(Files.notExists(Path.of(destinationPath + "/Backup")));
	}
}
