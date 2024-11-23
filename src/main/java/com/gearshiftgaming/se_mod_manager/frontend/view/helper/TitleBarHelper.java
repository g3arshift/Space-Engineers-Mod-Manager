package com.gearshiftgaming.se_mod_manager.frontend.view.helper;


import com.gearshiftgaming.se_mod_manager.frontend.view.win_native.CustomDecorationWindowProc;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.scene.paint.Color;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
//TODO: Class is junk, read this https://medium.com/swlh/customizing-the-title-bar-of-an-application-window-50a4ac3ed27e
public class TitleBarHelper {

	private final CustomDecorationWindowProc windowProcEx;

	public TitleBarHelper(String color, String title) {
		windowProcEx = new CustomDecorationWindowProc();
		Color.web(color);

		//This can only run AFTER the stage exists.
		WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, title);

		windowProcEx.init(hwnd);
	}
}