package com.example.financial_api.demo.service;

import com.example.financial_api.demo.entity.transaction.*;
import com.example.financial_api.demo.entity.user.User;
import com.example.financial_api.demo.repository.TransactionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction createTransaction(
            LocalDate date, String description, Double amount, User user, Type type) {
        validate(date, description, amount, type);
        return transactionRepository.save(new Transaction(date, description, amount, user, type));
    }

    public List<Transaction> listTransactions(Long id) {
        return transactionRepository.findByUserId(id);
    }

    public List<Transaction> filterTransactionsByMonth(Long userId, int year, int month) {
        YearMonth period = period(year, month);
        return transactionRepository.findByUserIdAndDateBetween(
                userId, period.atDay(1), period.atEndOfMonth());
    }

    public ResponseEntity<Void> deleteTransaction(Long transactionId, User user) {
        Transaction transaction = ownedTransaction(transactionId, user.getId());
        transactionRepository.delete(transaction);
        return ResponseEntity.noContent().build();
    }

    public List<Transaction> filterTransactionsByType(Long userId, Type type) {
        return type == null
                ? listTransactions(userId)
                : transactionRepository.findByUserIdAndType(userId, type);
    }

    public Transaction updateTransaction(Long id, Transaction input, Long userId) {
        Transaction transaction = ownedTransaction(id, userId);
        validate(input.getDate(), input.getDescription(), input.getAmount(), input.getType());
        transaction.setAmount(input.getAmount());
        transaction.setDate(input.getDate());
        transaction.setDescription(input.getDescription());
        transaction.setType(input.getType());
        return transactionRepository.save(transaction);
    }

    public SummaryDTO monthSummary(Long userId, int year, int month) {
        List<Transaction> transactions = filterTransactionsByMonth(userId, year, month);
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        BigDecimal largest = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            BigDecimal amount = BigDecimal.valueOf(transaction.getAmount());
            if (transaction.getType() == Type.EXPENSE) {
                expense = expense.add(amount);
            } else {
                income = income.add(amount);
            }
            largest = largest.max(amount);
        }
        int count = transactions.size();
        SummaryDTO summary = new SummaryDTO();
        summary.setTotalIncome(income.doubleValue());
        summary.setTotalExpense(expense.doubleValue());
        summary.setBalance(income.subtract(expense).doubleValue());
        summary.setTransactionCount(count);
        summary.setBiggestTransaction(largest.doubleValue());
        summary.setAverageTransaction(
                count == 0
                        ? 0.0
                        : income.add(expense)
                                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                                .doubleValue());
        return summary;
    }

    private Transaction ownedTransaction(Long id, Long userId) {
        Transaction transaction =
                transactionRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Transaction not found"));
        if (transaction.getUser() == null || !userId.equals(transaction.getUser().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Transaction belongs to another user");
        }
        return transaction;
    }

    private YearMonth period(int year, int month) {
        try {
            if (year < 1 || year > 9999) {
                throw new DateTimeException("Invalid year");
            }
            return YearMonth.of(year, month);
        } catch (DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid year or month");
        }
    }

    private void validate(LocalDate date, String description, Double amount, Type type) {
        if (date == null
                || date.getYear() < 1
                || date.getYear() > 9999
                || description == null
                || description.isBlank()
                || description.length() > 255
                || type == null
                || amount == null
                || !Double.isFinite(amount)
                || amount <= 0
                || amount > 99999999.99
                || BigDecimal.valueOf(amount).stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transaction data");
        }
    }
}
