package com.app.pebble.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.app.pebble.data.model.CategoryTotal;
import com.app.pebble.data.model.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    long insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    Transaction getTransactionByIdSync(int id);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getRecentTransactions();

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE'")
    LiveData<Double> getTotalExpenseAmount();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND date >= :startOfMonth AND date <= :endOfMonth")
    LiveData<Double> getMonthlyIncome(long startOfMonth, long endOfMonth);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND date >= :startOfMonth AND date <= :endOfMonth")
    LiveData<Double> getMonthlyExpense(long startOfMonth, long endOfMonth);

    @Query("SELECT categoryId, SUM(amount) as total FROM transactions WHERE type = 'INCOME' AND date >= :startOfMonth AND date <= :endOfMonth GROUP BY categoryId")
    LiveData<List<CategoryTotal>> getIncomeByCategoryForMonth(long startOfMonth, long endOfMonth);

    @Query("SELECT categoryId, SUM(amount) as total FROM transactions WHERE type = 'EXPENSE' AND date >= :startOfMonth AND date <= :endOfMonth GROUP BY categoryId")
    LiveData<List<CategoryTotal>> getExpenseByCategoryForMonth(long startOfMonth, long endOfMonth);

    @Query("SELECT * FROM transactions WHERE walletId = :walletId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsForWallet(int walletId);

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId")
    List<Transaction> getTransactionsForCategorySync(int categoryId);

    @Query("DELETE FROM transactions WHERE categoryId = :categoryId")
    void deleteTransactionsForCategory(int categoryId);

    @Query("DELETE FROM transactions WHERE walletId = :walletId")
    void deleteTransactionsForWallet(int walletId);
}
