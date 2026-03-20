package com.example.demo.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;


import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void save_thenFindById_returnsBook() {
        Book book = new Book("id-1", "Clean Code", "Robert C. Martin", 464);
        bookRepository.save(book);

        Optional<Book> found = bookRepository.findById("id-1");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Clean Code");
        assertThat(found.get().getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(found.get().getPageCount()).isEqualTo(464);
    }

    @Test
    void findAll_returnsAllPersistedBooks() {
        bookRepository.save(new Book("one",   "Title One", "Author A"));
        bookRepository.save(new Book("two",   "Title Two", "Author B"));
        bookRepository.save(new Book("three", "Title Three", "Author C"));

        List<Book> books = bookRepository.findAll();

        assertThat(books).hasSize(3);
        assertThat(books).extracting(Book::getTitle)
                .containsExactlyInAnyOrder("Title One", "Title Two", "Title Three");
    }

    @Test
    void findAll_emptyDatabase_returnsEmptyList() {
        assertThat(bookRepository.findAll()).isEmpty();
    }

    @Test
    void save_duplicateId_performsUpsert() {
        bookRepository.save(new Book("dup-1", "Original Title", "Author"));
        bookRepository.save(new Book("dup-1", "Updated Title",  "Author"));

        List<Book> books = bookRepository.findAll();
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void save_bookWithNullPageCount_persistsSuccessfully() {
        Book book = new Book("no-pages", "No Pages Book", "Some Author", null);
        bookRepository.save(book);

        Optional<Book> found = bookRepository.findById("no-pages");
        assertThat(found).isPresent();
        assertThat(found.get().getPageCount()).isNull();
    }

    @Test
    void deleteAll_removesAllBooks() {
        bookRepository.save(new Book("x", "X", "Y"));
        bookRepository.deleteAll();

        assertThat(bookRepository.findAll()).isEmpty();
    }
}