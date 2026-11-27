package gr.ihu.escapehub.movement.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.auth.entity.User;
import gr.ihu.escapehub.exhibit.service.ExhibitService;
import gr.ihu.escapehub.journal.service.JournalService;
import gr.ihu.escapehub.movement.dto.PlayerState;
import gr.ihu.escapehub.movement.service.MovementStateService;
import gr.ihu.escapehub.movement.service.MovementTokenService;
import gr.ihu.escapehub.puzzle.service.PuzzleService;
import gr.ihu.escapehub.mission.dto.MissionDto;
import gr.ihu.escapehub.state.service.GameStateManager;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MovementHandler extends TextWebSocketHandler {

    private static final Set<String> COLORS =
            Set.of("blue", "pink", "purple", "green", "red", "yellow");

    private static final Set<String> DIRECTIONS =
            Set.of("up", "down", "left", "right");

    private final ObjectMapper mapper;
    private final MovementStateService state;
    private final MovementTokenService tokens;
    private final ExhibitService exhibits;
    private final PuzzleService puzzles;
    private final JournalService journal;
    private final GameStateManager gameStateManager;

    private final Map<String, WebSocketSession> sessions =
            new LinkedHashMap<>();

    private final Map<String, String> playerIds =
            new LinkedHashMap<>();
    public MovementHandler(
            ObjectMapper mapper,
            MovementStateService state,
            MovementTokenService tokens,
            ExhibitService exhibits,
            PuzzleService puzzles,
            JournalService journal
    )
    {
        this(mapper, state, tokens, exhibits, puzzles, journal, new GameStateManager(3600, 3));
    }
    @Autowired
    public MovementHandler(
            ObjectMapper mapper,
            MovementStateService state,
            MovementTokenService tokens,
            ExhibitService exhibits,
            PuzzleService puzzles,
            JournalService journal,
            GameStateManager gameStateManager
    ) {
        this.mapper = mapper;
        this.state = state;
        this.tokens = tokens;
        this.exhibits = exhibits;
        this.puzzles = puzzles;
        this.journal = journal;
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(4096);
    }

    @Override
    protected synchronized void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) throws Exception {
        JsonNode event;

        try {
            event = mapper.readTree(message.getPayload());
        } catch (IOException exception) {
            error(session, "INVALID_JSON", "Μη έγκυρο JSON.");
            return;
        }

        if (event == null || !event.isObject()) {
            error(session, "INVALID_MESSAGE", "Αναμένεται αντικείμενο JSON.");
            return;
        }

        String type = event.path("type").asText();

        if ("join".equals(type)) {
            join(session, event);
            return;
        }

        if (!playerIds.containsKey(session.getId())) {
            error(session, "AUTH_REQUIRED", "Πρέπει πρώτα να γίνει join.");
            return;
        }

        if ("move".equals(type)) {
            move(session, event);
        } else if ("collect".equals(type)) {
            collect(session, event);
        } else {
            error(session, "UNKNOWN_TYPE", "Άγνωστος τύπος μηνύματος.");
        }
    }

    private void join(
            WebSocketSession session,
            JsonNode event
    ) throws IOException {
        if (playerIds.containsKey(session.getId())) {
            error(session, "ALREADY_JOINED", "Έχεις ήδη συνδεθεί.");
            return;
        }

        User user;

        try {
            user = tokens.authenticate(event.path("token").asText());
        } catch (SecurityException exception) {
            error(session, "INVALID_TOKEN", exception.getMessage());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String color = event.path("color").asText("blue");
        String nickname = event.path("nickname")
                .asText(user.getUsername()).trim();

        if (!COLORS.contains(color)
                || nickname.isBlank()
                || nickname.length() > 40) {
            error(session, "INVALID_PLAYER", "Μη έγκυρο όνομα ή χρώμα.");
            return;
        }

        String playerId = user.getId().toString();

        PlayerState player = new PlayerState(
                playerId,
                nickname,
                color,
                100,
                500,
                "up"
        );

        try {
            state.addPlayer(player);
        } catch (IllegalStateException exception) {
            error(session, "JOIN_REJECTED", exception.getMessage());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        sessions.put(session.getId(), session);
        playerIds.put(session.getId(), playerId);
        gameStateManager.addPlayer(playerId);

        send(session, Map.of(
                "type", "welcome",
                "player", player
        ));

        updatePuzzle();
        broadcastSnapshot();
    }

    private void move(
            WebSocketSession session,
            JsonNode event
    ) throws IOException {
        JsonNode xNode = event.path("x");
        JsonNode yNode = event.path("y");
        String direction = event.path("direction").asText();

        if (!xNode.isNumber() || !yNode.isNumber()) {
            error(
                    session,
                    "INVALID_POSITION",
                    "Τα x και y πρέπει να είναι αριθμοί."
            );
            return;
        }

        double x = xNode.asDouble();
        double y = yNode.asDouble();

        // Αίθουσα 800×600, τοίχοι 20px και χαρακτήρας 30px.
        if (!Double.isFinite(x) || !Double.isFinite(y)
                || x < 20 || x > 750
                || y < 20 || y > 550
                || !DIRECTIONS.contains(direction)) {
            error(
                    session,
                    "INVALID_MOVEMENT",
                    "Μη έγκυρη θέση ή κατεύθυνση."
            );
            return;
        }

        String playerId = playerIds.get(session.getId());

        PlayerState updated = state.updatePosition(
                playerId,
                x,
                y,
                direction
        );

        Map<String, Object> response = Map.of(
                "type", "move",
                "player", updated
        );

        for (WebSocketSession peer : List.copyOf(sessions.values())) {
            if (!peer.getId().equals(session.getId())) {
                sendSafely(peer, response);
            }
        }

        updatePuzzle();
    }

    private void collect(
            WebSocketSession session,
            JsonNode event
    ) throws IOException {
        JsonNode exhibitId = event.path("exhibitId");

        if (!exhibitId.isTextual() || exhibitId.asText().isBlank()) {
            error(session, "INVALID_EXHIBIT", "Απαιτείται exhibitId.");
            return;
        }

        String playerId = playerIds.get(session.getId());
        PlayerState player = state.findPlayer(playerId).orElse(null);

        try {
            var collected = exhibits.collect(exhibitId.asText(), player);

            journal.recordExhibit(collected.id(), collected.label());

            Map<String, Object> response = Map.of(
                    "type", "exhibit_collected",
                    "exhibit", collected
            );

            for (WebSocketSession peer : List.copyOf(sessions.values())) {
                sendSafely(peer, response);
            }

            broadcastJournal();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            error(session, "COLLECT_REJECTED", exception.getMessage());
        }
    }

    private void updatePuzzle() {
        var previous = puzzles.getState();
        var updated = puzzles.evaluate(state.getPlayers());

        boolean newlySolved = !previous.solved() && updated.solved();

        if (newlySolved) {
            journal.recordPuzzle(updated.id(), updated.label());
        }

        if (previous.equals(updated)) {
            return;
        }

        Map<String, Object> response = Map.of(
                "type", "puzzle_state",
                "puzzle", updated
        );

        for (WebSocketSession peer : List.copyOf(sessions.values())) {
            sendSafely(peer, response);
        }

        if (newlySolved) {
            broadcastJournal();
        }
    }

    private void broadcastJournal() {
        Map<String, Object> response = Map.of(
                "type", "journal_state",
                "entries", journal.getEntries()
        );

        for (WebSocketSession peer : List.copyOf(sessions.values())) {
            sendSafely(peer, response);
        }
    }
    public void broadcastMissionUpdated(MissionDto mission, List<MissionDto> missions) {
        Map<String, Object> response = Map.of(
                "type", "MISSION_UPDATED",
                "mission", mission,
                "missions", missions
        );

        for (WebSocketSession peer : List.copyOf(sessions.values())) {
            sendSafely(peer, response);
        }
    }

    @Override
    public synchronized void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) throws IOException {
        sessions.remove(session.getId());
        String playerId = playerIds.remove(session.getId());

        if (playerId != null) {
            state.removePlayer(playerId);
            updatePuzzle();
            broadcastSnapshot();
            GameStateManager.LeaveResult leaveResult = gameStateManager.removePlayer(playerId);
            if (leaveResult.outcome() == GameStateManager.PlayerLeaveOutcome.GAME_STOPPED) {
                Map<String, Object> abortMsg = Map.of(
                        "type", "GAME_ABORTED",
                        "remainingPlayers", leaveResult.remainingPlayers(),
                        "reason", leaveResult.message()
                );
                for (WebSocketSession peer : List.copyOf(sessions.values())) {
                    sendSafely(peer, abortMsg);
                }
            }
        }
    }

    private void broadcastSnapshot() {
        Map<String, Object> response = Map.of(
                "type", "snapshot",
                "players", state.getPlayers(),
                "exhibits", exhibits.getExhibits(),
                "puzzle", puzzles.getState(),
                "journal", journal.getEntries()
        );

        for (WebSocketSession session : List.copyOf(sessions.values())) {
            sendSafely(session, response);
        }
    }

    private void error(
            WebSocketSession session,
            String code,
            String message
    ) throws IOException {
        send(session, Map.of(
                "type", "error",
                "code", code,
                "message", message
        ));
    }

    private void send(
            WebSocketSession session,
            Object data
    ) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(
                    new TextMessage(mapper.writeValueAsString(data))
            );
        }
    }

    private void sendSafely(WebSocketSession session, Object data) {
        try {
            send(session, data);
        } catch (IOException exception) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException ignored) {
                // Η σύνδεση έχει ήδη διακοπεί.
            }
        }
    }
}