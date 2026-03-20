package com.example.demo.exception;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String bookId) {
        super("Book not found in personal list: " + bookId);
    }
}
