package com.app.pebble.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.app.pebble.data.model.Wallet;

import java.util.List;

@Dao
public interface WalletDao {

    @Insert
    long insert(Wallet wallet);

    @Update
    void update(Wallet wallet);

    @Delete
    void delete(Wallet wallet);

    @Query("SELECT * FROM wallets ORDER BY createdAt DESC")
    LiveData<List<Wallet>> getAllWallets();

    @Query("SELECT * FROM wallets WHERE id = :id")
    LiveData<Wallet> getWalletById(int id);

    @Query("SELECT * FROM wallets WHERE id = :id")
    Wallet getWalletByIdSync(int id);

    @Query("SELECT * FROM wallets ORDER BY createdAt DESC")
    List<Wallet> getAllWalletsSync();
}
