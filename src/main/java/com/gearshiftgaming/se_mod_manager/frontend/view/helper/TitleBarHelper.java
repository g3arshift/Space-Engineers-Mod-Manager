package com.gearshiftgaming.se_mod_manager.frontend.view.helper;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import javafx.stage.Stage;
import javafx.stage.Window;
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

	public static WinDef.HWND getNativeHandleForStage(Stage stage) {
		try {
			val getPeer = Window.class.getDeclaredMethod("getPeer", null);
			getPeer.setAccessible(true);
			val tkStage = getPeer.invoke(stage);
			val getRawHandle = tkStage.getClass().getMethod("getRawHandle");
			getRawHandle.setAccessible(true);
			val pointer = new Pointer((Long) getRawHandle.invoke(tkStage));
			return new WinDef.HWND(pointer);
		} catch (Exception ex) {
			System.err.println("Unable to determine native handle for window");
			return null;
		}
	}

	public static void setDarkMode(Stage stage, boolean darkMode) {
		val hwnd = TitleBarHelper.getNativeHandleForStage(stage);
		val dwmapi = Dwmapi.INSTANCE;
		WinDef.BOOLByReference darkModeRef = new WinDef.BOOLByReference(new WinDef.BOOL(darkMode));

		dwmapi.DwmSetWindowAttribute(hwnd, 20, darkModeRef, Native.getNativeSize(WinDef.BOOLByReference.class));

		forceRedrawOfWindowTitleBar(stage);
	}

	private static void forceRedrawOfWindowTitleBar(Stage stage) {
		val maximized = stage.isMaximized();
		stage.setMaximized(!maximized);
		stage.setMaximized(maximized);
	}

}
