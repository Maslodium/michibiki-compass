package app.michibiki.navigator;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView title = Ui.text(this, "PRIVACY", 28, true);
        title.setText("PRIVACY & PROVIDERS");
        title.setLetterSpacing(0.10f);
        content.addView(title);
        TextView subtitle = Ui.text(this, "EVERY PROVIDER IS UNDER YOUR CONTROL", 11, true);
        subtitle.setLetterSpacing(0.08f);
        subtitle.setTextColor(Ui.MUTED);
        subtitle.setPadding(0, 0, 0, dp(18));
        content.addView(subtitle);

        addSwitch(content, "Use satellite GPS", "use_gps", true);
        addSwitch(content, "Use Wi-Fi / cellular network location\nMay be processed by the Android system location provider", "use_network", false);
        addSwitch(content, "Voice address input", "voice_enabled", true);
        addSwitch(content, "Online address geocoding", "online_geocoding", true);
        addSwitch(content, "Bias search to the current city\nThe system geocoder may process a reverse-geocoding request", "city_bias", true);
        addSwitch(content, "Online OpenStreetMap tiles", "online_maps", true);
        addSwitch(content, "Live widget location updates\nUses a foreground service with a persistent notification", "live_widget", true);

        TextView note = Ui.text(this, "", 13, false);
        note.setText("The prototype contains no analytics, advertising SDK, account system or custom tracking server. Exact coordinates are stored locally in app preferences.");
        note.setTextColor(Ui.MUTED);
        note.setPadding(0, dp(24), 0, 0);
        content.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        Ui.prepare(this, shell, 18, 14);
        shell.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(shell);
    }

    private void addSwitch(LinearLayout parent, String label, String key, boolean fallback) {
        Switch toggle = new Switch(this);
        toggle.setText(label);
        toggle.setTextColor(Color.WHITE);
        toggle.setTextSize(16);
        toggle.setPadding(dp(16), dp(14), dp(12), dp(14));
        GradientDrawable card = new GradientDrawable();
        card.setColor(Ui.PANEL);
        card.setCornerRadius(dp(16));
        card.setStroke(dp(1), Color.rgb(55, 55, 55));
        toggle.setBackground(card);
        toggle.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.WHITE, Color.rgb(125, 125, 125)}));
        toggle.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.rgb(105, 105, 105), Color.rgb(48, 48, 48)}));
        toggle.setChecked(NavStore.option(this, key, fallback));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            NavStore.setOption(this, key, checked);
            if ("live_widget".equals(key)) {
                if (checked) NavWidgetProvider.startLiveUpdates(this);
                else stopService(new android.content.Intent(this, LiveNavigationService.class));
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        parent.addView(toggle, params);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
