package com.app.pebble.utils;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public final class NumberUtils {

    private NumberUtils() {}

    /**
     * Formats amount as currency. Uses Indian Rupee (₹) by default.
     * Change locale/currency for other regions.
     */
    public static String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setCurrency(Currency.getInstance("INR"));
        return format.format(amount);
    }

    /**
     * Short format for large amounts: ₹1.2K, ₹3.5L, etc.
     */
    public static String formatCurrencyShort(double amount) {
        if (amount >= 10000000) {
            return "₹" + String.format(Locale.getDefault(), "%.1fCr", amount / 10000000);
        } else if (amount >= 100000) {
            return "₹" + String.format(Locale.getDefault(), "%.1fL", amount / 100000);
        } else if (amount >= 1000) {
            return "₹" + String.format(Locale.getDefault(), "%.1fK", amount / 1000);
        } else {
            return "₹" + String.format(Locale.getDefault(), "%.0f", amount);
        }
    }
}
