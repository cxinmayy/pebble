package com.app.pebble.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions",
        foreignKeys = {
                @ForeignKey(entity = Wallet.class,
                        parentColumns = "id",
                        childColumns = "walletId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Category.class,
                        parentColumns = "id",
                        childColumns = "categoryId",
                        onDelete = ForeignKey.SET_DEFAULT)
        },
        indices = {
                @Index("walletId"),
                @Index("categoryId")
        })
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private double amount;
    private String type; // "INCOME", "EXPENSE", "TRANSFER"
    private String title;
    private int categoryId; // 0 for transfers
    private int walletId;
    private int targetWalletId; // for transfers only, else 0
    private String note;
    private long date;

    public Transaction(double amount, String type, String title, int categoryId,
                       int walletId, int targetWalletId, String note, long date) {
        this.amount = amount;
        this.type = type;
        this.title = title;
        this.categoryId = categoryId;
        this.walletId = walletId;
        this.targetWalletId = targetWalletId;
        this.note = note;
        this.date = date;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public int getWalletId() { return walletId; }
    public void setWalletId(int walletId) { this.walletId = walletId; }

    public int getTargetWalletId() { return targetWalletId; }
    public void setTargetWalletId(int targetWalletId) { this.targetWalletId = targetWalletId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
}
