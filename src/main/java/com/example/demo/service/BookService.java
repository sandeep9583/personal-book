package com.example.demo.service;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.exception.InvalidBookDataException;
import com.example.demo.google.GoogleBook;
import com.example.demo.google.GoogleBookService;
import com.example.demo.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final GoogleBookService googleBookService;
    private final BookMapper bookMapper;

    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        log.debug("Fetching all books from repository");
        return bookRepository.findAll();
    }


    public GoogleBook searchGoogleBooks(String query, Integer maxResults, Integer startIndex) {
        log.debug("Delegating Google Books search: query='{}'", query);
        return googleBookService.searchBooks(query, maxResults, startIndex);
    }


    @Transactional
    public Book addBookFromGoogle(String googleId) {
        log.info("Adding book from Google Books, volumeId='{}'", googleId);

        GoogleBook.Item item = googleBookService.getVolumeById(googleId);

        validate(item, googleId);

        Book book = bookMapper.toBook(item);
        Book saved = bookRepository.save(book);

        log.info("Book persisted: id='{}', title='{}'", saved.getId(), saved.getTitle());
        return saved;
    }


    private void validate(GoogleBook.Item item, String googleId) {
        if (item == null) {
            throw new InvalidBookDataException(
                    "Google Books returned no item for volumeId: " + googleId);
        }
        if (!StringUtils.hasText(item.id())) {
            throw new InvalidBookDataException(
                    "Google Books item is missing an id for volumeId: " + googleId);
        }
        if (item.volumeInfo() == null) {
            throw new InvalidBookDataException(
                    "Google Books item has no volumeInfo for volumeId: " + googleId);
        }
        if (!StringUtils.hasText(item.volumeInfo().title())) {
            throw new InvalidBookDataException(
                    "Google Books item has no title for volumeId: " + googleId);
        }
    }
}