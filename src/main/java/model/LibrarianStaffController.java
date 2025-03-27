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
import java.time.LocalDate;
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
                    boolean available = rs.getBoolean("is_available");

                    String status = available ? "Available" : "Checked Out";
                    books.add(author + "; " + title + "; " + isbn + "; " + category + "; " + year + "; " + status);
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
            String[] bookDetails = selectedBook.split(";");
            String isbn = bookDetails[2].trim();

            // Check if book is available
            if (!isBookAvailable(isbn)) {
                showAlert("Book Not Available", "This book is already checked out.");
                return;
            }

            // Create dialog for loan details
            Dialog<List<String>> dialog = createLoanDialog();
            Optional<List<String>> result = dialog.showAndWait();

            if (result.isPresent()) {
                List<String> loanDetails = result.get();
                String memberName = loanDetails.get(0);
                String phone = loanDetails.get(1);
                String dueDate = loanDetails.get(2);

                // Process the loan
                processLoan(isbn, memberName, phone, dueDate);
            }
        } else {
            showAlert("No Selection", "Please select a book to loan.");
        }
    }

    // Create Dialog Box
    private Dialog<List<String>> createLoanDialog() {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Loan Book");
        dialog.setHeaderText("Enter Member Info and Due Date\n(Note: Fine of 5QR per day late)");

        ButtonType loanButtonType = new ButtonType("Loan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loanButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField memberField = new TextField();
        memberField.setPromptText("Firstname Lastname");

        TextField phoneField = new TextField();
        phoneField.setPromptText("55123456");

        TextField dueDateField = new TextField();
        dueDateField.setPromptText("yyyy-MM-dd");

        grid.add(new Label("Member Name:"), 0, 0);
        grid.add(memberField, 1, 0);
        grid.add(new Label("Phone Number:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Due Date:"), 0, 2);
        grid.add(dueDateField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> memberField.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loanButtonType) {
                return Arrays.asList(
                        memberField.getText().trim(),
                        phoneField.getText().trim(),
                        dueDateField.getText().trim()
                );
            }
            return null;
        });

        return dialog;
    }

    // Checks if Book is available
    private boolean isBookAvailable(String isbn) {
        Connection con = DBUtils.establishConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean available = false;

        try {
            String query = "SELECT is_available FROM books WHERE isbn = ?";
            ps = con.prepareStatement(query);
            ps.setString(1, isbn);
            rs = ps.executeQuery();

            if (rs.next()) {
                available = rs.getBoolean("is_available");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(con, ps);
        }

        return available;
    }

    private void processLoan(String isbn, String memberName, String phone, String dueDate) {
        // Validate member name
        if (!isValidMemberName(memberName)) {
            showAlert("Invalid Member Name",
                    "Please enter member name in 'Firstname Lastname' format, make sure first letter is capitalized");
            return;
        }

        // Validate phone number
        if (!isValidPhoneNumber(phone)) {
            showAlert("Invalid Phone Number",
                    "Phone number must be Qatari number (e.g., 55123456)");
            return;
        }

        // Validate date format
        if (!isValidDate(dueDate)) {
            showAlert("Invalid Due Date",
                    "Please enter due date in yyyy-MM-dd format (e.g., 2025-12-31)");
            return;
        }

        // Check if due date is in the future
        LocalDate dueLocalDate = LocalDate.parse(dueDate);
        if (dueLocalDate.isBefore(LocalDate.now())) {
            showAlert("Invalid Due Date",
                    "Due date must be in the future");
            return;
        }

        Connection con = DBUtils.establishConnection();
        PreparedStatement ps = null;

        try {
            con.setAutoCommit(false);

            int memberId = getOrCreateMember(con, memberName, phone);

            String loanQuery = "INSERT INTO loans (isbn, member_id, loan_date, due_date, fine_per_day) " +
                    "VALUES (?, ?, CURDATE(), ?, 5.00)";
            ps = con.prepareStatement(loanQuery);
            ps.setString(1, isbn);
            ps.setInt(2, memberId);
            ps.setString(3, dueDate);
            ps.executeUpdate();

            String updateBookQuery = "UPDATE books SET is_available = 0 WHERE isbn = ?";
            ps = con.prepareStatement(updateBookQuery);
            ps.setString(1, isbn);
            ps.executeUpdate();

            con.commit();

            // Show success alert
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Success");
            successAlert.setHeaderText("Book Checkout Successful");
            successAlert.setContentText(String.format(
                    "Book successfully checked out to: %s\n" +
                            "Phone: %s\n" +
                            "Due Date: %s",
                    memberName, phone, dueDate
            ));
            successAlert.showAndWait();

            loadBooks();
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            String errorMessage;
            if (e.getSQLState() != null && e.getSQLState().equals("23000")) {
                errorMessage = "This book is already checked out to another member.";
            } else {
                errorMessage = "Database error: " + e.getMessage();
            }

            showAlert("Checkout Failed", errorMessage);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            DBUtils.closeConnection(con, ps);
        }
    }

    private int getOrCreateMember(Connection con, String name, String phone) throws SQLException {
        // Check if member exists
        String checkQuery = "SELECT member_id FROM members WHERE phone = ?";
        PreparedStatement ps = con.prepareStatement(checkQuery);
        ps.setString(1, phone);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("member_id");
        }

        // Create new member if not exists
        String insertQuery = "INSERT INTO members (name, phone) VALUES (?, ?)";
        ps = con.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
        ps.setString(1, name);
        ps.setString(2, phone);
        ps.executeUpdate();

        rs = ps.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
        }

        throw new SQLException("Failed to create member");
    }

    @FXML
    protected void returnBook() {
        String selectedBook = booksList.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            String[] bookDetails = selectedBook.split(";");
            String isbn = bookDetails[2].trim();

            // Check if book is actually checked out
            if (isBookAvailable(isbn)) {
                showAlert("Book Available", "This book is not currently checked out.");
                return;
            }

            // Confirm return
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Return");
            confirm.setHeaderText("Confirm Book Return");
            confirm.setContentText("Are you sure you want to return this book?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                processReturn(isbn);
            }
        } else {
            showAlert("No Selection", "Please select a book to return.");
        }
    }

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

            if (!rs.next()) {
                showAlert("Error", "No active loan found for this book.");
                return;
            }

            int loanId = rs.getInt("loan_id");
            int memberId = rs.getInt("member_id");
            LocalDate dueDate = rs.getDate("due_date").toLocalDate();
            LocalDate returnDate = LocalDate.now();

            // Calculate fine (5 QR per day after due date)
            int daysLate = (int) Math.max(0, returnDate.toEpochDay() - dueDate.toEpochDay());
            double fineAmount = daysLate * 5.0;

            // Update loan record
            String updateLoanQuery = "UPDATE loans SET returned = 1, return_date = CURDATE() WHERE loan_id = ?";
            ps = con.prepareStatement(updateLoanQuery);
            ps.setInt(1, loanId);
            ps.executeUpdate();

            // Update book availability
            String updateBookQuery = "UPDATE books SET is_available = 1 WHERE isbn = ?";
            ps = con.prepareStatement(updateBookQuery);
            ps.setString(1, isbn);
            ps.executeUpdate();

            // Get member details for the alert message
            String memberQuery = "SELECT name FROM members WHERE member_id = ?";
            ps = con.prepareStatement(memberQuery);
            ps.setInt(1, memberId);
            rs = ps.executeQuery();
            String memberName = rs.next() ? rs.getString("name") : "Unknown Member";

            con.commit();

            // Show return confirmation with fine details
            showReturnConfirmation(memberName, isbn, dueDate, returnDate, fineAmount);
            loadBooks();

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

    private void showReturnConfirmation(String memberName, String isbn, LocalDate dueDate, LocalDate returnDate, double fineAmount) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Return Processed");
        alert.setHeaderText("Book Returned Successfully");

        String fineMessage = fineAmount > 0 ?
                String.format("Fine Amount: %.2f QR (%d days late)", fineAmount, (int)(returnDate.toEpochDay() - dueDate.toEpochDay())) :
                "No fine applied (returned on time)";

        alert.setContentText(String.format(
                "Book with ISBN %s has been returned.\n" +
                        "Member: %s\n" +
                        "Due Date: %s\n" +
                        "Return Date: %s\n" +
                        "%s",
                isbn, memberName, dueDate, returnDate, fineMessage
        ));

        alert.showAndWait();
    }

    // Input Validation methods using regular expressions
    private boolean isValidDate(String dateStr) {
        String dateRegex = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$";
        return dateStr.matches(dateRegex);
    }

    private boolean isValidMemberName(String name) {
        String nameRegex = "^[A-Z][a-z]+(\\s[A-Z][a-z]+)+$";
        return name.matches(nameRegex);
    }

    private boolean isValidPhoneNumber(String phone) {
        String phoneRegex = "^[1-9]\\d{7}$";
        return phone.matches(phoneRegex);
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
