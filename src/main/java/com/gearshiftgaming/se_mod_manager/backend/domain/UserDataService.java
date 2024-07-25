package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class UserDataService {
    private UserConfiguration userConfiguration;

    private final UserDataRepository userDataRepository;

    public UserDataService(UserDataRepository userDataRepository) {
        this.userDataRepository = userDataRepository;
    }

    private Result<UserConfiguration> loadUserData(File userConfigurationFile) {
        Result<UserConfiguration> result = new Result<>();
        try {
            JAXBContext context = JAXBContext.newInstance(UserConfiguration.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            UserConfiguration userConfiguration = (UserConfiguration) unmarshaller.unmarshal(userConfigurationFile);
            result.addMessage("Successfully loaded user data.", ResultType.SUCCESS);
            result.setPayload(userConfiguration);
            return result;
        } catch (JAXBException e) {
            result.addMessage("Failed to load user data from file.", ResultType.FAILED);
            return result;
        }
    }

}
