package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import jakarta.xml.bind.JAXBException;

import java.io.File;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
public interface UserDataRepository {
    UserConfiguration loadUserData(File userConfigurationFile) throws JAXBException;

    boolean saveUserData(UserConfiguration userConfiguration, File userConfigurationFile);
}
