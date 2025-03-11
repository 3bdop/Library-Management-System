package main.java.model;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utlis.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminController {

    private Stage stage;
    private String username;

    // FXML-injected controls from AdminView.fxml
    @FXML private TextField newUser;
    @FXML private PasswordField newPass;
    @FXML private Button createBtn;
    @FXML private ComboBox<String> newRole;
    @FXML private Label welcUsername;
    @FXML private Button logout;
    @FXML private ListView<String> listUsers;


    public AdminController(Stage stage, String username){
        this.stage = stage;
        this.username = username;
        loadView();
    }

    private void loadView() {
        try {
            // Adjust the path to match your resource folder structure.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminView.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Library Management System - Admin");
            stage.show();

            welcUsername.setText("Welcome, "+username+"!");

            createBtn.setOnAction(e->handleNewUser());
            logout.setOnAction(e->handleLogout());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        populateRoles();
        handleShowUsers();
    }

    private void populateRoles() {
        newRole.getItems().clear();
        newRole.getItems().addAll("Librarian", "Cataloger");
    }

    @FXML
    private void handleNewUser() {
        String username = newUser.getText();
        String password = newPass.getText();
        String role = newRole.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please fill all fields.");
            return;
        }

        String insertQuery = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DBUtils.establishConnection();
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "User created successfully.");
                clearFields();
                handleShowUsers();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to create user.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearFields() {
        newUser.clear();
        newPass.clear();
        newRole.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleShowUsers() {
        // Clear the list view first
        listUsers.getItems().clear();

        String query = "SELECT username, role FROM users";
        try (Connection conn = DBUtils.establishConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String user = rs.getString("username");
                String role = rs.getString("role");
                // You can format the string as you like
                listUsers.getItems().add(user + " - " + role);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to retrieve users.");
        }
    }


    @FXML
    private void handleLogout() {
        new UserLogin(stage);
    }

}
