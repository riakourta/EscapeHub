package gr.ihu.escapehub.auth;

import gr.ihu.escapehub.auth.dto.LoginDto;
import gr.ihu.escapehub.auth.dto.RegisterDto;
import gr.ihu.escapehub.auth.entity.User;
import gr.ihu.escapehub.auth.repository.UserRepository;
import gr.ihu.escapehub.auth.service.AuthService;
import gr.ihu.escapehub.profile.entity.Profile;
import gr.ihu.escapehub.profile.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private AuthService authService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtSecret",
                "EscapeHubSuperSecretKeyForJwtSigning2026MuseumHeist!");
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 86400000L);
    }

    @Test
    void register_ShouldSaveUserAndDefaultProfile_WhenUsernameDoesNotExist() {
        RegisterDto dto = new RegisterDto("robber1", "secret123");
        when(userRepository.findByUsername("robber1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        authService.register(dto);

        verify(userRepository, times(1)).save(any(User.class));
        verify(profileRepository, times(1)).save(any(Profile.class));
    }

    @Test
    void register_ShouldThrowException_WhenUsernameAlreadyExists() {
        RegisterDto dto = new RegisterDto("robber1", "secret123");
        when(userRepository.findByUsername("robber1"))
                .thenReturn(Optional.of(new User("robber1", "hashed")));

        assertThrows(IllegalArgumentException.class, () -> authService.register(dto));
        verify(userRepository, never()).save(any(User.class));
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void login_ShouldReturnJwtTokenAndExtractUserId_WhenCredentialsAreValid() {
        String rawPassword = "correctPassword";
        String hashedPassword = encoder.encode(rawPassword);
        User mockUser = new User("robber1", hashedPassword);
        mockUser.setId(42L);

        when(userRepository.findByUsername("robber1")).thenReturn(Optional.of(mockUser));

        String token = authService.login(new LoginDto("robber1", rawPassword));

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
        assertEquals(42L, authService.extractUserIdFromToken(token));
    }

    @Test
    void login_ShouldThrowSecurityException_WhenPasswordIsWrong() {
        String hashedPassword = encoder.encode("correctPassword");
        User mockUser = new User("robber1", hashedPassword);

        when(userRepository.findByUsername("robber1")).thenReturn(Optional.of(mockUser));

        assertThrows(SecurityException.class,
                () -> authService.login(new LoginDto("robber1", "wrongPassword")));
    }
}