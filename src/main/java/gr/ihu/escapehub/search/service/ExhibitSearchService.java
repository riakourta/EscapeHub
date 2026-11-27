package gr.ihu.escapehub.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.search.dto.SearchResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class ExhibitSearchService {

    private static final Duration API_TIMEOUT = Duration.ofSeconds(3);
    private static final int MIN_QUERY_LENGTH = 2;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern GREEK_CHARS = Pattern.compile(".*[\\u0370-\\u03FF\\u1F00-\\u1FFF].*");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<String, List<SearchResultDto>> cache = new ConcurrentHashMap<>();

    private record FallbackEntry(List<String> keywords, SearchResultDto dto) {
    }

    private static final List<FallbackEntry> FALLBACK_EXHIBITS = List.of(
            new FallbackEntry(
                    List.of("στεμμα", "λουδοβικ", "crown", "louis", "xv", "ιε", "1722"),
                    new SearchResultDto(
                            "Στέμμα του Λουδοβίκου ΙΕ' (Crown of Louis XV)",
                            "Βασιλικό στέμμα που κατασκευάστηκε από τον Claude Rondé για τη στέψη του νεαρού βασιλιά Λουδοβίκου ΙΕ' της Γαλλίας το 1722. Αρχικά στο μπροστινό μέρος του έφερε το περίφημο Διαμάντι Regent.",
                            null,
                            "Αρχείο Μουσείου Λούβρου (Fallback)",
                            "Claude Rondé",
                            "1722"
                    )
            ),
            new FallbackEntry(
                    List.of("διαμαντι", "regent", "ρεζαντ", "diamond", "δακρυ", "καρατια", "140"),
                    new SearchResultDto(
                            "Διαμάντι Regent (Le Régent)",
                            "Ένα από τα πιο καθαρά και διάσημα διαμάντια στον κόσμο, με βάρος 140.64 καράτια (περίπου 140 καράτια). Ανακαλύφθηκε στην Ινδία και στόλιζε το στέμμα του Λουδοβίκου ΙΕ' πριν εκτεθεί ξεχωριστά στο Λούβρο.",
                            null,
                            "Αρχείο Μουσείου Λούβρου (Fallback)",
                            "Βασιλικοί Θησαυροί Γαλλίας",
                            "1698 (140.64 καράτια)"
                    )
            ),
            new FallbackEntry(
                    List.of("κωδικας", "χαμουραμπι", "hammurabi", "βαβυλωνα", "στηλη"),
                    new SearchResultDto(
                            "Κώδικας του Χαμουραμπί",
                            "Μνημειακή στήλη από μαύρο βασάλτη με χαραγμένους σε πέτρα τους αρχαίους νόμους της Βαβυλώνας (περίπου 1750 π.Χ.). Φυλάσσεται στην Πτέρυγα Richelieu του Λούβρου.",
                            null,
                            "Αρχείο Μουσείου Λούβρου (Fallback)",
                            "Βαβυλωνιακή Δυναστεία",
                            "περ. 1750 π.Χ."
                    )
            ),
            new FallbackEntry(
                    List.of("φτερωτ", "ταυρ", "χορσαμπαντ", "lamassu", "winged", "bulls"),
                    new SearchResultDto(
                            "Φτερωτοί Ταύροι του Χορσαμπάντ (Lamassu)",
                            "Κολοσσιαία ασσυριακά αγάλματα με σώμα ταύρου, ανθρώπινο κεφάλι και μεγάλα φτερά που φρουρούσαν τις πύλες του παλατιού του Σαργών Β'.",
                            null,
                            "Αρχείο Μουσείου Λούβρου (Fallback)",
                            "Ασσυριακή Τέχνη",
                            "περ. 713–706 π.Χ."
                    )
            ),
            new FallbackEntry(
                    List.of("ξιφος", "καρλομαγν", "joyeuse", "charlemagne", "sword"),
                    new SearchResultDto(
                            "Ξίφος του Καρλομάγνου (Joyeuse)",
                            "Τελετουργικό βασιλικό ξίφος που χρησιμοποιήθηκε στις στέψεις των Γάλλων βασιλέων και εκτίθεται στο Μουσείο του Λούβρου.",
                            null,
                            "Αρχείο Μουσείου Λούβρου (Fallback)",
                            "Βασιλικοί Θησαυροί Γαλλίας",
                            "10ος–13ος αιώνας"
                    )
            )
    );

    @Autowired
    public ExhibitSearchService(ObjectMapper objectMapper) {
        this(
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(API_TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build()
        );
    }

    // Package-private constructor για εύκολο mocking στα unit tests
    public ExhibitSearchService(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public List<SearchResultDto> search(String rawQuery) {
        if (rawQuery == null) {
            return List.of();
        }

        String trimmed = rawQuery.trim();
        if (trimmed.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }

        String cacheKey = normalize(trimmed);
        List<SearchResultDto> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<SearchResultDto> results = fetchFromWikipedia(trimmed);

        if (results.isEmpty()) {
            results = searchFallback(cacheKey);
        }

        List<SearchResultDto> immutableResults = List.copyOf(results);
        cache.put(cacheKey, immutableResults);
        return immutableResults;
    }

    private List<SearchResultDto> fetchFromWikipedia(String query) {
        String lang = GREEK_CHARS.matcher(query).matches() ? "el" : "en";
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://" + lang + ".wikipedia.org/w/rest.php/v1/search/page?q="
                + encodedQuery + "&limit=4";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(API_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "EscapeHub/1.0 (IHU University Project)")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                return List.of();
            }

            return parseWikipediaResponse(response.body(), lang);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
    }

    private List<SearchResultDto> parseWikipediaResponse(String jsonBody, String lang) throws IOException {
        JsonNode root = objectMapper.readTree(jsonBody);
        JsonNode pages = root.path("pages");

        if (!pages.isArray() || pages.isEmpty()) {
            return List.of();
        }

        List<SearchResultDto> results = new ArrayList<>();
        String sourceLabel = "el".equals(lang) ? "Wikipedia (EL)" : "Wikipedia (EN)";

        for (JsonNode page : pages) {
            String title = page.path("title").asText("").trim();
            if (title.isBlank()) {
                continue;
            }

            String description = page.path("description").asText("").trim();
            String excerpt = cleanHtml(page.path("excerpt").asText(""));
            String summary = !excerpt.isBlank() ? excerpt : description;

            String rawThumb = page.path("thumbnail").path("url").asText("").trim();
            String imageUrl = rawThumb.startsWith("//") ? "https:" + rawThumb : (rawThumb.isBlank() ? null : rawThumb);

            results.add(new SearchResultDto(
                    title,
                    summary,
                    imageUrl,
                    sourceLabel,
                    null,
                    null
            ));
        }

        return results;
    }

    private List<SearchResultDto> searchFallback(String normalizedQuery) {
        String[] tokens = normalizedQuery.split("\\s+");
        List<SearchResultDto> matches = new ArrayList<>();

        for (FallbackEntry entry : FALLBACK_EXHIBITS) {
            boolean matched = false;

            for (String keyword : entry.keywords()) {
                if (normalizedQuery.contains(keyword)) {
                    matched = true;
                    break;
                }
                for (String token : tokens) {
                    if (token.length() >= 3 && (keyword.contains(token) || token.contains(keyword))) {
                        matched = true;
                        break;
                    }
                }
                if (matched) {
                    break;
                }
            }

            if (matched) {
                matches.add(entry.dto());
            }
        }

        return matches;
    }

    private String cleanHtml(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return HTML_TAGS.matcher(text)
                .replaceAll("")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
                .trim();
    }

    private String normalize(String input) {
        String decomposed = Normalizer.normalize(input.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("");
    }
}