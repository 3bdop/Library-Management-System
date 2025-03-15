package main.java.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import utlis.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CatalogStaffController {

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
    public CatalogStaffController(Stage stage) {
        this.stage = stage; // Initialize the stage variable
        loadView(); // Load the view after initializing the stage
    }

    // Method to load the view
    private void loadView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/resources/CatalogStaffView.fxml"));
            loader.setController(this); // Set this class as the controller
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Library Management System - Cataloging Staff");
            logout.setOnAction(e->handleLogout());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @FXML
    public void initialize() {
        loadBooks(); // Load books when the view is initialized
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

    @FXML
    protected void editBook() {
        String selectedBook = booksList.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            String[] bookDetails = selectedBook.split(";");
            String oldIsbn = bookDetails[2];

            UpdateBook updateBook = new UpdateBook();
            String result = updateBook.getResult();

            if (result != null) {
                String[] newDetails = result.split(";");
                String newAuthor = newDetails[0];
                String newTitle = newDetails[1];
                String newIsbn = newDetails[2];
                String newCategory = newDetails[3];
                String newYear= newDetails[4];

                Connection con = DBUtils.establishConnection();
                PreparedStatement ps = null;
                try {
                    String query = "UPDATE books SET author=?, title=?, isbn=?, category=?, published_year=? WHERE isbn=?";
                    ps = con.prepareStatement(query);
                    ps.setString(1, newAuthor);
                    ps.setString(2, newTitle);
                    ps.setString(3, newIsbn);
                    ps.setString(4, newCategory);
                    ps.setString(5, newYear);
                    ps.setString(6, oldIsbn);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    DBUtils.closeConnection(con, ps);
                }

                loadBooks();
                search.setText("");
            }
        } else {
            showAlert("No Selection", "Please select a book to edit.");
        }
    }

    @FXML
    protected void deleteBook() {
        String selectedBook = booksList.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            String isbnToDelete = selectedBook.split(";")[2];

            Connection con = DBUtils.establishConnection();
            PreparedStatement ps = null;
            try {
                String query = "DELETE FROM books WHERE isbn=?";
                ps = con.prepareStatement(query);
                ps.setString(1, isbnToDelete);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                DBUtils.closeConnection(con, ps);
            }

            loadBooks();
            search.setText("");
        } else {
            showAlert("No Selection", "Please select a book to delete.");
        }
    }

    @FXML
    protected void addItem() {
        String authorText = author.getText();
        String bookText = book.getText();
        String isbnText = isbn.getText();
        String categoryText = category.getText();
        String yearText = year.getText();

        if (authorText.isEmpty() || bookText.isEmpty() || isbnText.isEmpty() || categoryText.isEmpty() || yearText.isEmpty()) {
            showAlert("Missing Information", "Please fill in all fields.");
            return;
        }

        Connection con = DBUtils.establishConnection();
        PreparedStatement ps = null;
        try {
            String query = "INSERT INTO books (author, title, isbn, category, published_year) VALUES (?, ?, ?, ?, ?)";
            ps = con.prepareStatement(query);
            ps.setString(1, authorText);
            ps.setString(2, bookText);
            ps.setString(3, isbnText);
            ps.setString(4, categoryText);
            ps.setString(5, yearText);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(con, ps);
        }

        author.setText("");
        book.setText("");
        isbn.setText("");
        category.setText("");
        year.setText("");

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
                books.add(author + ";" + title + ";" + isbn + ";" + category + ";" + year);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(con, ps);
        }
        booksList.setItems(books);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
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