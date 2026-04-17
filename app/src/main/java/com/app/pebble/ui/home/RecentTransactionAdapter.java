package com.app.pebble.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.app.pebble.R;
import com.app.pebble.data.model.Category;
import com.app.pebble.data.model.Transaction;
import com.app.pebble.data.model.Wallet;
import com.app.pebble.utils.Constants;
import com.app.pebble.utils.DateUtils;
import com.app.pebble.utils.NumberUtils;
import com.app.pebble.viewmodel.ExpenseViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentTransactionAdapter extends RecyclerView.Adapter<RecentTransactionAdapter.VH> {

    private List<Transaction> transactions = new ArrayList<>();
    private final ExpenseViewModel viewModel;

    public RecentTransactionAdapter(ExpenseViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void setTransactions(List<Transaction> list) {
        this.transactions = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Transaction t = transactions.get(position);
        Context ctx = holder.itemView.getContext();

        // --- 1. Date Header Logic ---
        String currentDateStr = DateUtils.formatDate(t.getDate());
        boolean showHeader = false;

        if (position == 0) {
            showHeader = true; // First item always shows header
        } else {
            Transaction prev = transactions.get(position - 1);
            String prevDateStr = DateUtils.formatDate(prev.getDate());
            if (!currentDateStr.equals(prevDateStr)) {
                showHeader = true;
            }
        }

        if (showHeader) {
            holder.layoutDateHeader.setVisibility(View.VISIBLE);
            
            // Format nice date strings
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d.", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            holder.tvHeaderDate.setText(dateFormat.format(new Date(t.getDate())));
            holder.tvHeaderDay.setText(dayFormat.format(new Date(t.getDate())));

            // Calculate daily total
            double dailyTotal = 0;
            for (int i = position; i < transactions.size(); i++) {
                Transaction dt = transactions.get(i);
                if (DateUtils.formatDate(dt.getDate()).equals(currentDateStr)) {
                    if (Constants.TYPE_EXPENSE.equals(dt.getType())) {
                        dailyTotal -= dt.getAmount(); // treating net impact
                    } else if (Constants.TYPE_INCOME.equals(dt.getType())) {
                        dailyTotal += dt.getAmount();
                    }
                } else {
                    break;
                }
            }
            holder.tvHeaderTotal.setText(NumberUtils.formatCurrency(Math.abs(dailyTotal)));
            if (dailyTotal < 0) {
                holder.tvHeaderTotal.setTextColor(Color.parseColor("#FFFFFF")); // Expense day -> white
            } else {
                holder.tvHeaderTotal.setTextColor(Color.parseColor("#2ECCA0")); // Income day -> green
            }
        } else {
            holder.layoutDateHeader.setVisibility(View.GONE);
        }

        // --- 2. Bind Basic Content ---
        holder.tvTitle.setText(t.getTitle());
        String note = t.getNote();
        
        holder.tvSubtitle.setText("...");

        // --- 3. Dynamic Category and Wallet fetching ---
        new Thread(() -> {
            Category c = viewModel.getRepository().getCategoryByIdSync(t.getCategoryId());
            Wallet w = viewModel.getRepository().getWalletByIdSync(t.getWalletId());
            
            holder.itemView.post(() -> {
                StringBuilder subtitle = new StringBuilder();
                if (c != null && !TextUtils.isEmpty(c.getName())) {
                    subtitle.append(c.getName());
                } else if (Constants.TYPE_TRANSFER.equals(t.getType())) {
                    subtitle.append("Transfer");
                }
                
                if (w != null && !TextUtils.isEmpty(w.getName())) {
                    if (subtitle.length() > 0) subtitle.append(" • ");
                    subtitle.append(w.getName());
                }
                
                if (!TextUtils.isEmpty(note)) {
                    if (subtitle.length() > 0) subtitle.append(" • ");
                    subtitle.append(note);
                }
                
                holder.tvSubtitle.setText(subtitle.toString());
            });
        }).start();

        // --- 4. Income / Expense Styling ---
        if (Constants.TYPE_INCOME.equals(t.getType())) {
            holder.tvAmount.setText("+" + NumberUtils.formatCurrency(t.getAmount()));
            holder.tvAmount.setTextColor(Color.parseColor("#2ECCA0"));
            holder.boxIndicator.setBackgroundResource(R.drawable.bg_circle_income);
            holder.ivIndicator.setImageResource(R.drawable.ic_minimal_down);
        } else if (Constants.TYPE_EXPENSE.equals(t.getType())) {
            holder.tvAmount.setText("-" + NumberUtils.formatCurrency(t.getAmount()));
            holder.tvAmount.setTextColor(Color.parseColor("#FFFFFF"));
            holder.boxIndicator.setBackgroundResource(R.drawable.bg_circle_expense);
            holder.ivIndicator.setImageResource(R.drawable.ic_minimal_up);
        } else {
            // Transfer
            holder.tvAmount.setText(NumberUtils.formatCurrency(t.getAmount()));
            holder.tvAmount.setTextColor(Color.parseColor("#A0A0A0"));
            holder.boxIndicator.setBackgroundResource(R.drawable.bg_circle_expense);
            holder.ivIndicator.setImageResource(R.drawable.ic_minimal_up);
        }
        
        // iOS Physical Squish Touches
        com.app.pebble.utils.UIUtils.applySquishTouch(holder.itemView);

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(ctx, com.app.pebble.ui.transaction.AddTransactionActivity.class);
            i.putExtra(Constants.EXTRA_TRANSACTION_TYPE, t.getType());
            i.putExtra(Constants.EXTRA_TRANSACTION_ID, t.getId());
            ctx.startActivity(i);
            
            if (ctx instanceof android.app.Activity) {
                ((android.app.Activity) ctx).overridePendingTransition(R.anim.slide_up_in, R.anim.stay);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(ctx, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
            View view = LayoutInflater.from(ctx).inflate(R.layout.bottom_sheet_confirm_delete, null);
            dialog.setContentView(view);
            
            View parent = (View) view.getParent();
            if (parent != null) {
                parent.setBackgroundResource(android.R.color.transparent);
            }
            
            TextView tvBody = view.findViewById(R.id.tv_delete_body);
            tvBody.setText("Deleting this transaction will remove it from the transaction history and update the balance accordingly.");
            
            view.findViewById(R.id.btn_cancel).setOnClickListener(btn -> dialog.dismiss());
            view.findViewById(R.id.btn_delete).setOnClickListener(btn -> {
                viewModel.deleteTransaction(t);
                dialog.dismiss();
            });
            
            dialog.show();
            return true;
        });

        // Add cascading entrance stagger effect
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(60f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(position * 60L) // Staggered entry
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class VH extends RecyclerView.ViewHolder {
        LinearLayout layoutDateHeader;
        TextView tvHeaderDate, tvHeaderDay, tvHeaderTotal;
        TextView tvTitle, tvSubtitle, tvAmount;
        View boxIndicator, cardView;
        ImageView ivIndicator;

        VH(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_transaction);
            layoutDateHeader = itemView.findViewById(R.id.layout_date_header);
            tvHeaderDate = itemView.findViewById(R.id.tv_header_date);
            tvHeaderDay = itemView.findViewById(R.id.tv_header_day);
            tvHeaderTotal = itemView.findViewById(R.id.tv_header_total);
            
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            
            boxIndicator = itemView.findViewById(R.id.box_indicator);
            ivIndicator = itemView.findViewById(R.id.iv_indicator);
        }
    }
}
