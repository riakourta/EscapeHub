package gr.ihu.escapehub.profile.service;

import gr.ihu.escapehub.profile.dto.UpdateProfileDto;
import gr.ihu.escapehub.profile.entity.Profile;
import gr.ihu.escapehub.profile.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final ProfileRepository profileRepository;
    public ProfileService(ProfileRepository profileRepository) { this.profileRepository = profileRepository; }

    @Transactional
    public Profile getProfile(String username) {
        return profileRepository.findByOwnerUsername(username).orElseGet(() -> {
            Profile profile = new Profile();
            profile.setOwnerUsername(username);
            profile.setNickname(username);
            profile.setAvatarColor("Red");
            return profileRepository.save(profile);
        });
    }

    @Transactional
    public Profile updateProfile(String username, UpdateProfileDto request) {
        Profile profile = getProfile(username);
        profile.setNickname(request.getNickname().trim());
        profile.setAvatarColor(request.getAvatarColor());
        return profileRepository.save(profile);
    }
}
