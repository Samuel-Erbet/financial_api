package com.example.financial_api.demo.service;

import com.example.financial_api.demo.entity.Transaction;
import com.example.financial_api.demo.entity.Type;
import com.example.financial_api.demo.entity.User;
import com.example.financial_api.demo.repository.TransactionRepository;
import com.example.financial_api.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;



    // tenho que testar, mas deve criar uma transação

    @PostMapping("/saveTransactions")
    public Transaction createTransaction(
            LocalDate date,
            String description,
            Double amount,
            User user,
            Type type
    ){
        Transaction transaction = new Transaction(date, description, amount, user, type);
        return transaction;
    }

    // VAI LISTAR TODAS TRANSAÇÕES
    @GetMapping("/transactions")
    public List<Transaction> listTransactions(@RequestParam("id")Long id){
        List<Transaction> transactions = new ArrayList<>();
        transactions.add((Transaction) transactionRepository.findByUserId(id));
        return transactions;
    }

    // VAI LISTAR TODAS TRANSAÇÕES POR MÊS
    @GetMapping("/filterTransactions")
    public List<Transaction> filterTransactionsByMonth(
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month
    ){

        YearMonth date = YearMonth.of(year,month);
        LocalDate firstDay = date.atDay(1);
        LocalDate lastDay = date.atEndOfMonth();

        return transactionRepository.findByUserIdAndDateBetween(userId,firstDay,lastDay);
    }

    @DeleteMapping("/{id}")
    public String deleteTransaction(@PathVariable("id") Long id){
        transactionRepository.deleteById(id);
        return "Transaction id: "+id+" has been deleted.";
    }

    @GetMapping("/filterType")
    public List<Transaction> filterTransactionsByType(
            @RequestParam Long userId, 
            @RequestParam Type type
    ) {
        return transactionRepository.findByUserIdAndType(userId,type);
    }

    public Transaction changeTransaction(
           @PathVariable("id") Long id,
           @RequestBody Transaction transaction
    ) {
        Optional<Transaction> newTransaction = transactionRepository.findById(id);
        newTransaction.get().setAmount(transaction.getAmount());
        newTransaction.get().setCreatedAt(LocalDate.now());
        newTransaction.get().setDate(transaction.getDate());
        newTransaction.get().setDescription(transaction.getDescription());
        newTransaction.get().setType(transaction.getType());

        return newTransaction.orElse(null);
    }


    @GetMapping("/transactions/summary")
    public List monthSummary(
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month
    ){
        List<Transaction> transactions;

        Double totalIncome = 0.0;
        Double totalExpense = 0.0;
        Double balance = 0.0;
        int transactionCount = 0;
        Double biggestTransaction = 0.0;
        Double averageTransaction = 0.0;

        YearMonth date = YearMonth.of(year, month);
        LocalDate firstDay = date.atDay(1);
        LocalDate lastDay = date.atEndOfMonth();

        transactions = transactionRepository.findByUserIdAndDateBetween(userId, firstDay, lastDay);

        // o erro ta aqui
        if (transactions.isEmpty()){
            System.out.println("error empty summary");
        } else {
            for (Transaction t : transactions) {
                transactionCount += 1;
                System.out.println(transactionCount);
                if (t.getType().equals(Type.EXPENSE)) {
                    totalExpense += t.getAmount();
                } else {
                    totalIncome += t.getAmount();
                }

                if (t.getAmount() > biggestTransaction) {
                    biggestTransaction = t.getAmount();
                }

            }
            averageTransaction = totalExpense / transactionCount;
            balance = totalIncome - totalExpense;
        }

        List result = new ArrayList<>();

        return result;
    }


}
