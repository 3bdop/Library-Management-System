package main.java.model;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import utlis.DBUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
    @FXML private Text userWelcome;
    @FXML private Button logout;
    @FXML private ListView<String> listUsers;

    String algorithm = "SHA-256";
    byte[] salt = createSalt();

    public AdminController(Stage stage, String username){
        this.stage = stage;
        this.username = username;
        loadView();
    }

    private void loadView() {
        try {
            // Adjust the path to match your resource folder structure.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/resources/AdminView.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Library Management System - Admin");
            stage.show();

            userWelcome.setText("Welcome, "+username+"!");

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

        if(!isValidUsername(username)){
            showAlert(Alert.AlertType.ERROR,"Invalid Username", "Usernames must be all lowercase and characters between 3 and 7");
            return;
        }

        String checkUsername = "SELECT username FROM users where username=?";
        try(
        Connection conn = DBUtils.establishConnection();
        PreparedStatement ps = conn.prepareStatement(checkUsername)){
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                showAlert(Alert.AlertType.ERROR, "Invalid Username", "Username already exists, must be unique user.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to create user.");
        }

        String insertQuery = "INSERT INTO users (username, password, salt, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBUtils.establishConnection();
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
            //? -------------------------Storing the hashed password and generated salt-------------------------
            stmt.setString(1, username);
            stmt.setString(2, generateHash(password, algorithm, salt));
            stmt.setString(3, bytesToStringHex(salt));
            stmt.setString(4, role);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "User created successfully.");
                clearFields();
                handleShowUsers();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to create user.");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
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
//?---------------------------Password Hashing and Salting--------------------------------------
    private static String generateHash(String data, String algorithm, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.reset();
        digest.update(salt);
        byte[] hash = digest.digest(data.getBytes());
        return bytesToStringHex(hash);
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToStringHex(byte[] bytes){
        char[] hexChars = new char[bytes.length *  2];
        for(int j = 0; j < bytes.length; j++){
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] createSalt(){
        byte[] bytes = new byte[5];
        SecureRandom rand = new SecureRandom();
        rand.nextBytes(bytes);
        return bytes;
    }


    private boolean isValidUsername(String username) {
        return username.matches("^[a-z ]{3,7}$") && username.length() < 8;
    }

    @FXML
    private void handleLogout() {
        new UserLogin(stage);
    }

}
