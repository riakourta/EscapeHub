package gr.ihu.escapehub.auth.service;

import gr.ihu.escapehub.auth.dto.LoginDto;
import gr.ihu.escapehub.auth.dto.RegisterDto;
import gr.ihu.escapehub.auth.entity.User;
import gr.ihu.escapehub.auth.repository.UserRepository;
import gr.ihu.escapehub.profile.entity.Profile;
import gr.ihu.escapehub.profile.repository.ProfileRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${jwt.secret:EscapeHubSuperSecretKeyForJwtSigning2026MuseumHeist!}")
    private String jwtSecret;

    @Value("${jwt.expirationMs:86400000}")
    private long jwtExpirationMs;

    public AuthService(UserRepository userRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
    @Transactional
    public void register(RegisterDto request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username and password cannot be empty");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User newUser = new User(request.getUsername(), hashedPassword);
        User savedUser = userRepository.save(newUser);

        Profile defaultProfile = new Profile(savedUser, savedUser.getUsername(), "RED");
        profileRepository.save(defaultProfile);
    }

    public String login(LoginDto request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new SecurityException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new SecurityException("Invalid username or password");
        }

        Key signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
    public Long extractUserIdFromToken(String token) {
        Key signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }
}