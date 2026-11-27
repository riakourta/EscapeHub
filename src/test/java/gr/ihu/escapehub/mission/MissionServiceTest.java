package gr.ihu.escapehub.mission;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.mission.dto.MissionDto;
import gr.ihu.escapehub.mission.service.MissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MissionServiceTest {

    private MissionService missionService;

    @BeforeEach
    void setUp() {
        missionService = new MissionService(new ObjectMapper());
    }

    @Test
    void loadsMissionsFromJsonInOrder() {
        List<MissionDto> missions = missionService.getAllMissions();
        assertEquals(3, missions.size());
        assertEquals("mission-1-time", missions.get(0).getId());
        assertFalse(missions.get(0).isCompleted());
    }

    @Test
    void completesExistingMission() {
        Optional<MissionDto> updated = missionService.completeMission("mission-1-time");
        assertTrue(updated.isPresent());
        assertTrue(updated.get().isCompleted());

        MissionDto stored = missionService.getAllMissions().stream()
                .filter(m -> m.getId().equals("mission-1-time"))
                .findFirst()
                .orElseThrow();
        assertTrue(stored.isCompleted());
    }

    @Test
    void returnsEmptyForUnknownMissionId() {
        Optional<MissionDto> result = missionService.completeMission("non-existent-id");
        assertTrue(result.isEmpty());
    }
}
