package com.app.pebble.ui.transaction;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

import com.app.pebble.R;
import com.app.pebble.data.model.Category;
import com.app.pebble.data.model.Transaction;
import com.app.pebble.data.model.Wallet;
import com.app.pebble.utils.Constants;
import com.app.pebble.utils.DateUtils;
import com.app.pebble.utils.NumberUtils;
import com.app.pebble.viewmodel.ExpenseViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddTransactionActivity extends AppCompatActivity {

    private ExpenseViewModel viewModel;

    // Core Navigation
    private ViewFlipper viewFlipper;
    private String transactionType;
    private long selectedDateMillis;
    private int editTransactionId = -1;
    private Transaction editingTransaction = null;

    // Data
    private List<Category> categories = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();
    private Wallet selectedWallet = null;
    private Category selectedCategory = null;

    // Step 1: Numpad
    private TextView tvAmountDisplay;
    private LinearLayout llWalletChips1;
    private String amountBuffer = "0";

    // Step 2: Details
    private EditText etTitle, etNote;
    private TextView tvDate, btnAddCategory, tvAmountPreview, tvAddMoneyTo;
    private LinearLayout llWalletChips2;

    private Typeface outfitFont = null;

    // Chip color constants
    private static final String COLOR_SELECTED_BG = "#FFFFFF";
    private static final String COLOR_SELECTED_TEXT = "#121212";
    private static final String COLOR_UNSELECTED_TEXT = "#9E99B3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Shared Element Transitions
        getWindow().requestFeature(android.view.Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setSharedElementEnterTransition(new android.transition.ChangeBounds());
        getWindow().setReturnTransition(new android.transition.Fade());

        setContentView(R.layout.activity_add_transaction);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        transactionType = getIntent().getStringExtra(Constants.EXTRA_TRANSACTION_TYPE);
        if (transactionType == null)
            transactionType = Constants.TYPE_INCOME;
        editTransactionId = getIntent().getIntExtra(Constants.EXTRA_TRANSACTION_ID, -1);

        if (editTransactionId != -1) {
            View root = findViewById(R.id.root_container);
            if (root != null) {
                root.setTransitionName("transaction_card_" + editTransactionId);
            }
        }

        try {
            outfitFont = ResourcesCompat.getFont(this, R.font.outfit);
        } catch (Exception ignored) {
        }

        viewFlipper = findViewById(R.id.view_flipper);

        initStep1Views();
        initStep2Views();
        setupDate();
        loadData();
    }

    private void initStep1Views() {
        TextView btnToggleType = findViewById(R.id.btn_type_toggle_1);
        llWalletChips1 = findViewById(R.id.ll_wallet_chips_1);
        tvAmountDisplay = findViewById(R.id.tv_amount_display);

        findViewById(R.id.btn_close_1).setOnClickListener(v -> finish());
        findViewById(R.id.btn_cancel_1).setOnClickListener(v -> finish());

        btnToggleType.setText(Constants.TYPE_INCOME.equals(transactionType) ? "↓ Income" : "↑ Expense");
        btnToggleType.setOnClickListener(v -> {
            transactionType = Constants.TYPE_INCOME.equals(transactionType) ? Constants.TYPE_EXPENSE
                    : Constants.TYPE_INCOME;
            updateTypeToggles();
        });

        // Initialize Numpad clicks
        GridLayout grid = findViewById(R.id.grid_numpad);
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            String tag = (String) child.getTag();
            if (tag != null) {
                child.setOnClickListener(v -> handleNumpadInput(tag, v));
            }
        }
        animateNumpadEntrance();

        findViewById(R.id.btn_enter).setOnClickListener(v -> {
            if (Double.parseDouble(amountBuffer) <= 0) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedWallet == null) {
                if (!wallets.isEmpty())
                    selectedWallet = wallets.get(0);
                else {
                    Toast.makeText(this, "Please select an account", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            tvAmountPreview.setText(NumberUtils.formatCurrency(Double.parseDouble(amountBuffer)));
            viewFlipper.setDisplayedChild(1);
            
            if (selectedCategory == null) {
                showCategoryBottomSheet();
            }
        });
    }

    private void initStep2Views() {
        TextView btnToggleType2 = findViewById(R.id.btn_type_toggle_2);
        findViewById(R.id.btn_close_2).setOnClickListener(v -> finish());
        btnToggleType2.setOnClickListener(v -> {
            transactionType = Constants.TYPE_INCOME.equals(transactionType) ? Constants.TYPE_EXPENSE
                    : Constants.TYPE_INCOME;
            updateTypeToggles();
        });

        etTitle = findViewById(R.id.et_title);
        etNote = findViewById(R.id.et_note);
        tvDate = findViewById(R.id.tv_date);
        btnAddCategory = findViewById(R.id.btn_add_category);

        tvAddMoneyTo = findViewById(R.id.tv_add_money_to);
        llWalletChips2 = findViewById(R.id.ll_wallet_chips_2);
        tvAmountPreview = findViewById(R.id.tv_amount_preview);

        findViewById(R.id.btn_back_to_numpad).setOnClickListener(v -> {
            viewFlipper.setDisplayedChild(0);
            animateNumpadEntrance();
        });
        tvAmountPreview.setOnClickListener(v -> {
            viewFlipper.setDisplayedChild(0);
            animateNumpadEntrance();
        });
        findViewById(R.id.btn_final_add).setOnClickListener(v -> saveTransaction());

        btnAddCategory.setOnClickListener(v -> showCategoryBottomSheet());

        updateTypeToggles();
    }

    private void updateTypeToggles() {
        TextView t1 = findViewById(R.id.btn_type_toggle_1);
        TextView t2 = findViewById(R.id.btn_type_toggle_2);
        String txt = Constants.TYPE_INCOME.equals(transactionType) ? "↓ Income" : "↑ Expense";
        t1.setText(txt);
        t2.setText(txt);

        if (Constants.TYPE_INCOME.equals(transactionType)) {
            etTitle.setHint("Income title");
            tvAddMoneyTo.setText("Add money to");
        } else {
            etTitle.setHint("Expense title");
            tvAddMoneyTo.setText("Deduct money from");
        }
    }

    private void handleNumpadInput(String input, View tappedView) {
        // High-end tactile pop
        tappedView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        tappedView.setScaleX(0.80f);
        tappedView.setScaleY(0.80f);
        tappedView.animate().scaleX(1f).scaleY(1f).setDuration(200).setInterpolator(new OvershootInterpolator(2f))
                .start();

        if ("DEL".equals(input)) {
            if (amountBuffer.length() > 1) {
                amountBuffer = amountBuffer.substring(0, amountBuffer.length() - 1);
            } else {
                amountBuffer = "0";
            }
        } else if (".".equals(input)) {
            if (!amountBuffer.contains(".")) {
                amountBuffer += ".";
            }
        } else {
            if (amountBuffer.equals("0")) {
                amountBuffer = input;
            } else {
                if (amountBuffer.length() < 10) {
                    amountBuffer += input;
                }
            }
        }
        tvAmountDisplay.setText(amountBuffer + " INR");

        // Bounce the main amount display text
        tvAmountDisplay.setScaleX(1.05f);
        tvAmountDisplay.setScaleY(1.05f);
        tvAmountDisplay.animate().scaleX(1f).scaleY(1f).setDuration(200)
                .setInterpolator(new OvershootInterpolator(1.5f)).start();
    }

    private void setupDate() {
        selectedDateMillis = System.currentTimeMillis();
        tvDate.setText(DateUtils.formatDateTime(selectedDateMillis));

        findViewById(R.id.btn_change_date).setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selectedDateMillis);
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                selectedDateMillis = calendar.getTimeInMillis();
                tvDate.setText(DateUtils.formatDateTime(selectedDateMillis));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void animateNumpadEntrance() {
        GridLayout grid = findViewById(R.id.grid_numpad);
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            // Stagger entrance
            child.setTranslationY(80f);
            child.setAlpha(0f);
            child.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setStartDelay(50 + (i * 25L))
                    .setDuration(400)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }
    }

    private void loadData() {
        new Thread(() -> {
            List<Wallet> rawWallets = viewModel.getRepository().getAllWalletsSync();
            // Apply user-defined custom sort order
            wallets = com.app.pebble.utils.WalletSortUtils.getSortedWallets(this, rawWallets);
            categories = viewModel.getRepository().getAllCategoriesSync();

            if (editTransactionId != -1) {
                editingTransaction = viewModel.getTransactionByIdSync(editTransactionId);
            }

            // Check for prefilled wallet (e.g. launched from WalletDetails)
            int prefilledWalletId = getIntent().getIntExtra("PREFILLED_WALLET_ID", -1);

            runOnUiThread(() -> {
                if (editingTransaction != null) {
                    amountBuffer = String.valueOf(editingTransaction.getAmount());
                    if (amountBuffer.endsWith(".0"))
                        amountBuffer = amountBuffer.substring(0, amountBuffer.length() - 2);
                    tvAmountDisplay.setText(amountBuffer + " INR");

                    etTitle.setText(editingTransaction.getTitle());
                    etNote.setText(editingTransaction.getNote());
                    selectedDateMillis = editingTransaction.getDate();
                    tvDate.setText(DateUtils.formatDateTime(selectedDateMillis));

                    for (Wallet w : wallets) {
                        if (w.getId() == editingTransaction.getWalletId()) {
                            selectedWallet = w;
                            break;
                        }
                    }

                    for (Category c : categories) {
                        if (c.getId() == editingTransaction.getCategoryId()) {
                            selectedCategory = c;
                            break;
                        }
                    }

                    if (selectedCategory != null) {
                        btnAddCategory.setText(selectedCategory.getName());
                        btnAddCategory.setBackgroundResource(R.drawable.bg_pill_selected);
                        btnAddCategory.setTextColor(Color.parseColor(COLOR_SELECTED_TEXT));
                    }

                    ((TextView) findViewById(R.id.btn_final_add)).setText("Update");
                } else if (prefilledWalletId != -1) {
                    // Wallet prefilled from WalletDetails screen
                    for (Wallet w : wallets) {
                        if (w.getId() == prefilledWalletId) {
                            selectedWallet = w;
                            break;
                        }
                    }
                    if (selectedWallet == null && !wallets.isEmpty()) {
                        selectedWallet = wallets.get(0); // Fallback to first sorted
                    }
                } else if (!wallets.isEmpty()) {
                    // Default: pre-select the #1 wallet in user's custom sort order
                    selectedWallet = wallets.get(0);
                }
                renderWalletChips(false);
            });
        }).start();
    }

    // ──────────────────────────────────────────────
    // Wallet Chip Rendering with Bubble Animation
    // ──────────────────────────────────────────────

    private void renderWalletChips(boolean animate) {
        llWalletChips1.removeAllViews();
        llWalletChips2.removeAllViews();

        for (int i = 0; i < wallets.size(); i++) {
            Wallet w = wallets.get(i);
            boolean isSelected = (selectedWallet != null && w.getId() == selectedWallet.getId());

            TextView chip1 = createChip(w, isSelected);
            TextView chip2 = createChip(w, isSelected);

            llWalletChips1.addView(chip1);
            llWalletChips2.addView(chip2);

            // Bubble pop animation on the selected chip
            if (isSelected && animate) {
                bubbleAnimate(chip1);
                bubbleAnimate(chip2);
            }
        }
    }

    private void bubbleAnimate(View view) {
        // Start slightly smaller, then overshoot to full size
        view.setScaleX(0.7f);
        view.setScaleY(0.7f);
        view.setAlpha(0.5f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.7f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.7f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.5f, 1.0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setDuration(350);
        set.setInterpolator(new OvershootInterpolator(1.5f));
        set.start();
    }

    private TextView createChip(Wallet w, boolean isSelected) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 24, 0);
        chip.setLayoutParams(params);

        chip.setText(w.getName());
        chip.setTextSize(16);
        chip.setPadding(64, 32, 64, 32);
        chip.setGravity(android.view.Gravity.CENTER);

        if (outfitFont != null) {
            chip.setTypeface(outfitFont, isSelected ? Typeface.BOLD : Typeface.NORMAL);
        } else {
            chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
        }

        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_pill_selected);
            chip.setTextColor(Color.parseColor(COLOR_SELECTED_TEXT));
            chip.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_minimal_wallet, 0, 0, 0);
            chip.setCompoundDrawablePadding(16);
            chip.setCompoundDrawableTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor(COLOR_SELECTED_TEXT)));
        } else {
            chip.setBackgroundResource(R.drawable.bg_pill_minimal);
            chip.setTextColor(Color.parseColor("#B0B0B0")); // Brighter aesthetic unselected text
            chip.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        chip.setOnClickListener(v -> {
            if (selectedWallet == null || w.getId() != selectedWallet.getId()) {
                selectedWallet = w;
                renderWalletChips(true); // animate on selection change
            }
        });

        return chip;
    }

    // ──────────────────────────────────────────────
    // Category Bottom Sheet — Aesthetic Pill Grid
    // ──────────────────────────────────────────────

    private void showCategoryBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this,
                com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_categories, null);
        dialog.setContentView(view);
        View parent = (View) view.getParent();
        if (parent != null) {
            parent.setBackgroundResource(android.R.color.transparent);
        }

        TextView title = view.findViewById(R.id.tv_sheet_title);
        title.setText("Category");

        ChipGroup chipGroup = view.findViewById(R.id.chip_group_categories);
        chipGroup.setChipSpacingVertical(16);
        chipGroup.setChipSpacingHorizontal(12);

        if (categories.isEmpty()) {
            Toast.makeText(this, "No categories yet. Add one in settings.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            boolean alreadySelected = (selectedCategory != null && selectedCategory.getId() == c.getId());

            TextView pill = new TextView(this);
            pill.setText(c.getName());
            pill.setTextSize(18);
            pill.setPadding(56, 32, 56, 32);
            pill.setGravity(android.view.Gravity.CENTER);

            if (outfitFont != null) {
                pill.setTypeface(outfitFont, Typeface.BOLD);
            } else {
                pill.setTypeface(null, Typeface.BOLD);
            }

            if (alreadySelected) {
                pill.setBackgroundResource(R.drawable.bg_pill_selected);
                pill.setTextColor(Color.parseColor("#121212"));
            } else {
                pill.setBackgroundResource(R.drawable.bg_pill_minimal);
                pill.setTextColor(Color.WHITE);
            }

            pill.setOnClickListener(v -> {
                if (selectedCategory != null && selectedCategory.getId() == c.getId()) {
                    // Deselect
                    selectedCategory = null;
                    btnAddCategory.setText("+ Add category");
                    btnAddCategory.setBackgroundResource(R.drawable.bg_pill_minimal);
                    btnAddCategory.setTextColor(Color.WHITE);
                } else {
                    // Select
                    selectedCategory = c;
                    btnAddCategory.setText(c.getName());
                    btnAddCategory.setBackgroundResource(R.drawable.bg_pill_selected);
                    btnAddCategory.setTextColor(Color.parseColor(COLOR_SELECTED_TEXT));
                    bubbleAnimate(btnAddCategory);
                }
                dialog.dismiss();
            });

            // Domino Effect Animation for Category Pill
            pill.setTranslationY(80f);
            pill.setAlpha(0f);
            pill.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setStartDelay(100 + (i * 35L)) // Delay staggered cascade
                    .setDuration(450)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();

            // Wrap in a FrameLayout for ChipGroup compatibility
            android.widget.FrameLayout wrapper = new android.widget.FrameLayout(this);
            wrapper.addView(pill);
            chipGroup.addView(wrapper);
        }

        dialog.show();
    }

    // ──────────────────────────────────────────────
    // Save Transaction
    // ──────────────────────────────────────────────

    private void saveTransaction() {
        String title = etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountBuffer);
        if (amount <= 0) {
            Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedWallet == null) {
            Toast.makeText(this, "Please select a wallet", Toast.LENGTH_SHORT).show();
            return;
        }

        String note = etNote.getText().toString().trim();

        Transaction t = new Transaction(
                amount,
                transactionType,
                title,
                selectedCategory.getId(),
                selectedWallet.getId(),
                0,
                note,
                selectedDateMillis);

        if (editingTransaction != null) {
            t.setId(editingTransaction.getId());
            viewModel.updateTransaction(t, editingTransaction);
            Toast.makeText(this, "Transaction updated!", Toast.LENGTH_SHORT).show();
        } else {
            viewModel.insertTransaction(t);
            Toast.makeText(this, "Transaction saved!", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out);
    }
}
