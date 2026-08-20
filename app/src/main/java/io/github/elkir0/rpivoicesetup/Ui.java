package io.github.elkir0.rpivoicesetup;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int NAVY = Color.rgb(16, 24, 39);
    static final int SURFACE = Color.rgb(24, 34, 53);
    static final int BLUE = Color.rgb(90, 167, 255);
    static final int CYAN = Color.rgb(87, 227, 210);
    static final int GREEN = Color.rgb(69, 212, 131);
    static final int AMBER = Color.rgb(255, 202, 97);
    static final int RED = Color.rgb(255, 107, 120);
    static final int WHITE = Color.rgb(247, 250, 255);
    static final int MUTED = Color.rgb(170, 184, 204);

    private Ui() {}

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable rounded(int color, float radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, Math.round(radiusDp)));
        return drawable;
    }

    static TextView text(Context context, String value, float sp, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0f, 1.08f);
        return view;
    }

    static TextView title(Context context, String value) {
        TextView view = text(context, value, 24, WHITE);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    static TextView sectionTitle(Context context, String value) {
        TextView view = text(context, value, 18, WHITE);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    static Button button(Context context, String label, View.OnClickListener listener) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(15);
        button.setTextColor(NAVY);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(context, 50));
        button.setPadding(dp(context, 18), dp(context, 8), dp(context, 18), dp(context, 8));
        button.setBackground(rounded(CYAN, 12, context));
        button.setOnClickListener(listener);
        return button;
    }

    static Button secondaryButton(Context context, String label, View.OnClickListener listener) {
        Button button = button(context, label, listener);
        button.setTextColor(WHITE);
        button.setBackground(rounded(Color.rgb(43, 59, 82), 12, context));
        return button;
    }

    static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14));
        card.setBackground(rounded(SURFACE, 16, context));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(context, 12));
        card.setLayoutParams(params);
        return card;
    }

    static TextView status(Context context, String label, int color) {
        TextView view = text(context, label, 13, color);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5));
        int background = Color.argb(38, Color.red(color), Color.green(color), Color.blue(color));
        view.setBackground(rounded(background, 20, context));
        return view;
    }

    static LinearLayout horizontal(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    static void marginTop(View view, int topDp) {
        ViewGroup.LayoutParams current = view.getLayoutParams();
        LinearLayout.LayoutParams params = current instanceof LinearLayout.LayoutParams
                ? (LinearLayout.LayoutParams) current
                : new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(view.getContext(), topDp);
        view.setLayoutParams(params);
    }
}

