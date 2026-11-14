package gr.ihu.escapehub.profile.controller;

import gr.ihu.escapehub.profile.dto.UpdateProfileDto;
import gr.ihu.escapehub.profile.entity.Profile;
import gr.ihu.escapehub.profile.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<Profile> getProfile() {
        // TODO: Εδώ θα συνδέσουμε το JWT Token από τον κώδικα που γράφει το Άτομο 1
        Long tempUserId = 1L; // Προσωρινό ID μέχρι να ενωθεί ο κώδικας

        try {
            Profile profile = profileService.getProfile(tempUserId);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping
    public ResponseEntity<Profile> updateProfile(@RequestBody UpdateProfileDto request) {
        // TODO: Το ίδιο, εδώ θα διαβάζουμε ποιος είναι ο χρήστης από το JWT Token
        Long tempUserId = 1L;

        try {
            Profile updatedProfile = profileService.updateProfile(tempUserId, request);
            return ResponseEntity.ok(updatedProfile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}