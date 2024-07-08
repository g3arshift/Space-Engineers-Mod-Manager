package models;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.SteamWorkshopConnection;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class SteamWorkshopConnectionTest {

    @Test
    void shouldGetWorkshopConnection() throws IOException {
        SteamWorkshopConnection steamWorkshopConnection = new SteamWorkshopConnection(mock(Logger.class));
        steamWorkshopConnection.checkWorkshopConnectivity();
        assertTrue(steamWorkshopConnection.isSteamWorkshopConnectionActive());
    }

    @Test
    void shouldNotGetWorkshopConnection() throws IOException {
        String badConnectionCheckUrl = "https://steamcommunity.com/sharedfiles/filedetails/?id=2135416552227";
        String connectionCheckTitle = "Steam Workshop::Halo Mod - Weapons";

        SteamWorkshopConnection steamWorkshopConnection = new SteamWorkshopConnection(mock(Logger.class), badConnectionCheckUrl, connectionCheckTitle);
        steamWorkshopConnection.checkWorkshopConnectivity();
        assertFalse(steamWorkshopConnection.isSteamWorkshopConnectionActive());
    }
}
