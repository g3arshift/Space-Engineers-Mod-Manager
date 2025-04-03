package com.gearshiftgaming.se_mod_manager.backend.data.mappers;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveStatus;
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
public class SaveProfileMapper implements RowMapper<SaveProfile> {
    @Override
    public SaveProfile map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new SaveProfile(UUID.fromString(rs.getString("save_profile_id")),
                rs.getString("profile_name"),
                rs.getString("save_name"),
                rs.getString("save_path"),
                rs.getString("last_used_mod_list_profile_id") != null ? UUID.fromString(rs.getString("last_used_mod_list_profile_id")) : null,
                SaveStatus.fromString(rs.getString("last_save_status")),
                rs.getString("last_saved"),
                rs.getInt("save_exists") >= 1,
                SpaceEngineersVersion.valueOf(rs.getString("space_engineers_version")));
    }
}
