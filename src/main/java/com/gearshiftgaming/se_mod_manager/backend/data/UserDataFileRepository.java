package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import jakarta.xml.bind.*;

import java.io.*;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
//TODO: Implement file locks
public class UserDataFileRepository implements UserDataRepository {
    public UserConfiguration loadUserData(File userConfigurationFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(UserConfiguration.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (UserConfiguration) unmarshaller.unmarshal(userConfigurationFile);
        } catch (JAXBException f) {
            return new UserConfiguration();
        }
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
