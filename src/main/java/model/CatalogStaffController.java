package main.java.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import utlis.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import main.java.model.Book;
import main.java.model.UpdateBook;

public class CatalogStaffController {

    private Stage stage; // Declare the stage variable

    @FXML private TableView<Book> booksList;
    @FXML private TableColumn<Book, Integer> bookIdColumn;
    @FXML private TableColumn<Book, String> isbnColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> categoryColumn;
    @FXML private TableColumn<Book, String> yearColumn;
    @FXML private TableColumn<Book, String> availabilityColumn;

    @FXML private TextField author;
    @FXML private TextField book;
    @FXML private TextField isbn;
    @FXML private TextField category;
    @FXML private TextField year;
    @FXML private TextField search;
    @FXML private Button logout;

    private ObservableList<Book> allBooks = FXCollections.observableArrayList();

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
            logout.setOnAction(e -> handleLogout());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        // Initialize table columns with appropriate property names from the Book model
        bookIdColumn.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("publishedYear"));
        availabilityColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadBooks();
    }

    @FXML
    protected void searchBook() {
        String searchText = search.getText().strip().toLowerCase();
        if (searchText.length() >= 3) {
            ObservableList<Book> filteredBooks = FXCollections.observableArrayList();
            for (Book bk : allBooks) {
                if (
                        bk.getTitle().toLowerCase().contains(searchText) ||
                        bk.getAuthor().toLowerCase().contains(searchText) ||
                        bk.getIsbn().toLowerCase().contains(searchText) ||
                        bk.getCategory().toLowerCase().contains(searchText) ||
                        bk.getPublishedYear().contains(searchText)
                ) {
                    filteredBooks.add(bk);
                }
            }
            booksList.setItems(filteredBooks);
        } else {
            booksList.setItems(allBooks);
        }
    }

    @FXML
    protected void editBook() {
        // Get the selected Book from the TableView
        Book selectedBook = booksList.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            // Open the update dialog, passing the selected Book for pre-population
            // Assume UpdateBook now returns a Book object with updated details
            UpdateBook updateBook = new UpdateBook(selectedBook);
            Book updatedBook = updateBook.showAndWait(); // You must implement this so that it returns a Book

            if (updatedBook != null) {
                Connection con = DBUtils.establishConnection();
                PreparedStatement ps = null;
                try {
                    // Use book_id in the WHERE clause, since ISBN might be updated
                    String query = "UPDATE books SET author=?, title=?, isbn=?, category=?, published_year=? WHERE book_id=?";
                    ps = con.prepareStatement(query);
                    ps.setString(1, updatedBook.getAuthor());
                    ps.setString(2, updatedBook.getTitle());
                    ps.setString(3, updatedBook.getIsbn());
                    ps.setString(4, updatedBook.getCategory());
                    ps.setString(5, updatedBook.getPublishedYear());
                    ps.setInt(6, selectedBook.getBookId());
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
        Book selectedBook = booksList.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            String isbnToDelete = selectedBook.getIsbn();

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

        Calendar now = Calendar.getInstance();
        int realYear = now.get(Calendar.YEAR);

        if (authorText.isEmpty() || bookText.isEmpty() || isbnText.isEmpty() || categoryText.isEmpty() || yearText.isEmpty()) {
            showAlert("Missing Information", "Please fill in all fields.");
            return;
        }
        // Regex validations:
        if (!isValidTitle(bookText)) {
            showAlert("Invalid Title", "Title must contain only letters, numbers, and spaces.");
            return;
        }
        if (!isValidAuthor(authorText)) {
            showAlert("Invalid Author", "Author must contain only letters (no digits).");
            return;
        }
        if (!isValidPublishedYear(yearText)) {
            showAlert("Invalid Published Year", "Published Year must be exactly 4 digits.");
            return;
        }
        if (!isValidCategory(categoryText)) {
            showAlert("Invalid Category", "Category must contain only letters (no spaces).");
            return;
        }

        if (Integer.parseInt(yearText) > realYear ) {
            showAlert("Wrong Year", "Please enter a year before or equal to " + realYear);
            return;
        }

        Connection con = DBUtils.establishConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // Get the last book_id from the books table
            String getLastID = "SELECT book_id FROM books ORDER BY book_id DESC LIMIT 1";
            ps = con.prepareStatement(getLastID);
            rs = ps.executeQuery();
            int newBookId = 1; // Default to 1 if no books exist
            if (rs.next()) {
                newBookId = rs.getInt("book_id") + 1;
            }
            rs.close();
            ps.close();

            // Insert the new book with the calculated book_id
            String query = "INSERT INTO books (book_id, author, title, isbn, category, published_year) VALUES (?, ?, ?, ?, ?, ?)";
            ps = con.prepareStatement(query);
            ps.setInt(1, newBookId);
            ps.setString(2, authorText);
            ps.setString(3, bookText);
            ps.setString(4, isbnText);
            ps.setString(5, categoryText);
            ps.setString(6, yearText);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(con, ps);
        }

        // Clear the text fields
        author.setText("");
        book.setText("");
        isbn.setText("");
        category.setText("");
        year.setText("");

        // Reload the books to update the TableView
        loadBooks();
    }

    private void loadBooks() {
        allBooks.clear();
        Connection con = DBUtils.establishConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            String query = "SELECT * FROM books";
            ps = con.prepareStatement(query);
            rs = ps.executeQuery();

            while (rs.next()) {
                int bookId = rs.getInt("book_id");
                String isbn = rs.getString("isbn");
                String title = rs.getString("title");
                String author = rs.getString("author");
                String category = rs.getString("category");
                String year = rs.getString("published_year");
                boolean available = rs.getBoolean("is_available");

                String status = available ? "Available" : "Checked Out";
                Book bk = new Book(bookId, isbn, title, author, category, year, status);
                allBooks.add(bk);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(con, ps);
        }

        booksList.setItems(allBooks);
    }

    private boolean isValidTitle(String title) {
        return title.matches("^[A-Za-z0-9 ]+$");
    }

     private boolean isValidAuthor(String author) {
        return author.matches("^[A-Za-z\\s]+$");
    }

    private boolean isValidPublishedYear(String year) {
        return year.matches("^[0-9]{4}$");
    }

    private boolean isValidCategory(String category) {
        return category.matches("^[A-Za-z]+$");
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