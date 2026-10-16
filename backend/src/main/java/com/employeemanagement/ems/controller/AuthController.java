package com.employeemanagement.ems.controller;

import com.employeemanagement.ems.model.AuthRequests.LoginRequest;
import com.employeemanagement.ems.model.AuthRequests.RegisterRequest;
import com.employeemanagement.ems.model.User;
import com.employeemanagement.ems.security.JwtService;
import com.employeemanagement.ems.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        authService.register(request.getName(), request.getEmail(), request.getPassword(), "EMPLOYER");
        return ResponseEntity.ok(Map.of("message", "Registration successful. Please login.", "status", "SUCCESS"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            User user = authService.authenticate(request.getEmail(), request.getPassword());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Login successful.");
            response.put("status", "SUCCESS");
            response.put("token", jwtService.generateToken(user));
            response.put("user", publicUser(user));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage(), "status", "ERROR"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException("Missing JWT.");
            String email = jwtService.getEmail(authorization.substring(7));
            User user = authService.findByEmail(email);
            if (user == null) throw new IllegalArgumentException("User not found.");
            return ResponseEntity.ok(publicUser(user));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage(), "status", "UNAUTHORIZED"));
        }
    }

    private Map<String, Object> publicUser(User user) {
        return Map.of("name", user.getName(), "email", user.getEmail(), "role", "EMPLOYER");
    }
}
