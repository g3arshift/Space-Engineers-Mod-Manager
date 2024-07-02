package com.gearshiftgaming.se_mod_manager.models.utility;

import lombok.Getter;
import org.jsoup.Jsoup;

import java.io.IOException;

@Getter
public class SteamWorkshopConnection {

    private boolean steamWorkshopConnectionActive = false;

    public void checkWorkshopConnectivity(String urlToCheckAgainst, String titleToCompareTo) throws IOException {
        this.steamWorkshopConnectionActive = (Jsoup.connect(urlToCheckAgainst)
                .timeout(5000)
                .get()).title().equals(titleToCompareTo);
    }
}
