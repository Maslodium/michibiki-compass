package app.michibiki.navigator;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public final class MainActivity extends Activity implements SensorEventListener, LocationProvider.Listener {
    private static final int LOCATION_PERMISSION = 40;
    private CompassView compass;
    private TextView targetText;
    private TextView distanceText;
    private TextView statusText;
    private EditText latitudeInput;
    private EditText longitudeInput;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private SystemLocationProvider locationProvider;
    private final AngleFilter compassFilter = new AngleFilter(0.12, 32.0, 360.0);
    private final TargetDirectionEngine targetDirection = new TargetDirectionEngine();
    private float compassHeading;
    private boolean detourMode;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildContent());
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        locationProvider = new SystemLocationProvider(this);
        ensureLocationPermission();
    }

    private ViewGroup buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView title = Ui.text(this, "MICHIBIKI", 28, true);
        title.setLetterSpacing(0.14f);
        root.addView(title);
        TextView privacy = Ui.text(this, "DIRECT WAYPOINT NAVIGATION", 11, true);
        privacy.setLetterSpacing(0.12f);
        privacy.setTextColor(Ui.MUTED);
        root.addView(privacy);
        gap(root, 10);

        compass = new CompassView(this);
        compass.setOnLongClickListener(view -> {
            detourMode = !detourMode;
            NavStore.setOption(this, "detour_mode", detourMode);
            compass.setDetourMode(detourMode);
            NavWidgetProvider.updateAll(this);
            Toast.makeText(this, detourMode ? "Detour preview enabled" : "Detour preview disabled", Toast.LENGTH_SHORT).show();
            return true;
        });
        root.addView(compass, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(292)));

        distanceText = Ui.text(this, "—", 42, true);
        distanceText.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(distanceText);
        targetText = Ui.text(this, "No destination selected", 18, true);
        targetText.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(targetText);
        statusText = Ui.text(this, "Waiting for location", 13, false);
        statusText.setTextColor(Ui.MUTED);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(statusText);
        gap(root, 18);

        LinearLayout actions = horizontal();
        actions.addView(Ui.button(this, "VOICE", true, v -> startActivity(new Intent(this, VoiceSearchActivity.class))), weighted(1f));
        gapHorizontal(actions, 8);
        actions.addView(Ui.button(this, "MAP", false, v -> startActivity(new Intent(this, MapPickerActivity.class))), weighted(1f));
        gapHorizontal(actions, 8);
        actions.addView(Ui.button(this, "SETTINGS", false, v -> startActivity(new Intent(this, SettingsActivity.class))), weighted(1.25f));
        root.addView(actions);

        TextView manual = Ui.text(this, "COORDINATES", 12, true);
        manual.setLetterSpacing(0.12f);
        manual.setTextColor(Ui.MUTED);
        manual.setPadding(0, dp(22), 0, dp(9));
        root.addView(manual);
        LinearLayout coordinates = horizontal();
        latitudeInput = coordinateInput("Latitude");
        longitudeInput = coordinateInput("Longitude");
        coordinates.addView(latitudeInput, weighted(1f));
        gapHorizontal(coordinates, 8);
        coordinates.addView(longitudeInput, weighted(1f));
        root.addView(coordinates);
        gap(root, 8);
        root.addView(Ui.button(this, "SAVE DESTINATION", true, v -> saveManualTarget()), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        gap(root, 8);
        LinearLayout glyphActions = horizontal();
        glyphActions.addView(Ui.button(this, "GLYPH TOY", false, v -> openGlyphManager()), weighted(1f));
        gapHorizontal(glyphActions, 8);
        glyphActions.addView(Ui.button(this, "CURRENT POINT", false, v -> rememberCurrentPoint()), weighted(1f));
        root.addView(glyphActions);
        gap(root, 12);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        Ui.prepare(this, shell, 18, 14);
        shell.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return shell;
    }

    private EditText coordinateInput(String hint) {
        EditText input = Ui.input(this, hint);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        return input;
    }

    private LinearLayout horizontal() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout.LayoutParams weighted(float weight) {
        return new LinearLayout.LayoutParams(0, dp(52), weight);
    }

    private void gap(LinearLayout parent, int dp) {
        Space space = new Space(this);
        parent.addView(space, new LinearLayout.LayoutParams(1, Ui.dp(this, dp)));
    }

    private void gapHorizontal(LinearLayout parent, int dp) {
        Space space = new Space(this);
        parent.addView(space, new LinearLayout.LayoutParams(Ui.dp(this, dp), 1));
    }

    private int dp(int value) { return Ui.dp(this, value); }

    private void ensureLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, LOCATION_PERMISSION + 1);
        }
    }

    private void startLocation() { locationProvider.start(this); }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOCATION_PERMISSION) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                startLocation();
                NavWidgetProvider.startLiveUpdates(this);
                if (Build.VERSION.SDK_INT >= 33
                        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, LOCATION_PERMISSION + 1);
                }
            } else {
                statusText.setText("Location permission denied — manual destination still works");
            }
        }
    }

    @Override protected void onResume() {
        super.onResume();
        detourMode = NavStore.option(this, "detour_mode", false);
        compass.setDetourMode(detourMode);
        if (rotationSensor != null) sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_FASTEST);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationProvider.stop();
            startLocation();
        }
        refresh();
        NavWidgetProvider.startLiveUpdates(this);
    }

    @Override protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        locationProvider.stop();
    }

    @Override protected void onDestroy() {
        locationProvider.stop();
        super.onDestroy();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float[] matrix = new float[9];
        float[] orientation = new float[3];
        SensorManager.getRotationMatrixFromVector(matrix, event.values);
        SensorManager.getOrientation(matrix, orientation);
        double rawHeading = GeoMath.normalize(Math.toDegrees(orientation[0]));
        compassHeading = (float) compassFilter.update(rawHeading, event.timestamp);
        targetDirection.onMagneticHeading(rawHeading, event.timestamp);
        refresh();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override public void onLocation(Location location) {
        targetDirection.onLocation(location);
        NavStore.setLocation(this, location);
        refresh();
    }

    @Override public void onProviderError(String message) { statusText.setText(message); }

    private void refresh() {
        if (!NavStore.hasTarget(this)) {
            compass.setNavigation(0, compassHeading, false);
            targetText.setText("No destination selected");
            distanceText.setText("—");
            return;
        }
        targetText.setText(NavStore.targetName(this));
        if (!latitudeInput.hasFocus()) latitudeInput.setText(String.format(Locale.US, "%.6f", NavStore.targetLat(this)));
        if (!longitudeInput.hasFocus()) longitudeInput.setText(String.format(Locale.US, "%.6f", NavStore.targetLon(this)));
        if (!NavStore.hasLocation(this)) {
            compass.setNavigation(0, compassHeading, false);
            statusText.setText("Destination saved · waiting for location");
            return;
        }
        double bearing = GeoMath.bearingDegrees(NavStore.locationLat(this), NavStore.locationLon(this), NavStore.targetLat(this), NavStore.targetLon(this));
        double distance = GeoMath.distanceMeters(NavStore.locationLat(this), NavStore.locationLon(this), NavStore.targetLat(this), NavStore.targetLon(this));
        compass.setNavigation((float) targetDirection.relativeBearing(bearing), compassHeading, targetDirection.hasReference());
        distanceText.setText(GeoMath.formatDistance(distance));
        statusText.setText(String.format(Locale.getDefault(), "Bearing %.0f° · %s · accuracy %.0f m",
                bearing, targetDirection.sourceLabel(), NavStore.locationAccuracy(this)));
    }

    private void saveManualTarget() {
        try {
            double lat = Double.parseDouble(latitudeInput.getText().toString().trim());
            double lon = Double.parseDouble(longitudeInput.getText().toString().trim());
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new NumberFormatException();
            NavStore.setTarget(this, lat, lon, "Manual waypoint");
            refresh();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter valid latitude and longitude", Toast.LENGTH_SHORT).show();
        }
    }

    private void rememberCurrentPoint() {
        if (!NavStore.hasLocation(this)) {
            Toast.makeText(this, "Current location is not available", Toast.LENGTH_SHORT).show();
            return;
        }
        NavStore.setTarget(this, NavStore.locationLat(this), NavStore.locationLon(this), "Remembered place");
        refresh();
    }

    private void openGlyphManager() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"));
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
            Toast.makeText(this, "Open Glyph Interface → Glyph Toys and activate Michibiki", Toast.LENGTH_LONG).show();
        }
    }
}
