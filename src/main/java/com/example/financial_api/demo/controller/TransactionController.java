package com.example.financial_api.demo.controller;

import com.example.financial_api.demo.entity.Transaction;
import com.example.financial_api.demo.entity.Type;
import com.example.financial_api.demo.repository.TransactionRepository;
import com.example.financial_api.demo.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
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

    // ESSA ROTA  VAI CRIAR A TRANSAÇÃO
    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction){
        transactionRepository.save(transactionService.createTransaction(
                transaction.getDate(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getUser(),
                transaction.getType()
                )
        );
        return transaction;
    }

    // ESSA ROTA  VAI LISTAR TODAS AS TRANSAÇÕES
    @GetMapping
    public List<Transaction> listTransactions(@RequestParam Long id){
        return transactionService.listTransactions(id);
    }

    // ESSA ROTA  VAI FILTRAR TODAS TRANSAÇÕES POR MES
    @GetMapping("/filterMonth")
    public List<Transaction> filterTransactionsByMonth(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam Long userId
    ){
        return transactionService.filterTransactionsByMonth(userId, year, month);
    }

    // ESSA ROTA VAI FILTRAR POR TIPO
    @GetMapping("/filterType")
    public List<Transaction> filterTransactionByType(
            @RequestParam Long userId,
            @RequestParam(required = false) Type type
    ){
        return transactionService.filterTransactionsByType(userId, type);
    }

    // ESSA ROTA VAI DELETAR
    @DeleteMapping("/{id}")
    public void deleteTransaction(@PathVariable Long id){
        transactionService.deleteTransaction(id);
    }

    @PutMapping("/{id}")
    public List<Transaction> changeTransaction(
            @PathVariable Long id,
            @RequestBody Transaction transaction
    ){
        return Collections.singletonList(transactionService.changeTransaction(id, transaction));
    }

    @GetMapping("/summary")
    public List<Transaction> summary(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam Long userId
    ){
        return transactionService.monthSummary(userId, year, month);
    }
}
