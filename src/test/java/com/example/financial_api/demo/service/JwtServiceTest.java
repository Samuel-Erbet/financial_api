package com.example.financial_api.demo.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.financial_api.demo.entity.user.User;

import io.jsonwebtoken.Jwts;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import javax.crypto.SecretKey;

class JwtServiceTest {
    JwtService service;
    SecretKey key;
    User user = new User("Test", "test@example.com", "unused");

    @BeforeEach
    void setup() {
        service = new JwtService();
        key = Jwts.SIG.HS256.key().build();
        ReflectionTestUtils.setField(
                service, "secretKey", Base64.getEncoder().encodeToString(key.getEncoded()));
        ReflectionTestUtils.setField(service, "jwtExpiration", 3600000L);
    }

    @Test
    void generatedTokenContainsSubjectAndCustomClaims() {
        String token = service.generateToken(Map.of("role", "test"), user);
        assertEquals(user.getEmail(), service.extractUsername(token));
        assertEquals("test", service.extractClaim(token, claims -> claims.get("role")));
        assertTrue(service.isTokenValid(token, user));
        assertEquals(3600000, service.getExpirationTime());
    }

    @Test
    void differentUserIsRejected() {
        assertFalse(
                service.isTokenValid(
                        service.generateToken(user),
                        new User("Other", "other@example.com", "unused")));
    }

    @Test
    void expiredTokenIsRejected() {
        String token =
                Jwts.builder()
                        .subject(user.getEmail())
                        .expiration(new Date(1))
                        .signWith(key)
                        .compact();
        assertFalse(service.isTokenValid(token, user));
    }

    @Test
    void invalidSignatureIsRejected() {
        String token =
                Jwts.builder()
                        .subject(user.getEmail())
                        .expiration(new Date(System.currentTimeMillis() + 60000))
                        .signWith(Jwts.SIG.HS256.key().build())
                        .compact();
        assertFalse(service.isTokenValid(token, user));
    }

    @Test
    void malformedTokenIsRejected() {
        assertFalse(service.isTokenValid("invalid", user));
    }

    @Test
    void missingSubjectOrExpiryIsRejected() {
        assertFalse(
                service.isTokenValid(
                        Jwts.builder()
                                .expiration(new Date(System.currentTimeMillis() + 60000))
                                .signWith(key)
                                .compact(),
                        user));
        assertFalse(
                service.isTokenValid(
                        Jwts.builder().subject(user.getEmail()).signWith(key).compact(), user));
    }
}
