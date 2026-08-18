package net.ankan.budget.service.impl;

import lombok.RequiredArgsConstructor;
import net.ankan.budget.dto.*;
import net.ankan.budget.entity.User;
import net.ankan.budget.exception.EmailAlreadyExistsException;
import net.ankan.budget.exception.ResourceNotFoundException;
import net.ankan.budget.entity.PasswordResetToken;
import net.ankan.budget.repository.PasswordResetTokenRepository;
import net.ankan.budget.repository.UserRepository;
import net.ankan.budget.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.reset.expiry-minutes:30}")
    private long resetExpiryMinutes;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getFullName(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getFullName(), user.getEmail());
    }

    public void changePassword(ChangePasswordRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Always completes without telling the caller whether the address is
     * registered. Revealing that turns the endpoint into a free account
     * enumeration tool, which is why there is no "no such user" branch here.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> maybeUser = userRepository.findByEmail(request.getEmail());
        if (maybeUser.isEmpty()) {
            return;
        }
        User user = maybeUser.get();

        // Requesting a new link kills any earlier one.
        resetTokenRepository.invalidateAllForUser(user, LocalDateTime.now());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        resetTokenRepository.save(PasswordResetToken.builder()
                .tokenHash(sha256(rawToken))
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(resetExpiryMinutes))
                .build());

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository
                .findByTokenHash(sha256(request.getToken()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "This reset link is invalid or has already been used"));

        if (!token.isUsable()) {
            throw new IllegalArgumentException(
                    "This reset link has expired. Request a new one.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Burns this token and any other outstanding one for the account in a
        // single statement, so a second email sitting in the inbox is dead too.
        resetTokenRepository.invalidateAllForUser(user, LocalDateTime.now());
    }

    /** Hex SHA-256. Only hashes are persisted, never the token itself. */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
