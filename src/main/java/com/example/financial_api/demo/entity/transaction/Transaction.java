package com.example.financial_api.demo.entity.transaction;

import com.example.financial_api.demo.entity.user.User;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private String description;

    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(precision = 10, scale = 2)
    private Double amount;

    @ManyToOne(cascade = CascadeType.MERGE)
    private User user;

    @Enumerated(EnumType.STRING)
    private Type type;

    @CreationTimestamp private LocalDate createdAt;

    public Transaction(LocalDate date, String description, Double amount, User user, Type type) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.user = user;
        this.type = type;
    }

    public Transaction() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
