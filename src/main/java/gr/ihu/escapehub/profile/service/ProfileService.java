package gr.ihu.escapehub.profile.service;

import gr.ihu.escapehub.profile.dto.UpdateProfileDto;
import gr.ihu.escapehub.profile.entity.Profile;
import gr.ihu.escapehub.profile.repository.ProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public Profile getProfile(Long userId) {
        /*
        Όταν ο φίλος σου ανεβάσει το User entity, ο κώδικας θα είναι κάπως έτσι:
        User user = userRepository.findById(userId).orElseThrow();
        return profileRepository.findByUser(user).orElseThrow();
        */

        // Προσωρινός κώδικας για να μην χτυπάει μέχρι να μπει το User
        return profileRepository.findById(userId).orElseThrow(() -> new RuntimeException("Profile not found"));
    }

    public Profile updateProfile(Long userId, UpdateProfileDto request) {
        Profile profile = getProfile(userId);

        profile.setNickname(request.getNickname());
        profile.setAvatarColor(request.getAvatarColor());

        return profileRepository.save(profile);
    }
}
