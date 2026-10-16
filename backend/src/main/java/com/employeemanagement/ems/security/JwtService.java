package com.employeemanagement.ems.security;

import com.employeemanagement.ems.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.jwt.secret:EMS_Assignment3_JWT_Secret_2026_ChangeThisKey_AtLeast_32_Chars}") String secret,
            @Value("${app.jwt.expiration-hours:4}") long expirationHours) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 characters.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = Math.max(1, expirationHours) * 60 * 60 * 1000L;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("name", user.getName())
                .claim("role", "EMPLOYER")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String getEmail(String token) {
        try {
            Jws<Claims> parsed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            String email = parsed.getPayload().getSubject();
            if (email == null || email.isBlank()) throw new IllegalArgumentException("Invalid JWT subject.");
            return email;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid or expired JWT.");
        }
    }
}
