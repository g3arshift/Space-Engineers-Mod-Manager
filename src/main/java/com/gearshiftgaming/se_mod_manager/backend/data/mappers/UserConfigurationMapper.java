package com.gearshiftgaming.se_mod_manager.backend.data.mappers;

import com.gearshiftgaming.se_mod_manager.backend.models.ApplicationMode;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
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
public class UserConfigurationMapper implements RowMapper<UserConfiguration> {
    @Override
    public UserConfiguration map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new UserConfiguration(rs.getString("user_theme"),
                rs.getString("last_modified_save_profile_id") != null ? UUID.fromString(rs.getString("last_modified_save_profile_id")) : null,
                rs.getString("last_active_mod_profile_id") != null ? UUID.fromString(rs.getString("last_active_mod_profile_id")) : null,
                rs.getString("last_active_save_profile_id") != null ? UUID.fromString(rs.getString("last_active_save_profile_id")) : null,
                rs.getBoolean("run_first_time_setup"),
                rs.getString("application_mode") != null ?ApplicationMode.valueOf(rs.getString("application_mode")) : null);
    }
}
