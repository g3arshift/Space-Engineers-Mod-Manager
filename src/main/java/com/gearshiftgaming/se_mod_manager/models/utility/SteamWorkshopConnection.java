package com.gearshiftgaming.se_mod_manager.models.utility;

import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

@Getter
public class SteamWorkshopConnection {

    private boolean steamWorkshopConnectionActive = false;

    public void checkWorkshopConnectivity(String urlToCheckAgainst, String titleToCompareTo) throws IOException {
        Document doc = Jsoup.connect(urlToCheckAgainst)
                .timeout(5000)
                .get();
        this.steamWorkshopConnectionActive = doc.title().equals(titleToCompareTo);
    }
}
