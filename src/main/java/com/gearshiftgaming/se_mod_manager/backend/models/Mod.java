package com.gearshiftgaming.se_mod_manager.backend.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Mod {

    private String friendlyName;
    private final String modId;
    private String publishedServiceName;

    public Mod(String modId) {
        this.modId = modId;
        friendlyName = "UNKNOWN_NAME";
        publishedServiceName = "UNKNOWN_SERVICE";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mod mod)) return false;
        return Objects.equals(modId, mod.modId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId);
    }
}
