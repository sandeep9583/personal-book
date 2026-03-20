package com.example.demo.exception;

public class GoogleBooksApiException extends RuntimeException {

    private final int statusCode;

    public GoogleBooksApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public GoogleBooksApiException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }


    public int getStatusCode() {
        return statusCode;
    }
}