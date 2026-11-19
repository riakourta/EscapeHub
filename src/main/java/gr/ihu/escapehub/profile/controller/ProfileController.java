package gr.ihu.escapehub.profile.controller;

import gr.ihu.escapehub.auth.entity.User;
import gr.ihu.escapehub.auth.service.AuthService;
import gr.ihu.escapehub.profile.dto.UpdateProfileDto;
import gr.ihu.escapehub.profile.entity.Profile;
import gr.ihu.escapehub.profile.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final AuthService authService;

    public ProfileController(ProfileService profileService, AuthService authService) {
        this.profileService = profileService;
        this.authService = authService;
    }
    private Long resolveUserId(User principalUser, String authHeader) {
        if (principalUser != null) {
            return principalUser.getId();
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                return authService.extractUserIdFromToken(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
    @GetMapping("/colors")
    public ResponseEntity<List<String>> getAvailableColors() {
        return ResponseEntity.ok(ProfileService.ALLOWED_COLORS);
    }

    @GetMapping
    public ResponseEntity<Profile> getProfile(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = resolveUserId(user, authHeader);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Profile profile = profileService.getProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateProfileDto request) {
        Long userId = resolveUserId(user, authHeader);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Profile updatedProfile = profileService.updateProfile(userId, request);
            return ResponseEntity.ok(updatedProfile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}