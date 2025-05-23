package com.gearshiftgaming.se_mod_manager.backend.data.mappers;

import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SpaceEngineersVersion;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModListProfileMapper implements RowMapper<ModListProfile> {
    @Override
    public ModListProfile map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new ModListProfile(UUID.fromString(rs.getString("mod_list_profile_id")),
                rs.getString("profile_name"),
                SpaceEngineersVersion.valueOf(rs.getString("space_engineers_version")));
    }
}
