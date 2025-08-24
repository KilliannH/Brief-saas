package com.killiann.briefsaas.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository emailTokenRepository;

    @Value("${google.client.id}")
    private String googleClientId;

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

    public AuthResponse googleLogin(Map<String, String> body) {
        String idToken = body.get("idToken");

        try {
            // Vérifier et décoder le token Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);

            if (googleIdToken == null) {
                throw new IllegalArgumentException("Token Google invalide");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            // Extraire les informations utilisateur
            String email = payload.getEmail();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");
            String profileImage = (String) payload.get("picture");
            String googleId = payload.getSubject();

            // Vérifier si l'utilisateur existe déjà
            Optional<User> existingUser = userRepository.findByEmail(email);

            User user;
            if (existingUser.isPresent()) {
                user = existingUser.get();

                // Mettre à jour les informations Google si nécessaire
                if (user.getGoogleId() == null) {
                    user.setGoogleId(googleId);
                }
                if (user.getProfileImage() == null && profileImage != null) {
                    user.setProfileImage(profileImage);
                }

                user = userRepository.save(user);
            } else {
                // Créer un nouveau utilisateur
                user = new User();
                user.setEmail(email);
                user.setRole(Role.ROLE_USER);
                user.setFirstname(firstName);
                user.setLastname(lastName);
                user.setProfileImage(profileImage);
                user.setGoogleId(googleId);
                user.setEnabled(true); // Google vérifie déjà l'email
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Mot de passe aléatoire
                user.setCreatedAt(LocalDateTime.now());

                user = userRepository.save(user);
            }

            // Générer le JWT
            String jwt = jwtUtil.generateToken(user.getEmail());

            return new AuthResponse(jwt);

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Erreur lors de la vérification du token Google", e);
        }
    }
}