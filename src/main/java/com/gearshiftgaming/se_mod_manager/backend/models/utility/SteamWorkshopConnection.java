package com.gearshiftgaming.se_mod_manager.backend.models.utility;

import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
public class SteamWorkshopConnection {

    private final String CONNECTION_CHECK_URL;
    private final String CONNECTION_CHECK_TITLE;

    private boolean steamWorkshopConnectionActive = false;

    public SteamWorkshopConnection(Logger logger) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
            properties.load(input);
        } catch (IOException e) {
            logger.error("Could not load SEMM.properties.");
        }
        this.CONNECTION_CHECK_URL = properties.getProperty("semm.connectionCheck.steam.url");
        this.CONNECTION_CHECK_TITLE = properties.getProperty("semm.connectionCheck.steam.title");
    }

    public SteamWorkshopConnection(Logger logger, String connectionCheckUrl, String connectionCheckTitle){
        this.CONNECTION_CHECK_URL = connectionCheckUrl;
        this.CONNECTION_CHECK_TITLE = connectionCheckTitle;
    }

    public void checkWorkshopConnectivity() throws IOException {
        this.steamWorkshopConnectionActive = (Jsoup.connect(CONNECTION_CHECK_URL)
                .timeout(5000)
                .get()).title().equals(CONNECTION_CHECK_TITLE);
    }
}