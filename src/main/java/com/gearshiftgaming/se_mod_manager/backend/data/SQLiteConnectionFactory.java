package com.gearshiftgaming.se_mod_manager.backend.data;

import org.jdbi.v3.core.ConnectionFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SQLiteConnectionFactory implements ConnectionFactory {
    private final String jdbcUrl;

    public SQLiteConnectionFactory(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    @Override
    public Connection openConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement enableForeignKeys = conn.createStatement()) {
            enableForeignKeys.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }
}
