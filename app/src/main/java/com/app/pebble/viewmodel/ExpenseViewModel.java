package com.app.pebble.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.app.pebble.data.model.Category;
import com.app.pebble.data.model.CategoryTotal;
import com.app.pebble.data.model.Transaction;
import com.app.pebble.data.model.Wallet;
import com.app.pebble.repository.ExpenseRepository;

import java.util.List;

public class ExpenseViewModel extends AndroidViewModel {

    private final ExpenseRepository repository;

    // Exposed LiveData
    private final LiveData<List<Wallet>> allWallets;
    private final LiveData<List<Category>> allCategories;
    private final LiveData<List<Category>> incomeCategories;
    private final LiveData<List<Category>> expenseCategories;
    private final LiveData<List<Transaction>> recentTransactions;
    private final LiveData<Double> currentMonthIncome;
    private final LiveData<Double> currentMonthExpense;
    private final LiveData<List<CategoryTotal>> incomeByCategoryThisMonth;
    private final LiveData<List<CategoryTotal>> expenseByCategoryThisMonth;

    public ExpenseViewModel(@NonNull Application application) {
        super(application);
        repository = new ExpenseRepository(application);

        allWallets = repository.getAllWallets();
        allCategories = repository.getAllCategories();
        incomeCategories = repository.getIncomeCategories();
        expenseCategories = repository.getExpenseCategories();
        recentTransactions = repository.getRecentTransactions();
        currentMonthIncome = repository.getCurrentMonthIncome();
        currentMonthExpense = repository.getCurrentMonthExpense();
        incomeByCategoryThisMonth = repository.getIncomeByCategoryThisMonth();
        expenseByCategoryThisMonth = repository.getExpenseByCategoryThisMonth();
    }

    // ───── Wallet ─────

    public LiveData<List<Wallet>> getAllWallets() {
        return allWallets;
    }

    public LiveData<Wallet> getWalletById(int id) {
        return repository.getWalletById(id);
    }

    public void insertWallet(Wallet wallet) {
        repository.insertWallet(wallet);
    }

    public void updateWallet(Wallet wallet) {
        repository.updateWallet(wallet);
    }

    public void deleteWallet(Wallet wallet) {
        repository.deleteWallet(wallet);
    }

    // ───── Category ─────

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public LiveData<List<Category>> getIncomeCategories() {
        return incomeCategories;
    }

    public LiveData<List<Category>> getExpenseCategories() {
        return expenseCategories;
    }

    public void insertCategory(Category category) {
        repository.insertCategory(category);
    }

    public void deleteCategory(Category category) {
        repository.deleteCategory(category);
    }

    public LiveData<Integer> getTransactionCountForCategory(int categoryId) {
        return repository.getTransactionCountForCategory(categoryId);
    }

    // ───── Transaction ─────

    public LiveData<List<Transaction>> getRecentTransactions() {
        return recentTransactions;
    }

    public LiveData<Double> getCurrentMonthIncome() {
        return currentMonthIncome;
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return repository.getAllTransactions();
    }

    public LiveData<Double> getTotalExpenseAmount() {
        return repository.getTotalExpenseAmount();
    }

    public LiveData<Double> getCurrentMonthExpense() {
        return currentMonthExpense;
    }

    public LiveData<List<CategoryTotal>> getIncomeByCategoryThisMonth() {
        return incomeByCategoryThisMonth;
    }

    public LiveData<List<CategoryTotal>> getExpenseByCategoryThisMonth() {
        return expenseByCategoryThisMonth;
    }

    public void insertTransaction(Transaction transaction) {
        repository.insertTransaction(transaction);
    }

    public void updateTransaction(Transaction newTx, Transaction oldTx) {
        repository.updateTransaction(newTx, oldTx);
    }

    public Transaction getTransactionByIdSync(int id) {
        return repository.getTransactionByIdSync(id);
    }

    public void deleteTransaction(Transaction transaction) {
        repository.deleteTransaction(transaction);
    }

    public void deleteWalletWithTransactions(Wallet wallet) {
        repository.deleteWalletWithTransactions(wallet);
    }

    public void transferBetweenWallets(int fromId, int toId, double amount, String note) {
        repository.transferBetweenWallets(fromId, toId, amount, note);
    }

    // ───── Sync helpers (for spinners, called on background thread) ─────

    public ExpenseRepository getRepository() {
        return repository;
    }
}
