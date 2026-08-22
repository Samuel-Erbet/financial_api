package com.example.financial_api.demo.controller;

import com.example.financial_api.demo.entity.transaction.SummaryDTO;
import com.example.financial_api.demo.entity.transaction.Transaction;
import com.example.financial_api.demo.entity.transaction.Type;
import com.example.financial_api.demo.entity.user.User;
import com.example.financial_api.demo.repository.TransactionRepository;
import com.example.financial_api.demo.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    TransactionService transactionService;

    @Autowired
    TransactionRepository transactionRepository;

    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction, @AuthenticationPrincipal User user){
        return transactionService.createTransaction(
                transaction.getDate(),
                transaction.getDescription(),
                transaction.getAmount(),
                user,
                transaction.getType()
        );
    }



    @GetMapping("/list")
    public List<Transaction> listTransactions(@AuthenticationPrincipal User userId){
        return transactionService.listTransactions(userId.getId());
    }



    @GetMapping("/filterMonth")
    public List<Transaction> filterTransactionsByMonth(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal User userId
    ){
        return transactionService.filterTransactionsByMonth(userId.getId(), year, month);
    }



    @GetMapping("/filterType")
    public List<Transaction> filterTransactionByType(
            @AuthenticationPrincipal User userId,
            @RequestParam(required = false) Type type
    ){
        return transactionService.filterTransactionsByType(userId.getId(), type);
    }



    @DeleteMapping("/{id}")
    public ResponseEntity deleteTransaction(@PathVariable Long id, @AuthenticationPrincipal User user){
        return transactionService.deleteTransaction(id, user);
    }

    @PutMapping("/{id}")
    public Transaction changeTransaction(
            @PathVariable Long id,
            @RequestBody Transaction transaction,
            @AuthenticationPrincipal User userId
    ){
        return transactionService.updateTransaction(id, transaction, userId.getId());
    }



    @GetMapping("/summary")
    public SummaryDTO summary(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal User userId
    ){
        return transactionService.monthSummary(userId.getId(), year, month);
    }
}
