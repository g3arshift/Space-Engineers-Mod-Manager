package com.gearshiftgaming.se_mod_manager.backend.data.mappers;

import com.gearshiftgaming.se_mod_manager.backend.data.utility.StringCodepressor;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.ModDownloadStatus;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.ModIoMod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.SteamMod;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
//Note, the DB stores steam times as epoch.
public class ModMapper implements RowMapper<Mod> {
    @Override
    public Mod map(ResultSet rs, StatementContext ctx) throws SQLException {
        Mod mod;
        try {
            if (rs.getString("published_service_name").equalsIgnoreCase("Steam")) {
                mod = new SteamMod(rs.getString("mod_id"),
                        rs.getString("friendly_name"),
                        rs.getString("published_service_name"),
                        rs.getInt("load_priority"),
                        rs.getString("categories") != null ? Arrays.asList(rs.getString("categories").split(",")) :List.of("UNKNOWN"),
                        rs.getInt("active") >= 1,
                        StringCodepressor.decompressAndDecodeString(rs.getString("description")),
                        rs.getString("steam_mod_last_updated") != null ?
                                Instant.ofEpochMilli(Long.parseLong(rs.getString("steam_mod_last_updated"))).atZone(ZoneId.systemDefault()).toLocalDateTime() :
                        LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC),
                        rs.getString("download_status") != null ? ModDownloadStatus.fromString(rs.getString("download_status")) : ModDownloadStatus.UNSTARTED);

            } else {
                mod = new ModIoMod(rs.getString("mod_id"),
                        rs.getString("friendly_name"),
                        rs.getString("published_service_name"),
                        rs.getInt("load_priority"),
                        rs.getString("categories") != null ? Arrays.asList(rs.getString("categories").split(",")) :List.of("UNKNOWN"),
                        rs.getInt("active") >= 1,
                        StringCodepressor.decompressAndDecodeString(rs.getString("description")),
                        rs.getString("last_updated_year") != null ? Year.parse(rs.getString("last_updated_year")) : Year.parse("1970"),
                        rs.getString("last_updated_month_day") != null ? MonthDay.parse(rs.getString("last_updated_month_day")) : null,
                        rs.getString("last_updated_hour") != null ? LocalTime.parse(rs.getString("last_updated_hour")) : null,
                        rs.getString("download_status") != null ? ModDownloadStatus.fromString(rs.getString("download_status")) : ModDownloadStatus.UNSTARTED);
            }
            return mod;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
