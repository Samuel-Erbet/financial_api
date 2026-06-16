package com.example.financial_api.demo.repository;

import com.example.financial_api.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
}
