package com.eit.automation.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Utility class for Database Operations.
 * Designed to be zero-maintenance: it pulls credentials from the
 * configuration properties loaded at runtime.
 */
public class DatabaseUtils {

    /**
     * Executes a SQL command (DELETE/UPDATE) using the configuration loaded in Main.
     * @param sql    The SQL query to execute.
     * @param config The Properties object already loaded in your Main class.
     * @throws Exception if database config is missing or SQL is unsafe.
     */
    public static void executeCleanup(String sql, Properties config) throws Exception {

        // 1. DYNAMIC CREDENTIAL FETCHING
        String url = config.getProperty("db.url");
        String user = config.getProperty("db.user");
        String pass = config.getProperty("db.password");

        // Null check to prevent NullPointerException if properties are missing
        if (url == null || user == null || pass == null) {
            throw new Exception("❌ Database Configuration Error: 'db.url', 'db.user', or 'db.password' missing in properties file.");
        }

        // 2. SAFETY FIREWALL
        // Prevents execution of DELETE or UPDATE statements that lack a WHERE clause
        String normalizedSql = sql.trim().toLowerCase();
        if (normalizedSql.startsWith("delete") || normalizedSql.startsWith("update")) {
            if (!normalizedSql.contains("where")) {
                throw new Exception("🛑 ACCIDENTAL DELETION PREVENTED: SQL command missing 'WHERE' clause. Query: " + sql);
            }
        }

        // 3. DATABASE EXECUTION
        // Uses try-with-resources to ensure Connection and Statement are closed automatically
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {

            System.out.println("🗄️ Database Action: " + sql);
            int affectedRows = stmt.executeUpdate(sql);
            System.out.println("✅ Operation Successful. Rows affected: " + affectedRows);

        } catch (SQLException e) {
            throw new Exception("❌ JDBC Error: " + e.getMessage());
        }
    }

    /**
     * Executes a SELECT query and returns the first column text value of the first record found.
     * Highly optimized for extracting runtime codes like dynamic OTPs and alphanumeric referrals.
     * * @param sql    The SELECT query to execute.
     * @param config The Properties object already loaded in your Main class.
     * @return String value fetched from the database, or null if no records found.
     * @throws Exception if database config is missing or SQL errors occur.
     */
    public static String executeQueryAndFetchValue(String sql, Properties config) throws Exception {

        // 1. DYNAMIC CREDENTIAL FETCHING
        String url = config.getProperty("db.url");
        String user = config.getProperty("db.user");
        String pass = config.getProperty("db.password");

        if (url == null || user == null || pass == null) {
            throw new Exception("❌ Database Configuration Error: 'db.url', 'db.user', or 'db.password' missing in properties file.");
        }

        // 2. QUERY VALIDATION FIREWALL
        String normalizedSql = sql.trim().toLowerCase();
        if (!normalizedSql.startsWith("select")) {
            throw new Exception("🛑 INVALID OPERATION: executeQueryAndFetchValue can only process 'SELECT' queries. Query: " + sql);
        }

        // 3. DATABASE EXECUTION & DATA FETCHING
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("🗄️ Querying Database: " + sql);
            if (rs.next()) {
                String fetchedValue = rs.getString(1); // Pulls data out of the first selected column
                return (fetchedValue != null) ? fetchedValue.trim() : "";
            } else {
                System.out.println("⚠️ Database returned no records for this query.");
                return null;
            }
        } catch (SQLException e) {
            throw new Exception("❌ JDBC Selection Error: " + e.getMessage());
        }
    }
}