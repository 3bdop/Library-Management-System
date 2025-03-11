package main.java.model;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utlis.DBUtils;
import javafx.scene.control.Alert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserLogin {

    private Stage stage;

    // FXML-injected controls from LoginView.fxml
    @FXML private TextField username;
    @FXML private PasswordField password;
    @FXML private Button loginBtn;

    public UserLogin(Stage stage) {
        this.stage = stage;
        loadView();
    }

    private void loadView() {
        try {
            // Adjust the path to match your resource folder structure.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginView.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Library Management System - Login");
            stage.show();
            loginBtn.setOnAction(e->handleLogin());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // This method should be wired to the login button's onAction in the FXML.
    @FXML
    private void handleLogin() {
        String user = username.getText();
        String pass = password.getText();

        // Validate user input and display an error alert if missing.
        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Login Error");
            alert.setHeaderText("Missing Credentials");
            alert.setContentText("Please enter both username and password.");
            alert.showAndWait();
            return;
        }

        Connection con = DBUtils.establishConnection();
        PreparedStatement ps = null;
        try {
            String query = "SELECT role FROM users WHERE username=? AND password=?";
            ps = con.prepareStatement(query);
            ps.setString(1, user);
            ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                System.out.println("Login successful. Role: " + role);
                // Role-based redirection.
                switch (role.toLowerCase()) {
                    case "admin":
                        System.out.println("Redirecting to Admin Dashboard...");
                        //? load the Admin Dashboard scene.
                        new AdminController(stage, user);
                        break;
                    case "cataloging":
                        System.out.println("Redirecting to Cataloging Dashboard...");
                        // TODO: load the Cataloging Dashboard scene.
                        break;
                    case "librarian":
                        System.out.println("Redirecting to Librarian Dashboard...");
                        // TODO: load the Librarian Dashboard scene.
                        break;
                    default:
                        System.out.println("Unknown role. Access denied.");
                        break;
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Error");
                alert.setHeaderText("Invalid Credentials");
                alert.setContentText("The username or password is incorrect.");
                alert.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(con, ps);
        }
    }

}
