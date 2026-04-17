package com.app.pebble.utils;

import android.graphics.Color;

public class ColorUtils {

    /**
     * Deterministically generates or maps a high-quality aesthetic color from a category name.
     */
    public static int getCategoryColor(String categoryName) {
        if (categoryName == null) return Color.parseColor("#444444");
        
        String cleanName = categoryName.trim().toLowerCase();
        
        switch (cleanName) {
            case "food":
            case "dining":
            case "groceries": return Color.parseColor("#FFB74D"); // Soft Orange
            
            case "travel":
            case "transport":
            case "fuel": return Color.parseColor("#64B5F6"); // Soft Blue
            
            case "shopping":
            case "clothes": return Color.parseColor("#F06292"); // Pink
            
            case "utilities":
            case "bills":
            case "internet": return Color.parseColor("#4DB6AC"); // Teal
            
            case "entertainment":
            case "movies": return Color.parseColor("#BA68C8"); // Purple
            
            case "health":
            case "medical": return Color.parseColor("#E57373"); // Soft Red
            
            case "salary":
            case "income": return Color.parseColor("#81C784"); // Soft Green
            
            default:
                // Generate a deterministic pastely hash color
                int hash = cleanName.hashCode();
                int r = (hash & 0xFF0000) >> 16;
                int g = (hash & 0x00FF00) >> 8;
                int b = (hash & 0x0000FF);
                
                // Soften toward white/pastel for dark mode vibrancy
                r = (r + 255) / 2;
                g = (g + 255) / 2;
                b = (b + 255) / 2;
                
                return Color.rgb(r, g, b);
        }
    }

    /**
     * Adjusts the alpha transparency of a given color.
     */
    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}
