package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import jakarta.xml.bind.*;

import java.io.*;

/**
 * Loads and saves user configuration data to the filesystem using JAXB.
 *
 * @author Gear Shift
 * @version 1.0
 */
//TODO: Implement file locks
public class UserDataFileRepository implements UserDataRepository {
    public UserConfiguration loadUserData(File userConfigurationFile) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(UserConfiguration.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (UserConfiguration) unmarshaller.unmarshal(userConfigurationFile);
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
