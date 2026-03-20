package com.example.demo.service;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.exception.GoogleBooksApiException;
import com.example.demo.exception.InvalidBookDataException;
import com.example.demo.google.GoogleBook;
import com.example.demo.google.GoogleBookService;
import com.example.demo.mapper.BookMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private GoogleBookService googleBookService;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookService bookService;

    private GoogleBook.Item validItem;
    private Book mappedBook;

    @BeforeEach
    void setUp() {
        GoogleBook.VolumeInfo volumeInfo = new GoogleBook.VolumeInfo(
                "Effective Java", List.of("Joshua Bloch"), "2008-05-08",
                "Pearson", 346, "BOOK", "NOT_MATURE",
                List.of("Computers"), "en", "https://preview", "https://info");

        validItem  = new GoogleBook.Item("zyTCAlFPjgYC", "https://self", volumeInfo, null);
        mappedBook = new Book("zyTCAlFPjgYC", "Effective Java", "Joshua Bloch", 346);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addBookFromGoogle — happy path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void addBookFromGoogle_happyPath_fetchesValidatesMapsPersistsAndReturns() {
        when(googleBookService.getVolumeById("zyTCAlFPjgYC")).thenReturn(validItem);
        when(bookMapper.toBook(validItem)).thenReturn(mappedBook);
        when(bookRepository.save(mappedBook)).thenReturn(mappedBook);

        Book result = bookService.addBookFromGoogle("zyTCAlFPjgYC");

        assertThat(result.getId()).isEqualTo("zyTCAlFPjgYC");
        assertThat(result.getTitle()).isEqualTo("Effective Java");
        assertThat(result.getAuthor()).isEqualTo("Joshua Bloch");
        assertThat(result.getPageCount()).isEqualTo(346);

        verify(googleBookService).getVolumeById("zyTCAlFPjgYC");
        verify(bookMapper).toBook(validItem);
        verify(bookRepository).save(mappedBook);
    }

    @Test
    void addBookFromGoogle_happyPath_bookPersistedExactlyOnce() {
        when(googleBookService.getVolumeById(any())).thenReturn(validItem);
        when(bookMapper.toBook(any())).thenReturn(mappedBook);
        when(bookRepository.save(any())).thenReturn(mappedBook);

        bookService.addBookFromGoogle("zyTCAlFPjgYC");

        verify(bookRepository, times(1)).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addBookFromGoogle — validation failures (requirement 7)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void addBookFromGoogle_nullItem_throwsInvalidBookDataException() {
        when(googleBookService.getVolumeById("bad-id")).thenReturn(null);

        assertThatThrownBy(() -> bookService.addBookFromGoogle("bad-id"))
                .isInstanceOf(InvalidBookDataException.class)
                .hasMessageContaining("bad-id");

        verify(bookRepository, never()).save(any());
    }

    @Test
    void addBookFromGoogle_blankItemId_throwsInvalidBookDataException() {
        GoogleBook.VolumeInfo vi = new GoogleBook.VolumeInfo("Title", List.of("Author"),
                null, null, null, null, null, null, null, null, null);
        GoogleBook.Item itemWithBlankId = new GoogleBook.Item("", "https://self", vi, null);

        when(googleBookService.getVolumeById("x")).thenReturn(itemWithBlankId);

        assertThatThrownBy(() -> bookService.addBookFromGoogle("x"))
                .isInstanceOf(InvalidBookDataException.class);

        verify(bookRepository, never()).save(any());
    }

    @Test
    void addBookFromGoogle_nullVolumeInfo_throwsInvalidBookDataException() {
        GoogleBook.Item itemNoVolumeInfo = new GoogleBook.Item("some-id", "https://self", null, null);
        when(googleBookService.getVolumeById("some-id")).thenReturn(itemNoVolumeInfo);

        assertThatThrownBy(() -> bookService.addBookFromGoogle("some-id"))
                .isInstanceOf(InvalidBookDataException.class)
                .hasMessageContaining("volumeInfo");

        verify(bookRepository, never()).save(any());
    }

    @Test
    void addBookFromGoogle_blankTitle_throwsInvalidBookDataException() {
        GoogleBook.VolumeInfo viBlankTitle = new GoogleBook.VolumeInfo(
                "   ", List.of("Author"), null, null, null, null, null, null, null, null, null);
        GoogleBook.Item itemBlankTitle = new GoogleBook.Item("id-x", "https://self", viBlankTitle, null);

        when(googleBookService.getVolumeById("id-x")).thenReturn(itemBlankTitle);

        assertThatThrownBy(() -> bookService.addBookFromGoogle("id-x"))
                .isInstanceOf(InvalidBookDataException.class)
                .hasMessageContaining("title");

        verify(bookRepository, never()).save(any());
    }

    @Test
    void addBookFromGoogle_nullTitle_throwsInvalidBookDataException() {
        GoogleBook.VolumeInfo viNullTitle = new GoogleBook.VolumeInfo(
                null, List.of("Author"), null, null, null, null, null, null, null, null, null);
        GoogleBook.Item itemNullTitle = new GoogleBook.Item("id-y", "https://self", viNullTitle, null);

        when(googleBookService.getVolumeById("id-y")).thenReturn(itemNullTitle);

        assertThatThrownBy(() -> bookService.addBookFromGoogle("id-y"))
                .isInstanceOf(InvalidBookDataException.class);

        verify(bookRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addBookFromGoogle — upstream errors propagate
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void addBookFromGoogle_googleApi404_propagatesGoogleBooksApiException() {
        when(googleBookService.getVolumeById("missing"))
                .thenThrow(new GoogleBooksApiException(404, "Volume not found"));

        assertThatThrownBy(() -> bookService.addBookFromGoogle("missing"))
                .isInstanceOf(GoogleBooksApiException.class)
                .satisfies(ex -> assertThat(((GoogleBooksApiException) ex).getStatusCode()).isEqualTo(404));

        verify(bookRepository, never()).save(any());
    }

    @Test
    void addBookFromGoogle_googleApi401_propagatesGoogleBooksApiException() {
        when(googleBookService.getVolumeById("any"))
                .thenThrow(new GoogleBooksApiException(401, "Unauthorised"));

        assertThatThrownBy(() -> bookService.addBookFromGoogle("any"))
                .isInstanceOf(GoogleBooksApiException.class)
                .satisfies(ex -> assertThat(((GoogleBooksApiException) ex).getStatusCode()).isEqualTo(401));
    }

    @Test
    void addBookFromGoogle_googleApi503_propagatesGoogleBooksApiException() {
        when(googleBookService.getVolumeById("any"))
                .thenThrow(new GoogleBooksApiException(503, "Service unavailable"));

        assertThatThrownBy(() -> bookService.addBookFromGoogle("any"))
                .isInstanceOf(GoogleBooksApiException.class)
                .satisfies(ex -> assertThat(((GoogleBooksApiException) ex).getStatusCode()).isEqualTo(503));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllBooks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getAllBooks_delegatesToRepository() {
        List<Book> books = List.of(
                new Book("a", "Book A", "Author A", 100),
                new Book("b", "Book B", "Author B", 200));
        when(bookRepository.findAll()).thenReturn(books);

        List<Book> result = bookService.getAllBooks();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Book::getTitle).containsExactlyInAnyOrder("Book A", "Book B");
        verify(bookRepository).findAll();
    }

    @Test
    void getAllBooks_emptyRepository_returnsEmptyList() {
        when(bookRepository.findAll()).thenReturn(List.of());

        assertThat(bookService.getAllBooks()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // searchGoogleBooks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void searchGoogleBooks_delegatesToGoogleBookService() {
        GoogleBook expected = new GoogleBook("books#volumes", 1,
                List.of(new GoogleBook.Item("id", "self", null, null)));
        when(googleBookService.searchBooks("java", 5, 0)).thenReturn(expected);

        GoogleBook result = bookService.searchGoogleBooks("java", 5, 0);

        assertThat(result).isEqualTo(expected);
        verify(googleBookService).searchBooks("java", 5, 0);
    }

    @Test
    void searchGoogleBooks_passesNullPaginationToService() {
        when(googleBookService.searchBooks("test", null, null))
                .thenReturn(new GoogleBook("books#volumes", 0, List.of()));

        bookService.searchGoogleBooks("test", null, null);

        verify(googleBookService).searchBooks("test", null, null);
    }
}