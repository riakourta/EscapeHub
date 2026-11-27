package gr.ihu.escapehub.mission.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.mission.dto.MissionDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MissionService {

    private static final String MISSIONS_PATH = "missions/room1_missions.json";
    private final ObjectMapper objectMapper;
    private final Map<String, MissionDto> missions = new ConcurrentHashMap<>();

    public MissionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        loadMissions();
    }
    private record RoomMissionsFile(int roomId, List<MissionDto> missions) {}
    private void loadMissions() {
        try (InputStream is = new ClassPathResource(MISSIONS_PATH).getInputStream()) {
            RoomMissionsFile file = objectMapper.readValue(is, RoomMissionsFile.class);
            missions.clear();
            if (file.missions() != null) {
                for (MissionDto mission : file.missions()) {
                    missions.put(mission.getId(), mission);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Αποτυχία φόρτωσης αποστολών από " + MISSIONS_PATH, e);
        }
    }

    public List<MissionDto> getAllMissions() {
        return missions.values().stream()
                .sorted(Comparator.comparingInt(MissionDto::getOrder))
                .map(MissionDto::copy)
                .toList();
    }

    public Optional<MissionDto> completeMission(String missionId) {
        if (missionId == null) return Optional.empty();
        MissionDto mission = missions.get(missionId);
        if (mission == null) return Optional.empty();

        synchronized (mission) {
            mission.setCompleted(true);
            return Optional.of(mission.copy());
        }
    }

    public void resetMissions() {
        missions.values().forEach(m -> {
            synchronized (m) {
                m.setCompleted(false);
            }
        });
    }
}