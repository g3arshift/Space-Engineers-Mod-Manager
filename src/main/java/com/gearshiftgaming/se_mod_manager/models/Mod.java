package com.gearshiftgaming.se_mod_manager.models;

public class Mod {

    private String friendlyName;
    private final int modId;
    private String publishedServiceName;

    public Mod(int modId) {
        this.modId = modId;
    }

}
