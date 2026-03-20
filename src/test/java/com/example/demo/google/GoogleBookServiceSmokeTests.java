package com.example.demo.google;

import com.example.demo.exception.GoogleBooksApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
class GoogleBookServiceTest {

    static MockWebServer server;

    @BeforeAll
    static void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        server.shutdown();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("google.books.base-url", () -> server.url("/").toString());
        registry.add("google.books.api-key",  () -> "test-api-key-12345");
    }

    @Autowired
    private GoogleBookService googleBookService;




    // ─────────────────────────────────────────────────────────────────────────
    // searchBooks — API key (requirement 9)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void searchBooks_apiKeyIsAppendedToRequest() throws Exception {
        enqueueFileResponse(200, "effectivejava.json");

        googleBookService.searchBooks("java", 5, 0);

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath())
                .as("API key must be present in the request URL")
                .contains("key=test-api-key-12345");
    }





    // ─────────────────────────────────────────────────────────────────────────
    // Error codes — 401 / 403 (requirement 9)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void searchBooks_401_throwsGoogleBooksApiExceptionWith401() {
        server.enqueue(new MockResponse().setResponseCode(401));

        assertThatThrownBy(() -> googleBookService.searchBooks("java", 5, 0))
                .isInstanceOf(GoogleBooksApiException.class)
                .satisfies(ex -> assertThat(((GoogleBooksApiException) ex).getStatusCode()).isEqualTo(401))
                .hasMessageContaining("Unauthorised");
    }

    @Test
    void searchBooks_403_throwsGoogleBooksApiExceptionWith403() {
        server.enqueue(new MockResponse().setResponseCode(403));

        assertThatThrownBy(() -> googleBookService.searchBooks("java", 5, 0))
                .isInstanceOf(GoogleBooksApiException.class)
                .satisfies(ex -> assertThat(((GoogleBooksApiException) ex).getStatusCode()).isEqualTo(403))
                .hasMessageContaining("Forbidden");
    }

    @Test
    void getVolumeById_401_throwsGoogleBooksApiException() {
        server.enqueue(new MockResponse().setResponseCode(401));

        assertThatThrownBy(() -> googleBookService.getVolumeById("bad-id"))
                .isInstanceOf(GoogleBooksApiException.class)
                .satisfies(ex -> assertThat(((GoogleBooksApiException) ex).getStatusCode()).isEqualTo(401));
    }

    @Test
    void getVolumeById_403_throwsGoogleBooksApiException() {
        server.enqueue(new MockResponse().setResponseCode(403));

        assertThatThrownBy(() -> googleBookService.getVolumeById("bad-id"))
                .isInstanceOf(GoogleBooksApiException.class)
                .satisfies(ex -> assertThat(((GoogleBooksApiException) ex).getStatusCode()).isEqualTo(403));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Error codes — 404, 500, 502, 503, 504 (requirement 12)
    // ─────────────────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {404, 500, 502, 503, 504})
    void searchBooks_errorCodes_throwGoogleBooksApiExceptionWithCorrectStatus(int statusCode) {
        server.enqueue(new MockResponse().setResponseCode(statusCode));

        assertThatThrownBy(() -> googleBookService.searchBooks("java", 5, 0))
                .isInstanceOf(GoogleBooksApiException.class)
                .satisfies(ex -> assertThat(((GoogleBooksApiException) ex).getStatusCode()).isEqualTo(statusCode));
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 500, 502, 503, 504})
    void getVolumeById_errorCodes_throwGoogleBooksApiExceptionWithCorrectStatus(int statusCode) {
        server.enqueue(new MockResponse().setResponseCode(statusCode));

        assertThatThrownBy(() -> googleBookService.getVolumeById("some-id"))
                .isInstanceOf(GoogleBooksApiException.class)
                .satisfies(ex -> assertThat(((GoogleBooksApiException) ex).getStatusCode()).isEqualTo(statusCode));
    }

    @Test
    void getVolumeById_404_errorMessageMentionsNotFound() {
        server.enqueue(new MockResponse().setResponseCode(404));

        assertThatThrownBy(() -> googleBookService.getVolumeById("nonexistent"))
                .isInstanceOf(GoogleBooksApiException.class)
                .hasMessageContaining("not found");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void enqueueFileResponse(int code, String filename) throws IOException {
        String body = Files.readString(Paths.get("src/test/resources", filename));
        server.enqueue(new MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", "application/json")
                .setBody(body));
    }
}