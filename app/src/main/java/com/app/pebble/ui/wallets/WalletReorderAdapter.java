package com.app.pebble.ui.wallets;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.pebble.R;
import com.app.pebble.data.model.Wallet;

import java.util.Collections;
import java.util.List;

public class WalletReorderAdapter extends RecyclerView.Adapter<WalletReorderAdapter.ReorderVH> {

    private final List<Wallet> wallets;
    private final OnDragStartListener dragStartListener;

    public interface OnDragStartListener {
        void onDragStarted(RecyclerView.ViewHolder viewHolder);
    }

    public WalletReorderAdapter(List<Wallet> wallets, OnDragStartListener listener) {
        this.wallets = wallets;
        this.dragStartListener = listener;
    }

    @NonNull
    @Override
    public ReorderVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reorder_wallet, parent, false);
        return new ReorderVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReorderVH holder, int position) {
        Wallet wallet = wallets.get(position);
        holder.tvName.setText(wallet.getName());

        holder.ivHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dragStartListener.onDragStarted(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return wallets.size();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(wallets, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(wallets, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public List<Wallet> getWallets() {
        return wallets;
    }

    static class ReorderVH extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivHandle;

        ReorderVH(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_reorder_name);
            ivHandle = itemView.findViewById(R.id.iv_drag_handle);
        }
    }
}
