package gr.ihu.escapehub.movement;

import gr.ihu.escapehub.auth.entity.User;
import gr.ihu.escapehub.auth.repository.UserRepository;
import gr.ihu.escapehub.movement.service.MovementTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovementTokenServiceTest {

    private static final String SECRET =
            "EscapeHubSuperSecretKeyForJwtSigning2026MuseumHeist!";

    private UserRepository repository;
    private MovementTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        service = new MovementTokenService(repository, SECRET);
    }

    private String token(String username, long userId, long expiresAt) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setExpiration(new Date(expiresAt))
                .signWith(
                        Keys.hmacShaKeyFor(
                                SECRET.getBytes(StandardCharsets.UTF_8)
                        ),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    @Test
    void acceptsValidTokenForExistingUser() {
        User user = new User("aggelos", "unused-password-hash");
        user.setId(1L);

        when(repository.findByUsername("aggelos"))
                .thenReturn(Optional.of(user));

        String jwt = token(
                "aggelos", 1L,
                System.currentTimeMillis() + 60_000
        );

        assertSame(user, service.authenticate(jwt));
    }

    @Test
    void rejectsExpiredTokenBeforeLookingUpUser() {
        String jwt = token(
                "aggelos", 1L,
                System.currentTimeMillis() - 60_000
        );

        assertThrows(
                SecurityException.class,
                () -> service.authenticate(jwt)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsTokenWhoseUserIdDoesNotMatchDatabase() {
        User user = new User("aggelos", "unused-password-hash");
        user.setId(1L);

        when(repository.findByUsername("aggelos"))
                .thenReturn(Optional.of(user));

        String jwt = token(
                "aggelos", 999L,
                System.currentTimeMillis() + 60_000
        );

        assertThrows(
                SecurityException.class,
                () -> service.authenticate(jwt)
        );
    }

    @Test
    void rejectsMissingAndMalformedTokens() {
        assertThrows(
                SecurityException.class,
                () -> service.authenticate(null)
        );

        assertThrows(
                SecurityException.class,
                () -> service.authenticate("not-a-valid-token")
        );

        verifyNoInteractions(repository);
    }
}