package com.gearshiftgaming.se_mod_manager.backend.domain.archive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public interface ArchiveTool {

    /**
     * Checks the first few bytes of the provided file to see if it is the type we expect.
     * @param file The actual file we want to check
     * @return Returns true if the file matches the expected signature, returns false if it is not or if it doesn't exist.
     */
    boolean isSupportedArchive(File file) throws IOException;

    /**
     * Extracts a given archive to a specified destination. Returns -1 if the destination path or the file does not exist.
     * @param archiveFilePath The archive we want to extract.
     * @param destinationPath The location we want to extract the archive to.
     * @return The number of entries extracted from the archive.
     */
    int extractArchive(Path archiveFilePath, Path destinationPath) throws IOException;
}
