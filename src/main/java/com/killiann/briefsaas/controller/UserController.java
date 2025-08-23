package com.killiann.briefsaas.controller;

import com.killiann.briefsaas.dto.UpdateProfileRequest;
import com.killiann.briefsaas.dto.UserResponse;
import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.repository.UserRepository;
import com.killiann.briefsaas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateMe(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        User user = getUser(authentication);

        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setProfileImage(request.getProfileImage());

        userRepository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteCurrentUser(Authentication authentication) {
        User user = getUser(authentication);
        userService.delete(user);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getProfileImage(),
                user.getCurrentPriceId(),
                user.isSubscriptionActive(),
                user.isCancelAtPeriodEnd(),
                user.getSubscriptionEndAt()
        );
    }
}