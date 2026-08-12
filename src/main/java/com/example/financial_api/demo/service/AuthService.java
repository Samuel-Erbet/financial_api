package com.example.financial_api.demo.service;

import com.example.financial_api.demo.entity.user.LoginUserDTO;
import com.example.financial_api.demo.entity.user.RegisterUserDTO;
import com.example.financial_api.demo.entity.user.User;
import com.example.financial_api.demo.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService  {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;


    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public User signup(RegisterUserDTO input){
        User user = new User(
                input.getName(),
                input.getEmail(),
                passwordEncoder.encode(input.getPassword())
        );

        return userRepository.save(user);
    }

    public User authenticate (LoginUserDTO input){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        return userRepository.findByEmail(input.getEmail())
                .orElseThrow();
    }
}
