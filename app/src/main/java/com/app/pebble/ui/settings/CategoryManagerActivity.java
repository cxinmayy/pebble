package com.app.pebble.ui.settings;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.pebble.R;
import com.app.pebble.data.model.Category;
import com.app.pebble.data.model.Transaction;
import com.app.pebble.ui.home.RecentTransactionAdapter;
import com.app.pebble.utils.Constants;
import com.app.pebble.utils.NumberUtils;
import com.app.pebble.viewmodel.ExpenseViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryManagerActivity extends AppCompatActivity {

    private ExpenseViewModel viewModel;
    
    private RecyclerView rvCategoryCards;
    private RecyclerView rvTransactions;
    private TextView tvTotalExpense;
    
    private CategoryCardAdapter categoryCardAdapter;
    private RecentTransactionAdapter transactionAdapter;

    private List<Category> latestCategories = new ArrayList<>();
    private List<Transaction> latestTransactions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        rvCategoryCards = findViewById(R.id.rv_category_cards);
        rvTransactions = findViewById(R.id.rv_all_transactions);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add_category);

        // Setup Category Cards (Horizontal)
        categoryCardAdapter = new CategoryCardAdapter();
        rvCategoryCards.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategoryCards.setAdapter(categoryCardAdapter);

        // Setup Transactions (Vertical)
        transactionAdapter = new RecentTransactionAdapter(viewModel);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);

        observeData();

        fabAdd.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void observeData() {
        viewModel.getAllCategories().observe(this, categories -> {
            latestCategories = categories != null ? categories : new ArrayList<>();
            updateCategoryAdapter();
        });

        viewModel.getAllTransactions().observe(this, transactions -> {
            if (transactions == null) {
                latestTransactions = new ArrayList<>();
            } else {
                long startOfMonth = com.app.pebble.utils.DateUtils.getStartOfCurrentMonth();
                long endOfMonth = com.app.pebble.utils.DateUtils.getEndOfCurrentMonth();
                List<Transaction> monthTransactions = new ArrayList<>();
                for (Transaction t : transactions) {
                    if (t.getDate() >= startOfMonth && t.getDate() <= endOfMonth) {
                        monthTransactions.add(t);
                    }
                }
                latestTransactions = monthTransactions;
            }
            updateCategoryAdapter();
            transactionAdapter.setTransactions(latestTransactions);
        });
        
        viewModel.getTotalExpenseAmount().observe(this, total -> {
            if (total != null) {
                tvTotalExpense.setText(NumberUtils.formatCurrency(total));
            }
        });
    }

    private void updateCategoryAdapter() {
        categoryCardAdapter.setCategoriesAndTransactions(latestCategories, latestTransactions);
    }

    private void showAddCategoryDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_minimal_input, null);
        
        dialog.setContentView(view);
        View parent = (View) view.getParent();
        if (parent != null) {
            parent.setBackgroundResource(android.R.color.transparent);
        }

        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        EditText etName = view.findViewById(R.id.et_input_1);
        View btnSave = view.findViewById(R.id.btn_save);

        tvTitle.setText(R.string.btn_add_category);
        etName.setHint(R.string.hint_category_name);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.error_category_name_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            // Categories map broadly, using ALL type
            viewModel.insertCategory(new Category(name, "ALL", null));
            Toast.makeText(this, R.string.category_added, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    // ───── Inner Adapter for Category Cards ─────

    class CategoryCardAdapter extends RecyclerView.Adapter<CategoryCardAdapter.VH> {
        private List<Category> categories = new ArrayList<>();
        private Map<Integer, Double> categorySpends = new HashMap<>(); // Negative = Expense, Positive = Income
        private double totalExpense = 0;
        private double totalIncome = 0;
        
        // Define some nice glowing colors for cards
        private final String[] COLORS = {"#2ECCA0", "#8066FF", "#FF6B8B", "#FFB259", "#4DA6FF"};

        void setCategoriesAndTransactions(List<Category> list, List<Transaction> transactions) {
            this.categories = list;
            
            this.totalExpense = 0;
            this.totalIncome = 0;
            this.categorySpends.clear();
            
            for (Transaction t : transactions) {
                if (Constants.TYPE_EXPENSE.equals(t.getType())) {
                    totalExpense += t.getAmount();
                    categorySpends.put(t.getCategoryId(), 
                        categorySpends.getOrDefault(t.getCategoryId(), 0.0) - t.getAmount());
                } else if (Constants.TYPE_INCOME.equals(t.getType())) {
                    totalIncome += t.getAmount();
                    categorySpends.put(t.getCategoryId(), 
                        categorySpends.getOrDefault(t.getCategoryId(), 0.0) + t.getAmount());
                }
            }
            
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Category category = categories.get(position);
            holder.tvName.setText(category.getName());
            
            // Color sequence
            String colorHex = COLORS[position % COLORS.length];
            int parsedColor = Color.parseColor(colorHex);
            holder.viewBgColor.setBackgroundColor(parsedColor);
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                holder.itemView.setOutlineSpotShadowColor(parsedColor);
                holder.itemView.setOutlineAmbientShadowColor(parsedColor);
            }
            
            // Progress Calculation
            double netAmount = categorySpends.getOrDefault(category.getId(), 0.0);
            int percent = 0;
            if (netAmount < 0) {
                // It's a net expense category
                if (totalExpense > 0) {
                    percent = (int) Math.round((Math.abs(netAmount) / totalExpense) * 100);
                }
                holder.tvPercent.setText("spent " + percent + "%");
            } else if (netAmount > 0) {
                // It's a net income category
                if (totalIncome > 0) {
                    percent = (int) Math.round((netAmount / totalIncome) * 100);
                }
                holder.tvPercent.setText("earned " + percent + "%");
            } else {
                holder.tvPercent.setText("spent 0%");
            }
            
            // Clamp up to 100
            if (percent > 100) percent = 100;
            
            // Adjust linear weight to make vertical progress fill
            LinearLayout.LayoutParams emptyParams = (LinearLayout.LayoutParams) holder.viewProgressEmpty.getLayoutParams();
            emptyParams.weight = 100 - percent;
            holder.viewProgressEmpty.setLayoutParams(emptyParams);
            
            LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) holder.viewProgressFill.getLayoutParams();
            fillParams.weight = percent;
            holder.viewProgressFill.setLayoutParams(fillParams);

            holder.itemView.setOnLongClickListener(v -> {
                new Thread(() -> {
                    int count = viewModel.getRepository().getTransactionCountForCategorySync(category.getId());
                    runOnUiThread(() -> {
                        String message;
                        if (count > 0) {
                            message = "⚠️ This will permanently delete \"" + category.getName() 
                                + "\" and all " + count + " linked transaction(s).\n\nWallet balances will be adjusted accordingly.\n\nThis action cannot be undone.";
                        } else {
                            message = "Delete category \"" + category.getName() + "\"?\n\nThis action cannot be undone.";
                        }
                        
                        new AlertDialog.Builder(CategoryManagerActivity.this)
                            .setTitle("Delete Category")
                            .setMessage(message)
                            .setPositiveButton("Delete", (dialog, which) -> {
                                if (count > 0) {
                                    viewModel.getRepository().deleteCategoryWithTransactions(category);
                                } else {
                                    viewModel.deleteCategory(category);
                                }
                                Toast.makeText(CategoryManagerActivity.this, 
                                    "\"" + category.getName() + "\" deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show();
                    });
                }).start();
                return true;
            });
            
            holder.itemView.setOnClickListener(v -> {
               Toast.makeText(CategoryManagerActivity.this, "Hold to delete category", Toast.LENGTH_SHORT).show(); 
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPercent;
            View viewBgColor, viewProgressEmpty, viewProgressFill;

            VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_cat_name);
                tvPercent = itemView.findViewById(R.id.tv_spent_percent);
                viewBgColor = itemView.findViewById(R.id.bg_color);
                viewProgressEmpty = itemView.findViewById(R.id.progress_empty_weight);
                viewProgressFill = itemView.findViewById(R.id.progress_fill_weight);
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_right);
    }
}
