package net.darho;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    
    private static final String URL = "jdbc:postgresql://ep-lively-unit-ags2i95j-pooler.c-2.eu-central-1.aws.neon.tech/neondb?sslmode=require";
    private static final String USER = "neondb_owner";
    private static final String PASSWORD = "npg_KZLubHef8xw0";

    public static Connection getConnection() throws SQLException {
        try{
            Connection neonDB = DriverManager.getConnection(URL, USER, PASSWORD);
            return neonDB;
        }
        catch (SQLException e){
            SQLException DBFailed = new SQLException("Database failed", e);
            throw DBFailed;
        }

    }
}
