package dev.dmrstudio.vod_autouploader.service;

import dev.dmrstudio.vod_autouploader.dto.CreateUserRequest;
import dev.dmrstudio.vod_autouploader.model.User;
import dev.dmrstudio.vod_autouploader.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }

    public User createUser(CreateUserRequest requestUser) {
        User user = new User(null, requestUser.getEmail(), requestUser.getName(), requestUser.getGoogleId(), null, null, null);
        return userRepository.save(user);
    }

    public User findOrCreateUser(String googleId, String email, String name,
                                 String youtubeAccessToken, LocalDateTime youtubeTokenExpiry) {
        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> new User(null, email, name, googleId, null, null, null));

        user.setYoutubeAccessToken(youtubeAccessToken);
        user.setYoutubeTokenExpiry(youtubeTokenExpiry);
        return userRepository.save(user);
    }
}
