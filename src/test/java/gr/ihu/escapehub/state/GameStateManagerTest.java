package gr.ihu.escapehub.state;

import gr.ihu.escapehub.state.service.GameStateManager;
import gr.ihu.escapehub.state.service.GameStateManager.LeaveResult;
import gr.ihu.escapehub.state.service.GameStateManager.PlayerLeaveOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateManagerTest {

    @Test
    void gameContinuesWhenDroppingFromFourToThreePlayers() {
        GameStateManager manager = new GameStateManager(3600, 3);
        manager.addPlayer("1");
        manager.addPlayer("2");
        manager.addPlayer("3");
        manager.addPlayer("4");

        LeaveResult result = manager.removePlayer("4");

        assertEquals(PlayerLeaveOutcome.CONTINUE, result.outcome());
        assertEquals(3, result.remainingPlayers());
        assertTrue(manager.isGameActive());
    }

    @Test
    void gameStopsWhenDroppingFromThreeToTwoPlayers() {
        GameStateManager manager = new GameStateManager(3600, 3);
        manager.addPlayer("1");
        manager.addPlayer("2");
        manager.addPlayer("3");

        LeaveResult result = manager.removePlayer("3");

        assertEquals(PlayerLeaveOutcome.GAME_STOPPED, result.outcome());
        assertEquals(2, result.remainingPlayers());
        assertFalse(manager.isGameActive());
        assertNotNull(result.message());
    }
}
