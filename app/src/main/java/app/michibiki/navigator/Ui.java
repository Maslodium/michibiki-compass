package app.michibiki.navigator;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.TextView;

final class Ui {
    static final int BLACK = Color.rgb(8, 8, 8);
    static final int PANEL = Color.rgb(24, 24, 24);
    static final int MUTED = Color.rgb(160, 160, 160);
    static final int WHITE = Color.WHITE;

    private Ui() {}

    static void prepare(Activity activity, View root, int horizontal, int vertical) {
        activity.getWindow().setStatusBarColor(BLACK);
        activity.getWindow().setNavigationBarColor(BLACK);
        root.setBackgroundColor(BLACK);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left + dp(view.getContext(), horizontal),
                    bars.top + dp(view.getContext(), vertical),
                    bars.right + dp(view.getContext(), horizontal),
                    bars.bottom + dp(view.getContext(), vertical));
            return insets;
        });
        root.requestApplyInsets();
    }

    static TextView text(Context context, String value, int sp, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(WHITE);
        view.setTextSize(sp);
        view.setFontFeatureSettings("tnum");
        if (bold) view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return view;
    }

    static TextView button(Context context, String label, boolean primary, View.OnClickListener listener) {
        TextView button = text(context, label, 13, true);
        button.setGravity(Gravity.CENTER);
        button.setLetterSpacing(0.08f);
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(listener);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 16));
        shape.setColor(primary ? WHITE : PANEL);
        shape.setStroke(dp(context, 1), primary ? WHITE : Color.rgb(70, 70, 70));
        button.setTextColor(primary ? BLACK : WHITE);
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(primary ? Color.LTGRAY : Color.DKGRAY), shape, null));
        return button;
    }

    static EditText input(Context context, String hint) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setTextColor(WHITE);
        input.setHintTextColor(MUTED);
        input.setTextSize(15);
        input.setSingleLine(true);
        input.setPadding(dp(context, 16), 0, dp(context, 16), 0);
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(context, 14));
        background.setColor(PANEL);
        background.setStroke(dp(context, 1), Color.rgb(70, 70, 70));
        input.setBackground(background);
        return input;
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
