package com.example.financial_api.demo.entity.transaction;

public class SummaryDTO {
    private Double totalIncome       ;
    private Double totalExpense;
    private Double balance;
    private int transactionCount;
    private Double biggestTransaction;
    private Double averageTransaction;

    public SummaryDTO() {
    }

    public Double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(Double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public Double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(Double totalExpense) {
        this.totalExpense = totalExpense;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Double getBiggestTransaction() {
        return biggestTransaction;
    }

    public void setBiggestTransaction(Double biggestTransaction) {
        this.biggestTransaction = biggestTransaction;
    }

    public Double getAverageTransaction() {
        return averageTransaction;
    }

    public void setAverageTransaction(Double averageTransaction) {
        this.averageTransaction = averageTransaction;
    }
}
