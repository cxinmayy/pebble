package com.app.pebble.ui.wallets;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.app.pebble.R;
import com.app.pebble.data.model.Wallet;
import com.app.pebble.utils.NumberUtils;
import com.app.pebble.viewmodel.ExpenseViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class WalletsActivity extends AppCompatActivity {

    private ExpenseViewModel viewModel;
    private RecyclerView rvWallets;
    private TextView tvEmpty;
    private WalletCardPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallets);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        rvWallets = findViewById(R.id.rv_wallets);
        tvEmpty = findViewById(R.id.tv_empty);
        TextView tvTotalBalance = findViewById(R.id.tv_total_balance);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        TextView btnAdd = findViewById(R.id.btn_add_wallet_full);
        btnAdd.setOnClickListener(v -> showAddWalletDialog());

        // Setup horizontal RecyclerView with SnapHelper for Carousel effect
        adapter = new WalletCardPagerAdapter(viewModel);
        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvWallets.setLayoutManager(lm);
        rvWallets.setAdapter(adapter);

        findViewById(R.id.btn_reorder).setOnClickListener(v -> showReorderDialog());

        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(rvWallets);

        // Advanced smooth scaling card animation with snap haptics
        rvWallets.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int lastCenteredPosition = -1;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int centerX = recyclerView.getWidth() / 2;
                int closestChildIndex = -1;
                float minDistance = Float.MAX_VALUE;
                
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    int childCenterX = (child.getLeft() + child.getRight()) / 2;
                    float distanceFromCenter = Math.abs(centerX - childCenterX);
                    
                    if (distanceFromCenter < minDistance) {
                        minDistance = distanceFromCenter;
                        closestChildIndex = recyclerView.getChildAdapterPosition(child);
                    }
                    
                    float maxDistance = recyclerView.getWidth() / 2f;
                    float fraction = Math.min(1f, distanceFromCenter / maxDistance);
                    
                    float scale = 1f - (0.15f * fraction); // Center 1.0, Edges 0.85
                    child.setScaleX(scale);
                    child.setScaleY(scale);
                    
                    float alpha = 1f - (0.4f * fraction); // Center 1.0, Edges 0.6
                    child.setAlpha(alpha);
                }

                if (closestChildIndex != -1 && closestChildIndex != lastCenteredPosition) {
                    if (lastCenteredPosition != -1) { // Only tick when changing from one card to another
                        recyclerView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                    }
                    lastCenteredPosition = closestChildIndex;
                }
            }
        });

        rvWallets.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rvWallets.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // Trigger an initial animation state calculation
                rvWallets.scrollBy(1, 0);
                rvWallets.scrollBy(-1, 0);
            }
        });

        viewModel.getAllWallets().observe(this, baseWallets -> {
            if (baseWallets != null && !baseWallets.isEmpty()) {
                tvEmpty.setVisibility(View.GONE);
                rvWallets.setVisibility(View.VISIBLE);
                
                List<Wallet> sortedWallets = com.app.pebble.utils.WalletSortUtils.getSortedWallets(this, baseWallets);
                adapter.setWallets(sortedWallets);
                
                double total = 0;
                for (Wallet w : sortedWallets) {
                    total += w.getBalance();
                }
                tvTotalBalance.setText(NumberUtils.formatCurrency(total));
            } else {
                tvEmpty.setVisibility(View.VISIBLE);
                rvWallets.setVisibility(View.GONE);
                tvTotalBalance.setText(NumberUtils.formatCurrency(0));
            }
        });
    }

    private void showReorderDialog() {
        if (adapter.getItemCount() <= 0) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_reorder, null);
        
        dialog.setContentView(view);
        View parent = (View) view.getParent();
        if (parent != null) {
            parent.setBackgroundResource(android.R.color.transparent);
        }

        RecyclerView rvReorder = view.findViewById(R.id.rv_reorder);
        rvReorder.setLayoutManager(new LinearLayoutManager(this));

        // Start with current sorted state
        List<Wallet> currentOrder = new ArrayList<>(adapter.getWallets());
        androidx.recyclerview.widget.ItemTouchHelper[] touchHelperRef = new androidx.recyclerview.widget.ItemTouchHelper[1];

        WalletReorderAdapter reorderAdapter = new WalletReorderAdapter(currentOrder, viewHolder -> {
            if (touchHelperRef[0] != null) {
                touchHelperRef[0].startDrag(viewHolder);
            }
        });
        rvReorder.setAdapter(reorderAdapter);

        androidx.recyclerview.widget.ItemTouchHelper.Callback callback = new androidx.recyclerview.widget.ItemTouchHelper.Callback() {
            @Override
            public boolean isLongPressDragEnabled() { return true; }
            @Override
            public boolean isItemViewSwipeEnabled() { return false; }
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN, 0);
            }
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                reorderAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        };
        
        touchHelperRef[0] = new androidx.recyclerview.widget.ItemTouchHelper(callback);
        touchHelperRef[0].attachToRecyclerView(rvReorder);

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            com.app.pebble.utils.WalletSortUtils.saveWalletOrder(this, reorderAdapter.getWallets());
            // Update local adapter immediately
            adapter.setWallets(reorderAdapter.getWallets());
            // Reset to the newly requested primary wallet position
            rvReorder.post(() -> rvWallets.smoothScrollToPosition(0));
            dialog.dismiss();
            Toast.makeText(this, "Order saved", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showAddWalletDialog() {
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

        tvTitle.setText(R.string.wallet_setup_title);
        etName.setHint(R.string.hint_wallet_name);
        
        etBalance.setVisibility(View.VISIBLE);
        etBalance.setHint(R.string.hint_initial_balance);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String balanceStr = etBalance.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, R.string.error_wallet_name_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            double balance = 0;
            if (!TextUtils.isEmpty(balanceStr)) {
                try {
                    balance = Double.parseDouble(balanceStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, R.string.error_wallet_balance_invalid, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            viewModel.insertWallet(new Wallet(name, balance, System.currentTimeMillis()));
            Toast.makeText(this, "Wallet added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}

class WalletCardPagerAdapter extends RecyclerView.Adapter<WalletCardPagerAdapter.VH> {

    private List<Wallet> wallets = new ArrayList<>();
    private final ExpenseViewModel viewModel;

    public WalletCardPagerAdapter(ExpenseViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void setWallets(List<Wallet> wallets) {
        this.wallets = new ArrayList<>(wallets);
        notifyDataSetChanged();
    }

    public List<Wallet> getWallets() {
        return wallets;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Wallet w = wallets.get(position);
        holder.tvName.setText(w.getName().toUpperCase());
        holder.tvBalance.setText(NumberUtils.formatCurrency(w.getBalance()));

        holder.itemView.setOnClickListener(v -> {
            android.content.Context ctx = holder.itemView.getContext();
            android.content.Intent intent = new android.content.Intent(ctx, WalletDetailsActivity.class);
            intent.putExtra("WALLET_ID", w.getId());
            if (ctx instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) ctx;
                ctx.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_up_in, R.anim.stay);
            } else {
                ctx.startActivity(intent);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            android.content.Context ctx = holder.itemView.getContext();
            com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(ctx, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
            View view = LayoutInflater.from(ctx).inflate(R.layout.bottom_sheet_confirm_delete, null);
            dialog.setContentView(view);
            
            View parent = (View) view.getParent();
            if (parent != null) {
                parent.setBackgroundResource(android.R.color.transparent);
            }
            
            TextView tvBody = view.findViewById(R.id.tv_delete_body);
            tvBody.setText("Deleting this wallet will remove it and all its associated transactions.");
            
            view.findViewById(R.id.btn_cancel).setOnClickListener(btn -> dialog.dismiss());
            view.findViewById(R.id.btn_delete).setOnClickListener(btn -> {
                viewModel.deleteWalletWithTransactions(w);
                dialog.dismiss();
            });
            
            dialog.show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return wallets.size();
    }

    class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvBalance;

        VH(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_wallet_name);
            tvBalance = itemView.findViewById(R.id.tv_wallet_balance);
        }
    }
}
