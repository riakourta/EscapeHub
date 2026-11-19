package gr.ihu.escapehub.state.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GameStateManager {

    private final AtomicInteger remainingTime;
    private final int minPlayers;
    private final Set<String> activePlayers = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean gameActive = new AtomicBoolean(false);

    public enum PlayerLeaveOutcome {
        CONTINUE,
        GAME_STOPPED,
        IGNORED
    }

    public record LeaveResult(
            PlayerLeaveOutcome outcome,
            int remainingPlayers,
            String message
    ) {}

    public GameStateManager(
            @Value("${escapehub.robbery.duration:3600}") int duration,
            @Value("${escapehub.lobby.min-players:3}") int minPlayers) {
        this.remainingTime = new AtomicInteger(duration);
        this.minPlayers = minPlayers;
    }

    public int getRemainingTime() {
        return remainingTime.get();
    }

    public void decrementTime() {
        if (remainingTime.get() > 0) {
            remainingTime.decrementAndGet();
        }
    }

    public void startGame(Collection<String> initialPlayers) {
        activePlayers.clear();
        if (initialPlayers != null) {
            activePlayers.addAll(initialPlayers);
        }
        gameActive.set(true);
    }

    public void addPlayer(String playerId) {
        if (playerId != null && !playerId.isBlank()) {
            activePlayers.add(playerId.trim());
        }
    }

    public LeaveResult removePlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return new LeaveResult(PlayerLeaveOutcome.IGNORED, activePlayers.size(), null);
        }

        boolean removed = activePlayers.remove(playerId.trim());
        if (!removed) {
            return new LeaveResult(PlayerLeaveOutcome.IGNORED, activePlayers.size(), null);
        }

        int currentCount = activePlayers.size();
        if (!gameActive.get()) {
            return new LeaveResult(PlayerLeaveOutcome.IGNORED, currentCount, null);
        }

        if (currentCount < minPlayers) {
            gameActive.set(false);
            return new LeaveResult(
                    PlayerLeaveOutcome.GAME_STOPPED,
                    currentCount,
                    "Το παιχνίδι τερματίστηκε/τέθηκε σε παύση καθώς έμειναν " + currentCount + " ενεργοί παίκτες (απαιτούνται τουλάχιστον " + minPlayers + ")."
            );
        }

        return new LeaveResult(
                PlayerLeaveOutcome.CONTINUE,
                currentCount,
                "Ο παίκτης αποχώρησε. Το παιχνίδι συνεχίζεται κανονικά με " + currentCount + " παίκτες."
        );
    }

    public int getActivePlayersCount() {
        return activePlayers.size();
    }

    public boolean isGameActive() {
        return gameActive.get();
    }
}