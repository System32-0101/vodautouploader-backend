package dev.dmrstudio.vod_autouploader.controller;

import dev.dmrstudio.vod_autouploader.dto.CreateUserRequest;
import dev.dmrstudio.vod_autouploader.model.User;
import dev.dmrstudio.vod_autouploader.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    public ResponseEntity<User> createUser(
            @Valid
            @RequestBody CreateUserRequest request
    ) {
        User savedUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }
}
