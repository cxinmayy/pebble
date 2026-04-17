package com.app.pebble.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.material.materialswitch.MaterialSwitch;

import androidx.appcompat.app.AppCompatActivity;

import com.app.pebble.App;
import com.app.pebble.R;
import com.app.pebble.ui.transaction.TransferActivity;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "PebblePrefs";
    public static final String PREF_HIDE_BALANCE = "hide_balance";

    private TextView tvUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tvUserName = findViewById(R.id.tv_user_name);
        refreshName();

        TextView tvVersion = findViewById(R.id.tv_version);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText(String.format(getString(R.string.version_format), versionName));
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText("v1.0");
        }

        View groupGeneral = findViewById(R.id.group_general);
        View groupPreferences = findViewById(R.id.group_preferences);
        
        // Settings Card Click Targets
        View cardName = findViewById(R.id.setting_name);
        View cardCat = findViewById(R.id.setting_categories);
        View cardTransfer = findViewById(R.id.setting_transfer);
        View cardHideBalance = findViewById(R.id.setting_hide_balance);
        View cardShare = findViewById(R.id.setting_share);
        MaterialSwitch switchHideBalance = findViewById(R.id.switch_hide_balance);

        // Load Preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isHidden = prefs.getBoolean(PREF_HIDE_BALANCE, false);
        switchHideBalance.setChecked(isHidden);

        cardHideBalance.setOnClickListener(v -> {
            boolean newState = !switchHideBalance.isChecked();
            switchHideBalance.setChecked(newState);
            prefs.edit().putBoolean(PREF_HIDE_BALANCE, newState).apply();
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        });
        
        // To intercept direct switch click properly as well
        switchHideBalance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PREF_HIDE_BALANCE, isChecked).apply();
        });
        
        cardShare.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Pebble Expense Tracker");
            String shareMessage = "Hey! Let's manage our money intelligently using Pebble. Check it out:\n\nhttps://github.com/cxinmayy/pebble/releases";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "Share Pebble via"));
        });

        // 1. Deck of Cards Cascade Animation
        View[] cards = {groupGeneral, groupPreferences};
        for (int i = 0; i < cards.length; i++) {
            View c = cards[i];
            c.setTranslationY(100f);
            c.setTranslationX(50f);
            c.setAlpha(0f);
            c.animate().translationY(0f).translationX(0f).alpha(1f)
                .setStartDelay(i * 60L).setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                .start();
        }

        // Apply physical squish touch to interactive elements
        com.app.pebble.utils.UIUtils.applySquishTouch(cardName);
        com.app.pebble.utils.UIUtils.applySquishTouch(cardCat);
        com.app.pebble.utils.UIUtils.applySquishTouch(cardTransfer);
        com.app.pebble.utils.UIUtils.applySquishTouch(cardHideBalance);
        com.app.pebble.utils.UIUtils.applySquishTouch(cardShare);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        cardCat.setOnClickListener(v -> {
            startActivity(new Intent(this, CategoryManagerActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.stay);
        });

        cardTransfer.setOnClickListener(v -> 
            startActivity(new Intent(this, TransferActivity.class)));

        cardName.setOnClickListener(v -> showEditNameDialog());
        
        // 3. The Haptic Easter Egg
        final int[] tapCount = {0};
        tvVersion.setOnClickListener(v -> {
            tapCount[0]++;
            if (tapCount[0] >= 5) {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                tvVersion.setText("Pebble Ultimate Unlocked 💎");
                tvVersion.setTextColor(android.graphics.Color.parseColor("#FFD700")); // Gold
                tvVersion.setTypeface(null, android.graphics.Typeface.BOLD_ITALIC);
                tvVersion.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150).setInterpolator(new android.view.animation.OvershootInterpolator(2f)).withEndAction(() -> {
                    tvVersion.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                }).start();
                tapCount[0] = 0;
            }
        });
    }

    private void refreshName() {
        tvUserName.setText(App.getInstance().getUserName());
    }

    private void showEditNameDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        android.view.View view = android.view.LayoutInflater.from(this).inflate(R.layout.bottom_sheet_minimal_input, null);
        
        dialog.setContentView(view);
        android.view.View parent = (android.view.View) view.getParent();
        if (parent != null) {
            parent.setBackgroundResource(android.R.color.transparent);
        }

        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        EditText etName = view.findViewById(R.id.et_input_1);
        EditText etBalance = view.findViewById(R.id.et_input_2);
        android.view.View btnSave = view.findViewById(R.id.btn_save);

        tvTitle.setText(R.string.dialog_edit_name);
        etName.setHint(R.string.setting_user_name);
        etName.setText(App.getInstance().getUserName());
        etName.setSelection(etName.getText().length());
        
        etBalance.setVisibility(android.view.View.GONE);

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (!newName.isEmpty()) {
                App.getInstance().setUserName(newName);
                refreshName();
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
