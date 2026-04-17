package com.app.pebble.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.pebble.R;
import com.app.pebble.data.model.Wallet;
import com.app.pebble.utils.NumberUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the swipeable wallet balance cards in the home screen ViewPager2.
 * Each page displays a wallet's balance and name using the gradient card design.
 */
public class WalletCardAdapter extends RecyclerView.Adapter<WalletCardAdapter.WalletCardVH> {

    private List<Wallet> wallets = new ArrayList<>();

    public void setWallets(List<Wallet> wallets) {
        this.wallets = wallets != null ? wallets : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Wallet> getWallets() {
        return wallets;
    }

    @NonNull
    @Override
    public WalletCardVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_balance_card, parent, false);
        return new WalletCardVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WalletCardVH holder, int position) {
        Wallet wallet = wallets.get(position);
        holder.tvWalletName.setText(wallet.getName().toUpperCase());

        android.content.SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences(com.app.pebble.ui.settings.SettingsActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        boolean hideBalance = prefs.getBoolean(com.app.pebble.ui.settings.SettingsActivity.PREF_HIDE_BALANCE, false);

        Runnable showBalanceAnim = () -> {
            android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, (float) wallet.getBalance());
            animator.setDuration(1200); // 1.2 second buttery count up
            animator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
            animator.addUpdateListener(animation -> {
                float val = (float) animation.getAnimatedValue();
                holder.tvBalance.setText(NumberUtils.formatCurrency(val));
            });
            animator.start();
        };

        if (hideBalance) {
            holder.tvBalance.setText("₹ ***");
            holder.tvBalance.setOnClickListener(v -> {
                showBalanceAnim.run();
                holder.tvBalance.setOnClickListener(null); // Prevent multiple clicks
                holder.tvBalance.postDelayed(() -> {
                    // Re-hide after 5 seconds
                    notifyItemChanged(position);
                }, 5000);
            });
        } else {
            holder.tvBalance.setOnClickListener(null); // Reset
            showBalanceAnim.run();
        }
    }

    @Override
    public int getItemCount() {
        return wallets.size();
    }

    static class WalletCardVH extends RecyclerView.ViewHolder {
        final TextView tvBalance;
        final TextView tvWalletName;

        WalletCardVH(View itemView) {
            super(itemView);
            tvBalance = itemView.findViewById(R.id.tv_card_balance);
            tvWalletName = itemView.findViewById(R.id.tv_card_wallet_name);
        }
    }
}
