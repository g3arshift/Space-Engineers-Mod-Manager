package com.gearshiftgaming.se_mod_manager.backend.data.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * Encrypts and compresses any string into base64. Hence, the name, cryptpressor.
 */
public class StringCodepressor {

    public static String decompressAndDecodeString(String input) throws IOException {
        if (input == null || input.isEmpty()) {
            return input;
        }
        byte[] compressedString = Base64.getDecoder().decode(input);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedString);
        StringBuilder output = new StringBuilder();

        try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipInputStream.read(buffer)) != -1) {
                output.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
            }
        }
        return output.toString();
    }

    public static String compressandEncodeString(String input) throws IOException {
        if (input == null || input.isEmpty()) {
            return input;
        }

        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(bytes);
        }

        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }
}
