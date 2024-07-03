package models;

import com.gearshiftgaming.se_mod_manager.models.utility.SteamWorkshopConnection;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class SteamWorkshopConnectionTest {
    private final String connectionCheckUrl = "https://steamcommunity.com/sharedfiles/filedetails/?id=2135416557";
    private final String connectionCheckTitle = "Steam Workshop::Halo Mod - Weapons";

    @Test
    void shouldGetWorkshopConnection() throws IOException {
        SteamWorkshopConnection steamWorkshopConnection = new SteamWorkshopConnection();
        steamWorkshopConnection.checkWorkshopConnectivity(connectionCheckUrl, connectionCheckTitle);
        assertTrue(steamWorkshopConnection.isSteamWorkshopConnectionActive());
    }

    @Test
    void shouldNotGetWorkshopConnection() throws IOException {
        SteamWorkshopConnection steamWorkshopConnection = new SteamWorkshopConnection();
        steamWorkshopConnection.checkWorkshopConnectivity(connectionCheckUrl + "22", connectionCheckTitle);
        assertFalse(steamWorkshopConnection.isSteamWorkshopConnectionActive());
    }
}
