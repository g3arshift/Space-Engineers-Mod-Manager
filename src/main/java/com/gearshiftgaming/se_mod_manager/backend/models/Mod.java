package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Stores the UI and file information required for a single mod
 * @author Gear Shift
 * @version 1.0
 */
@Getter
@Setter
public class Mod {

    //These are the fields required for the sandbox_config.sbc file
    @XmlAttribute
    private final String id;
    private String friendlyName;
    private String publishedServiceName;

    //These are the fields for the UI
    private ModType modType;
    private String modVersion;
    private Date lastUpdated;
    private int loadPriority;
    private ModSourceType source;

    private List<String> categories;

    private boolean active;

    public Mod(String id) {
        this.id = id;
        friendlyName = "UNKNOWN_NAME";
        publishedServiceName = "UNKNOWN_SERVICE";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mod mod)) return false;
        return Objects.equals(id, mod.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @XmlElementWrapper(name = "categories")
    @XmlElement(name = "category")
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}
