package com.app.pebble.data.model;

/**
 * POJO for category-wise aggregate totals.
 * Used by TransactionDao queries that GROUP BY categoryId.
 */
public class CategoryTotal {
    public int categoryId;
    public double total;
}
