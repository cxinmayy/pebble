package com.app.pebble.ui.onboarding;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

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

import java.util.ArrayList;
import java.util.List;

public class NameInputActivity extends AppCompatActivity {

    private ViewFlipper viewFlipper;
    private EditText etName;
    private TextView btnContinue, btnGetStarted, tvWelcomeName, btnAddAnother;
    private RecyclerView rvWallets;
    private ExpenseViewModel viewModel;

    private WalletAdapter adapter;
    private final List<WalletEntry> walletEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name_input);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        viewFlipper = findViewById(R.id.view_flipper);
        etName = findViewById(R.id.et_name);
        btnContinue = findViewById(R.id.btn_continue);
        btnGetStarted = findViewById(R.id.btn_get_started);
        tvWelcomeName = findViewById(R.id.tv_welcome_name);
        btnAddAnother = findViewById(R.id.btn_add_another);
        rvWallets = findViewById(R.id.rv_wallets);

        // Make system nav bar blend in
        getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#121212"));
        // Status bar area black
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#000000"));

        // Setup Wallets RecyclerView
        walletEntries.add(new WalletEntry("", ""));
        adapter = new WalletAdapter();
        rvWallets.setLayoutManager(new LinearLayoutManager(this));
        rvWallets.setAdapter(adapter);

        btnAddAnother.setOnClickListener(v -> {
            walletEntries.add(new WalletEntry("", ""));
            adapter.notifyItemInserted(walletEntries.size() - 1);
            rvWallets.smoothScrollToPosition(walletEntries.size() - 1);
        });

        // Entrance animation for Step 1
        animateStep1Entrance();

        btnContinue.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                shakeView(etName);
                return;
            }

            App.getInstance().setUserName(name);
            tvWelcomeName.setText("Great, " + name + "!");

            // Animate transition to Step 2
            transitionToWallet();
        });

        btnGetStarted.setOnClickListener(v -> {
            // Read values from adapter views
            for (int i = 0; i < rvWallets.getChildCount(); i++) {
                View child = rvWallets.getChildAt(i);
                if (child != null) {
                    EditText wName = child.findViewById(R.id.et_wallet_name);
                    EditText wBal = child.findViewById(R.id.et_wallet_balance);
                    String n = wName.getText() != null ? wName.getText().toString().trim() : "";
                    String b = wBal.getText() != null ? wBal.getText().toString().trim() : "";
                    if (i < walletEntries.size()) {
                        walletEntries.get(i).name = n;
                        walletEntries.get(i).balance = b;
                    }
                }
            }

            boolean hasValidWallet = false;
            for (WalletEntry entry : walletEntries) {
                if (!TextUtils.isEmpty(entry.name)) {
                    double balance = 0;
                    if (!TextUtils.isEmpty(entry.balance)) {
                        try {
                            balance = Double.parseDouble(entry.balance);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    hasValidWallet = true;
                    viewModel.insertWallet(new Wallet(entry.name, balance, System.currentTimeMillis()));
                }
            }

            if (!hasValidWallet) {
                Toast.makeText(this, "Please add at least one wallet", Toast.LENGTH_SHORT).show();
                shakeView(rvWallets);
                return;
            }

            App.getInstance().setFirstRunComplete();
            animateExitAndNavigate();
        });
    }

    private void animateStep1Entrance() {
        View emoji = findViewById(R.id.iv_logo);
        View headline = findViewById(R.id.tv_headline);
        View subheadline = findViewById(R.id.tv_subheadline);

        View[] views = {emoji, headline, subheadline, etName, btnContinue};
        
        for (int i = 0; i < views.length; i++) {
            if (views[i] == null) continue;
            views[i].setAlpha(0f);
            views[i].setTranslationY(60f);
            
            views[i].animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setStartDelay(300 + (i * 100))
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .start();
        }
    }

    private void transitionToWallet() {
        View step1 = findViewById(R.id.layout_step_name);

        // Smoothly fade out step 1 upwards
        step1.animate()
                .alpha(0f)
                .translationY(-120f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    viewFlipper.setDisplayedChild(1);

                    // Animate step 2 entrance flawlessly cascading upwards
                    View wGreeting = findViewById(R.id.tv_welcome_name);
                    View wHeadline = findViewById(R.id.tv_wallet_headline);
                    View wBtnAdd = findViewById(R.id.btn_add_another);
                    View wBtnStart = findViewById(R.id.btn_get_started);

                    View[] views = {wGreeting, wHeadline, rvWallets, wBtnAdd, wBtnStart};
                    for(int i = 0; i < views.length; i++) {
                        if (views[i] == null) continue;
                        views[i].setAlpha(0f);
                        views[i].setTranslationY(80f);
                        views[i].animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(600)
                            .setStartDelay(i * 70L)
                            .setInterpolator(new DecelerateInterpolator(1.5f))
                            .start();
                    }
                })
                .start();
    }

    private void animateExitAndNavigate() {
        View root = findViewById(R.id.rounded_container);

        root.animate()
                .alpha(0f)
                .translationY(-100f)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    Intent intent = new Intent(NameInputActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                })
                .start();
    }

    private void shakeView(View view) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX",
                0f, -15f, 15f, -10f, 10f, -5f, 5f, 0f);
        shake.setDuration(400);
        shake.start();
    }

    @Override
    public void onBackPressed() {
        if (viewFlipper.getDisplayedChild() == 1) {
            // Smoothly go back
            View step2 = findViewById(R.id.layout_step_wallet);
            step2.animate()
                    .alpha(0f)
                    .translationY(120f)
                    .setDuration(350)
                    .withEndAction(() -> {
                        viewFlipper.setDisplayedChild(0);
                        View step1 = findViewById(R.id.layout_step_name);
                        step1.setAlpha(0f);
                        step1.setTranslationY(-120f);
                        step1.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(400)
                                .setInterpolator(new DecelerateInterpolator(1.5f))
                                .start();
                    })
                    .start();
        } else {
            super.onBackPressed();
        }
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

    class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wallet_setup_minimal, parent, false);
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
            
            // Optionally add staggered entrance for list items if needed
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(40f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(position * 50L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        @Override
        public int getItemCount() {
            return walletEntries.size();
        }

        class VH extends RecyclerView.ViewHolder {
            EditText etName, etBalance;
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
