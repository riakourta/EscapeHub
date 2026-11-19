package gr.ihu.escapehub.movement.service;

import gr.ihu.escapehub.movement.dto.PlayerState;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MovementStateService {

    private static final int MAX_PLAYERS = 6;

    private final Map<String, PlayerState> players =
            new LinkedHashMap<>();

    public synchronized void addPlayer(PlayerState player) {
        if (players.containsKey(player.id())) {
            throw new IllegalStateException(
                    "Ο παίκτης είναι ήδη συνδεδεμένος."
            );
        }

        if (players.size() >= MAX_PLAYERS) {
            throw new IllegalStateException(
                    "Το δωμάτιο είναι γεμάτο."
            );
        }

        players.put(player.id(), player);
    }

    public synchronized Optional<PlayerState> findPlayer(String id) {
        return Optional.ofNullable(players.get(id));
    }

    public synchronized PlayerState updatePosition(
            String id,
            double x,
            double y,
            String direction
    ) {
        PlayerState current = players.get(id);

        if (current == null) {
            throw new IllegalStateException(
                    "Ο παίκτης δεν βρίσκεται στο δωμάτιο."
            );
        }

        PlayerState updated = new PlayerState(
                current.id(),
                current.nickname(),
                current.color(),
                x,
                y,
                direction
        );

        players.put(id, updated);
        return updated;
    }

    public synchronized void removePlayer(String id) {
        players.remove(id);
    }

    public synchronized List<PlayerState> getPlayers() {
        return List.copyOf(players.values());
    }
}