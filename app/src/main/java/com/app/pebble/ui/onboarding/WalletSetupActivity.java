package com.app.pebble.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.pebble.App;
import com.app.pebble.R;
import com.app.pebble.data.model.Wallet;
import com.app.pebble.ui.home.HomeActivity;
import com.app.pebble.viewmodel.ExpenseViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class WalletSetupActivity extends AppCompatActivity {

    private RecyclerView rvWallets;
    private MaterialButton btnAddWallet, btnGetStarted;
    private ExpenseViewModel viewModel;
    private WalletSetupAdapter adapter;
    private final List<WalletEntry> walletEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_setup);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        rvWallets = findViewById(R.id.rv_wallets);
        btnAddWallet = findViewById(R.id.btn_add_wallet);
        btnGetStarted = findViewById(R.id.btn_get_started);

        // Start with one empty wallet entry
        walletEntries.add(new WalletEntry("", ""));
        adapter = new WalletSetupAdapter();
        rvWallets.setLayoutManager(new LinearLayoutManager(this));
        rvWallets.setAdapter(adapter);

        btnAddWallet.setOnClickListener(v -> {
            walletEntries.add(new WalletEntry("", ""));
            adapter.notifyItemInserted(walletEntries.size() - 1);
        });

        btnGetStarted.setOnClickListener(v -> saveAndProceed());
    }

    private void saveAndProceed() {
        // Read values from adapter views
        for (int i = 0; i < rvWallets.getChildCount(); i++) {
            View child = rvWallets.getChildAt(i);
            if (child != null) {
                TextInputEditText etName = child.findViewById(R.id.et_wallet_name);
                TextInputEditText etBalance = child.findViewById(R.id.et_wallet_balance);
                String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                String balance = etBalance.getText() != null ? etBalance.getText().toString().trim() : "";
                if (i < walletEntries.size()) {
                    walletEntries.get(i).name = name;
                    walletEntries.get(i).balance = balance;
                }
            }
        }

        // Validate
        boolean hasValidWallet = false;
        for (WalletEntry entry : walletEntries) {
            if (!TextUtils.isEmpty(entry.name)) {
                double balance;
                try {
                    balance = TextUtils.isEmpty(entry.balance) ? 0 : Double.parseDouble(entry.balance);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, R.string.error_wallet_balance_invalid, Toast.LENGTH_SHORT).show();
                    return;
                }
                hasValidWallet = true;
                viewModel.insertWallet(new Wallet(entry.name, balance, System.currentTimeMillis()));
            }
        }

        if (!hasValidWallet) {
            Toast.makeText(this, R.string.error_add_one_wallet, Toast.LENGTH_SHORT).show();
            return;
        }

        // Mark first run complete
        App.getInstance().setFirstRunComplete();

        // Navigate to Home
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent going back from wallet setup
    }

    // ───── Inner adapter for wallet entry rows ─────

    static class WalletEntry {
        String name;
        String balance;

        WalletEntry(String name, String balance) {
            this.name = name;
            this.balance = balance;
        }
    }

    class WalletSetupAdapter extends RecyclerView.Adapter<WalletSetupAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wallet_setup, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            WalletEntry entry = walletEntries.get(position);
            holder.etName.setText(entry.name);
            holder.etBalance.setText(entry.balance);

            // Hide delete for the first wallet
            holder.btnRemove.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);

            holder.btnRemove.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && walletEntries.size() > 1) {
                    walletEntries.remove(pos);
                    notifyItemRemoved(pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return walletEntries.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextInputEditText etName, etBalance;
            ImageButton btnRemove;

            VH(View itemView) {
                super(itemView);
                etName = itemView.findViewById(R.id.et_wallet_name);
                etBalance = itemView.findViewById(R.id.et_wallet_balance);
                btnRemove = itemView.findViewById(R.id.btn_remove);
            }
        }
    }
}
