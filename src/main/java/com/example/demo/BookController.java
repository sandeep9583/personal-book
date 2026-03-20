package com.example.demo;

import com.example.demo.db.Book;
import com.example.demo.google.GoogleBook;
import com.example.demo.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping("/books")
    public ResponseEntity<List<Book>> getAllBooks() {
        log.debug("GET /books");
        return ResponseEntity.ok(bookService.getAllBooks());
    }


    @PostMapping("/books/{googleId}")
    public ResponseEntity<Book> addBook(@PathVariable String googleId) {
        log.info("POST /books/{}", googleId);
        Book saved = bookService.addBookFromGoogle(googleId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


    @GetMapping("/google")
    public ResponseEntity<GoogleBook> searchGoogleBooks(
            @RequestParam("q") String query,
            @RequestParam(value = "maxResults", required = false) Integer maxResults,
            @RequestParam(value = "startIndex", required = false) Integer startIndex) {
        log.debug("GET /google?q={}", query);
        return ResponseEntity.ok(bookService.searchGoogleBooks(query, maxResults, startIndex));
    }
}