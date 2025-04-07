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

    // TableView and its columns
    @FXML private TableView<Book> booksList;
    @FXML private TableColumn<Book, Integer> bookIdColumn;
    @FXML private TableColumn<Book, String> isbnColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> categoryColumn;
    @FXML private TableColumn<Book, String> yearColumn;
    @FXML private TableColumn<Book, String> availabilityColumn;

    // Other controls
    @FXML private TextField author;
    @FXML private TextField book;
    @FXML private TextField isbn;
    @FXML private TextField category;
    @FXML private TextField year;
    @FXML private TextField search;
    @FXML private Button logout;

    // Store all books for filtering/searching
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

    // Filters and displays books in the TableView based on a search query.
    @FXML
    protected void searchBook() {
        // Get trimmed and lowercased search text from the input field
        String searchText = search.getText().strip().toLowerCase();
        // Proceed only if the user has entered 3 or more
        if (searchText.length() >= 3) {
            ObservableList<Book> filteredBooks = FXCollections.observableArrayList();
            // Loop will go through all books and add matches to the filtered list
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
            booksList.setItems(filteredBooks); // Display the filtered results in the TableView
        } else {
            booksList.setItems(allBooks); // If search text is too short, show the full list of books
        }
    }

    @FXML
    protected void editBook() {
        // Get the selected Book from the TableView
        Book selectedBook = booksList.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            // Open the update dialog, passing the selected Book for pre-population
            UpdateBook updateBook = new UpdateBook(selectedBook);
            Book updatedBook = updateBook.showAndWait();  // Must implement this so that it returns a Book

            if (updatedBook != null) {
                // Validate all fields before updating
                if (!isValidAuthor(updatedBook.getAuthor())) {
                    showAlert("Invalid Author", "Please enter a valid Author name.", AlertType.ERROR);
                    return;
                }

                if (!isValidTitle(updatedBook.getTitle())) {
                    showAlert("Invalid Title", "Please enter a valid Book Title.", AlertType.ERROR);
                    return;
                }

                if (!isValidISBN(updatedBook.getIsbn())) {
                    showAlert("Invalid ISBN","Please enter a valid ISBN. ISBN starts with 978 or 979 followed by 10 digits", AlertType.ERROR);
                    return;
                }

                if (!isValidCategory(updatedBook.getCategory())) {
                    showAlert("Invalid Category", "Please enter a valid Category.", AlertType.ERROR);
                    return;
                }

                if (!isValidPublishedYear(updatedBook.getPublishedYear())) {
                    showAlert("Invalid Year", "Please enter a valid year.", AlertType.ERROR);
                    return;
                }

                Connection con = DBUtils.establishConnection();
                PreparedStatement ps = null;
                try {
                    // Used book_id in the WHERE clause, since ISBN might be updated
                    String query = "UPDATE books SET author=?, title=?, isbn=?, category=?, published_year=? WHERE book_id=?";
                    ps = con.prepareStatement(query);
                    ps.setString(1, updatedBook.getAuthor());
                    ps.setString(2, updatedBook.getTitle());
                    ps.setString(3, updatedBook.getIsbn());
                    ps.setString(4, updatedBook.getCategory());
                    ps.setString(5, updatedBook.getPublishedYear());
                    ps.setInt(6, selectedBook.getBookId());
                    ps.executeUpdate();

                    showAlert("Success", "Book updated successfully!", AlertType.INFORMATION);
                } catch (SQLException e) {
                    showAlert("Database Error", "Failed to update book: " + e.getMessage(), AlertType.ERROR);
                    e.printStackTrace();
                } finally {
                    DBUtils.closeConnection(con, ps);
                }
                loadBooks();
                search.setText("");
            }
        } else {
            showAlert("No Selection", "Please select a book to edit.", AlertType.ERROR);
        }
    }

    @FXML
    protected void deleteBook() {
        Book selectedBook = booksList.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            // Show confirmation dialog before deleting
            Alert confirmation = new Alert(AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Deletion");
            confirmation.setHeaderText("Delete Book");
            confirmation.setContentText("Are you sure you want to delete the book:\n" + selectedBook.getTitle() + " by " + selectedBook.getAuthor() + "?");
            // Wait for user response
            ButtonType result = confirmation.showAndWait().orElse(ButtonType.CANCEL);

            if (result == ButtonType.OK) {
                String isbnToDelete = selectedBook.getIsbn();
                Connection con = DBUtils.establishConnection();
                PreparedStatement ps = null;
                try {
                    String query = "DELETE FROM books WHERE isbn=?";
                    ps = con.prepareStatement(query);
                    ps.setString(1, isbnToDelete);
                    int rowsAffected = ps.executeUpdate();

                    if (rowsAffected > 0) {
                        // Show success message after deletion
                        showAlert("Success",
                                "Book '" + selectedBook.getTitle() + "' has been successfully deleted.",
                                AlertType.INFORMATION);
                    }
                } catch (SQLException e) {
                    showAlert("Database Error", "Failed to delete book: " + e.getMessage(), AlertType.ERROR);
                    e.printStackTrace();
                } finally {
                    DBUtils.closeConnection(con, ps);
                }

                loadBooks();
                search.setText("");
            }
        } else {
            showAlert("No Selection", "Please select a book to delete.", AlertType.ERROR);
        }
    }

    @FXML
    protected void addBook() {
        String authorText = author.getText().trim();
        String bookText = book.getText().trim();
        String isbnText = isbn.getText().trim();
        String categoryText = category.getText().trim();
        String yearText = year.getText().trim();

        // Validate all fields
        if (authorText.isEmpty() || bookText.isEmpty() || isbnText.isEmpty() ||
                categoryText.isEmpty() || yearText.isEmpty()) {
            showAlert("Missing Information", "Please fill in all fields.", AlertType.ERROR);
            return;
        }

        if (!isValidAuthor(authorText)) {
            showAlert("Invalid Author","Please enter a valid Author name.", AlertType.ERROR);
            return;
        }

        if (!isValidTitle(bookText)) {
            showAlert("Invalid Title","Please enter a valid Book Title.", AlertType.ERROR);
            return;
        }

        if (!isValidISBN(isbnText)) {
            showAlert("Invalid ISBN","Please enter a valid ISBN. ISBN starts with 978 or 979 followed by 10 digits", AlertType.ERROR);
            return;
        }

        if (!isValidCategory(categoryText)) {
            showAlert("Invalid Category","Please enter a valid Category.", AlertType.ERROR);
            return;
        }

        if (!isValidPublishedYear(yearText)) {
            showAlert("Invalid Year","Please enter a valid year.", AlertType.ERROR);
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBUtils.establishConnection();

            // Check if ISBN already exists
            String checkQuery = "SELECT isbn FROM books WHERE isbn = ?";
            ps = con.prepareStatement(checkQuery);
            ps.setString(1, isbnText);
            rs = ps.executeQuery();

            if (rs.next()) {
                showAlert("Duplicate ISBN", "A book with this ISBN already exists.", AlertType.ERROR);
                return;
            }
            rs.close();
            ps.close();

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

            // Insert the new book
            String query = "INSERT INTO books (book_id, author, title, isbn, category, published_year) VALUES (?, ?, ?, ?, ?, ?)";
            ps = con.prepareStatement(query);
            ps.setInt(1, newBookId);
            ps.setString(2, authorText);
            ps.setString(3, bookText);
            ps.setString(4, isbnText);
            ps.setString(5, categoryText);
            ps.setString(6, yearText);
            ps.executeUpdate();

            // Clear the text fields
            author.setText("");
            book.setText("");
            isbn.setText("");
            category.setText("");
            year.setText("");

            showAlert("Success", "Book added successfully!", AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to add book: " + e.getMessage(), AlertType.ERROR);
        } finally {
            DBUtils.closeConnection(con, ps);
            // Reload the books to update the TableView
            loadBooks();
        }
    }

    // Loads all books from the database and populates the TableView.
    private void loadBooks() {
        allBooks.clear(); // Clear existing book entries before reloading
        Connection con = DBUtils.establishConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            // Query to retrieve all books from the database
            String query = "SELECT * FROM books";
            ps = con.prepareStatement(query);
            rs = ps.executeQuery();

            // Process each record from the result set
            while (rs.next()) {
                int bookId = rs.getInt("book_id");
                String isbn = rs.getString("isbn");
                String title = rs.getString("title");
                String author = rs.getString("author");
                String category = rs.getString("category");
                String year = rs.getString("published_year");
                boolean available = rs.getBoolean("is_available");

                // Set the status of the book: "Available" if true, otherwise "Checked Out"
                String status = available ? "Available" : "Checked Out";
                Book bk = new Book(bookId, isbn, title, author, category, year, status);
                allBooks.add(bk); // Create Book object and add it to the list
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(con, ps);
        }

        booksList.setItems(allBooks); // Set the TableView to display the updated list of books
    }

    // Input Validation methods using regular expressions
    private boolean isValidISBN(String isbn) {
        return isbn.matches("^(978|979)\\d{10}$");
    }

    private boolean isValidTitle(String title) {
        return title.matches("^[A-Za-z0-9 .,'\"!?()-]{1,254}[A-Za-z0-9]$") && title.length() < 255;
    }

     private boolean isValidAuthor(String author) {
        return author.matches("^([A-Za-z.'’]{2,}( [A-Za-z.'’]{2,})?)(, [A-Za-z.'’]{2,}( [A-Za-z.'’]{2,})?)*$") && author.length() < 255;
    }

    private boolean isValidPublishedYear(String year) {
        return year.matches("^[0-9]{4}$");
    }

    private boolean isValidCategory(String category) {
        return category.matches("^[A-Za-z ]{2,100}$") && category.length() < 100;
    }

    //Show alerts
    private void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(type);
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