package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import jakarta.xml.bind.JAXBException;

import java.io.File;

public interface BackendController {
    Result<UserConfiguration> getUserData(File file) throws JAXBException;
}
