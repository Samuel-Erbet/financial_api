package com.example.financial_api.demo.entity.user;

import jakarta.validation.constraints.*;

public class LoginUserDTO {

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(max = 72)
    private String password;

    public LoginUserDTO() {}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
