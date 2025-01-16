package com.gearshiftgaming.se_mod_manager.frontend.view.utility;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.val;

import java.util.Locale;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class NativeWindowUtility {

	/**
	 * Sets the Windows title bar appearance based on the application's stylesheet.
	 * @param stage The JavaFX Stage whose title bar appearance is to be modified.
	 */
	public static void SetWindowsTitleBar(Stage stage) {
		if (Platform.isWindows()) {
			val dwmapi = Dwmapi.INSTANCE;
			WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
			WinDef.BOOLByReference ref;
			if (!Application.getUserAgentStylesheet().toLowerCase(Locale.ROOT).contains("light")) {
				ref = new WinDef.BOOLByReference(new WinDef.BOOL(true));
			} else {
				ref = new WinDef.BOOLByReference(new WinDef.BOOL(false));
			}

			dwmapi.DwmSetWindowAttribute(hwnd, 20, ref, WinDef.BOOL.SIZE);
			forceRedrawOfWindow(stage);
		} else if(Platform.isLinux()) {
			//TODO: The linux equivalent
		}
	}

	/**
	 * Forces a redraw of the window by slightly adjusting its height.
	 * This is necessary to ensure the appearance changes take effect.
	 * @param stage The JavaFX Stage to be redrawn.
	 */
	private static void forceRedrawOfWindow(Stage stage) {
		stage.setHeight(stage.getHeight() + 1);
	}
}