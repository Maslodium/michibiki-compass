package app.michibiki.navigator;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoiceSearchActivity extends Activity {
    private static final int SPEECH = 70;
    private static final int AUDIO_PERMISSION = 71;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!NavStore.option(this, "voice_enabled", true)) {
            Toast.makeText(this, "Voice input is disabled in privacy settings", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
        } else startSpeech();
    }

    private void startSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say an address or place");
        try { startActivityForResult(intent, SPEECH); }
        catch (Exception e) {
            Toast.makeText(this, "No speech recognition service is available", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == AUDIO_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startSpeech();
        else finish();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SPEECH || resultCode != RESULT_OK || data == null) { finish(); return; }
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) { finish(); return; }
        resolve(results.get(0));
    }

    private void resolve(String query) {
        if (!NavStore.option(this, "online_geocoding", true)) {
            Toast.makeText(this, "Online address search is disabled. Select a point on the map.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Toast.makeText(this, "Searching: " + query, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                List<Address> addresses = new AndroidGeocoderProvider(this).search(query, 5);
                runOnUiThread(() -> showCandidates(query, addresses));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Address search failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void showCandidates(String query, List<Address> addresses) {
        if (addresses.isEmpty()) {
            Toast.makeText(this, "Nothing found for “" + query + "”", Toast.LENGTH_LONG).show();
            finish();
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
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }
}
