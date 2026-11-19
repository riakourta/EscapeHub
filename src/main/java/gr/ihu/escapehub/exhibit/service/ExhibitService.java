package gr.ihu.escapehub.exhibit.service;

import gr.ihu.escapehub.exhibit.dto.ExhibitState;
import gr.ihu.escapehub.movement.dto.PlayerState;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExhibitService {

    private static final double PLAYER_SIZE = 30;
    private static final double REACH = 25;

    private final Map<String, ExhibitState> exhibits =
            new LinkedHashMap<>();

    public ExhibitService() {
        addExhibit(
                "exhibit-1", "Διαμάντι Regent",
                180, 150, 70, 70
        );

        addExhibit(
                "exhibit-2", "Στέμμα Λουδοβίκου",
                520, 150, 70, 70
        );

        addExhibit(
                "exhibit-3", "Ξίφος Καρλομάγνου",
                350, 360, 90, 70
        );
    }

    private void addExhibit(
            String id,
            String label,
            double x,
            double y,
            double width,
            double height
    ) {
        exhibits.put(id, new ExhibitState(
                id, label, x, y, width, height,
                false, null
        ));
    }

    public synchronized List<ExhibitState> getExhibits() {
        return List.copyOf(exhibits.values());
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