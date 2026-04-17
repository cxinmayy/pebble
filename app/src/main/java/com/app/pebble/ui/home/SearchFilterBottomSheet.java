package com.app.pebble.ui.home;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.app.pebble.R;
import com.app.pebble.data.model.Category;
import com.app.pebble.data.model.Wallet;
import com.app.pebble.viewmodel.ExpenseViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SearchFilterBottomSheet extends BottomSheetDialogFragment {

    public interface OnFiltersAppliedListener {
        void onFiltersApplied(Long startDate, Long endDate, String typeFilter, Integer walletId, Integer categoryId);
    }
    
    private OnFiltersAppliedListener listener;
    
    // State
    private Long currentStartDate = null;
    private Long currentEndDate = null;
    private String currentTypeFilter = "ALL"; // ALL, INCOME, EXPENSE
    private Integer currentWalletId = null;
    private Integer currentCategoryId = null;

    // UI
    private TextView btnFromDate;
    private TextView btnToDate;
    private RadioGroup rgType;
    private ChipGroup cgAccounts;
    private ChipGroup cgCategories;
    
    private ExpenseViewModel viewModel;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    
    public SearchFilterBottomSheet(Long startDate, Long endDate, String initialType, Integer walletId, Integer categoryId, OnFiltersAppliedListener listener) {
        this.currentStartDate = startDate;
        this.currentEndDate = endDate;
        this.currentTypeFilter = initialType;
        this.currentWalletId = walletId;
        this.currentCategoryId = categoryId;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_search_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);
        
        btnFromDate = view.findViewById(R.id.btn_from_date);
        btnToDate = view.findViewById(R.id.btn_to_date);
        rgType = view.findViewById(R.id.rg_tx_type);
        cgAccounts = view.findViewById(R.id.cg_accounts);
        cgCategories = view.findViewById(R.id.cg_categories);
        
        // Setup initial UI states
        updateDateTexts();
        
        switch(currentTypeFilter) {
            case "INCOME": rgType.check(R.id.rb_type_income); break;
            case "EXPENSE": rgType.check(R.id.rb_type_expense); break;
            default: rgType.check(R.id.rb_type_all); break;
        }
        
        // Listeners for Dates
        btnFromDate.setOnClickListener(v -> showDatePicker(true));
        btnToDate.setOnClickListener(v -> showDatePicker(false));
        
        view.findViewById(R.id.btn_clear_filters).setOnClickListener(v -> {
            currentStartDate = null;
            currentEndDate = null;
            currentTypeFilter = "ALL";
            currentWalletId = null;
            currentCategoryId = null;
            
            updateDateTexts();
            rgType.check(R.id.rb_type_all);
            
            if (cgAccounts.getChildCount() > 0) ((Chip)cgAccounts.getChildAt(0)).setChecked(true);
            if (cgCategories.getChildCount() > 0) ((Chip)cgCategories.getChildAt(0)).setChecked(true);
        });
        
        view.findViewById(R.id.btn_apply_filters).setOnClickListener(v -> {
            String newType = "ALL";
            int checkedType = rgType.getCheckedRadioButtonId();
            if (checkedType == R.id.rb_type_income) newType = "INCOME";
            else if (checkedType == R.id.rb_type_expense) newType = "EXPENSE";
            
            Integer newWalletId = null;
            int selWallet = cgAccounts.getCheckedChipId();
            if (selWallet != View.NO_ID && selWallet != 0) {
                newWalletId = selWallet;
            }
            
            Integer newCategoryId = null;
            int selCat = cgCategories.getCheckedChipId();
            if (selCat != View.NO_ID && selCat != 0) {
                newCategoryId = selCat;
            }
            
            if (listener != null) {
                listener.onFiltersApplied(currentStartDate, currentEndDate, newType, newWalletId, newCategoryId);
            }
            dismiss();
        });
        
        observeData();
    }
    
    private void updateDateTexts() {
        if (currentStartDate == null) {
            btnFromDate.setText("From Date");
        } else {
            btnFromDate.setText(dateFormat.format(currentStartDate));
        }
        
        if (currentEndDate == null) {
            btnToDate.setText("To Date");
        } else {
            btnToDate.setText(dateFormat.format(currentEndDate));
        }
    }
    
    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        Long initial = isStart ? currentStartDate : currentEndDate;
        if (initial != null) {
            cal.setTimeInMillis(initial);
        }
        
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            if (isStart) {
                selected.set(Calendar.HOUR_OF_DAY, 0);
                selected.set(Calendar.MINUTE, 0);
                selected.set(Calendar.SECOND, 0);
                currentStartDate = selected.getTimeInMillis();
            } else {
                selected.set(Calendar.HOUR_OF_DAY, 23);
                selected.set(Calendar.MINUTE, 59);
                selected.set(Calendar.SECOND, 59);
                currentEndDate = selected.getTimeInMillis();
            }
            updateDateTexts();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }
    
    private void observeData() {
        viewModel.getAllWallets().observe(getViewLifecycleOwner(), wallets -> {
            cgAccounts.removeAllViews();
            Chip allAccounts = new Chip(requireContext());
            allAccounts.setId(0);
            allAccounts.setText("All Accounts");
            allAccounts.setCheckable(true);
            allAccounts.setChecked(currentWalletId == null);
            cgAccounts.addView(allAccounts);
            
            if (wallets != null) {
                for (Wallet w : wallets) {
                    Chip c = new Chip(requireContext());
                    c.setId(w.getId()); // Using DB ID as view ID since they are positive integers
                    c.setText(w.getName());
                    c.setCheckable(true);
                    if (currentWalletId != null && currentWalletId == w.getId()) {
                        c.setChecked(true);
                    }
                    cgAccounts.addView(c);
                }
            }
        });
        
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            cgCategories.removeAllViews();
            Chip allCategories = new Chip(requireContext());
            allCategories.setId(0);
            allCategories.setText("All Categories");
            allCategories.setCheckable(true);
            allCategories.setChecked(currentCategoryId == null);
            cgCategories.addView(allCategories);
            
            if (categories != null) {
                for (Category cat : categories) {
                    Chip c = new Chip(requireContext());
                    c.setId(cat.getId());
                    c.setText(cat.getName());
                    c.setCheckable(true);
                    if (currentCategoryId != null && currentCategoryId == cat.getId()) {
                        c.setChecked(true);
                    }
                    cgCategories.addView(c);
                }
            }
        });
    }
}
