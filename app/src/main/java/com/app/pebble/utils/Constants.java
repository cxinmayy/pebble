package com.app.pebble.utils;

public final class Constants {

    private Constants() {} // Prevent instantiation

    // SharedPreferences
    public static final String PREFS_NAME = "pebble_prefs";
    public static final String KEY_FIRST_RUN = "is_first_run";
    public static final String KEY_USER_NAME = "user_name";

    // Transaction types
    public static final String TYPE_INCOME = "INCOME";
    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_TRANSFER = "TRANSFER";

    // Intent extras
    public static final String EXTRA_TRANSACTION_TYPE = "extra_transaction_type";
    public static final String EXTRA_TRANSACTION_ID = "extra_transaction_id";
}
