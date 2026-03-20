package com.example.demo.exception;


public class InvalidBookDataException extends RuntimeException {

    public InvalidBookDataException(String message) {
        super(message);
    }
}