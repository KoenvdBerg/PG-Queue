package org.pg.queue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class PG {

    public static Connection connect() throws SQLException {
        final String url = "jdbc:postgresql://localhost:5432/postgres";
        final Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "docker");
        return DriverManager.getConnection(url, props);
    }
}


