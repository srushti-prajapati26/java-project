package com.employeemanagement.ems.security;

import com.employeemanagement.ems.model.User;
import com.employeemanagement.ems.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AuthService authService;

    public JwtAuthenticationFilter(JwtService jwtService, AuthService authService) {
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String email = jwtService.getEmail(header.substring(7));
                User user = authService.findByEmail(email);
                if (user != null) {
                    var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());
                    var authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (IllegalArgumentException ignored) {
                // The secured endpoint will return 401 when no valid authentication is present.
            }
        }
        filterChain.doFilter(request, response);
    }
}
