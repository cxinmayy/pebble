package com.app.pebble;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.app.pebble.data.db.AppDatabase;
import com.app.pebble.utils.Constants;

public class App extends Application {

    private static App instance;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        // Force dark theme by default
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        // Initialize Room database eagerly so default categories are seeded
        AppDatabase.getInstance(this);
    }

    public static App getInstance() {
        return instance;
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    public boolean isFirstRun() {
        return prefs.getBoolean(Constants.KEY_FIRST_RUN, true);
    }

    public void setFirstRunComplete() {
        prefs.edit().putBoolean(Constants.KEY_FIRST_RUN, false).apply();
    }

    public String getUserName() {
        return prefs.getString(Constants.KEY_USER_NAME, "User");
    }

    public void setUserName(String name) {
        prefs.edit().putString(Constants.KEY_USER_NAME, name).apply();
    }
}
