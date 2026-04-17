package com.app.pebble.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.app.pebble.data.dao.CategoryDao;
import com.app.pebble.data.dao.TransactionDao;
import com.app.pebble.data.dao.WalletDao;
import com.app.pebble.data.model.Category;
import com.app.pebble.data.model.Transaction;
import com.app.pebble.data.model.Wallet;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Wallet.class, Category.class, Transaction.class}, version = 2, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    public abstract WalletDao walletDao();
    public abstract CategoryDao categoryDao();
    public abstract TransactionDao transactionDao();

    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newSingleThreadExecutor();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "pebble_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Pre-populate default categories on first database creation.
     */
    private static final RoomDatabase.Callback sRoomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                CategoryDao dao = INSTANCE.categoryDao();

                // Default Income categories
                dao.insert(new Category("Salary", "INCOME", "ic_work"));
                dao.insert(new Category("Freelance", "INCOME", "ic_freelance"));
                dao.insert(new Category("Investment", "INCOME", "ic_investment"));
                dao.insert(new Category("Gift", "INCOME", "ic_gift"));
                dao.insert(new Category("Other", "INCOME", "ic_other"));

                // Default Expense categories
                dao.insert(new Category("Food", "EXPENSE", "ic_food"));
                dao.insert(new Category("Transport", "EXPENSE", "ic_transport"));
                dao.insert(new Category("Shopping", "EXPENSE", "ic_shopping"));
                dao.insert(new Category("Bills", "EXPENSE", "ic_bills"));
                dao.insert(new Category("Entertainment", "EXPENSE", "ic_entertainment"));
                dao.insert(new Category("Health", "EXPENSE", "ic_health"));
                dao.insert(new Category("Education", "EXPENSE", "ic_education"));
                dao.insert(new Category("Other", "EXPENSE", "ic_other"));
            });
        }
    };
}
