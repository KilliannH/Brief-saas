package com.killiann.briefsaas.service;

import com.killiann.briefsaas.dto.AuthResponse;
import com.killiann.briefsaas.dto.LoginRequest;
import com.killiann.briefsaas.dto.SignupRequest;
import com.killiann.briefsaas.entity.EmailVerificationToken;
import com.killiann.briefsaas.entity.Role;
import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.repository.UserRepository;
import com.killiann.briefsaas.repository.EmailVerificationTokenRepository;
import com.killiann.briefsaas.util.DisposableEmailChecker;
import com.killiann.briefsaas.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository emailTokenRepository;
    private final MailService mailService;
    private final DisposableEmailChecker disposableEmailChecker;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        if (disposableEmailChecker.isDisposable(request.getEmail())) {
            throw new RuntimeException("Disposable email addresses are not allowed.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .profileImage(request.getProfileImage())
                .language(request.getLanguage())
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);

        String _token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(_token);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailTokenRepository.save(verificationToken);

        mailService.sendVerificationEmail(user, _token);

        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthResponse(token);
    }

    @Transactional
    public AuthResponse verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalide ou expiré"));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expiré");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        return new AuthResponse(token);
    }
}