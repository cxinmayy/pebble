package com.app.pebble;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.app.pebble.ui.home.HomeActivity;
import com.app.pebble.ui.onboarding.NameInputActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Clean routing without showing layout wrapper
        
        if (App.getInstance().isFirstRun()) {
            startActivity(new Intent(this, NameInputActivity.class));
        } else {
            startActivity(new Intent(this, HomeActivity.class));
        }
        finish(); // Remove from back stack
    }
}