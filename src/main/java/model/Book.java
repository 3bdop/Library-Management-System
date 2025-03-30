package main.java.model;

public class Book {
    private final int bookId;
    private final String isbn;
    private final String title;
    private final String author;
    private final String category;
    private final String publishedYear;
    private final String status;

    public Book(int bookId, String isbn, String title, String author, String category, String publishedYear, String status) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.publishedYear = publishedYear;
        this.status = status;
    }

    public int getBookId() {
        return bookId;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public String getPublishedYear() {
        return publishedYear;
    }

    public String getStatus() {
        return status;
    }
}