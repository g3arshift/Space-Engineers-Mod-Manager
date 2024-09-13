package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */

//TODO: Change this into an abstract, and remove the field for modDownloadSourceType. Instead, make two subclasses for steam and modio mods.
@Getter
@Setter
@NoArgsConstructor
public abstract class Mod {

    //These are the fields required for the sandbox_config.sbc file
    private String id;
    private String friendlyName;
    private String publishedServiceName;

    //These are the fields for the UI
    private String modVersion;
    private Date lastUpdated;
    private int loadPriority;
    private ModImportSourceType source;

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

    @XmlAttribute
    public void setId(String id) {
        this.id = id;
    }

    @XmlElementWrapper(name = "categories")
    @XmlElement(name = "category")
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}
