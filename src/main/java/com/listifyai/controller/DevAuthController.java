package com.listifyai.controller;

import com.listifyai.entity.User;
import com.listifyai.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class DevAuthController {

    private final UserService userService;
    private final boolean devAuthEnabled;
    private final String devEmail;
    private final String devAdminEmail;
    private final SecretKey signingKey;

    public DevAuthController(
            UserService userService,
            @Value("${dev.auth.enabled:false}") boolean devAuthEnabled,
            @Value("${dev.auth.email:developer@listifyai.local}") String devEmail,
            @Value("${dev.auth.admin-email:admin@tagwing.local}") String devAdminEmail,
            @Value("${supabase.jwt.secret}") String jwtSecret) {
        this.userService = userService;
        this.devAuthEnabled = devAuthEnabled;
        this.devEmail = devEmail;
        this.devAdminEmail = devAdminEmail;
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/dev-login")
    public ResponseEntity<Map<String, Object>> devLogin(@RequestParam(defaultValue = "false") boolean admin) {
        if (!devAuthEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dev login is not enabled.");
        }

        String email = admin ? devAdminEmail : devEmail;
        String supabaseId = (admin ? "dev-admin:" : "dev:") + email;
        User user = admin
                ? userService.findOrCreateDeveloperAdminUser(supabaseId, email)
                : userService.findOrCreateDeveloperUser(supabaseId, email);
        Instant expiresAt = Instant.now().plus(12, ChronoUnit.HOURS);
        String token = Jwts.builder()
                .setSubject(supabaseId)
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(expiresAt))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "expiresAt", expiresAt.toString(),
                "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "plan", user.getPlan(),
                        "role", user.getRole()
                )
        ));
    }

    @PostMapping("/dev-admin-login")
    public ResponseEntity<Map<String, Object>> devAdminLogin() {
        return devLogin(true);
    }
}
