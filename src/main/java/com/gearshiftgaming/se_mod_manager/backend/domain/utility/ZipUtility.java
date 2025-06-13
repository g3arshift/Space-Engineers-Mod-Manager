package com.gearshiftgaming.se_mod_manager.backend.domain.utility;

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
public class ZipUtility {

    private ZipUtility(){}

    /**
     * Checks the first four bytes of provided file to see if it's actually a zip file
     * @param zipFile The actual file we want to check
     * @return Returns true if the file is a zip file, returns false if it is not, or if it doesn't exist.
     */
    public static boolean isZip(File zipFile) throws IOException {
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

    /**
     * Extracts a given zip archive to a specified destination. Returns -1 if the destination path or the zip file do not exist.
     * @param zipFilePath The zip archive we want to extract.
     * @param destinationPath The location we want to extract the zip archive to.
     * @return The number of entries extracted from the zip archive.
     */
    public static int extractZipArchive(Path zipFilePath, Path destinationPath) throws IOException {
        if(Files.notExists(zipFilePath))
            return -1;

        if(Files.notExists(destinationPath))
            return -1;

        int entriesExtracted = 0;
        Path normalizedDestination = destinationPath.toAbsolutePath().normalize();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entriesExtracted++;
                Path outputPath = normalizedDestination.resolve(entry.getName()).normalize();

                //Prevent zip slip vulnerability
                if (!outputPath.startsWith(normalizedDestination))
                    throw new IOException("Zip slip! Entry is outside of the target directory: " + entry.getName());

                if (entry.isDirectory())
                    Files.createDirectories(outputPath);
                else {
                    if (outputPath.getParent() != null)
                        Files.createDirectories(outputPath.getParent());

                    try (OutputStream os = Files.newOutputStream(outputPath)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0)
                            os.write(buffer, 0, len);
                    }
                }
            }
            zis.closeEntry();
        }
        return entriesExtracted;
    }
}
