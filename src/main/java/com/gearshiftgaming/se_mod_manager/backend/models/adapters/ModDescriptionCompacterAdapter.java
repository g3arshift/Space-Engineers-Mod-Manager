package com.gearshiftgaming.se_mod_manager.backend.models.adapters;

import com.gearshiftgaming.se_mod_manager.backend.data.utility.StringCodepressor;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

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
		return StringCodepressor.decompressAndDecodeString(s);
	}

	//Save
	@Override
	public String marshal(String s) throws Exception {
		return StringCodepressor.compressandEncodeString(s);
	}
}
