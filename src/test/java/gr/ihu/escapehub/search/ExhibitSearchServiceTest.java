package gr.ihu.escapehub.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.search.dto.SearchResultDto;
import gr.ihu.escapehub.search.service.ExhibitSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExhibitSearchServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private ExhibitSearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new ExhibitSearchService(new ObjectMapper(), httpClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_returnsStructuredResults_whenExternalApiSucceeds() throws Exception {
        String mockWikipediaJson = """
                {
                  "pages": [
                    {
                      "title": "Regent Diamond",
                      "description": "140.64-carat diamond housed in the Louvre",
                      "excerpt": "The <span>Regent Diamond</span> weighs 140.64 carats.",
                      "thumbnail": {
                        "url": "//upload.wikimedia.org/regent.jpg"
                      }
                    }
                  ]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockWikipediaJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        List<SearchResultDto> results = searchService.search("Regent Diamond");

        assertEquals(1, results.size());
        SearchResultDto first = results.get(0);
        assertEquals("Regent Diamond", first.title());
        assertEquals("The Regent Diamond weighs 140.64 carats.", first.summary());
        assertEquals("https://upload.wikimedia.org/regent.jpg", first.imageUrl());
        assertEquals("Wikipedia (EN)", first.source());
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_usesInMemoryCache_onRepeatedNormalizedQuery() throws Exception {
        String mockWikipediaJson = """
                {
                  "pages": [
                    {
                      "title": "Crown of Louis XV",
                      "description": "French royal crown from 1722",
                      "excerpt": "Made in 1722 for the coronation of Louis XV."
                    }
                  ]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockWikipediaJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        List<SearchResultDto> firstCall = searchService.search("Louis XV");
        List<SearchResultDto> secondCall = searchService.search("  louis xv  ");

        assertEquals(1, firstCall.size());
        assertEquals(firstCall, secondCall);
        // Το εξωτερικό API πρέπει να έχει κληθεί ΜΟΝΟ 1 φορά
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_returnsFallback_whenExternalApiTimesOutOrFails() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("Request timed out"));

        List<SearchResultDto> results = assertDoesNotThrow(() -> searchService.search("Στέμμα Λουδοβίκου"));

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).title().contains("Λουδοβίκου"));
        assertEquals("1722", results.get(0).date());
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_returnsFallback_onIoException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Network unreachable"));

        List<SearchResultDto> results = assertDoesNotThrow(() -> searchService.search("regent"));

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).summary().contains("140.64"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_returnsEmptyList_forNullBlankOrTooShortQuery() throws Exception {
        assertTrue(searchService.search(null).isEmpty());
        assertTrue(searchService.search("").isEmpty());
        assertTrue(searchService.search("   ").isEmpty());
        assertTrue(searchService.search("a").isEmpty());

        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}