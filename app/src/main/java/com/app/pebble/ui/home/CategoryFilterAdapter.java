package com.app.pebble.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.app.pebble.R;
import com.app.pebble.data.model.Category;
import java.util.ArrayList;
import java.util.List;

public class CategoryFilterAdapter extends RecyclerView.Adapter<CategoryFilterAdapter.ViewHolder> {

    private List<Category> categories = new ArrayList<>();
    private Integer selectedCategoryId = null; // null means "All"
    private FilterClickListener listener;

    public interface FilterClickListener {
        void onFilterSelected(Integer categoryId);
    }

    public void setCategories(List<Category> categories, FilterClickListener listener) {
        this.categories = categories != null ? categories : new ArrayList<>();
        this.listener = listener;
        notifyDataSetChanged();
    }

    public Integer getSelectedCategoryId() {
        return selectedCategoryId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_filter_pill, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        com.app.pebble.utils.UIUtils.applySquishTouch(holder.itemView);
        
        Integer currentId;
        if (position == 0) {
            holder.tvTitle.setText("All");
            currentId = null;
        } else {
            Category cat = categories.get(position - 1);
            holder.tvTitle.setText(cat.getName());
            currentId = cat.getId();
        }

        boolean isSelected = (selectedCategoryId == null && currentId == null) || 
                             (selectedCategoryId != null && selectedCategoryId.equals(currentId));

        if (isSelected) {
            holder.tvTitle.setBackgroundResource(R.drawable.bg_pill_dark);
            holder.tvTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.color_surface));
        } else {
            holder.tvTitle.setBackgroundResource(R.drawable.bg_pill_minimal);
            holder.tvTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.color_on_surface_secondary));
        }

        holder.itemView.setOnClickListener(v -> {
            boolean clickedSame = (selectedCategoryId == null && currentId == null) || 
                                  (selectedCategoryId != null && selectedCategoryId.equals(currentId));
            if (!clickedSame) {
                selectedCategoryId = currentId;
                notifyDataSetChanged();
                if (listener != null) listener.onFilterSelected(selectedCategoryId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size() + 1; // +1 for "All"
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tv_category_filter);
        }
    }
}
