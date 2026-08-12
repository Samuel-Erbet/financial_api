package com.example.financial_api.demo.service;

import com.example.financial_api.demo.entity.transaction.SummaryDTO;
import com.example.financial_api.demo.entity.transaction.Transaction;
import com.example.financial_api.demo.entity.transaction.Type;
import com.example.financial_api.demo.entity.user.User;
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

    //CRIA UMA TRANSAÇÃO
    public Transaction createTransaction (
            LocalDate date,
            String description,
            Double amount,
            User user,
            Type type
    ){

        Transaction transaction = new Transaction(date, description, amount, user, type);

        return transactionRepository.save(transaction);
    }

    // VAI LISTAR TODAS TRANSAÇÕES
    public List<Transaction> listTransactions(Long id){
        return transactionRepository.findByUserId(id);
    }

    // VAI LISTAR TODAS TRANSAÇÕES POR MÊS
    public List<Transaction> filterTransactionsByMonth(
            Long userId,
            int year,
            int month
    ){

        YearMonth date = YearMonth.of(year,month);
        LocalDate firstDay = date.atDay(1);
        LocalDate lastDay = date.atEndOfMonth();

        return transactionRepository.findByUserIdAndDateBetween(userId,firstDay,lastDay);
    }

    // VAI DELETEAR A TRANSAÇÃO BASEADA NO ID DELA
    public String deleteTransaction(Long id){
        transactionRepository.deleteById(id);
        return "Transaction id: "+id+" has been deleted.";
    }

    // VAI FILTRAR AS TRANSAÇÕES PELO TIPO DELA
    public List<Transaction> filterTransactionsByType(
            Long userId,
            Type type
    ) {
        return transactionRepository.findByUserIdAndType(userId,type);
    }

    // VAI ALTERAR A TRANSAÇÃO PELO ID DELA
    public Transaction changeTransaction(
            Long id,
            Transaction transaction,
            Long userId
    ) {
       Transaction newTransaction = transactionRepository.findById(id)
               .orElseThrow(() ->new RuntimeException("Transaction not found"));


        if(!newTransaction.getUser().getId().equals(userId)){
            throw new RuntimeException("Transaction not found");
        }

        newTransaction.setAmount(transaction.getAmount());
        newTransaction.setCreatedAt(LocalDate.now());
        newTransaction.setDate(transaction.getDate());
        newTransaction.setDescription(transaction.getDescription());
        newTransaction.setType(transaction.getType());

        return transactionRepository.save(newTransaction);
    }

    // VAI RETORNAR INFORMAÇÕES SOBRE AS COMPRAS DO MÊS QUE O USUÁRIO ESCOLHER
    public SummaryDTO monthSummary(
            Long userId,
            int year,
            int month
    ){

        // vai guardar todas as transações do mes
        List<Transaction> transactions;

        // aqui são as variáveis que guardarão as informações a serem retornadas
        Double totalIncome = 0.0;
        Double totalExpense = 0.0;
        double balance = 0.0;
        int transactionCount = 0;
        Double biggestTransaction = 0.0;
        Double averageTransaction = 0.0;

        SummaryDTO summary = new SummaryDTO();


        // vai pegar o valor do mês com base no ultimo e primeiro dia
        YearMonth date = YearMonth.of(year, month);
        LocalDate firstDay = date.atDay(1);
        LocalDate lastDay = date.atEndOfMonth();

        transactions = transactionRepository.findByUserIdAndDateBetween(userId, firstDay, lastDay);

        if (transactions.isEmpty()){

            return summary;
        } else {
            // aqui vai ser contado as transações, em quantidade e valores
            for (Transaction t : transactions) {
                transactionCount += 1;

                if (t.getType().equals(Type.EXPENSE)) {
                    totalExpense += t.getAmount();
                } else {
                    totalIncome += t.getAmount();
                }

                if (t.getAmount() > biggestTransaction) {
                    biggestTransaction = t.getAmount();
                }
            }
            // média de transações
            averageTransaction = totalExpense / transactionCount;

            // saldo
            balance = totalIncome - totalExpense;

        }
        summary.setAverageTransaction(averageTransaction);
        summary.setBalance(balance);
        summary.setBiggestTransaction(biggestTransaction);
        summary.setTotalExpense(totalExpense);
        summary.setTotalIncome(totalIncome);
        summary.setTransactionCount(transactionCount);

        return summary;
    }




}
