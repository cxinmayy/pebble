package com.app.pebble.ui.transaction;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

import com.app.pebble.R;
import com.app.pebble.data.model.Wallet;
import com.app.pebble.viewmodel.ExpenseViewModel;

import java.util.ArrayList;
import java.util.List;

public class TransferActivity extends AppCompatActivity {

    private ExpenseViewModel viewModel;
    private EditText etAmount, etNote;
    private LinearLayout llFromChips, llToChips;

    private List<Wallet> wallets = new ArrayList<>();
    private Wallet selectedFrom = null;
    private Wallet selectedTo = null;
    private Typeface outfitFont = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        try {
            outfitFont = ResourcesCompat.getFont(this, R.font.outfit);
        } catch (Exception ignored) {}

        initViews();
        loadWallets();
    }

    private void initViews() {
        etAmount = findViewById(R.id.et_amount);
        etNote = findViewById(R.id.et_note);
        llFromChips = findViewById(R.id.ll_from_chips);
        llToChips = findViewById(R.id.ll_to_chips);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Swap from/to wallets
        findViewById(R.id.btn_swap).setOnClickListener(v -> {
            Wallet temp = selectedFrom;
            selectedFrom = selectedTo;
            selectedTo = temp;
            renderChips();
        });

        findViewById(R.id.btn_transfer).setOnClickListener(v -> performTransfer());
    }

    private void loadWallets() {
        new Thread(() -> {
            wallets = viewModel.getRepository().getAllWalletsSync();
            runOnUiThread(() -> {
                if (wallets.size() >= 1) {
                    selectedFrom = wallets.get(0);
                }
                if (wallets.size() >= 2) {
                    selectedTo = wallets.get(1);
                }
                renderChips();
            });
        }).start();
    }

    private void renderChips() {
        llFromChips.removeAllViews();
        llToChips.removeAllViews();

        for (Wallet w : wallets) {
            boolean isFromSelected = (selectedFrom != null && w.getId() == selectedFrom.getId());
            boolean isToSelected = (selectedTo != null && w.getId() == selectedTo.getId());

            llFromChips.addView(createChip(w, isFromSelected, true));
            llToChips.addView(createChip(w, isToSelected, false));
        }
    }

    private TextView createChip(Wallet w, boolean isSelected, boolean isFromRow) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 12, 0);
        chip.setLayoutParams(params);

        chip.setText(w.getName());
        chip.setTextSize(15);
        chip.setPadding(48, 24, 48, 24);
        chip.setGravity(android.view.Gravity.CENTER);

        if (outfitFont != null) {
            chip.setTypeface(outfitFont, isSelected ? Typeface.BOLD : Typeface.NORMAL);
        } else {
            chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
        }

        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_pill_selected);
            chip.setTextColor(Color.parseColor("#121212"));
            chip.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_minimal_wallet, 0, 0, 0);
            chip.setCompoundDrawablePadding(12);
            chip.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#121212")));
        } else {
            chip.setBackgroundResource(R.drawable.bg_pill_minimal);
            chip.setTextColor(Color.parseColor("#9E99B3"));
            chip.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        chip.setOnClickListener(v -> {
            if (isFromRow) {
                if (selectedFrom == null || w.getId() != selectedFrom.getId()) {
                    selectedFrom = w;
                    renderChips();
                }
            } else {
                if (selectedTo == null || w.getId() != selectedTo.getId()) {
                    selectedTo = w;
                    renderChips();
                }
            }
        });

        return chip;
    }

    private void performTransfer() {
        String amountStr = etAmount.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, R.string.error_amount_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_amount_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFrom == null || selectedTo == null) {
            Toast.makeText(this, R.string.error_select_wallet, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFrom.getId() == selectedTo.getId()) {
            Toast.makeText(this, R.string.error_same_wallet, Toast.LENGTH_SHORT).show();
            return;
        }

        String note = etNote.getText().toString().trim();

        viewModel.transferBetweenWallets(selectedFrom.getId(), selectedTo.getId(), amount, note);
        Toast.makeText(this, R.string.transfer_saved, Toast.LENGTH_SHORT).show();
        finish();
    }
}
