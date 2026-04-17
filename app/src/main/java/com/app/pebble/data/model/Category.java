package com.app.pebble.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String type; // "INCOME" or "EXPENSE"
    private String iconResName; // drawable resource name, nullable

    public Category(String name, String type, String iconResName) {
        this.name = name;
        this.type = type;
        this.iconResName = iconResName;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIconResName() { return iconResName; }
    public void setIconResName(String iconResName) { this.iconResName = iconResName; }
}
