package gr.ihu.escapehub.profile.repository;

import gr.ihu.escapehub.auth.entity.User;
import gr.ihu.escapehub.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUser(User user);

}
