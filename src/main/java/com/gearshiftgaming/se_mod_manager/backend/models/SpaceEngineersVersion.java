package com.gearshiftgaming.se_mod_manager.backend.models;

public enum SpaceEngineersVersion {
    SPACE_ENGINEERS_ONE("SPACE_ENGINEERS_ONE"),
    SPACE_ENGINEERS_TWO("SPACE_ENGINEERS_TWO");

    private final String name;

    SpaceEngineersVersion(String name) {this.name = name;}

    public static SpaceEngineersVersion fromString(String name) {
        for(SpaceEngineersVersion b : SpaceEngineersVersion.values()) {
            if(b.name.equalsIgnoreCase((name))) {
                return b;
            }
        }
        return null;
    }
}
