package com.app.pebble.utils;

import android.view.MotionEvent;
import android.view.View;

public class UIUtils {

    /**
     * Applies a premium iOS-style interactive 'squish' bounce to a view.
     * When touched down, it softly scales inwardly. 
     * When released or cancelled, it overshoots back to 1.0f.
     */
    public static void applySquishTouch(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.96f)
                            .scaleY(0.96f)
                            .setDuration(100)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                            .start();
                    break;
            }
            return false; // let normal click listeners process
        });
    }
}
