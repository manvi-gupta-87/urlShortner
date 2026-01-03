package com.urlshortener.service;

import com.urlshortener.dto.AuthResponse;
import com.urlshortener.dto.LoginRequest;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.model.Role;
import com.urlshortener.model.User;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 * Tests authentication and registration business logic.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword123")
                .role(Role.USER)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }

    @Nested
    @DisplayName("register tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void register_Success() {
            // Given
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token-123");

            // When
            AuthResponse result = authService.register(registerRequest);

            // Then
            assertNotNull(result);
            assertEquals("jwt-token-123", result.getToken());
            assertNotNull(result.getUsername());

            // Verify user was saved with correct data
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertEquals("newuser", savedUser.getUsername());
            assertEquals("newuser@example.com", savedUser.getEmail());
            assertEquals("encodedPassword", savedUser.getPassword());
            assertEquals(Role.USER, savedUser.getRole());
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void register_ThrowsException_WhenUsernameExists() {
            // Given
            when(userRepository.existsByUsername("newuser")).thenReturn(true);

            // When / Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    authService.register(registerRequest)
            );

            assertTrue(exception.getMessage().contains("Username already exists"));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void register_ThrowsException_WhenEmailExists() {
            // Given
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

            // When / Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    authService.register(registerRequest)
            );

            assertTrue(exception.getMessage().contains("Email already exists"));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should encode password before saving")
        void register_EncodesPassword() {
            // Given
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedHash");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("token");

            // When
            authService.register(registerRequest);

            // Then
            verify(passwordEncoder).encode("password123");
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals("$2a$10$encodedHash", userCaptor.getValue().getPassword());
        }
    }

    @Nested
    @DisplayName("login tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_Success() {
            // Given
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null); // Authentication success
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(testUser)).thenReturn("jwt-token-456");

            // When
            AuthResponse result = authService.login(loginRequest);

            // Then
            assertNotNull(result);
            assertEquals("jwt-token-456", result.getToken());
            assertEquals("testuser", result.getUsername());

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Should throw exception with invalid credentials")
        void login_ThrowsException_WhenInvalidCredentials() {
            // Given
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When / Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    authService.login(loginRequest)
            );

            assertTrue(exception.getMessage().contains("Invalid username or password"));
            verify(userRepository, never()).findByUsername(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found after authentication")
        void login_ThrowsException_WhenUserNotFound() {
            // Given
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            // When / Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    authService.login(loginRequest)
            );

            assertTrue(exception.getMessage().contains("User not found"));
        }
    }

    @Nested
    @DisplayName("findByUsername tests")
    class FindByUsernameTests {

        @Test
        @DisplayName("Should return user when found")
        void findByUsername_ReturnsUser_WhenFound() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            Optional<User> result = authService.findByUsername("testuser");

            // Then
            assertTrue(result.isPresent());
            assertEquals("testuser", result.get().getUsername());
        }

        @Test
        @DisplayName("Should return empty when user not found")
        void findByUsername_ReturnsEmpty_WhenNotFound() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When
            Optional<User> result = authService.findByUsername("nonexistent");

            // Then
            assertTrue(result.isEmpty());
        }
    }
}
