package com.gearshiftgaming.se_mod_manager.frontend.view.utility;

import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Applies window dressing icons to a given stage.
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class WindowDressingUtility {

	public static void appendStageIcon(final Stage STAGE) {
		STAGE.getIcons().add(new Image(Objects.requireNonNull(WindowDressingUtility.class.getResourceAsStream("/icons/logo_128.png"))));
		STAGE.getIcons().add(new Image(Objects.requireNonNull(WindowDressingUtility.class.getResourceAsStream("/icons/logo_64.png"))));
		STAGE.getIcons().add(new Image(Objects.requireNonNull(WindowDressingUtility.class.getResourceAsStream("/icons/logo_32.png"))));
		STAGE.getIcons().add(new Image(Objects.requireNonNull(WindowDressingUtility.class.getResourceAsStream("/icons/logo_16.png"))));
	}
}
