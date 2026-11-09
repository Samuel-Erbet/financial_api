package com.example.financial_api.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.financial_api.demo.entity.user.*;
import com.example.financial_api.demo.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    UserRepository repository;
    @Mock
    PasswordEncoder encoder;
    @Mock
    AuthenticationManager manager;
    @InjectMocks
    AuthService service;

    RegisterUserDTO registration() {
        return new RegisterUserDTO("Test", "test@example.com", "test-password");
    }

    LoginUserDTO login() {
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("test-password");
        return dto;
    }

    @Test
    void rejectsPasswordLongerThanBcryptByteLimit() {
        RegisterUserDTO input = registration();
        input.setPassword("á".repeat(37));
        assertEquals(
                400,
                assertThrows(ResponseStatusException.class, () -> service.signup(input))
                        .getStatusCode()
                        .value());
        LoginUserDTO login = login();
        login.setPassword(input.getPassword());
        assertThrows(BadCredentialsException.class, () -> service.authenticate(login));
        verifyNoInteractions(repository, encoder, manager);
    }

    @Test
    void signupHashesPassword() {
        when(encoder.encode("test-password")).thenReturn("hashed");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User result = service.signup(registration());
        assertEquals("hashed", result.getPassword());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void duplicateEmailReturns409WithoutSaving() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.of(new User()));
        assertEquals(
                409,
                assertThrows(ResponseStatusException.class, () -> service.signup(registration()))
                        .getStatusCode()
                        .value());
        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void concurrentDuplicateReturns409() {
        when(repository.findByEmail(anyString()))
                .thenReturn(Optional.empty(), Optional.of(new User()));
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertEquals(
                409,
                assertThrows(ResponseStatusException.class, () -> service.signup(registration()))
                        .getStatusCode()
                        .value());
    }

    @Test
    void otherDatabaseFailureIsNotMisreportedAsDuplicate() {
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("failure"));
        assertThrows(DataIntegrityViolationException.class, () -> service.signup(registration()));
    }

    @Test
    void authenticateChecksCredentials() {
        User user = new User();
        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        assertSame(user, service.authenticate(login()));
        verify(manager)
                .authenticate(
                        argThat(
                                auth ->
                                        auth.getName().equals("test@example.com")
                                                && auth.getCredentials().equals("test-password")));
    }

    @Test
    void wrongPasswordDoesNotReadUser() {
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        assertThrows(BadCredentialsException.class, () -> service.authenticate(login()));
        verifyNoInteractions(repository);
    }

    @Test
    void missingUserIsAuthenticationFailure() {
        assertThrows(BadCredentialsException.class, () -> service.authenticate(login()));
    }
}
