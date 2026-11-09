package com.example.financial_api.demo.controller;

import com.example.financial_api.demo.entity.transaction.LoginResponse;
import com.example.financial_api.demo.entity.user.LoginUserDTO;
import com.example.financial_api.demo.entity.user.RegisterUserDTO;
import com.example.financial_api.demo.entity.user.User;
import com.example.financial_api.demo.entity.user.UserResponseDTO;
import com.example.financial_api.demo.repository.UserRepository;
import com.example.financial_api.demo.service.AuthService;
import com.example.financial_api.demo.service.JwtService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(
            JwtService jwtService, AuthService authService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> Login(@Valid @RequestBody LoginUserDTO loginUserDTO) {
        User authenticatedUser = authService.authenticate(loginUserDTO);
        String jwtToken = jwtService.generateToken(authenticatedUser);

        LoginResponse response = new LoginResponse();
        response.setToken(jwtToken);
        response.setExpiresIn(jwtService.getExpirationTime());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDTO> signup(@Valid @RequestBody RegisterUserDTO data) {

        User registeredUSer = authService.signup(data);

        return ResponseEntity.ok(
                new UserResponseDTO(
                        registeredUSer.getId(),
                        registeredUSer.getName(),
                        registeredUSer.getEmail()));
    }
}
