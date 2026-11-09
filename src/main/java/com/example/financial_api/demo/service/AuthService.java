package com.example.financial_api.demo.service;

import com.example.financial_api.demo.entity.user.LoginUserDTO;
import com.example.financial_api.demo.entity.user.RegisterUserDTO;
import com.example.financial_api.demo.entity.user.User;
import com.example.financial_api.demo.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public User signup(RegisterUserDTO input) {
        if (input.getPassword() == null
                || input.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                        > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password exceeds the supported length");
        }
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user =
                new User(
                        input.getName(),
                        input.getEmail(),
                        passwordEncoder.encode(input.getPassword()));

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            if (userRepository.findByEmail(input.getEmail()).isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Email already registered", ex);
            }
            throw ex;
        }
    }

    public User authenticate(LoginUserDTO input) {
        if (input.getPassword() == null
                || input.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                        > 72) {
            throw new BadCredentialsException("Invalid credentials");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword()));

        return userRepository
                .findByEmail(input.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    }
}
