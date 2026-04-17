package com.app.pebble.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wallets")
public class Wallet {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private double balance;
    private long createdAt;

    public Wallet(String name, double balance, long createdAt) {
        this.name = name;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
