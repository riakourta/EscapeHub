package gr.ihu.escapehub.exhibit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.exhibit.dto.ExhibitInfoDto;
import gr.ihu.escapehub.exhibit.dto.ExhibitState;
import gr.ihu.escapehub.movement.dto.PlayerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExhibitService {

    private static final String DEFAULT_EXHIBITS_RESOURCE = "missions/room1_exhibits.json";
    private static final double PLAYER_SIZE = 30;
    private static final double REACH = 25;

    private final Map<String, ExhibitState> exhibits = new LinkedHashMap<>();
    private final Map<String, ExhibitInfoDto> exhibitDetails = new LinkedHashMap<>();

    @Autowired
    public ExhibitService() {
        this(DEFAULT_EXHIBITS_RESOURCE, new ObjectMapper());
    }

    public ExhibitService(String resourcePath, ObjectMapper objectMapper) {
        loadExhibitsFromJson(resourcePath, objectMapper);
    }

    private void loadExhibitsFromJson(String resourcePath, ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            List<ExhibitInfoDto> loaded = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<ExhibitInfoDto>>() {}
            );

            for (ExhibitInfoDto info : loaded) {
                ExhibitInfoDto normalized = new ExhibitInfoDto(
                        info.id(),
                        info.label(),
                        info.name(),
                        info.category(),
                        info.artist(),
                        info.date(),
                        info.imageUrl(),
                        info.description(),
                        info.securityNote(),
                        info.x(),
                        info.y(),
                        info.width(),
                        info.height(),
                        info.clues() == null ? List.of() : List.copyOf(info.clues())
                );
                exhibitDetails.put(normalized.id(), normalized);

                exhibits.put(normalized.id(), new ExhibitState(
                        normalized.id(),
                        normalized.label(),
                        normalized.x(),
                        normalized.y(),
                        normalized.width(),
                        normalized.height(),
                        false,
                        null
                ));
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Αποτυχία φόρτωσης εκθεμάτων από το αρχείο: " + resourcePath,
                    e
            );
        }
    }

    public synchronized List<ExhibitState> getExhibits() {
        return List.copyOf(exhibits.values());
    }

    public synchronized List<ExhibitInfoDto> getAllExhibitDetails() {
        return List.copyOf(exhibitDetails.values());
    }

    public synchronized Optional<ExhibitInfoDto> getExhibitDetailsById(String exhibitId) {
        if (exhibitId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(exhibitDetails.get(exhibitId));
    }

    public synchronized ExhibitState collect(
            String exhibitId,
            PlayerState player
    ) {
        if (player == null) {
            throw new IllegalArgumentException(
                    "Ο παίκτης δεν βρίσκεται στο δωμάτιο."
            );
        }

        ExhibitState exhibit = exhibits.get(exhibitId);

        if (exhibit == null) {
            throw new IllegalArgumentException(
                    "Το έκθεμα δεν υπάρχει."
            );
        }

        if (exhibit.collected()) {
            throw new IllegalStateException(
                    "Το έκθεμα έχει ήδη συλλεχθεί."
            );
        }

        if (!isNearby(player, exhibit)) {
            throw new IllegalStateException(
                    "Πλησίασε το έκθεμα για να το συλλέξεις."
            );
        }

        ExhibitState collected = new ExhibitState(
                exhibit.id(),
                exhibit.label(),
                exhibit.x(),
                exhibit.y(),
                exhibit.width(),
                exhibit.height(),
                true,
                player.id()
        );

        exhibits.put(exhibitId, collected);
        return collected;
    }

    private boolean isNearby(
            PlayerState player,
            ExhibitState exhibit
    ) {
        double left = player.x() - REACH;
        double top = player.y() - REACH;
        double right = player.x() + PLAYER_SIZE + REACH;
        double bottom = player.y() + PLAYER_SIZE + REACH;

        return left < exhibit.x() + exhibit.width()
                && right > exhibit.x()
                && top < exhibit.y() + exhibit.height()
                && bottom > exhibit.y();
    }
}