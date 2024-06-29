package com.gearshiftgaming.se_mod_manager.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Mod {

    private String friendlyName;
    private String modId;
    private String publishedServiceName;

    public Mod(String modId) {
        this.modId = modId;
        friendlyName = "UNKNOWN_NAME";
        publishedServiceName = "UNKNOWN_SERVICE";
    }

}
