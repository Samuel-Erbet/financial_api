package com.example.financial_api.demo.repository;

import com.example.financial_api.demo.entity.Transaction;
import com.example.financial_api.demo.entity.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    List<Transaction> findByUserId(Long id);
    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<Transaction> findByUserIdAndType(Long userId, Type type);
}
