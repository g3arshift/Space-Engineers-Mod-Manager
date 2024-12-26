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

	//We can't actually set the stage back to its original size or the refresh doesn't actually set in.
	private static void forceRedrawOfWindow(Stage stage) {
		stage.setHeight(stage.getHeight() + 1);
	}
}