package main.java.model;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class UpdateBook {

    private Book selectedBook;

    public UpdateBook(Book selectedBook) {
        this.selectedBook = selectedBook;
    }

    /**
     * Opens a dialog pre-populated with the selected book’s details and waits for user input.
     * Returns a new Book object with updated details (using the same book_id and status) or null if cancelled.
     */
    public Book showAndWait() {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle("Update Book");
        dialog.setHeaderText("Update Book Details");

        ButtonType confirm = new ButtonType("Edit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirm, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Pre-populate fields with the selected book's current details.
        TextField authorField = new TextField(selectedBook.getAuthor());
        authorField.setPromptText("Author");

        TextField titleField = new TextField(selectedBook.getTitle());
        titleField.setPromptText("Title");

        TextField isbnField = new TextField(selectedBook.getIsbn());
        isbnField.setPromptText("ISBN");

        TextField categoryField = new TextField(selectedBook.getCategory());
        categoryField.setPromptText("Category");

        TextField yearField = new TextField(selectedBook.getPublishedYear());
        yearField.setPromptText("Published Year");

        grid.add(new Label("Author:"), 0, 0);
        grid.add(authorField, 1, 0);
        grid.add(new Label("Title:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("ISBN:"), 0, 2);
        grid.add(isbnField, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryField, 1, 3);
        grid.add(new Label("Published Year:"), 0, 4);
        grid.add(yearField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to a Book object when the confirm button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirm) {
                return new Book(
                        selectedBook.getBookId(), // keep the same ID
                        isbnField.getText(),
                        titleField.getText(),
                        authorField.getText(),
                        categoryField.getText(),
                        yearField.getText(),
                        selectedBook.getStatus()  // preserve the current status
                );
            }
            return null;
        });

        Optional<Book> result = dialog.showAndWait();
        return result.orElse(null);
    }
}