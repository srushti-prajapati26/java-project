package com.employeemanagement.ems.service;

import com.employeemanagement.ems.model.User;
import com.employeemanagement.ems.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L} .'-]{1,99}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    void initialize() {
        if (userRepository.findByEmail("admin@ems.com").isEmpty()) {
            register("EMS Employer", "admin@ems.com", "admin123", "EMPLOYER");
        }
    }

    public synchronized User register(String name, String email, String password, String role) {
        String cleanName = name == null ? "" : name.trim();
        String normalized = normalizeEmail(email);
        if (!NAME.matcher(cleanName).matches()) throw new IllegalArgumentException("Name must be 2-100 letters, spaces, apostrophes, periods or hyphens.");
        if (!EMAIL.matcher(normalized).matches() || normalized.length() > 150) throw new IllegalArgumentException("Enter a valid email address.");
        if (password == null || password.length() < 8 || password.length() > 72) throw new IllegalArgumentException("Password must contain 8-72 characters.");
        if (userRepository.findByEmail(normalized).isPresent()) throw new IllegalArgumentException("An account with this email already exists.");

        String safeRole = "EMPLOYER";
        return userRepository.save(new User(cleanName, normalized, passwordEncoder.encode(password), safeRole));
    }

    public User authenticate(String email, String password) {
        User user = findByEmail(email);
        if (user == null || password == null || !matchesAndUpgrade(password, user)) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return user;
    }

    public User findByEmail(String email) { return userRepository.findByEmail(normalizeEmail(email)).orElse(null); }

    private boolean matchesAndUpgrade(String rawPassword, User user) {
        String stored = user.getPasswordHash();
        if (stored == null) return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, stored);
        }

        if (stored.contains(":")) {
            String[] parts = stored.split(":", 2);
            if (parts.length == 2) {
                String legacy = sha256(rawPassword, parts[1]);
                if (MessageDigest.isEqual(legacy.getBytes(StandardCharsets.UTF_8), parts[0].getBytes(StandardCharsets.UTF_8))) {
                    user.setPasswordHash(passwordEncoder.encode(rawPassword));
                    userRepository.save(user);
                    return true;
                }
            }
        }
        return false;
    }

    private String sha256(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to verify password.", ex);
        }
    }

    private String normalizeEmail(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
}
