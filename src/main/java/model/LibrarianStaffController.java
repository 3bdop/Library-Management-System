package main.java.model;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import utlis.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class LibrarianStaffController {
    private Stage stage; // Declare the stage variable

    @FXML private ListView<String> booksList;
    @FXML private TextField author;
    @FXML private TextField book;
    @FXML private TextField isbn;
    @FXML private TextField category;
    @FXML private TextField year;
    @FXML private TextField search;
    @FXML private Button logout;

    // Constructor that accepts a Stage parameter
    public LibrarianStaffController(Stage stage) {
        this.stage = stage; // Initialize the stage variable
        loadView(); // Load the view after initializing the stage
    }


    @FXML
    protected void searchBook() {
        String searchText = search.getText().strip().toLowerCase();
        if (searchText.length() >= 3) {
            ObservableList<String> filteredBooks = FXCollections.observableArrayList();
            for (String book : booksList.getItems()) {
                if (book.toLowerCase().contains(searchText)) {
                    filteredBooks.add(book);
                }
            }
            booksList.setItems(filteredBooks);
        } else {
            loadBooks();
        }
    }
    // Method to load the view
    private void loadView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/resources/LibrarianView.fxml"));
            loader.setController(this); // Set this class as the controller
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Library Management System - Librarian Staff");
            logout.setOnAction(e->handleLogout());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @FXML
    public void initialize() {
        loadBooks();
    }

    private void loadBooks() {
        ObservableList<String> books = FXCollections.observableArrayList();
        Connection con = DBUtils.establishConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String query = "SELECT * FROM books";
            ps = con.prepareStatement(query);
            rs = ps.executeQuery();
            while (rs.next()) {
                String author = rs.getString("author");
                String title = rs.getString("title");
                String isbn = rs.getString("isbn");
                String category = rs.getString("category");
                String year = rs.getString("published_year");
                books.add(author + "; " + title + "; " + isbn + "; " + category + "; " + year);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(con, ps);
        }
        booksList.setItems(books);
    }



    @FXML
    protected void loanBook() {
        String selectedBook = booksList.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            // Parse the selected book details
            String[] bookDetails = selectedBook.split(";");
            String oldIsbn = bookDetails[2].trim();

            // Create a custom dialog to get loan details
            Dialog<List<String>> dialog = new Dialog<>();
            dialog.setTitle("Loan Book");
            dialog.setHeaderText("Enter Customer Info and Loan Details\n(Note: Fine of 5QR per day late)");

            // Set the button types.
            ButtonType loanButtonType = new ButtonType("Loan", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(loanButtonType, ButtonType.CANCEL);

            // Create a grid for the input fields.
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField customerField = new TextField();
            customerField.setPromptText("Customer Name");
            TextField loanDateField = new TextField();
            loanDateField.setPromptText("Loan Date (yyyy-MM-dd)");
            TextField dueDateField = new TextField();
            dueDateField.setPromptText("Due Date (yyyy-MM-dd)");

            grid.add(new Label("Customer Name:"), 0, 0);
            grid.add(customerField, 1, 0);
            grid.add(new Label("Loan Date:"), 0, 1);
            grid.add(loanDateField, 1, 1);
            grid.add(new Label("Due Date:"), 0, 2);
            grid.add(dueDateField, 1, 2);

            dialog.getDialogPane().setContent(grid);

            // Request focus on the customer field.
            Platform.runLater(() -> customerField.requestFocus());

            // Convert the result to a list of strings when the loan button is clicked.
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == loanButtonType) {
                    return Arrays.asList(
                            customerField.getText().trim(),
                            loanDateField.getText().trim(),
                            dueDateField.getText().trim()
                    );
                }
                return null;
            });

            Optional<List<String>> resultDialog = dialog.showAndWait();

            if (resultDialog.isPresent()) {
                List<String> loanDetails = resultDialog.get();
                String customerName = loanDetails.get(0);
                String loanDate = loanDetails.get(1);
                String dueDate = loanDetails.get(2);

                // Establish connection and insert loan details into a "loans" table.
                Connection con = DBUtils.establishConnection();
                PreparedStatement ps = null;
                try {
                    // Assuming you have a "loans" table with columns:
                    // isbn, customer, loan_date, due_date, fine_per_day
                    String loanQuery = "INSERT INTO loans (isbn, customer, loan_date, due_date, fine_per_day) VALUES (?, ?, ?, ?, ?)";
                    ps = con.prepareStatement(loanQuery);
                    ps.setString(1, oldIsbn);
                    ps.setString(2, customerName);
                    ps.setString(3, loanDate);
                    ps.setString(4, dueDate);
                    ps.setInt(5, 5); // Fine: 5QR per day late
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Database Error", "Unable to record loan details.");
                } finally {
                    DBUtils.closeConnection(con, ps);
                }

                // Optionally, you can update the books list or mark the book as loaned.
                loadBooks();
                search.setText("");
            }
        } else {
            showAlert("No Selection", "Please select a book to loan.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    @FXML
    private void handleLogout() {
        new UserLogin(stage);
    }
}
