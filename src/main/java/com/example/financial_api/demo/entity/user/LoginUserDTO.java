package com.example.financial_api.demo.entity.user;

public class LoginUserDTO {

    private String email;
    private String password;

    public LoginUserDTO() {
    }


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
