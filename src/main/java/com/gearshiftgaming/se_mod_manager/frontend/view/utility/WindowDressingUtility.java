package com.gearshiftgaming.se_mod_manager.frontend.view.utility;

import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.*;

/**
 * Applies window dressing icons to a given stage.
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class WindowDressingUtility {

	private static final List<Image> ICONS;

	static {
		List<Image> icons = new ArrayList<>();
		for (int size : new int[]{128, 64, 32, 16}) {
			String path = "/icons/logo_" + size + ".png";
			try (InputStream is = WindowDressingUtility.class.getResourceAsStream(path)) {
				icons.add(new Image(Objects.requireNonNull(is, "Icon not found: " + path)));
			} catch (Exception e) {
				throw new RuntimeException("Failed to load icon: " + path, e);
			}
		}

		ICONS = Collections.unmodifiableList(icons);
	}

	public static void appendStageIcon(final Stage STAGE) {
		STAGE.getIcons().addAll(ICONS);
	}
}
