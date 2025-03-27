package utlis;

import java.sql.*;

public class DBUtils {
    private static String url = "jdbc:mysql://localhost:3306/dacsproject";

    //? created this user with minimal control (SELECT, INSERT, UPDATE, FILE)
    private static String appUsername = "root";
    private static String appPassword = "";

    public static Connection establishConnection(){
        Connection con = null;
        try{            
            con = DriverManager.getConnection(url, appUsername, appPassword);
            System.out.println("Connection Successful");
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
        return con;
    }
    public static void closeConnection(Connection con,Statement stmt){
        try{
            if (stmt != null) stmt.close();
            if (con != null) con.close();
            System.out.println("Connection is closed");        
        }catch(SQLException e){
            e.getMessage();
        }
    }
}

