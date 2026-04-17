package com.app.pebble.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.app.pebble.data.model.Wallet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletSortUtils {

    public static final String PREFS_NAME = "PebblePrefs";
    public static final String PREF_WALLET_ORDER = "wallet_order_csv";

    /**
     * Sorts wallets based on a saved CSV of IDs in SharedPreferences.
     */
    public static List<Wallet> getSortedWallets(Context context, List<Wallet> sourceList) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String orderCsv = prefs.getString(PREF_WALLET_ORDER, "");
        
        if (orderCsv.isEmpty()) {
            return new ArrayList<>(sourceList);
        }

        String[] orderIds = orderCsv.split(",");
        Map<Integer, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < orderIds.length; i++) {
            try {
                orderMap.put(Integer.parseInt(orderIds[i].trim()), i);
            } catch (NumberFormatException ignored) {}
        }

        List<Wallet> sortedList = new ArrayList<>(sourceList);
        Collections.sort(sortedList, (w1, w2) -> {
            Integer idx1 = orderMap.get(w1.getId());
            Integer idx2 = orderMap.get(w2.getId());

            if (idx1 != null && idx2 != null) {
                return idx1.compareTo(idx2);
            } else if (idx1 != null) {
                return -1; // w1 is configured, w2 is not (new wallet) -> w1 comes first
            } else if (idx2 != null) {
                return 1;
            } else {
                return Long.compare(w2.getCreatedAt(), w1.getCreatedAt()); // Fallback order
            }
        });

        return sortedList;
    }

    /**
     * Saves the new ordered subset of wallets to SharedPreferences.
     */
    public static void saveWalletOrder(Context context, List<Wallet> orderedWallets) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orderedWallets.size(); i++) {
            sb.append(orderedWallets.get(i).getId());
            if (i < orderedWallets.size() - 1) {
                sb.append(",");
            }
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_WALLET_ORDER, sb.toString()).apply();
    }
}
