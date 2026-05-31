package com.linguachat.service;

import com.linguachat.dto.AuthResponse;
import com.linguachat.dto.LoginRequest;
import com.linguachat.dto.RegisterRequest;
import com.linguachat.entity.User;
import com.linguachat.exception.AppException;
import com.linguachat.repository.UserRepository;
import com.linguachat.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AppException("Username already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException("Email already exists", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPreferredLanguage(request.preferredLanguage());
        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved.getUsername());
        return new AuthResponse(token, saved.getId(), saved.getUsername(), saved.getEmail(), saved.getPreferredLanguage());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password()));
        User user = userRepository.findByUsername(request.usernameOrEmail())
                .or(() -> userRepository.findByEmail(request.usernameOrEmail()))
                .orElseThrow(() -> new AppException("Invalid login", HttpStatus.UNAUTHORIZED));

        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getPreferredLanguage());
    }
}
