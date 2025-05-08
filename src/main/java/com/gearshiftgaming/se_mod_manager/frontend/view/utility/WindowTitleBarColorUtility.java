package com.gearshiftgaming.se_mod_manager.frontend.view.utility;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com
 */
public class WindowTitleBarColorUtility {

    private static final Logger log = LoggerFactory.getLogger(WindowTitleBarColorUtility.class);

    /**
     * Sets the Windows title bar appearance based on the application's stylesheet.
     *
     * @param stage The JavaFX Stage whose title bar appearance is to be modified.
     */
    public static void SetWindowsTitleBar(Stage stage) {
        if (Platform.isWindows()) {
            val dwmapi = Dwmapi.INSTANCE;
            WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
            if (hwnd == null) {
                log.error("Failed to find SEMM window. Cannot recolor titlebar.");
                return;
            }

            WinDef.BOOLByReference ref;
            if (!Application.getUserAgentStylesheet().toLowerCase(Locale.ROOT).contains("light")) {
                ref = new WinDef.BOOLByReference(new WinDef.BOOL(true));
            } else {
                ref = new WinDef.BOOLByReference(new WinDef.BOOL(false));
            }

            dwmapi.DwmSetWindowAttribute(hwnd, 20, ref, WinDef.BOOL.SIZE);

            try {
                //We check if we're using Windows 10 because this will make our title text dark mode and the titlebar light mode if we start in a dark mode theme, in Windows 11.
                if (isWindows10()) {
                    //Forces a redraw of the title bar by sending a pair of messages to the window to toggle its active state.
                    final int WM_NCACTIVATE = 0x0086;
                    User32.INSTANCE.SendMessage(hwnd, WM_NCACTIVATE, new WinDef.WPARAM(0), new WinDef.LPARAM(0));
                    User32.INSTANCE.SendMessage(hwnd, WM_NCACTIVATE, new WinDef.WPARAM(1), new WinDef.LPARAM(0));
                }
            } catch (IOException e) {
                log.error(getStackTrace(e));
            }
        } else if (Platform.isLinux()) {
            //TODO: The linux equivalent
            System.out.println("Linux title bar recoloring is not currently supported.");
        }
    }

    private static boolean isWindows10() throws IOException {
        Process process = new ProcessBuilder("cmd.exe", "/c", "ver").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines()
                    .filter(line -> line.contains("Microsoft Windows"))
                    .map(line -> {
                        Matcher m = Pattern.compile("Version \\d+\\.\\d+\\.(\\d+)").matcher(line);
                        return m.find() ? Integer.parseInt(m.group(1)) : -1;
                    })
                    .filter(buildNumber -> buildNumber != -1)
                    .anyMatch(buildNumber -> buildNumber <= 22000);
        }
    }
}