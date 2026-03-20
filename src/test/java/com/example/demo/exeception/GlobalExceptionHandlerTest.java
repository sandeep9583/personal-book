package com.example.demo.exeception;

import com.example.demo.BookController;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.GoogleBooksApiException;
import com.example.demo.exception.InvalidBookDataException;
import com.example.demo.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest({BookController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    // ─────────────────────────────────────────────────────────────────────────
    // InvalidBookDataException → 400
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void invalidBookData_returns400WithCorrectJsonShape() throws Exception {
        when(bookService.addBookFromGoogle("id-x"))
                .thenThrow(new InvalidBookDataException("Missing title for volumeId: id-x"));

        mockMvc.perform(post("/books/id-x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Missing title for volumeId: id-x"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/books/id-x"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GoogleBooksApiException → status depends on upstream code
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void googleApi401_mapsTo401Unauthorized() throws Exception {
        when(bookService.addBookFromGoogle("id-x"))
                .thenThrow(new GoogleBooksApiException(401, "Unauthorised – invalid or missing API key"));

        mockMvc.perform(post("/books/id-x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void googleApi403_mapsTo403Forbidden() throws Exception {
        when(bookService.addBookFromGoogle("id-x"))
                .thenThrow(new GoogleBooksApiException(403, "Forbidden – quota exceeded"));

        mockMvc.perform(post("/books/id-x"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void googleApi404_mapsTo404NotFound() throws Exception {
        when(bookService.addBookFromGoogle("id-x"))
                .thenThrow(new GoogleBooksApiException(404, "Volume not found"));

        mockMvc.perform(post("/books/id-x"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void googleApi500_mapsTo502BadGateway() throws Exception {
        when(bookService.addBookFromGoogle("id-x"))
                .thenThrow(new GoogleBooksApiException(500, "Internal server error"));

        mockMvc.perform(post("/books/id-x"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"));
    }

    @Test
    void googleApi502_mapsTo502BadGateway() throws Exception {
        when(bookService.addBookFromGoogle("id-x"))
                .thenThrow(new GoogleBooksApiException(502, "Bad gateway"));

        mockMvc.perform(post("/books/id-x"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void googleApi503_mapsTo502BadGateway() throws Exception {
        when(bookService.addBookFromGoogle("id-x"))
                .thenThrow(new GoogleBooksApiException(503, "Service unavailable"));

        mockMvc.perform(post("/books/id-x"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void googleApi504_mapsTo502BadGateway() throws Exception {
        when(bookService.addBookFromGoogle("id-x"))
                .thenThrow(new GoogleBooksApiException(504, "Gateway timeout"));

        mockMvc.perform(post("/books/id-x"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generic Exception → 500
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void unhandledException_returns500WithGenericMessage() throws Exception {
        when(bookService.addBookFromGoogle("id-x"))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(post("/books/id-x"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                .andExpect(jsonPath("$.path").value("/books/id-x"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ErrorResponse field completeness
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void errorResponse_alwaysContainsAllFiveFields() throws Exception {
        when(bookService.addBookFromGoogle("id-x"))
                .thenThrow(new InvalidBookDataException("Something wrong"));

        mockMvc.perform(post("/books/id-x"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }
}