package com.app.pebble.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import android.graphics.drawable.GradientDrawable;

import com.app.pebble.App;
import com.app.pebble.R;
import com.app.pebble.data.model.Transaction;
import com.app.pebble.data.model.Wallet;
import com.app.pebble.ui.income.IncomeDetailActivity;
import com.app.pebble.ui.settings.SettingsActivity;
import com.app.pebble.ui.transaction.AddTransactionActivity;
import com.app.pebble.ui.transaction.TransferActivity;
import com.app.pebble.ui.wallets.WalletsActivity;
import com.app.pebble.utils.Constants;
import com.app.pebble.utils.DateUtils;
import com.app.pebble.utils.NumberUtils;
import com.app.pebble.viewmodel.ExpenseViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ExpenseViewModel viewModel;
    private TextView tvGreeting, tvEmptyTransactions, tvNoWallets;
    private RecyclerView rvTransactions;
    private ViewPager2 vpWalletCards;
    private LinearLayout llDotIndicator;
    private MaterialButton btnAddIncome, btnAddExpense;
    private LineChart chartExpense;
    private BottomNavigationView bottomNav;

    private RecentTransactionAdapter adapter;
    private WalletCardAdapter walletCardAdapter;
    
    private TextView btnFilter7D, btnFilter1M;
    private long currentGraphFilterMillis = 7L * 24 * 60 * 60 * 1000;
    private List<Transaction> allRecentTransactions = new ArrayList<>();
    
    // Search System
    private android.widget.FrameLayout topBarContainer;
    private View llTopBarDefault, llTopBarSearch, llDashboardHero;
    private android.widget.EditText etSearch;
    private boolean isSearching = false;
    private android.view.View flChartContainer; // New container to hide/show full chart box
    private String currentSearchQuery = "";
    private Long searchStartDate = null;
    private Long searchEndDate = null;
    private Integer searchWalletId = null;
    private Integer searchCategoryId = null;
    private String searchTypeFilter = "ALL"; // ALL, INCOME, EXPENSE

    // Month Filter (session-only — resets to current month on each app launch)
    private int filterMonth; // Calendar.MONTH (0-based)
    private int filterYear;
    private TextView tvMonthFilter;
    private static final String[] MONTH_NAMES = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };
    private static final String[] MONTH_SHORT = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        // Session-only: always starts at current month on fresh launch
        java.util.Calendar cal = java.util.Calendar.getInstance();
        filterMonth = cal.get(java.util.Calendar.MONTH);
        filterYear = cal.get(java.util.Calendar.YEAR);

        initViews();
        setupRecyclerView();
        setupWalletCards();
        setupListeners();
        observeViewModel();

        // Disable animation when starting from router
        overridePendingTransition(0, 0);
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvEmptyTransactions = findViewById(R.id.tv_empty_transactions);
        tvEmptyTransactions.setTextColor(android.graphics.Color.WHITE);
        tvNoWallets = findViewById(R.id.tv_no_wallets);
        rvTransactions = findViewById(R.id.rv_transactions);
        vpWalletCards = findViewById(R.id.vp_wallet_cards);
        llDotIndicator = findViewById(R.id.ll_dot_indicator);
        btnAddIncome = findViewById(R.id.btn_add_income);
        btnAddExpense = findViewById(R.id.btn_add_expense);
        chartExpense = findViewById(R.id.chart_expense);
        bottomNav = findViewById(R.id.bottom_nav);

        // -- Parallax Scroll Effect Setup --
        androidx.core.widget.NestedScrollView scrollHome = findViewById(R.id.scroll_home);
        scrollHome.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            float walletParallax = scrollY * 0.5f;
            float walletAlpha = Math.max(0f, 1f - (scrollY / 400f));
            vpWalletCards.setTranslationY(walletParallax);
            vpWalletCards.setAlpha(walletAlpha);

            float chartParallax = scrollY * 0.3f;
            float chartAlpha = Math.max(0f, 1f - (scrollY / 600f));
            chartExpense.setTranslationY(chartParallax);
            chartExpense.setAlpha(chartAlpha);
        });

        // Greeting is updated in onResume

        // Select Home tab
        bottomNav.setSelectedItemId(R.id.nav_home);

        btnFilter7D = findViewById(R.id.btn_filter_7d);
        btnFilter1M = findViewById(R.id.btn_filter_1m);

        btnFilter7D.setOnClickListener(v -> setGraphFilter(7L * 24 * 60 * 60 * 1000, btnFilter7D));
        btnFilter1M.setOnClickListener(v -> setGraphFilter(30L * 24 * 60 * 60 * 1000, btnFilter1M));

        com.app.pebble.utils.UIUtils.applySquishTouch(btnAddIncome);
        com.app.pebble.utils.UIUtils.applySquishTouch(btnAddExpense);

        // -- Search System --
        topBarContainer = findViewById(R.id.top_bar_container);
        llTopBarDefault = findViewById(R.id.ll_top_bar_default);
        llTopBarSearch = findViewById(R.id.ll_top_bar_search);
        llDashboardHero = findViewById(R.id.ll_dashboard_hero);
        etSearch = findViewById(R.id.et_search);

        findViewById(R.id.btn_open_search).setOnClickListener(v -> toggleSearchBar(true));
        findViewById(R.id.btn_close_search).setOnClickListener(v -> toggleSearchBar(false));

        findViewById(R.id.btn_search_filter).setOnClickListener(v -> {
            SearchFilterBottomSheet popup = new SearchFilterBottomSheet(
                searchStartDate, searchEndDate, searchTypeFilter, searchWalletId, searchCategoryId,
                (start, end, type, wallet, cat) -> {
                    searchStartDate = start;
                    searchEndDate = end;
                    searchTypeFilter = type;
                    searchWalletId = wallet;
                    searchCategoryId = cat;
                    applySearchEngine();
            });
            popup.show(getSupportFragmentManager(), "SearchFilter");
        });

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                currentSearchQuery = s.toString().trim();
                applySearchEngine();
            }
        });

        flChartContainer = findViewById(R.id.fl_chart_container);

        // Month Filter Chip
        tvMonthFilter = findViewById(R.id.tv_month_filter);
        tvMonthFilter.setText(MONTH_SHORT[filterMonth]);
        tvMonthFilter.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            showMonthFilterSheet();
        });
    }

    private void setGraphFilter(long timeInMillis, TextView activeBtn) {
        currentGraphFilterMillis = timeInMillis;
        
        // Reset styles
        int inactiveColor = ContextCompat.getColor(this, R.color.color_on_surface_secondary);
        int activeTextColor = ContextCompat.getColor(this, R.color.color_surface);
        
        btnFilter7D.setTextColor(inactiveColor);
        btnFilter7D.setBackground(null);
        btnFilter1M.setTextColor(inactiveColor);
        btnFilter1M.setBackground(null);
        
        // Re-process chart (unless searching)
        if (!isSearching) updateChart(allRecentTransactions);
    }

    private void toggleSearchBar(boolean open) {
        if (isSearching == open) return;
        isSearching = open;
        
        android.transition.TransitionManager.beginDelayedTransition(topBarContainer, new android.transition.ChangeBounds());
        
        if (open) {
            llTopBarDefault.setVisibility(View.GONE);
            llTopBarSearch.setVisibility(View.VISIBLE);
            
            llDashboardHero.setVisibility(View.GONE);
            
            etSearch.requestFocus();
            applySearchEngine();
            
        } else {
            llTopBarDefault.setVisibility(View.VISIBLE);
            llTopBarSearch.setVisibility(View.GONE);
            
            llDashboardHero.setVisibility(View.VISIBLE);
            
            searchStartDate = null;
            searchEndDate = null;
            searchWalletId = null;
            searchCategoryId = null;
            searchTypeFilter = "ALL";
            currentSearchQuery = "";
            etSearch.setText("");
            
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            
            if (allRecentTransactions.isEmpty()) {
                tvEmptyTransactions.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);
            } else {
                tvEmptyTransactions.setVisibility(View.GONE);
                rvTransactions.setVisibility(View.VISIBLE);
                adapter.setTransactions(allRecentTransactions);
                updateChart(allRecentTransactions);
            }
        }
    }

    private void applySearchEngine() {
        if (!isSearching) return;
        if (allRecentTransactions == null || allRecentTransactions.isEmpty()) {
            tvEmptyTransactions.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
            return;
        }

        String queryLower = currentSearchQuery.toLowerCase();
        
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : allRecentTransactions) {
            if (searchStartDate != null && t.getDate() < searchStartDate) continue;
            if (searchEndDate != null && t.getDate() > searchEndDate) continue;
            
            if (searchWalletId != null && t.getWalletId() != searchWalletId) continue;
            if (searchCategoryId != null && t.getCategoryId() != searchCategoryId) continue;
            
            if (!"ALL".equals(searchTypeFilter) && !searchTypeFilter.equals(t.getType())) continue;
            
            if (!queryLower.isEmpty()) {
                String title = t.getTitle() != null ? t.getTitle().toLowerCase() : "";
                String desc = t.getNote() != null ? t.getNote().toLowerCase() : "";
                if (!title.contains(queryLower) && !desc.contains(queryLower)) continue;
            }
            filtered.add(t);
        }
        
        if (filtered.isEmpty()) {
            tvEmptyTransactions.setVisibility(View.VISIBLE);
            tvEmptyTransactions.setTextColor(android.graphics.Color.WHITE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvEmptyTransactions.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
            adapter.setTransactions(filtered);
        }
    }

    private void setupRecyclerView() {
        adapter = new RecentTransactionAdapter(viewModel);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
    }

    private void setupWalletCards() {
        walletCardAdapter = new WalletCardAdapter();
        vpWalletCards.setAdapter(walletCardAdapter);

        // Smooth depth page transformer for premium swipe animation
        vpWalletCards.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);

            // Scale down pages that are off-screen
            float scaleFactor = Math.max(0.85f, 1f - absPos * 0.15f);
            page.setScaleY(scaleFactor);
            page.setScaleX(scaleFactor);

            // Fade out pages as they slide away
            page.setAlpha(Math.max(0.5f, 1f - absPos * 0.5f));

            // Add slight translation for depth effect
            if (position < -1) {
                page.setAlpha(0f);
            } else if (position <= 0) {
                // Left page: slide out with slight downward shift
                page.setTranslationY(absPos * 30f);
            } else if (position <= 1) {
                // Right page: slide in from the right with upward rise
                page.setTranslationY(absPos * 30f);
            } else {
                page.setAlpha(0f);
            }
        });

        // Page change callback for dot indicators
        vpWalletCards.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDotIndicator(position);
            }
        });

        // Tap on empty state opens wallets screen to add one
        tvNoWallets.setOnClickListener(v -> {
            startActivity(new Intent(this, WalletsActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    private void setupListeners() {
        btnAddIncome.setOnClickListener(v -> {
            Intent i = new Intent(this, AddTransactionActivity.class);
            i.putExtra(Constants.EXTRA_TRANSACTION_TYPE, Constants.TYPE_INCOME);
            startActivity(i);
            overridePendingTransition(R.anim.slide_up_in, R.anim.stay);
        });

        btnAddExpense.setOnClickListener(v -> {
            Intent i = new Intent(this, AddTransactionActivity.class);
            i.putExtra(Constants.EXTRA_TRANSACTION_TYPE, Constants.TYPE_EXPENSE);
            startActivity(i);
            overridePendingTransition(R.anim.slide_up_in, R.anim.stay);
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_wallets) {
                startActivity(new Intent(this, WalletsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void observeViewModel() {
        // Observe wallets for the swipeable cards
        viewModel.getAllWallets().observe(this, baseWallets -> {
            if (baseWallets != null && !baseWallets.isEmpty()) {
                vpWalletCards.setVisibility(View.VISIBLE);
                llDotIndicator.setVisibility(View.VISIBLE);
                tvNoWallets.setVisibility(View.GONE);

                List<Wallet> sortedWallets = com.app.pebble.utils.WalletSortUtils.getSortedWallets(this, baseWallets);
                walletCardAdapter.setWallets(sortedWallets);
                buildDotIndicator(sortedWallets.size());

                // Keep the current page if it's still valid
                int currentPage = vpWalletCards.getCurrentItem();
                if (currentPage >= sortedWallets.size()) {
                    vpWalletCards.setCurrentItem(sortedWallets.size() - 1, true);
                }
            } else {
                vpWalletCards.setVisibility(View.GONE);
                llDotIndicator.setVisibility(View.GONE);
                tvNoWallets.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getRecentTransactions().observe(this, transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                allRecentTransactions = new ArrayList<>(transactions);

                if (isSearching) {
                    applySearchEngine();
                } else {
                    List<Transaction> monthFiltered = filterBySelectedMonth(allRecentTransactions);
                    if (monthFiltered.isEmpty()) {
                        tvEmptyTransactions.setVisibility(View.VISIBLE);
                        tvEmptyTransactions.setTextColor(android.graphics.Color.WHITE);
                        tvEmptyTransactions.setText("No transactions in " + MONTH_NAMES[filterMonth] + " yet");
                        rvTransactions.setVisibility(View.GONE);
                        if (flChartContainer != null) flChartContainer.setVisibility(View.GONE);
                    } else {
                        tvEmptyTransactions.setVisibility(View.GONE);
                        rvTransactions.setVisibility(View.VISIBLE);
                        if (flChartContainer != null) flChartContainer.setVisibility(View.VISIBLE);
                        adapter.setTransactions(monthFiltered);
                        updateChart(monthFiltered);
                    }
                }
            } else {
                tvEmptyTransactions.setVisibility(View.VISIBLE);
                tvEmptyTransactions.setTextColor(android.graphics.Color.WHITE);
                tvEmptyTransactions.setText("No transactions yet");
                rvTransactions.setVisibility(View.GONE);
                if (flChartContainer != null) flChartContainer.setVisibility(View.GONE);
            }
        });
    }

    /** Filter transactions to only those within the currently selected month+year */
    private List<Transaction> filterBySelectedMonth(List<Transaction> source) {
        List<Transaction> result = new ArrayList<>();
        java.util.Calendar c = java.util.Calendar.getInstance();
        for (Transaction t : source) {
            c.setTimeInMillis(t.getDate());
            if (c.get(java.util.Calendar.MONTH) == filterMonth
                    && c.get(java.util.Calendar.YEAR) == filterYear) {
                result.add(t);
            }
        }
        return result;
    }

    /** Show the month picker bottom sheet */
    private void showMonthFilterSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(
                        this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_onboarding_rounded_top);
        int p = dpToPx(24);
        root.setPadding(p, p, p, dpToPx(8));

        // Handle
        View handle = new View(this);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(4));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dpToPx(16);
        handle.setLayoutParams(handleParams);
        handle.setBackgroundColor(0x44FFFFFF);

        TextView title = new TextView(this);
        title.setText("Filter by Month");
        title.setTextSize(22);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dpToPx(20);
        title.setLayoutParams(titleParams);

        // Month grid: 3 columns
        android.widget.GridLayout monthGrid = new android.widget.GridLayout(this);
        monthGrid.setColumnCount(3);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gridParams.bottomMargin = dpToPx(20);
        monthGrid.setLayoutParams(gridParams);

        final int[] selectedMonthRef = {filterMonth}; // temp selection before Save
        final TextView[] chipRefs = new TextView[12];

        for (int i = 0; i < 12; i++) {
            final int monthIndex = i;
            TextView chip = new TextView(this);
            chip.setText(MONTH_SHORT[i]);
            chip.setTextSize(15);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

            android.widget.GridLayout.LayoutParams gp = new android.widget.GridLayout.LayoutParams();
            gp.width = 0;
            gp.columnSpec = android.widget.GridLayout.spec(i % 3, android.widget.GridLayout.FILL, 1f);
            gp.setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
            chip.setLayoutParams(gp);

            chipRefs[i] = chip;

            // Set initial appearance
            if (i == filterMonth) {
                chip.setBackgroundResource(R.drawable.bg_pill_selected);
                chip.setTextColor(0xFF121212);
                chip.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                chip.setBackgroundResource(R.drawable.bg_pill_minimal);
                chip.setTextColor(0xAAFFFFFF);
                chip.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            chip.setOnClickListener(v -> {
                selectedMonthRef[0] = monthIndex;
                // Update all chip appearances
                for (int j = 0; j < 12; j++) {
                    final int idx = j;
                    if (j == monthIndex) {
                        chipRefs[j].setBackgroundResource(R.drawable.bg_pill_selected);
                        chipRefs[j].setTextColor(0xFF121212);
                        chipRefs[j].setTypeface(null, android.graphics.Typeface.BOLD);
                        chipRefs[j].animate().scaleX(1.05f).scaleY(1.05f).setDuration(80)
                                .withEndAction(() -> chipRefs[idx].animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                                .start();
                    } else {
                        chipRefs[j].setBackgroundResource(R.drawable.bg_pill_minimal);
                        chipRefs[j].setTextColor(0xAAFFFFFF);
                        chipRefs[j].setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
            });

            monthGrid.addView(chip);
        }

        com.google.android.material.button.MaterialButton btnSave =
                new com.google.android.material.button.MaterialButton(this);
        btnSave.setText("Apply");
        btnSave.setTextColor(0xFF121212);
        btnSave.setTextSize(16);
        LinearLayout.LayoutParams btnParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(56));
        btnParams.topMargin = dpToPx(8);
        btnParams.bottomMargin = dpToPx(24);
        btnSave.setLayoutParams(btnParams);
        btnSave.setCornerRadius(dpToPx(16));
        btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));

        btnSave.setOnClickListener(v -> {
            filterMonth = selectedMonthRef[0];
            tvMonthFilter.setText(MONTH_SHORT[filterMonth]);

            // Immediately re-filter with new month
            if (!isSearching && !allRecentTransactions.isEmpty()) {
                List<Transaction> monthFiltered = filterBySelectedMonth(allRecentTransactions);
                if (monthFiltered.isEmpty()) {
                    tvEmptyTransactions.setVisibility(View.VISIBLE);
                    tvEmptyTransactions.setTextColor(android.graphics.Color.WHITE);
                    tvEmptyTransactions.setText("No transactions in " + MONTH_NAMES[filterMonth] + " yet");
                    rvTransactions.setVisibility(View.GONE);
                    if (flChartContainer != null) flChartContainer.setVisibility(View.GONE);
                } else {
                    tvEmptyTransactions.setVisibility(View.GONE);
                    rvTransactions.setVisibility(View.VISIBLE);
                    if (flChartContainer != null) flChartContainer.setVisibility(View.VISIBLE);
                    adapter.setTransactions(monthFiltered);
                    updateChart(monthFiltered);
                }
            } else if (allRecentTransactions.isEmpty()) {
                 tvEmptyTransactions.setVisibility(View.VISIBLE);
                 tvEmptyTransactions.setTextColor(android.graphics.Color.WHITE);
                 tvEmptyTransactions.setText("No transactions in " + MONTH_NAMES[filterMonth] + " yet");
                 if (flChartContainer != null) flChartContainer.setVisibility(View.GONE);
            }
            dialog.dismiss();
        });

        root.addView(handle);
        root.addView(title);
        root.addView(monthGrid);
        root.addView(btnSave);
        scrollView.addView(root);

        dialog.setContentView(scrollView);
        View sheetParent = (View) scrollView.getParent();
        if (sheetParent != null) sheetParent.setBackgroundResource(android.R.color.transparent);
        dialog.show();
    }

    // ───── Dot Indicator ─────

    private void buildDotIndicator(int count) {
        llDotIndicator.removeAllViews();

        if (count <= 1) {
            // No dots needed for a single wallet
            llDotIndicator.setVisibility(View.GONE);
            return;
        }

        llDotIndicator.setVisibility(View.VISIBLE);

        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(this);
            int size = dpToPx(8);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);

            if (i == vpWalletCards.getCurrentItem()) {
                dot.setImageResource(R.drawable.dot_active);
            } else {
                dot.setImageResource(R.drawable.dot_inactive);
            }

            llDotIndicator.addView(dot);
        }
    }

    private void updateDotIndicator(int selectedPosition) {
        int childCount = llDotIndicator.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView dot = (ImageView) llDotIndicator.getChildAt(i);
            if (i == selectedPosition) {
                dot.setImageResource(R.drawable.dot_active);
                // Subtle scale pop animation on selected dot
                dot.animate().scaleX(1.3f).scaleY(1.3f).setDuration(200).start();
            } else {
                dot.setImageResource(R.drawable.dot_inactive);
                dot.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ───── Chart ─────

    private void updateChart(List<Transaction> transactions) {
        if (transactions == null) return;

        // Always clear old data first so the chart fully redraws
        chartExpense.clear();

        List<Entry> entries = new ArrayList<>();
        List<Transaction> expenses = new ArrayList<>();
        long cutoffTime = System.currentTimeMillis() - currentGraphFilterMillis;

        for (Transaction t : transactions) {
            if (Constants.TYPE_EXPENSE.equals(t.getType()) && t.getDate() >= cutoffTime) {
                expenses.add(t);
            }
        }

        Collections.reverse(expenses); // Reverse to get chronological order from recent

        int x = 0;
        for (Transaction t : expenses) {
            Entry entry = new Entry(x++, (float) t.getAmount());
            entry.setData(t);
            entries.add(entry);
        }

        if (entries.isEmpty()) {
            chartExpense.setData(null);
            chartExpense.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(ContextCompat.getColor(this, R.color.color_primary));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER); // Curvy line graph
        dataSet.setDrawFilled(true);
        dataSet.setDrawHighlightIndicators(false); // Remove yellow crosshair lines

        // Gradient fill
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                        ContextCompat.getColor(this, R.color.color_primary_container),
                        Color.TRANSPARENT
                });
        dataSet.setFillDrawable(gradientDrawable);

        LineData lineData = new LineData(dataSet);
        chartExpense.setData(lineData);

        // Chart formatting
        chartExpense.getDescription().setEnabled(false);
        chartExpense.getLegend().setEnabled(false);
        chartExpense.setScaleEnabled(false);
        chartExpense.setTouchEnabled(true);
        chartExpense.setDragEnabled(false);
        chartExpense.setHighlightPerDragEnabled(true);
        chartExpense.setHighlightPerTapEnabled(true);

        CustomMarkerView mv = new CustomMarkerView(this, R.layout.custom_marker_view);
        mv.setChartView(chartExpense);
        chartExpense.setMarker(mv);

        chartExpense.setOnChartGestureListener(new com.github.mikephil.charting.listener.OnChartGestureListener() {
            @Override
            public void onChartGestureStart(android.view.MotionEvent me, com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture lastPerformedGesture) {}
            
            @Override
            public void onChartGestureEnd(android.view.MotionEvent me, com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture lastPerformedGesture) {
                chartExpense.highlightValue(null);
            }
            
            @Override
            public void onChartLongPressed(android.view.MotionEvent me) {}
            @Override
            public void onChartDoubleTapped(android.view.MotionEvent me) {}
            @Override
            public void onChartSingleTapped(android.view.MotionEvent me) {}
            @Override
            public void onChartFling(android.view.MotionEvent me1, android.view.MotionEvent me2, float velocityX, float velocityY) {}
            @Override
            public void onChartScale(android.view.MotionEvent me, float scaleX, float scaleY) {}
            @Override
            public void onChartTranslate(android.view.MotionEvent me, float dX, float dY) {}
        });

        // Add tactile scrubbing
        chartExpense.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                chartExpense.performHapticFeedback(android.view.HapticFeedbackConstants.TEXT_HANDLE_MOVE);
            }

            @Override
            public void onNothingSelected() {}
        });

        XAxis xAxis = chartExpense.getXAxis();
        xAxis.setEnabled(false);

        YAxis leftAxis = chartExpense.getAxisLeft();
        leftAxis.setEnabled(false);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = chartExpense.getAxisRight();
        rightAxis.setEnabled(false);

        chartExpense.notifyDataSetChanged();
        chartExpense.invalidate();
        // Give graph a premium sweeping draw animation
        chartExpense.animateY(1000, com.github.mikephil.charting.animation.Easing.EaseInOutSine);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update greeting in case it was changed
        if (tvGreeting != null) {
            String name = App.getInstance().getUserName();
            tvGreeting.setText(String.format(getString(R.string.greeting_format), name));
        }

        // Reset bottom nav selection if returning from wallets
        bottomNav.setSelectedItemId(R.id.nav_home);

        // Refresh wallet cards if settings changed or reordered
        if (viewModel != null && viewModel.getAllWallets().getValue() != null && walletCardAdapter != null) {
            List<Wallet> currentList = viewModel.getAllWallets().getValue();
            if (!currentList.isEmpty()) {
                walletCardAdapter.setWallets(com.app.pebble.utils.WalletSortUtils.getSortedWallets(this, currentList));
            }
        }
    }
}
