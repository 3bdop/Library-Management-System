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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import utlis.DBUtils;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import main.java.model.Book;

public class LibrarianStaffController {
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
    public LibrarianStaffController(Stage stage) {
        this.stage = stage; // Initialize the stage variable
        loadView(); // Load the view after initializing the stage
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
                if (bk.getTitle().toLowerCase().contains(searchText) ||
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

    // Method to load the view
    private void loadView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/resources/LibrarianView.fxml"));
            loader.setController(this); // Set this class as the controller
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Library Management System - Librarian Staff");
            // It's safe to set logout action here if the button is injected in FXML
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

    //Handles the book loan process initiated by the librarian. Then Calls processLoan() to complete the loan operation.
    @FXML
    protected void loanBook() {
        // Get the selected book from the TableView
        Book selectedBook = booksList.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert("No Selection", "Please select a book to loan.");
            return;
        } // Ensure a book is selected

        // Check if the selected book is available
        if (!isBookAvailable(selectedBook.getIsbn())) {
            showAlert("Book Not Available", "This book is already checked out.");
            return;
        }

        // Show loan dialog to collect loan details
        Dialog<List<String>> dialog = createLoanDialog();
        Optional<List<String>> result = dialog.showAndWait();

        // Process the loan details if user submits the dialog
        if (result.isPresent()) {
            List<String> loanDetails = result.get();
            String dueDate = loanDetails.get(3);

            // Validate due date was entered
            if (dueDate.isEmpty()) {
                showAlert("Due Date Required", "Please enter a due date");
                return;
            }

            // If its a new member, validate name and phone
            if (loanDetails.get(0).isEmpty() &&
                    (loanDetails.get(1).isEmpty() || loanDetails.get(2).isEmpty())) {
                showAlert("Information Required", "For new members, please provide both name and phone number");
                return;
            }

            processLoan(selectedBook.getIsbn(), loanDetails); // Call method to finalize and store the loan in the system
        }
    }

    // Create Dialog Box for loaning a book
    private Dialog<List<String>> createLoanDialog() {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Loan Book");
        dialog.setHeaderText("Enter Member Info & Due Date\n(Note: Fine of 5QR per day late)");

        ButtonType loanButtonType = new ButtonType("Loan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loanButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField memberIdField = new TextField();
        memberIdField.setPromptText("Member ID");

        TextField memberNameField = new TextField();
        memberNameField.setPromptText("Firstname Lastname");

        TextField phoneField = new TextField();
        phoneField.setPromptText("55123456");

        TextField dueDateField = new TextField();
        dueDateField.setPromptText("yyyy-MM-dd");

        grid.add(new Label("Member ID:"), 0, 0);
        grid.add(memberIdField, 1, 0);
        grid.add(new Label(""), 0, 1);
        grid.add(new Label("Register New Member"), 0, 2);
        grid.add(new Label("Member Name:"), 0, 3);
        grid.add(memberNameField, 1, 3);
        grid.add(new Label("Phone:"), 0, 4);
        grid.add(phoneField, 1, 4);
        grid.add(new Label(""), 0, 5);
        grid.add(new Label("Due Date:"), 0, 6);
        grid.add(dueDateField, 1, 6);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(memberIdField::requestFocus);

        // Convert the result to a list of strings when the "Loan" button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loanButtonType) {
                return Arrays.asList(
                        memberIdField.getText().trim(),
                        memberNameField.getText().trim(),
                        phoneField.getText().trim(),
                        dueDateField.getText().trim()
                );
            }
            return null;
        });

        return dialog;
    }

    // Checks if the book is available in the database using its ISBN
    private boolean isBookAvailable(String isbn) {
        Connection con = DBUtils.establishConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean available = false; // Default to false

        try {
            // SQL query to check the availability of the book
            String query = "SELECT is_available FROM books WHERE isbn = ?";
            ps = con.prepareStatement(query);
            ps.setString(1, isbn);
            rs = ps.executeQuery();

            // If the book is found, retrieve its availability status
            if (rs.next()) {
                available = rs.getBoolean("is_available");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(con, ps);
        }
        return available; // Return true if available, false otherwise
    }

    // Handles the full book loan process including member validation/creation, loan recording, and book status update
    private void processLoan(String isbn, List<String> loanDetails) {
        String memberIdStr = loanDetails.get(0);
        String memberName = loanDetails.get(1);
        String phone = loanDetails.get(2);
        String dueDate = loanDetails.get(3);

        // Validate due date first
        if (!isValidDate(dueDate)) {
            showAlert("Invalid Due Date", "Please enter due date in yyyy-MM-dd format (e.g., 2025-12-31)");
            return;
        }

        if (LocalDate.parse(dueDate).isBefore(LocalDate.now())) {
            showAlert("Invalid Due Date", "Due date must be in the future");
            return;
        }

        Connection con = DBUtils.establishConnection();
        PreparedStatement ps = null;

        try {
            con.setAutoCommit(false);
            int memberId;

            // If member exists (ID provided)
            if (!memberIdStr.isEmpty()) {
                try {
                    memberId = Integer.parseInt(memberIdStr);
                    // Verify member exists
                    String memberQuery = "SELECT name FROM members WHERE member_id = ?";
                    ps = con.prepareStatement(memberQuery);
                    ps.setInt(1, memberId);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        showAlert("Member Not Found", "No member found with ID: " + memberId);
                        return;
                    }
                    memberName = rs.getString("name"); // Get name from DB
                } catch (NumberFormatException e) {
                    showAlert("Invalid Member ID", "Member ID must be a number");
                    return;
                }
            } else {
                // If new member (ID not provided)
                // Validate new member details
                if (memberName.isEmpty()) {
                    showAlert("Name Required", "Please enter member name");
                    return;
                }
                if (!isValidMemberName(memberName)) {
                    showAlert("Invalid Member Name", "Please enter member name in 'Firstname Lastname' format, make sure first letter is capitalized");
                    return;
                }
                if (!isValidPhoneNumber(phone)) {
                    showAlert("Invalid Phone Number", "Phone number must be Qatari number (e.g., 55123456)");
                    return;
                }
                // Create new member into db
                String insertQuery = "INSERT INTO members (name, phone) VALUES (?, ?)";
                ps = con.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setString(1, memberName);
                ps.setString(2, phone);
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys(); // Retrieve the new member ID
                if (!rs.next()) {
                    throw new SQLException("Failed to create member");
                }
                memberId = rs.getInt(1);
            }

            // Insert loan record into the loans table
            String loanQuery = "INSERT INTO loans (isbn, member_id, loan_date, due_date, fine_per_day) VALUES (?, ?, CURDATE(), ?, 5.00)";
            ps = con.prepareStatement(loanQuery);
            ps.setString(1, isbn);
            ps.setInt(2, memberId);
            ps.setString(3, dueDate);
            ps.executeUpdate();

            // Update book status
            String updateBookQuery = "UPDATE books SET is_available = 0 WHERE isbn = ?";
            ps = con.prepareStatement(updateBookQuery);
            ps.setString(1, isbn);
            ps.executeUpdate();

            con.commit(); // Commit the transaction

            // Show confirmation dialog
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Loan Successful");
            successAlert.setHeaderText("Book Checkout Completed");
            successAlert.setContentText(String.format(
                    "Book successfully checked out to:\nMember ID: %d\nName: %s\nDue Date: %s",
                    memberId, memberName, dueDate
            ));
            successAlert.showAndWait();

            loadBooks(); // Refresh the book list

        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            String errorMessage = "Database error: " + e.getMessage();
            if (e.getSQLState() != null && e.getSQLState().equals("23000")) {
                errorMessage = "This book is already checked out to another member. Or Failed to register new member";
            }
            showAlert("Loan Failed", errorMessage);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            DBUtils.closeConnection(con, ps);
        }
    }

    // Handles the process of returning a book. Then Calls processReturn() to complete the return operation.
    @FXML
    protected void returnBook() {
        Book selectedBook = booksList.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert("No Selection", "Please select a book to return.");
            return;
        }
        String isbn = selectedBook.getIsbn();

        // Check if the book is actually checked out
        if (isBookAvailable(isbn)) {
            showAlert("Book Available", "This book is not currently checked out.");
            return;
        }

        // Ask the user to Confirm return
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Return");
        confirm.setHeaderText("Confirm Book Return");
        confirm.setContentText("Are you sure you want to return this book?");

        // Process the return if the user clicks OK
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            processReturn(isbn);
        }
    }

    // Handles the full book return process including calculating fines, updating loan record and book availability
    private void processReturn(String isbn) {
        Connection con = DBUtils.establishConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con.setAutoCommit(false);

            // Get the active loan for the book
            String getLoanQuery = "SELECT * FROM loans WHERE isbn = ? AND returned = 0";
            ps = con.prepareStatement(getLoanQuery);
            ps.setString(1, isbn);
            rs = ps.executeQuery();
            // If no active loan found, show error and return
            if (!rs.next()) {
                showAlert("Error", "No active loan found for this book.");
                return;
            }

            // Extract loan details from the result set
            int loanId = rs.getInt("loan_id");
            int memberId = rs.getInt("member_id");
            LocalDate dueDate = rs.getDate("due_date").toLocalDate();
            LocalDate returnDate = LocalDate.now();

            // Calculate fine (5 QR per day after due date)
            int daysLate = (int) Math.max(0, returnDate.toEpochDay() - dueDate.toEpochDay());
            double fineAmount = daysLate * 5.0;

            // Update loan record as returned in the database
            String updateLoanQuery = "UPDATE loans SET returned = 1, return_date = CURDATE() WHERE loan_id = ?";
            ps = con.prepareStatement(updateLoanQuery);
            ps.setInt(1, loanId);
            ps.executeUpdate();

            // Update book availability
            String updateBookQuery = "UPDATE books SET is_available = 1 WHERE isbn = ?";
            ps = con.prepareStatement(updateBookQuery);
            ps.setString(1, isbn);
            ps.executeUpdate();

            // Get member details for the confirmation message
            String memberQuery = "SELECT name FROM members WHERE member_id = ?";
            ps = con.prepareStatement(memberQuery);
            ps.setInt(1, memberId);
            rs = ps.executeQuery();
            String memberName = rs.next() ? rs.getString("name") : "Unknown Member";

            con.commit(); // Commit the transaction if all operations succeeded

            // Show confirmation dialog with return details
            showReturnConfirmation(memberName, isbn, dueDate, returnDate, fineAmount);
            loadBooks(); // Refresh the book list

        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            showAlert("Database Error", "Failed to process return: " + e.getMessage());
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            DBUtils.closeConnection(con, ps);
        }
    }

    // Confirmation Message after successful Book return
    private void showReturnConfirmation(String memberName, String isbn, LocalDate dueDate, LocalDate returnDate, double fineAmount) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Return Processed");
        alert.setHeaderText("Book Returned Successfully");

        String fineMessage = fineAmount > 0 ?
                String.format("Fine Amount: %.2f QR (%d days late)", fineAmount, (int) (returnDate.toEpochDay() - dueDate.toEpochDay())) :
                "No fine applied (returned on time)";

        alert.setContentText(String.format(
                "Book with ISBN %s has been returned.\nMember: %s\nDue Date: %s\nReturn Date: %s\n%s",
                isbn, memberName, dueDate, returnDate, fineMessage
        ));

        alert.showAndWait();
    }

    // Input Validation methods using regular expressions
    private boolean isValidDate(String dateStr) {
        String dateRegex = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$"; //format (YYYY-MM-DD)
        return dateStr.matches(dateRegex);
    }

    private boolean isValidMemberName(String name) {
        //First letter uppercase, there must be at least two words
        String nameRegex = "^[A-Z][a-z]+(\\s[A-Z][a-z]+)+$";
        return name.matches(nameRegex);
    }

    private boolean isValidPhoneNumber(String phone) {
        String phoneRegex = "^[1-9]\\d{7}$"; //8-digit Qatari Phone number
        return phone.matches(phoneRegex);
    }

    //Show error alerts
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
