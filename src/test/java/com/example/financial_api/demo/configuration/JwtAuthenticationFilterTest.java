package com.example.financial_api.demo.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.financial_api.demo.entity.user.User;
import com.example.financial_api.demo.service.JwtService;

import io.jsonwebtoken.MalformedJwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    JwtService jwt;
    @Mock
    UserDetailsService users;
    @Mock
    HandlerExceptionResolver resolver;
    @Mock
    FilterChain chain;
    JwtAuthenticationFilter filter;
    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(resolver, jwt, users);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    void token() {
        request.addHeader("Authorization", "Bearer test-token");
    }

    @Test
    void noTokenContinuesWithoutAuthentication() throws Exception {
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwt, users);
    }

    @Test
    void validTokenAuthenticatesUser() throws Exception {
        token();
        User user = new User("Test", "test@example.com", "unused");
        when(jwt.extractUsername("test-token")).thenReturn(user.getEmail());
        when(users.loadUserByUsername(user.getEmail())).thenReturn(user);
        when(jwt.isTokenValid("test-token", user)).thenReturn(true);
        filter.doFilter(request, response, chain);
        assertSame(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        token();
        when(jwt.extractUsername(anyString())).thenThrow(new MalformedJwtException("invalid"));
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain, resolver);
    }

    @Test
    void missingUserReturns401() throws Exception {
        token();
        when(jwt.extractUsername(anyString())).thenReturn("deleted@example.com");
        when(users.loadUserByUsername(anyString()))
                .thenThrow(new UsernameNotFoundException("deleted"));
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        token();
        when(jwt.extractUsername(anyString())).thenReturn("test@example.com");
        when(users.loadUserByUsername(anyString())).thenReturn(new User());
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void downstreamFailureIsNotSwallowed() throws Exception {
        token();
        when(jwt.extractUsername(anyString())).thenReturn(null);
        doThrow(new ServletException("controller failure")).when(chain).doFilter(request, response);
        assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));
        verifyNoInteractions(resolver);
    }
}
