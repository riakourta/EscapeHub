package gr.ihu.escapehub.movement.service;

import gr.ihu.escapehub.auth.entity.User;
import gr.ihu.escapehub.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class MovementTokenService {

    private final UserRepository userRepository;
    private final JwtParser jwtParser;

    public MovementTokenService(
            UserRepository userRepository,
            @Value("${jwt.secret:EscapeHubSuperSecretKeyForJwtSigning2026MuseumHeist!}")
            String jwtSecret
    ) {
        this.userRepository = userRepository;

        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(
                        Keys.hmacShaKeyFor(
                                jwtSecret.getBytes(StandardCharsets.UTF_8)
                        )
                )
                .build();
    }

    public User authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new SecurityException("Απαιτείται token σύνδεσης.");
        }

        Claims claims;

        try {
            var parsedToken = jwtParser.parseClaimsJws(token);

            if (!"HS256".equals(parsedToken.getHeader().getAlgorithm())) {
                throw new SecurityException(
                        "Μη αποδεκτός αλγόριθμος token."
                );
            }

            claims = parsedToken.getBody();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new SecurityException(
                    "Το token είναι μη έγκυρο ή έχει λήξει."
            );
        }

        String username = claims.getSubject();
        Object tokenUserId = claims.get("userId");

        if (username == null || username.isBlank()
                || claims.getExpiration() == null
                || !(tokenUserId instanceof Number)) {
            throw new SecurityException(
                    "Το token δεν περιέχει τα απαραίτητα στοιχεία."
            );
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityException(
                        "Ο χρήστης δεν υπάρχει."
                ));

        long claimedId = ((Number) tokenUserId).longValue();

        if (user.getId() == null || user.getId().longValue() != claimedId) {
            throw new SecurityException(
                    "Η ταυτότητα του χρήστη δεν συμφωνεί."
            );
        }

        return user;
    }
}