package com.gearshiftgaming.se_mod_manager.backend.models.mod;

import com.gearshiftgaming.se_mod_manager.backend.models.adapters.ModDescriptionCompacterAdapter;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */

@Getter
@NoArgsConstructor
@XmlSeeAlso({SteamMod.class, ModIoMod.class})
public abstract class Mod {
    //These are the fields required for the sandbox_config.sbc file
    //For mod.io mods we have to use an actual API here. https://docs.mod.io/support/search-by-id/
    private String id;
    @Setter
    private String friendlyName;
    @Setter
    private String publishedServiceName;

    private int loadPriority;

    private List<String> categories = new ArrayList<>();

    @Setter
    private boolean active;

    private String description;

    @Setter
    private List<String> modifiedPaths = new ArrayList<>();

    public Mod(String id) {
        this.id = id;
        friendlyName = "UNKNOWN_NAME";
        publishedServiceName = "UNKNOWN_SERVICE";
    }

    //We are intentionally forgoing copying load priority as it is a generated field
    @SuppressWarnings("CopyConstructorMissesField")
    public Mod(Mod mod) {
        this.id = mod.getId();
        this.friendlyName = mod.getFriendlyName();
        this.categories = new ArrayList<>(mod.getCategories());
        this.active = mod.isActive();
        this.description = mod.getDescription();
        this.modifiedPaths = mod.getModifiedPaths();
    }

    public Mod(String id, String friendlyName, String publishedServiceName, int loadPriority, List<String> categories, boolean active, String description) {
        this.id = id;
        this.friendlyName = friendlyName;
        this.publishedServiceName = publishedServiceName;
        this.loadPriority = loadPriority;
        this.categories = categories;
        this.active = active;
        this.description = description;
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
        if (this.categories == null) {
            this.categories = categories;
        } else {
            this.categories.clear();
            for (String category : categories) {
                this.categories.add(category.intern());
            }
        }
    }

    @XmlTransient
    public void setLoadPriority(int loadPriority) {
        this.loadPriority = loadPriority;
    }

    @XmlJavaTypeAdapter(value = ModDescriptionCompacterAdapter.class)
    public void setDescription(String description) {
        this.description = description;
    }
}