package controller;

import com.gearshiftgaming.se_mod_manager.backend.data.*;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.controller.StorageController;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class StorageControllerTest {
    private SandboxConfigRepository sandboxConfigRepository;

    private UserDataRepository userDataRepository;

    private SaveRepository saveRepository;

    private UserConfiguration userConfiguration;

    private StorageController storageController;

    @TempDir
    private File tempUserConfigFile;

    //TODO: UserDataSqliteRepo integration tests
    @BeforeEach
    void setup() {
        sandboxConfigRepository = mock(SandboxConfigFileRepository.class);
        //userDataRepository = mock(UserDataFileRepository.class);
        saveRepository = mock(SaveRepository.class);
        storageController = new StorageController(sandboxConfigRepository, userDataRepository, saveRepository);
        userConfiguration = new UserConfiguration();

        userConfiguration.setRunFirstTimeSetup(false);
        userConfiguration.setUserTheme("PrimerDark");

        ModListProfile firstProfile = new ModListProfile();
        firstProfile.setProfileName("Test modlist");
        firstProfile.getModList().add(new SteamMod("12345"));
        firstProfile.getModList().add(new SteamMod("67890"));
        firstProfile.getModList().add(new ModIoMod("44444"));

        ModListProfile secondProfile = new ModListProfile();
        secondProfile.setProfileName("Second list");
        secondProfile.getModList().add(new ModIoMod("5544"));
        secondProfile.getModList().add(new SteamMod("1221"));

//        userConfiguration.getModListProfilesBasicInfo().add(firstProfile);
//        userConfiguration.getModListProfilesBasicInfo().add(secondProfile);
    }
//
//    @Test
//    public void shouldSetLoadPriority() throws JAXBException {
//        Result<UserConfiguration> goodResult = new Result<>();
//        goodResult.setPayload(userConfiguration);
//        goodResult.addMessage("Successful.", ResultType.SUCCESS);
//        when(userDataRepository.loadAllData()).thenReturn(goodResult);
//
//        Result<UserConfiguration> returnedResult = storageController.loadStartupData();
//        assertTrue(returnedResult.isSuccess());
//
//        ModListProfile firstModListProfile = userConfiguration.getModListProfilesBasicInfo().get(1);
//        ModListProfile secondModListProfile = userConfiguration.getModListProfilesBasicInfo().getLast();
//
//        assertEquals(3, firstModListProfile.getModList().size());
//        assertEquals(2, secondModListProfile.getModList().size());
//
//        for(int i = 0; i < firstModListProfile.getModList().size(); i++) {
//            assertEquals(i + 1, firstModListProfile.getModList().get(i).getLoadPriority());
//        }
//
//        assertEquals(1, secondModListProfile.getModList().getFirst().getLoadPriority());
//        assertEquals(2, secondModListProfile.getModList().getLast().getLoadPriority());
//    }
}
