package dev.dmrstudio.vod_autouploader.repository;

import dev.dmrstudio.vod_autouploader.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> getUserByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
}
