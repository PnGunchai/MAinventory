package com.inventory.controller;

import com.inventory.model.User;
import com.inventory.security.JwtUtil;
import com.inventory.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://mainventory.vercel.app", "https://*.vercel.app"}, allowCredentials = "true")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userService.save(user);
        String token = jwtUtil.generateToken(savedUser.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", savedUser);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login attempt for username: {}", loginRequest.username());
            
            var userOptional = userService.findByUsername(loginRequest.username());
            if (userOptional.isEmpty()) {
                log.warn("User not found: {}", loginRequest.username());
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid username or password"));
            }

            var user = userOptional.get();
            log.info("Found user: {}, stored password hash: {}", user.getUsername(), user.getPassword());
            
            boolean passwordMatches = passwordEncoder.matches(loginRequest.password(), user.getPassword());
            log.info("Password match result: {}", passwordMatches);

            if (!passwordMatches) {
                log.warn("Password does not match for user: {}", loginRequest.username());
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid username or password"));
            }

            String token = jwtUtil.generateToken(user.getUsername());
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", user);
            
            log.info("Login successful for user: {}", loginRequest.username());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Login failed: " + e.getMessage()));
        }
    }
}

record LoginRequest(String username, String password) {} 