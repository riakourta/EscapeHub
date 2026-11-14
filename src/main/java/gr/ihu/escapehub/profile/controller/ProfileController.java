package gr.ihu.escapehub.profile.controller;

import gr.ihu.escapehub.profile.dto.UpdateProfileDto;
import gr.ihu.escapehub.profile.entity.Profile;
import gr.ihu.escapehub.profile.service.ProfileService;
import java.security.Principal;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    private static final Set<String> COLORS = Set.of("Red", "Blue", "Green", "Yellow");
    public ProfileController(ProfileService profileService) { this.profileService = profileService; }
    @GetMapping
    public ResponseEntity<Profile> getProfile(Principal principal) {
        return ResponseEntity.ok(profileService.getProfile(principal.getName()));
    }
    @PutMapping
    public ResponseEntity<Profile> updateProfile(Principal principal, @RequestBody UpdateProfileDto request) {
        if (request.getNickname() == null || request.getNickname().isBlank()
                || request.getNickname().length() > 40 || request.getAvatarColor() == null
                || !COLORS.contains(request.getAvatarColor())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(profileService.updateProfile(principal.getName(), request));
    }
}
