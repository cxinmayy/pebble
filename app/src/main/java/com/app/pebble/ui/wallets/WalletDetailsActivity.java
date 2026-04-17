package com.app.pebble.ui.wallets;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.pebble.R;
import com.app.pebble.data.model.Transaction;
import com.app.pebble.data.model.Wallet;
import com.app.pebble.ui.home.RecentTransactionAdapter;
import com.app.pebble.ui.transaction.AddTransactionActivity;
import com.app.pebble.utils.Constants;
import com.app.pebble.utils.DateUtils;
import com.app.pebble.utils.NumberUtils;
import com.app.pebble.viewmodel.ExpenseViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WalletDetailsActivity extends AppCompatActivity {

    private ExpenseViewModel viewModel;
    private Wallet currentWallet;
    private int walletId = -1;

    private TextView tvWalletName, tvWalletBalance;
    private TextView tvIncomeAmount, tvIncomeCount, tvExpenseAmount, tvExpenseCount;
    private TextView tvMonth;
    private RecyclerView rvTransactions;
    private RecentTransactionAdapter adapter;

    private Calendar displayCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_details);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        walletId = getIntent().getIntExtra("WALLET_ID", -1);
        if (walletId == -1) {
            finish();
            return;
        }

        displayCalendar = Calendar.getInstance();

        initViews();
        setupListeners();
        observeData();
        updateMonthDisplay();
    }

    private void initViews() {
        tvWalletName = findViewById(R.id.tv_wallet_name);
        tvWalletBalance = findViewById(R.id.tv_wallet_balance);
        
        tvIncomeAmount = findViewById(R.id.tv_income_amount);
        tvIncomeCount = findViewById(R.id.tv_income_count);
        tvExpenseAmount = findViewById(R.id.tv_expense_amount);
        tvExpenseCount = findViewById(R.id.tv_expense_count);
        
        tvMonth = findViewById(R.id.tv_month);
        
        rvTransactions = findViewById(R.id.rv_transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        
        // Use the same adapter as Home screen for transactions
        adapter = new RecentTransactionAdapter(viewModel);
        rvTransactions.setAdapter(adapter);
    }

    private void setupListeners() {
        findViewById(R.id.btn_close).setOnClickListener(v -> closeWithAnimation());

        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            if (currentWallet != null) {
                showEditWalletDialog();
            }
        });

        findViewById(R.id.btn_add_income).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("PREFILLED_WALLET_ID", walletId);
            intent.putExtra("PREFILLED_TYPE", Constants.TYPE_INCOME);
            startActivity(intent);
        });

        findViewById(R.id.btn_add_expense).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("PREFILLED_WALLET_ID", walletId);
            intent.putExtra("PREFILLED_TYPE", Constants.TYPE_EXPENSE);
            startActivity(intent);
        });

        findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            displayCalendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            observeTransactions();
        });

        findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            displayCalendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            observeTransactions();
        });
    }

    private void observeData() {
        viewModel.getWalletById(walletId).observe(this, wallet -> {
            if (wallet != null) {
                currentWallet = wallet;
                tvWalletName.setText(wallet.getName());
                tvWalletBalance.setText(NumberUtils.formatCurrency(wallet.getBalance()));
            } else {
                finish(); // Wallet deleted
            }
        });

        observeTransactions();
    }

    private void observeTransactions() {
        Calendar cal = (Calendar) displayCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfMonth = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        long endOfMonth = cal.getTimeInMillis() - 1;

        viewModel.getRepository().getAllTransactions().observe(this, allTransactions -> {
            if (allTransactions == null) return;
            
            List<Transaction> filtered = new ArrayList<>();
            double incomeTotal = 0, expenseTotal = 0;
            int incomeCount = 0, expenseCount = 0;
            
            for (Transaction t : allTransactions) {
                if (t.getWalletId() == walletId && t.getDate() >= startOfMonth && t.getDate() <= endOfMonth) {
                    filtered.add(t);
                    if (Constants.TYPE_INCOME.equals(t.getType())) {
                        incomeTotal += t.getAmount();
                        incomeCount++;
                    } else if (Constants.TYPE_EXPENSE.equals(t.getType())) {
                        expenseTotal += t.getAmount();
                        expenseCount++;
                    }
                }
            }
            
            adapter.setTransactions(filtered);
            
            tvIncomeAmount.setText(String.format(java.util.Locale.getDefault(), "%.2f", incomeTotal));
            tvIncomeCount.setText(String.valueOf(incomeCount));
            
            tvExpenseAmount.setText(String.format(java.util.Locale.getDefault(), "%.2f", expenseTotal));
            tvExpenseCount.setText(String.valueOf(expenseCount));
        });
    }

    private void updateMonthDisplay() {
        String[] months = new java.text.DateFormatSymbols().getMonths();
        String monthName = months[displayCalendar.get(Calendar.MONTH)];
        tvMonth.setText(monthName);
    }

    private void showEditWalletDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_minimal_input, null);
        
        dialog.setContentView(view);
        View parent = (View) view.getParent();
        if (parent != null) {
            parent.setBackgroundResource(android.R.color.transparent);
        }

        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        EditText etName = view.findViewById(R.id.et_input_1);
        EditText etBalance = view.findViewById(R.id.et_input_2);
        View btnSave = view.findViewById(R.id.btn_save);

        tvTitle.setText("Edit Wallet");
        
        etName.setHint("Wallet Name");
        etName.setText(currentWallet.getName());
        
        etBalance.setVisibility(View.VISIBLE);
        etBalance.setHint("Overall Balance");
        etBalance.setText(String.valueOf(currentWallet.getBalance()));

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String balanceStr = etBalance.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Wallet name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            double balance = 0;
            if (!TextUtils.isEmpty(balanceStr)) {
                try {
                    balance = Double.parseDouble(balanceStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid balance amount", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            currentWallet.setName(name);
            currentWallet.setBalance(balance);
            viewModel.updateWallet(currentWallet);
            Toast.makeText(this, "Wallet updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void closeWithAnimation() {
        finish();
    }

    @Override
    public void onBackPressed() {
        closeWithAnimation();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out);
    }
}
