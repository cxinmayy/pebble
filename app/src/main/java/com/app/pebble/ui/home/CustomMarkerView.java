package com.app.pebble.ui.home;

import android.content.Context;
import android.widget.TextView;

import com.app.pebble.R;
import com.app.pebble.data.model.Transaction;
import com.app.pebble.utils.NumberUtils;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomMarkerView extends MarkerView {

    private final TextView tvContent;
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e.getData() instanceof Transaction) {
            Transaction t = (Transaction) e.getData();
            String day = dayFormat.format(new Date(t.getDate()));
            tvContent.setText(day + "\n" + NumberUtils.formatCurrency(t.getAmount()));
        } else {
            tvContent.setText(NumberUtils.formatCurrency(e.getY()));
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 40); // Offset upwards
    }
}
