package com.example.demo.google;

import com.example.demo.exception.GoogleBooksApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class GoogleBookService {
    private final RestClient restClient;
    private final String apiKey;

    public GoogleBookService(
            @Value("${google.books.base-url:https://www.googleapis.com/books/v1}") String baseUrl,
            @Value("${google.books.api-key:}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        log.info("GoogleBookService initialised with baseUrl={}", baseUrl);
    }

    public GoogleBook searchBooks(String query, Integer maxResults, Integer startIndex) {
        log.debug("Searching Google Books: query='{}', maxResults={}, startIndex={}", query, maxResults, startIndex);
        try {
            GoogleBook result = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/volumes")
                            .queryParam("q", query)
                            .queryParam("maxResults", maxResults != null ? maxResults : 10)
                            .queryParam("startIndex", startIndex != null ? startIndex : 0)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw buildException(res.getStatusCode().value(), "search query: " + query);
                    })
                    .body(GoogleBook.class);
            log.debug("Google Books search returned {} total items", result != null ? result.totalItems() : 0);
            return result;
        } catch (GoogleBooksApiException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.error("Timeout/network error calling Google Books search", e);
            throw new GoogleBooksApiException(HttpStatus.GATEWAY_TIMEOUT.value(),
                    "Google Books API timed out or is unreachable", e);
        } catch (RestClientResponseException e) {
            log.error("HTTP error {} from Google Books search", e.getStatusCode().value(), e);
            throw buildException(e.getStatusCode().value(), "search query: " + query);
        }
    }


    public GoogleBook.Item getVolumeById(String volumeId) {
        log.debug("Fetching Google Books volume id='{}'", volumeId);
        try {
            GoogleBook.Item item = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/volumes/{id}")
                            .queryParam("key", apiKey)
                            .build(volumeId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw buildException(res.getStatusCode().value(), "volumeId: " + volumeId);
                    })
                    .body(GoogleBook.Item.class);
            log.debug("Fetched volume id='{}', title='{}'",
                    item != null ? item.id() : null,
                    item != null && item.volumeInfo() != null ? item.volumeInfo().title() : null);
            return item;
        } catch (GoogleBooksApiException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.error("Timeout/network error fetching Google Books volume id='{}'", volumeId, e);
            throw new GoogleBooksApiException(HttpStatus.GATEWAY_TIMEOUT.value(),
                    "Google Books API timed out or is unreachable", e);
        } catch (RestClientResponseException e) {
            log.error("HTTP error {} from Google Books volume fetch id='{}'", e.getStatusCode().value(), volumeId, e);
            throw buildException(e.getStatusCode().value(), "volumeId: " + volumeId);
        }
    }

    private GoogleBooksApiException buildException(int status, String context) {
        String message = switch (status) {
            case 401 -> "Unauthorised – invalid or missing API key (" + context + ")";
            case 403 -> "Forbidden – API key may be invalid or quota exceeded (" + context + ")";
            case 404 -> "Volume not found on Google Books (" + context + ")";
            case 500 -> "Google Books API returned an internal server error (" + context + ")";
            case 502 -> "Google Books API returned a bad gateway error (" + context + ")";
            case 503 -> "Google Books API is temporarily unavailable (" + context + ")";
            case 504 -> "Google Books API gateway timed out (" + context + ")";
            default  -> "Unexpected error " + status + " from Google Books API (" + context + ")";
        };
        log.error("GoogleBooks error status={} context='{}'", status, context);
        return new GoogleBooksApiException(status, message);
    }


}

