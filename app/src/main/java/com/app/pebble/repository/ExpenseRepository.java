package com.app.pebble.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.app.pebble.data.dao.CategoryDao;
import com.app.pebble.data.dao.TransactionDao;
import com.app.pebble.data.dao.WalletDao;
import com.app.pebble.data.db.AppDatabase;
import com.app.pebble.data.model.Category;
import com.app.pebble.data.model.CategoryTotal;
import com.app.pebble.data.model.Transaction;
import com.app.pebble.data.model.Wallet;
import com.app.pebble.utils.Constants;
import com.app.pebble.utils.DateUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseRepository {

    private final WalletDao walletDao;
    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final ExecutorService executor;

    // LiveData caches
    private final LiveData<List<Wallet>> allWallets;
    private final LiveData<List<Category>> allCategories;
    private final LiveData<List<Category>> incomeCategories;
    private final LiveData<List<Category>> expenseCategories;
    private final LiveData<List<Transaction>> recentTransactions;
    private final LiveData<Double> currentMonthIncome;
    private final LiveData<Double> currentMonthExpense;
    private final LiveData<List<CategoryTotal>> incomeByCategoryThisMonth;
    private final LiveData<List<CategoryTotal>> expenseByCategoryThisMonth;

    public ExpenseRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        walletDao = db.walletDao();
        categoryDao = db.categoryDao();
        transactionDao = db.transactionDao();
        executor = Executors.newSingleThreadExecutor();

        allWallets = walletDao.getAllWallets();
        allCategories = categoryDao.getAllCategories();
        incomeCategories = categoryDao.getCategoriesByType(Constants.TYPE_INCOME);
        expenseCategories = categoryDao.getCategoriesByType(Constants.TYPE_EXPENSE);
        recentTransactions = transactionDao.getRecentTransactions();

        long startOfMonth = DateUtils.getStartOfCurrentMonth();
        long endOfMonth = DateUtils.getEndOfCurrentMonth();
        currentMonthIncome = transactionDao.getMonthlyIncome(startOfMonth, endOfMonth);
        currentMonthExpense = transactionDao.getMonthlyExpense(startOfMonth, endOfMonth);
        incomeByCategoryThisMonth = transactionDao.getIncomeByCategoryForMonth(startOfMonth, endOfMonth);
        expenseByCategoryThisMonth = transactionDao.getExpenseByCategoryForMonth(startOfMonth, endOfMonth);
    }

    // ───── Wallet Operations ─────

    public LiveData<List<Wallet>> getAllWallets() {
        return allWallets;
    }

    public LiveData<Wallet> getWalletById(int id) {
        return walletDao.getWalletById(id);
    }

    public void insertWallet(Wallet wallet) {
        executor.execute(() -> walletDao.insert(wallet));
    }

    public void updateWallet(Wallet wallet) {
        executor.execute(() -> walletDao.update(wallet));
    }

    public void deleteWallet(Wallet wallet) {
        executor.execute(() -> walletDao.delete(wallet));
    }

    // ───── Category Operations ─────

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
        executor.execute(() -> categoryDao.insert(category));
    }

    public void deleteCategory(Category category) {
        executor.execute(() -> categoryDao.delete(category));
    }

    /**
     * Cascading delete: reverses wallet balances for all linked transactions,
     * deletes those transactions, then deletes the category itself.
     */
    public void deleteCategoryWithTransactions(Category category) {
        executor.execute(() -> {
            // 1. Get all transactions linked to this category
            List<Transaction> linked = transactionDao.getTransactionsForCategorySync(category.getId());

            // 2. Reverse wallet balance for each transaction
            for (Transaction t : linked) {
                Wallet wallet = walletDao.getWalletByIdSync(t.getWalletId());
                if (wallet != null) {
                    if (Constants.TYPE_INCOME.equals(t.getType())) {
                        wallet.setBalance(wallet.getBalance() - t.getAmount());
                    } else if (Constants.TYPE_EXPENSE.equals(t.getType())) {
                        wallet.setBalance(wallet.getBalance() + t.getAmount());
                    }
                    walletDao.update(wallet);
                }
            }

            // 3. Delete all transactions for this category
            transactionDao.deleteTransactionsForCategory(category.getId());

            // 4. Delete the category itself
            categoryDao.delete(category);
        });
    }

    public LiveData<Integer> getTransactionCountForCategory(int categoryId) {
        return categoryDao.getTransactionCountForCategory(categoryId);
    }

    public int getTransactionCountForCategorySync(int categoryId) {
        return categoryDao.getTransactionCountForCategorySync(categoryId);
    }

    // ───── Transaction Operations ─────

    public LiveData<List<Transaction>> getRecentTransactions() {
        return recentTransactions;
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    public LiveData<Double> getTotalExpenseAmount() {
        return transactionDao.getTotalExpenseAmount();
    }

    public LiveData<Double> getCurrentMonthIncome() {
        return currentMonthIncome;
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

    /**
     * Inserts a transaction and updates the associated wallet balance.
     * For INCOME: wallet.balance += amount
     * For EXPENSE: wallet.balance -= amount
     */
    public void insertTransaction(Transaction transaction) {
        executor.execute(() -> {
            transactionDao.insert(transaction);

            Wallet wallet = walletDao.getWalletByIdSync(transaction.getWalletId());
            if (wallet != null) {
                if (Constants.TYPE_INCOME.equals(transaction.getType())) {
                    wallet.setBalance(wallet.getBalance() + transaction.getAmount());
                } else if (Constants.TYPE_EXPENSE.equals(transaction.getType())) {
                    wallet.setBalance(wallet.getBalance() - transaction.getAmount());
                }
                walletDao.update(wallet);
            }
        });
    }

    public void deleteTransaction(Transaction transaction) {
        executor.execute(() -> {
            Wallet wallet = walletDao.getWalletByIdSync(transaction.getWalletId());
            if (wallet != null) {
                if (Constants.TYPE_INCOME.equals(transaction.getType())) {
                    wallet.setBalance(wallet.getBalance() - transaction.getAmount());
                } else if (Constants.TYPE_EXPENSE.equals(transaction.getType())) {
                    wallet.setBalance(wallet.getBalance() + transaction.getAmount());
                }
                walletDao.update(wallet);
            }
            transactionDao.delete(transaction);
        });
    }

    public void deleteWalletWithTransactions(Wallet wallet) {
        executor.execute(() -> {
            transactionDao.deleteTransactionsForWallet(wallet.getId());
            walletDao.delete(wallet);
        });
    }

    /**
     * Transfers amount between two wallets.
     * Debits source, credits target, and saves a TRANSFER transaction.
     */
    public void transferBetweenWallets(int fromWalletId, int toWalletId,
                                       double amount, String note) {
        executor.execute(() -> {
            // Debit source
            Wallet fromWallet = walletDao.getWalletByIdSync(fromWalletId);
            if (fromWallet != null) {
                fromWallet.setBalance(fromWallet.getBalance() - amount);
                walletDao.update(fromWallet);
            }

            // Credit target
            Wallet toWallet = walletDao.getWalletByIdSync(toWalletId);
            if (toWallet != null) {
                toWallet.setBalance(toWallet.getBalance() + amount);
                walletDao.update(toWallet);
            }

            // Save transfer transaction
            Transaction transfer = new Transaction(
                    amount,
                    Constants.TYPE_TRANSFER,
                    "Transfer",
                    0, // no category for transfers
                    fromWalletId,
                    toWalletId,
                    note,
                    System.currentTimeMillis()
            );
            transactionDao.insert(transfer);
        });
    }

    /**
     * Updates an existing transaction and adjusts wallet balances if necessary.
     */
    public void updateTransaction(Transaction newTx, Transaction oldTx) {
        executor.execute(() -> {
            // Revert old transaction effect on old wallet
            Wallet oldWallet = walletDao.getWalletByIdSync(oldTx.getWalletId());
            if (oldWallet != null) {
                if (Constants.TYPE_INCOME.equals(oldTx.getType())) {
                    oldWallet.setBalance(oldWallet.getBalance() - oldTx.getAmount());
                } else if (Constants.TYPE_EXPENSE.equals(oldTx.getType())) {
                    oldWallet.setBalance(oldWallet.getBalance() + oldTx.getAmount());
                }
                walletDao.update(oldWallet);
            }

            // Apply new transaction effect on new wallet
            Wallet newWallet = walletDao.getWalletByIdSync(newTx.getWalletId());
            if (newWallet != null) {
                if (Constants.TYPE_INCOME.equals(newTx.getType())) {
                    newWallet.setBalance(newWallet.getBalance() + newTx.getAmount());
                } else if (Constants.TYPE_EXPENSE.equals(newTx.getType())) {
                    newWallet.setBalance(newWallet.getBalance() - newTx.getAmount());
                }
                walletDao.update(newWallet);
            }

            transactionDao.update(newTx);
        });
    }

    public Transaction getTransactionByIdSync(int id) {
        return transactionDao.getTransactionByIdSync(id);
    }

    /**
     * Get category by ID synchronously (for display purposes on background thread).
     */
    public Category getCategoryByIdSync(int categoryId) {
        return categoryDao.getCategoryByIdSync(categoryId);
    }

    public Wallet getWalletByIdSync(int walletId) {
        return walletDao.getWalletByIdSync(walletId);
    }

    public List<Wallet> getAllWalletsSync() {
        return walletDao.getAllWalletsSync();
    }

    public List<Category> getAllCategoriesSync() {
        return categoryDao.getAllCategoriesSync();
    }

    public List<Category> getCategoriesByTypeSync(String type) {
        return categoryDao.getCategoriesByTypeSync(type);
    }
}
