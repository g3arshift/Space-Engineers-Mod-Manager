package com.gearshiftgaming.se_mod_manager.backend.domain.archive;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ZipArchiveTool implements ArchiveTool{

    public ZipArchiveTool(){}

    /**
     * Checks the first four bytes of provided file to see if it's actually a zip file
     * @param zipFile The actual file we want to check
     * @return Returns true if the file is a zip file, returns false if it is not or if it doesn't exist.
     */
    @Override
    public boolean isSupportedArchive(File zipFile) throws IOException {
        if(!zipFile.exists())
            return false;

        byte[] buffer = new byte[4];
        boolean isZip = false;
        try (InputStream is = new FileInputStream(zipFile)) {
            //Zip signature is "50 4b 03 04"
            if (is.read(buffer) == buffer.length) {
                isZip = buffer[0] == (byte) 0x50 &&
                        buffer[1] == (byte) 0x4B &&
                        buffer[2] == (byte) 0x03 &&
                        buffer[3] == (byte) 0x04;
            }
        }
        return isZip;
    }

    @Override
    public int extractArchive(Path zipFilePath, Path destinationPath) throws IOException {
        if(Files.notExists(zipFilePath))
            return -1;

        if(Files.notExists(destinationPath))
            return -1;

        int entriesExtracted = 0;
        Path normalizedDestination = destinationPath.toAbsolutePath().normalize();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = normalizedDestination.resolve(entry.getName()).normalize();

                //Prevent zip slip vulnerability
                if (!entryPath.startsWith(normalizedDestination))
                    throw new IOException("Zip slip! Entry is outside of the target directory: " + entry.getName());

                entriesExtracted = extractEntriesToFile(entry, entryPath, zis, entriesExtracted);
            }
            zis.closeEntry();
        }
        return entriesExtracted;
    }

    //This is TECHNCIALLY duplicated in TarballArchiveTool, but there's not actually a common parent between the entry variables
    private static int extractEntriesToFile(ZipEntry entry, Path entryPath, ZipInputStream zis, int entriesExtracted) throws IOException {
        if (entry.isDirectory())
            Files.createDirectories(entryPath);
        else {
            if (entryPath.getParent() != null)
                Files.createDirectories(entryPath.getParent());

            try (OutputStream os = Files.newOutputStream(entryPath)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zis.read(buffer)) > 0)
                    os.write(buffer, 0, len);
            }
        }
        entriesExtracted++;
        return entriesExtracted;
    }
}
