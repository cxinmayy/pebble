package com.app.pebble.ui.income;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.pebble.R;
import com.app.pebble.data.model.Category;
import com.app.pebble.data.model.CategoryTotal;
import com.app.pebble.utils.Constants;
import com.app.pebble.utils.NumberUtils;
import com.app.pebble.viewmodel.ExpenseViewModel;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncomeDetailActivity extends AppCompatActivity {

    private ExpenseViewModel viewModel;
    private PieChart pieChart;
    private RecyclerView rvCategories;
    private TextView tvEmpty;
    private CategoryBreakdownAdapter adapter;

    // Cache categories to map IDs to names
    private final Map<Integer, String> categoryMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_detail);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        pieChart = findViewById(R.id.pie_chart);
        rvCategories = findViewById(R.id.rv_categories);
        tvEmpty = findViewById(R.id.tv_empty);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        setupChart();
        setupRecyclerView();

        // Load categories first to show names
        viewModel.getIncomeCategories().observe(this, categories -> {
            for (Category c : categories) {
                categoryMap.put(c.getId(), c.getName());
            }
            observeIncome();
        });
    }

    private void setupChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setDrawEntryLabels(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setCenterTextSize(16f);
    }

    private void setupRecyclerView() {
        adapter = new CategoryBreakdownAdapter();
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(adapter);
    }

    private void observeIncome() {
        // Observe monthly total for center text
        viewModel.getCurrentMonthIncome().observe(this, total -> {
            double val = total == null ? 0 : total;
            pieChart.setCenterText("Total\n" + NumberUtils.formatCurrency(val));
        });

        // Observe category breakdown
        viewModel.getIncomeByCategoryThisMonth().observe(this, totals -> {
            if (totals == null || totals.isEmpty()) {
                pieChart.setVisibility(View.GONE);
                rvCategories.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                findViewById(R.id.tv_breakdown_header).setVisibility(View.GONE);
                return;
            }

            pieChart.setVisibility(View.VISIBLE);
            rvCategories.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            findViewById(R.id.tv_breakdown_header).setVisibility(View.VISIBLE);

            // Populate Chart
            List<PieEntry> entries = new ArrayList<>();
            for (CategoryTotal ct : totals) {
                String name = categoryMap.containsKey(ct.categoryId) ? categoryMap.get(ct.categoryId) : "Unknown";
                entries.add(new PieEntry((float) ct.total, name));
            }

            PieDataSet dataSet = new PieDataSet(entries, "Income Breakdown");
            // Setup colors from palette
            int[] colors = {
                    getColor(R.color.chart_1), getColor(R.color.chart_2),
                    getColor(R.color.chart_3), getColor(R.color.chart_4),
                    getColor(R.color.chart_5), getColor(R.color.chart_6),
                    getColor(R.color.chart_7), getColor(R.color.chart_8)
            };
            dataSet.setColors(colors);
            dataSet.setSliceSpace(2f);
            dataSet.setDrawValues(false);

            PieData data = new PieData(dataSet);
            pieChart.setData(data);
            pieChart.invalidate();

            // Populate List
            adapter.setData(totals);
        });
    }

    // ───── Inner Adapter ─────
    class CategoryBreakdownAdapter extends RecyclerView.Adapter<CategoryBreakdownAdapter.VH> {
        private List<CategoryTotal> items = new ArrayList<>();

        void setData(List<CategoryTotal> data) {
            this.items = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_breakdown, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CategoryTotal ct = items.get(position);
            String name = categoryMap.containsKey(ct.categoryId) ? categoryMap.get(ct.categoryId) : "Unknown";
            holder.tvName.setText(name);
            holder.tvTotal.setText(NumberUtils.formatCurrency(ct.total));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvTotal;
            VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_category_name);
                tvTotal = itemView.findViewById(R.id.tv_category_total);
            }
        }
    }
}
