package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
//TODO: Implement file locks
//TODO: Add a lock so that only this application can work on the UserData files. Allow it to access as much as it wants, but prevent outside stuff from writing to it.
public class UserDataFileRepository implements UserDataRepository {
    public Result<UserConfiguration> loadUserData(File userConfigurationFile) {
        Result<UserConfiguration> userConfigurationResult = new Result<>();
        try {
            JAXBContext context = JAXBContext.newInstance(UserConfiguration.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            UserConfiguration userConfiguration = (UserConfiguration) unmarshaller.unmarshal(userConfigurationFile);
            userConfigurationResult.addMessage("Successfully loaded user data.", ResultType.SUCCESS);
            userConfigurationResult.setPayload(userConfiguration);
        } catch (JAXBException f) {
            userConfigurationResult.addMessage("Failed to load user configuration. Error Details: " + f, ResultType.FAILED);
        }
        return userConfigurationResult;
    }

    public boolean saveUserData(UserConfiguration userConfiguration, File userConfigurationFile) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(userConfigurationFile))) {
            JAXBContext context = JAXBContext.newInstance(UserConfiguration.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();

            marshaller.marshal(userConfiguration, sw);
            bw.write(sw.toString());
            return true;
        } catch (JAXBException | IOException e) {
            return false;
        }
    }
}
