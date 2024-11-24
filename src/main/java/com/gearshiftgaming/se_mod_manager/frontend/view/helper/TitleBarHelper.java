package com.gearshiftgaming.se_mod_manager.frontend.view.helper;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.stage.Stage;
import lombok.val;
import org.apache.commons.lang3.SystemUtils;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
//TODO: Class is junk, read this https://medium.com/swlh/customizing-the-title-bar-of-an-application-window-50a4ac3ed27e
public class TitleBarHelper {

	public TitleBarHelper(String title, Stage stage) {
		val dwmapi = Dwmapi.INSTANCE;
		WinDef.BOOLByReference ref = new WinDef.BOOLByReference(new WinDef.BOOL(true));

		dwmapi.DwmSetWindowAttribute(User32.INSTANCE.FindWindow(null, title), 20, ref, WinDef.BOOL.SIZE);



		forceRedrawOfWindowTitleBar(stage);
	}

	private static void forceRedrawOfWindowTitleBar(Stage stage) {
		val maximized = stage.isMaximized();
		stage.setMaximized(!maximized);
		stage.setMaximized(maximized);
	}
}