package app.michibiki.navigator;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MapPickerActivity extends Activity {
    private WebView webView;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!NavStore.option(this, "online_maps", true)) {
            Toast.makeText(this, "Online map tiles are disabled in privacy settings", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        webView = new WebView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        Ui.prepare(this, root, 0, 0);
        TextView header = Ui.text(this, "CHOOSE A POINT", 17, true);
        header.setLetterSpacing(0.10f);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(Ui.dp(this, 18), 0, Ui.dp(this, 18), 0);
        header.setBackgroundColor(Ui.BLACK);
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 62)));
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                if (NavStore.hasLocation(MapPickerActivity.this)) {
                    view.evaluateJavascript("setCenter(" + NavStore.locationLat(MapPickerActivity.this) + "," + NavStore.locationLon(MapPickerActivity.this) + ")", null);
                } else if (NavStore.hasTarget(MapPickerActivity.this)) {
                    view.evaluateJavascript("setCenter(" + NavStore.targetLat(MapPickerActivity.this) + "," + NavStore.targetLon(MapPickerActivity.this) + ")", null);
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new Bridge(), "Android");
        webView.loadUrl("file:///android_asset/map_picker.html");
    }

    private final class Bridge {
        @JavascriptInterface public void onPointSelected(double latitude, double longitude) {
            runOnUiThread(() -> {
                NavStore.setTarget(MapPickerActivity.this, latitude, longitude, "Map waypoint");
                Toast.makeText(MapPickerActivity.this, "Destination saved", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
