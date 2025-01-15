package com.gearshiftgaming.se_mod_manager.frontend.view.utility;

/// Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
/// You may use, distribute, and modify this code under the terms of the GPL3 license.
///
/// You should have received a copy of the GPL3 license with
/// this file. If not, please write to: gearshift@gearshiftgaming.com.
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.PointerType;
import com.sun.jna.platform.win32.WinDef;

public interface Dwmapi extends Library {

	Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);

	void DwmSetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, PointerType pvAttribute, int cbAttribute);
}
