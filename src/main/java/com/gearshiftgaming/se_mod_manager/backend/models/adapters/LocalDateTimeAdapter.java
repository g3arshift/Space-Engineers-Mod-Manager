package com.gearshiftgaming.se_mod_manager.backend.models.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {
	private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.appendPattern("MMM dd',' yyyy '@' h:mma")
			.toFormatter();
	@Override
	public LocalDateTime unmarshal(String v) throws Exception {
		try {
			return LocalDateTime.parse(v, formatter);
		} catch (DateTimeParseException e) {
			throw new Exception("Invalid date format: " + v, e);
		}
	}

	@Override
	public String marshal(LocalDateTime v) {
		return v != null ? v.format(formatter) : null;
	}
}
