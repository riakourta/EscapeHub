package gr.ihu.escapehub.mission.controller;

import gr.ihu.escapehub.mission.dto.MissionDto;
import gr.ihu.escapehub.mission.service.MissionService;
import gr.ihu.escapehub.movement.handler.MovementHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;
    private final MovementHandler movementHandler;

    public MissionController(MissionService missionService, MovementHandler movementHandler) {
        this.missionService = missionService;
        this.movementHandler = movementHandler;
    }

    @GetMapping
    public ResponseEntity<List<MissionDto>> getMissions() {
        return ResponseEntity.ok(missionService.getAllMissions());
    }

    @PostMapping("/{missionId}/complete")
    public ResponseEntity<?> completeMission(@PathVariable String missionId) {
        return missionService.completeMission(missionId)
                .map(updated -> {
                    List<MissionDto> all = missionService.getAllMissions();
                    movementHandler.broadcastMissionUpdated(updated, all);
                    return ResponseEntity.ok(Map.of("mission", updated, "missions", all));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
