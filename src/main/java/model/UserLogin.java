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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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

    String algorithm = "SHA-256";
    byte[] salt = createSalt();

    public UserLogin(Stage stage) {
        this.stage = stage;
        loadView();
    }

    private void loadView() {
        try {
            // Adjust the path to match your resource folder structure.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/resources/LoginView.fxml"));
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
        String query = "SELECT password, salt, role FROM users WHERE username=?";
        try {
            ps = con.prepareStatement(query);
            ps.setString(1, user);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                //? getting the stored hashed pass with the salt
                String storedHash = rs.getString("password");
                String storedSaltHex = rs.getString("salt");
                //? converting the stored salt back into byte array
                //? and generating the users input hashed pass
                byte[] storedSalt = hexStringToByteArray(storedSaltHex);
                String inputHash = generateHash(pass, algorithm, storedSalt);

                if(storedHash.equals(inputHash)) { //? validating the stored hash with the input hash
                    String role = rs.getString("role");
                    System.out.println("Login successful. Role: " + role);
                    // Role-based redirection.
                    switch (role.toLowerCase()) {
                        case "admin":
                            System.out.println("Redirecting to Admin Dashboard...");
                            //? load the Admin Dashboard scene.
                            new AdminController(stage, user);
                            break;
                        case "cataloger":
                            System.out.println("Redirecting to Cataloging Dashboard...");
                            new CatalogStaffController(stage);
                            break;
                        case "librarian":
                            System.out.println("Redirecting to Librarian Dashboard...");
                            new LibrarianStaffController(stage);
                            break;
                        default:
                            System.out.println("Unknown role. Access denied.");
                            break;
                    }
                }
                 else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Login Error");
                    alert.setHeaderText("Invalid Credentials");
                    alert.setContentText("The username or password is incorrect.");
                    alert.showAndWait();
                }
            }  else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Error");
                alert.setHeaderText("Invalid Credentials");
                alert.setContentText("The username or password is incorrect.");
                alert.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } finally {
            DBUtils.closeConnection(con, ps);
        }
    }

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

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

}
