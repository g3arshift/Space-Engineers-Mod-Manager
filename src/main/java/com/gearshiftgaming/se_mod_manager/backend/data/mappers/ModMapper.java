package com.gearshiftgaming.se_mod_manager.backend.data.mappers;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModMapper implements RowMapper<Mod> {
    @Override
    public Mod map(ResultSet rs, StatementContext ctx) throws SQLException {
        return null;
    }
}
