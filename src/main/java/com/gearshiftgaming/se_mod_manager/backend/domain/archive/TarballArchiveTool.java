package com.gearshiftgaming.se_mod_manager.backend.domain.archive;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class TarballArchiveTool implements ArchiveTool {

    public TarballArchiveTool() {
    }

    /**
     * Checks the first two bytes of provided file to see if it's actually a .tar.gz
     *
     * @param tarFile The actual file we want to check
     * @return Returns true if the file is a .tar.gz file, returns false if it is not or if it doesn't exist.
     */
    @Override
    public boolean isSupportedArchive(File tarFile) throws IOException {
        if (!tarFile.exists())
            return false;

        byte[] buffer = new byte[2];
        boolean isTar = false;
        try (InputStream is = new FileInputStream(tarFile)) {
            //.tar.gz signature is 1F 8B
            if (is.read(buffer) == buffer.length) {
                isTar = buffer[0] == (byte) 0x1F &&
                        buffer[1] == (byte) 0x8B;
            }
        }
        return isTar;
    }

    @Override
    public int extractArchive(Path tarGzFilePath, Path destinationPath) throws IOException {
        if (Files.notExists(tarGzFilePath))
            return -1;

        if (Files.notExists(destinationPath))
            return -1;

        int entriesExtracted = 0;
        Path normalizedDestination = destinationPath.toAbsolutePath().normalize();
        try (InputStream fileInput = new FileInputStream(tarGzFilePath.toFile());
             InputStream bufferedInput = new BufferedInputStream(fileInput);
             InputStream gzipInput = new GzipCompressorInputStream(bufferedInput);
             TarArchiveInputStream tarInput = new TarArchiveInputStream(gzipInput)) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextEntry()) != null) {

                Path entryPath = destinationPath.resolve(entry.getName()).normalize();

                if (!entryPath.startsWith(normalizedDestination))
                    throw new IOException("Tar slip! Entry is outside of the target directory: " + entry.getName());

                entriesExtracted = extractEntriesToFile(entry, entryPath, tarInput, entriesExtracted);
            }
        }
        return entriesExtracted;
    }

    //This is TECHNCIALLY duplicated in ZipArchiveTool, but there's not actually a common parent between the entry variables
    private static int extractEntriesToFile(TarArchiveEntry entry, Path entryPath, TarArchiveInputStream tarInput, int entriesExtracted) throws IOException {
        if (entry.isDirectory())
            Files.createDirectories(entryPath);
        else {
            if (entryPath.getParent() != null)
                Files.createDirectories(entryPath.getParent());

            try (OutputStream os = Files.newOutputStream(entryPath)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = tarInput.read(buffer)) > 0)
                    os.write(buffer, 0, len);
            }
        }
        entriesExtracted++;
        return entriesExtracted;
    }
}
