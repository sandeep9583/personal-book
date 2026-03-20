package com.example.demo.mapper;

import com.example.demo.db.Book;
import com.example.demo.google.GoogleBook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class BookMapperTest {

    @Autowired
    private BookMapper bookMapper;

    @Test
    void toBook_allFieldsPresent_mapsCorrectly() {
        GoogleBook.Item item = buildItem("zyTCAlFPjgYC", "Effective Java",
                List.of("Joshua Bloch", "Second Author"), 346);

        Book book = bookMapper.toBook(item);

        assertThat(book.getId()).isEqualTo("zyTCAlFPjgYC");
        assertThat(book.getTitle()).isEqualTo("Effective Java");
        assertThat(book.getAuthor()).isEqualTo("Joshua Bloch");   // first author only
        assertThat(book.getPageCount()).isEqualTo(346);
    }

    @Test
    void toBook_singleAuthor_mapsCorrectly() {
        GoogleBook.Item item = buildItem("id-1", "Clean Code", List.of("Robert C. Martin"), 431);

        Book book = bookMapper.toBook(item);

        assertThat(book.getAuthor()).isEqualTo("Robert C. Martin");
    }

    @Test
    void toBook_emptyAuthorsList_authorIsNull() {
        GoogleBook.Item item = buildItem("id-2", "No Author Book", List.of(), null);

        Book book = bookMapper.toBook(item);

        assertThat(book.getAuthor()).isNull();
    }

    @Test
    void toBook_nullAuthorsList_authorIsNull() {
        GoogleBook.Item item = buildItem("id-3", "Null Authors", null, null);

        Book book = bookMapper.toBook(item);

        assertThat(book.getAuthor()).isNull();
    }

    @Test
    void toBook_nullPageCount_pageCountIsNull() {
        GoogleBook.Item item = buildItem("id-4", "No Pages", List.of("Author X"), null);

        Book book = bookMapper.toBook(item);

        assertThat(book.getPageCount()).isNull();
    }

    @Test
    void toBook_idMappedFromItemId() {
        GoogleBook.Item item = buildItem("specific-id", "Some Book", List.of("Author"), 100);

        Book book = bookMapper.toBook(item);

        assertThat(book.getId()).isEqualTo("specific-id");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private GoogleBook.Item buildItem(String id, String title, List<String> authors, Integer pageCount) {
        GoogleBook.VolumeInfo volumeInfo = new GoogleBook.VolumeInfo(
                title, authors, "2008-01-01", "Publisher", pageCount,
                "BOOK", "NOT_MATURE", List.of("Computers"), "en",
                "https://preview", "https://info");
        return new GoogleBook.Item(id, "https://self/" + id, volumeInfo, null);
    }
}