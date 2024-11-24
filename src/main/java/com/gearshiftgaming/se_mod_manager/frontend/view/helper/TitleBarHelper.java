package com.gearshiftgaming.se_mod_manager.frontend.view.helper;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.stage.Stage;
import lombok.val;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
//TODO: Class is junk, read this https://medium.com/swlh/customizing-the-title-bar-of-an-application-window-50a4ac3ed27e
public class TitleBarHelper {

	public static void SetTitleBar(Stage stage, String theme) {
		if (Platform.isWindows()) {
			val dwmapi = Dwmapi.INSTANCE;
			WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
			WinDef.BOOLByReference ref;
			if (!theme.contains("Light")) {
				ref = new WinDef.BOOLByReference(new WinDef.BOOL(true));
			} else {
				ref = new WinDef.BOOLByReference(new WinDef.BOOL(false));
			}

			dwmapi.DwmSetWindowAttribute(hwnd, 20, ref, WinDef.BOOL.SIZE);
			forceRedrawOfWindowTitleBar(stage);
		}
	}

	private static void forceRedrawOfWindowTitleBar(Stage stage) {
		stage.setIconified(true);
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		stage.setIconified(false);
	}
}