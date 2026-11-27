package gr.ihu.escapehub.exhibit;

import gr.ihu.escapehub.exhibit.dto.ExhibitInfoDto;
import gr.ihu.escapehub.exhibit.service.ExhibitService;
import gr.ihu.escapehub.movement.dto.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExhibitServiceTest {

    private final ExhibitService service = new ExhibitService();

    @Test
    void nearbyPlayerCanCollectExhibit() {
        var collected = service.collect(
                "exhibit-1", player("1", 150, 150)
        );

        assertTrue(collected.collected());
        assertEquals("1", collected.collectedBy());

        var stored = service.getExhibits().stream()
                .filter(exhibit -> exhibit.id().equals("exhibit-1"))
                .findFirst()
                .orElseThrow();

        assertEquals(collected, stored);
    }

    @Test
    void distantPlayerCannotCollectExhibit() {
        assertThrows(
                IllegalStateException.class,
                () -> service.collect(
                        "exhibit-1", player("1", 100, 500)
                )
        );

        assertTrue(service.getExhibits().stream()
                .noneMatch(exhibit -> exhibit.collected()));
    }

    @Test
    void exhibitCannotBeCollectedTwice() {
        service.collect("exhibit-1", player("1", 150, 150));

        assertThrows(
                IllegalStateException.class,
                () -> service.collect(
                        "exhibit-1", player("2", 150, 150)
                )
        );

        var stored = service.getExhibits().stream()
                .filter(exhibit -> exhibit.id().equals("exhibit-1"))
                .findFirst()
                .orElseThrow();

        assertEquals("1", stored.collectedBy());
    }

    @Test
    void unknownExhibitIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.collect(
                        "unknown", player("1", 150, 150)
                )
        );
    }

    @Test
    void loadsAllExhibitDetailsDynamicallyFromJson() {
        List<ExhibitInfoDto> details = service.getAllExhibitDetails();

        assertEquals(5, details.size());
        assertEquals(5, service.getExhibits().size());

        Optional<ExhibitInfoDto> crown = service.getExhibitDetailsById("exhibit-2");
        assertTrue(crown.isPresent());
        assertEquals("Στέμμα του Λουδοβίκου ΙΕ'", crown.get().name());
        assertFalse(crown.get().clues().isEmpty());
    }

    @Test
    void returnsEmptyDetailsForUnknownOrNullId() {
        assertTrue(service.getExhibitDetailsById("unknown").isEmpty());
        assertTrue(service.getExhibitDetailsById(null).isEmpty());
    }

    private PlayerState player(String id, double x, double y) {
        return new PlayerState(
                id, "Player " + id, "blue", x, y, "up"
        );
    }
}