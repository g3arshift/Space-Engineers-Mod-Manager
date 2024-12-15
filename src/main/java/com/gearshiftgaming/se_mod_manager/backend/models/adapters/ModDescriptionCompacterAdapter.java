package com.gearshiftgaming.se_mod_manager.backend.models.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModDescriptionCompacterAdapter extends XmlAdapter<String, String> {

	//Load
	@Override
	public String unmarshal(String s) throws Exception {
		if (s == null || s.isEmpty()) {
			return s;
		}

		byte[] compressedString = Base64.getDecoder().decode(s);

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedString);
		StringBuilder output = new StringBuilder();

		try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
			byte[] buffer = new byte[1024];
			int len;
			while ((len = gzipInputStream.read(buffer)) != -1) {
				output.append(new String(buffer, 0, len, "UTF-8"));
			}
		}

		return  output.toString();
	}

	//Save
	@Override
	public String marshal(String s) throws Exception {
		if (s == null || s.isEmpty()) {
			return s;
		}

		byte[] input = s.getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
			gzipOutputStream.write(input);
		}

		return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
	}
}
