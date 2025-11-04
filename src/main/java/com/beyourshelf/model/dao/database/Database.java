package com.beyourshelf.model.dao.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Singleton class for managing the database connection pool using HikariCP.
 * Provides methods for acquiring database connections and managing the
 * connection pool.
 */
public class Database {

    // The URL for the SQLite database
    private static final String DB_URL = "jdbc:sqlite:beyourshelf.db";

    // HikariCP DataSource for managing connections
    private static HikariDataSource dataSource;

    // Singleton instance of the Database class
    private static final Database instance = new Database();

    /**
     * Private constructor to prevent external instantiation.
     * Initializes the connection pool during class instantiation.
     */
    private Database() {
        initializeConnectionPool();
    }

    /**
     * Returns the singleton instance of the Database class.
     * Ensures that only one instance of this class is created and used.
     *
     * @return the singleton instance of Database
     */
    public static Database getInstance() {
        return instance;
    }

    /**
     * Initializes the HikariCP connection pool with the configured settings.
     * This method creates the pool if it hasn't been initialized already.
     */
    private static void initializeConnectionPool() {
        if (dataSource == null) {
            // HikariCP configuration settings
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL); // Set JDBC URL for the database
            config.setMaximumPoolSize(10); // Max number of connections in the pool
            config.setConnectionTimeout(30000); // Max wait time for a connection (30s)
            config.setIdleTimeout(600000); // Max idle time for a connection (10 minutes)
            config.setMaxLifetime(1800000); // Max lifetime for a connection (30 minutes)
            dataSource = new HikariDataSource(config);
        }
    }

    /**
     * Provides a database connection from the HikariCP pool.
     *
     * @return a connection object from the pool
     * @throws SQLException if unable to acquire a connection
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection(); // Retrieve connection from the HikariCP pool
    }
}
