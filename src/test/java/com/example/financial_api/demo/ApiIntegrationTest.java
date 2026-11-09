package com.example.financial_api.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.financial_api.demo.entity.user.User;
import com.example.financial_api.demo.repository.UserRepository;
import com.example.financial_api.demo.service.JwtService;

import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;

class ApiIntegrationTest {
    static org.springframework.context.ConfigurableApplicationContext context;
    static MockMvc mvc;

    // SpringExtension in Spring 7 requires JUnit 6; start explicitly to retain JUnit 5.
    @BeforeAll
    static void start() {
        context =
                new org.springframework.boot.builder.SpringApplicationBuilder(DemoApplication.class)
                        .run("--server.port=0");
        mvc =
                org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(
                                (org.springframework.web.context.WebApplicationContext) context)
                        .apply(
                                org.springframework.security.test.web.servlet.setup
                                        .SecurityMockMvcConfigurers.springSecurity())
                        .build();
    }

    @AfterAll
    static void stop() {
        if (context != null) {
            context.close();
        }
    }

    @BeforeEach
    void setup() {
        users = context.getBean(UserRepository.class);
        jwt = context.getBean(JwtService.class);
        context.getBean(com.example.financial_api.demo.repository.TransactionRepository.class)
                .deleteAll();
        users.deleteAll();
    }

    UserRepository users;
    JwtService jwt;

    String bearer() {
        User user =
                users.saveAndFlush(new User("Test", "integration@example.com", "test-only-hash"));
        return "Bearer " + jwt.generateToken(user);
    }

    @Test
    void signupLoginAndDuplicateEmail() throws Exception {
        String registration =
                "{\"name\":\"Test\",\"email\":\"signup@example.com\",\"password\":\"test-password\"}";
        mvc.perform(post("/auth/signup").contentType("application/json").content(registration))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("signup@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(
                        post("/auth/login")
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"signup@example.com\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiresIn").value(3600000));
        mvc.perform(post("/auth/signup").contentType("application/json").content(registration))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidPasswordIsNotEchoedInResponse() throws Exception {
        mvc.perform(
                        post("/auth/signup")
                                .contentType("application/json")
                                .content(
                                        "{\"name\":\"Test\",\"email\":\"test@example.com\",\"password\":\""
                                                + "x".repeat(73)
                                                + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"Invalid request data\"}"));
    }

    @Test
    void anonymousRequestReturns401() throws Exception {
        mvc.perform(get("/transactions/list")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mvc.perform(get("/transactions/list").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationRejectsInvalidRegistration() throws Exception {
        mvc.perform(
                        post("/auth/signup")
                                .contentType("application/json")
                                .content("{\"name\":\"\",\"email\":\"invalid\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongCredentialsReturn401() throws Exception {
        mvc.perform(
                        post("/auth/login")
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"missing@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightIsAccepted() throws Exception {
        mvc.perform(
                        options("/transactions")
                                .header("Origin", "http://localhost:8080")
                                .header("Access-Control-Request-Method", "POST")
                                .header(
                                        "Access-Control-Request-Headers",
                                        "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"));
    }

    @Test
    void transactionPersistsUsingMigrationsAndDoesNotExposePassword() throws Exception {
        String token = bearer();
        mvc.perform(
                        post("/transactions")
                                .header("Authorization", token)
                                .contentType("application/json")
                                .content(
                                        "{\"date\":\"2024-02-29\",\"description\":\"Lunch\",\"amount\":12.35,\"type\":\"EXPENSE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.user.password").doesNotExist());
        mvc.perform(get("/transactions/summary?year=2024&month=2").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(1))
                .andExpect(jsonPath("$.balance").value(-12.35));
        mvc.perform(get("/transactions/list").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(12.35))
                .andExpect(jsonPath("$[0].user.password").doesNotExist());
    }

    @Test
    void invalidPeriodReturns400() throws Exception {
        mvc.perform(
                        get("/transactions/summary?year=2024&month=13")
                                .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
    }
}
