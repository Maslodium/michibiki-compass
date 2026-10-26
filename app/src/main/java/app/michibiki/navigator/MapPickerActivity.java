package app.michibiki.navigator;

import android.app.Activity;
import android.app.AlertDialog;
import android.location.Address;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public final class MapPickerActivity extends Activity {
    private WebView webView;
    private EditText searchInput;
    private TextView searchButton;

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

        LinearLayout search = new LinearLayout(this);
        search.setOrientation(LinearLayout.HORIZONTAL);
        search.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 10));
        searchInput = Ui.input(this, "Search address or place");
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        searchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchAddress();
                return true;
            }
            return false;
        });
        search.addView(searchInput, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f));
        android.widget.Space space = new android.widget.Space(this);
        search.addView(space, new LinearLayout.LayoutParams(Ui.dp(this, 8), 1));
        searchButton = Ui.button(this, "SEARCH", true, view -> searchAddress());
        search.addView(searchButton, new LinearLayout.LayoutParams(Ui.dp(this, 104), Ui.dp(this, 52)));
        root.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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

    private void searchAddress() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            searchInput.setError("Enter an address or place");
            return;
        }
        if (!NavStore.option(this, "online_geocoding", true)) {
            Toast.makeText(this, "Online address search is disabled in privacy settings", Toast.LENGTH_LONG).show();
            return;
        }
        searchButton.setEnabled(false);
        searchButton.setText("…");
        new Thread(() -> {
            try {
                List<Address> addresses = new AndroidGeocoderProvider(this).search(query, 5);
                runOnUiThread(() -> showCandidates(query, addresses));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    restoreSearchButton();
                    Toast.makeText(this, "Address search failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showCandidates(String query, List<Address> addresses) {
        restoreSearchButton();
        if (isFinishing() || isDestroyed()) return;
        if (addresses.isEmpty()) {
            Toast.makeText(this, "Nothing found for “" + query + "”", Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[addresses.size()];
        for (int i = 0; i < addresses.size(); i++) {
            Address address = addresses.get(i);
            String line = address.getMaxAddressLineIndex() >= 0 ? address.getAddressLine(0) : null;
            labels[i] = line == null ? address.getLatitude() + ", " + address.getLongitude() : line;
        }
        new AlertDialog.Builder(this)
                .setTitle("Choose destination")
                .setItems(labels, (dialog, which) -> {
                    Address selected = addresses.get(which);
                    NavStore.setTarget(this, selected.getLatitude(), selected.getLongitude(), labels[which]);
                    Toast.makeText(this, "Destination saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void restoreSearchButton() {
        searchButton.setEnabled(true);
        searchButton.setText("SEARCH");
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
