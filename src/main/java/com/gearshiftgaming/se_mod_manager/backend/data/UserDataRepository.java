package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import jakarta.xml.bind.JAXBException;

import java.io.File;

public interface UserDataRepository {
    UserConfiguration loadUserData(File userConfigurationFile) throws JAXBException;

    boolean saveUserData(UserConfiguration userConfiguration, File userConfigurationFile);
}
