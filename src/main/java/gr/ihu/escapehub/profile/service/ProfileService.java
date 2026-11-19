package gr.ihu.escapehub.profile.service;

import gr.ihu.escapehub.auth.entity.User;
import gr.ihu.escapehub.auth.repository.UserRepository;
import gr.ihu.escapehub.profile.dto.UpdateProfileDto;
import gr.ihu.escapehub.profile.entity.Profile;
import gr.ihu.escapehub.profile.repository.ProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {
    public static final List<String> ALLOWED_COLORS = List.of(
            "RED", "BLUE", "GREEN", "YELLOW", "PURPLE", "PINK"
    );
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    public Profile getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return profileRepository.findByUser(user)
                .orElseGet(() -> profileRepository.save(new Profile(user, user.getUsername(), "RED")));
    }

    public Profile updateProfile(Long userId, UpdateProfileDto request) {
        if (request.getNickname() == null || request.getNickname().isBlank()) {
            throw new IllegalArgumentException("Nickname cannot be empty");
        }

        if (request.getAvatarColor() == null || !ALLOWED_COLORS.contains(request.getAvatarColor().toUpperCase())) {
            throw new IllegalArgumentException("Invalid color. Allowed colors: " + ALLOWED_COLORS);
        }

        Profile profile = getProfile(userId);
        profile.setNickname(request.getNickname().trim());
        profile.setAvatarColor(request.getAvatarColor().toUpperCase());

        return profileRepository.save(profile);
    }
}
