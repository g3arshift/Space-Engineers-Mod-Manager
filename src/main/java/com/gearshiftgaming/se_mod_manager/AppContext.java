package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.operatingsystem.OperatingSystemVersion;
import lombok.Getter;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public record AppContext (OperatingSystemVersion operatingSystemVersion){

    public boolean isWindows() {
        return (operatingSystemVersion == OperatingSystemVersion.WINDOWS_10 || operatingSystemVersion == OperatingSystemVersion.WINDOWS_11);
    }

    public boolean isWindows10() {
        return operatingSystemVersion == OperatingSystemVersion.WINDOWS_10;
    }

    public boolean isWindows11() {
        return operatingSystemVersion == OperatingSystemVersion.WINDOWS_11;
    }

    public boolean isLinux() {
        return operatingSystemVersion == OperatingSystemVersion.LINUX;
    }

}
