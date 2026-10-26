package app.michibiki.navigator;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** Compares the direct bearing with the first useful leg of a pedestrian OSM route. */
final class ObstacleRouteDetector {
    interface Listener { void onObstacleResult(); }

    private static final String ENDPOINT = "https://valhalla1.openstreetmap.de/route";
    private static final long MIN_INTERVAL_MS = 20_000L;
    private static final double MIN_MOVE_METERS = 10.0;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile boolean requestRunning;
    private static volatile long lastRequestAt;
    private static volatile double lastLatitude = Double.NaN;
    private static volatile double lastLongitude = Double.NaN;
    private static volatile Listener listener;

    private ObstacleRouteDetector() {}

    static void setListener(Listener value) { listener = value; }

    static void maybeRefresh(Context context) { refresh(context, false); }
    static void forceRefresh(Context context) { refresh(context, true); }

    private static synchronized void refresh(Context source, boolean force) {
        Context context = source.getApplicationContext();
        if (!NavStore.option(context, "automatic_obstacles", true)
                || !NavStore.hasLocation(context) || !NavStore.hasTarget(context)) {
            NavStore.setObstacleResult(context, false, 0);
            notifyResult(context);
            return;
        }
        long now = System.currentTimeMillis();
        double latitude = NavStore.locationLat(context);
        double longitude = NavStore.locationLon(context);
        double moved = Double.isNaN(lastLatitude) ? Double.MAX_VALUE
                : GeoMath.distanceMeters(lastLatitude, lastLongitude, latitude, longitude);
        if (requestRunning || (!force && now - lastRequestAt < MIN_INTERVAL_MS
                && moved < MIN_MOVE_METERS)) return;
        requestRunning = true;
        lastRequestAt = now;
        lastLatitude = latitude;
        lastLongitude = longitude;
        EXECUTOR.execute(() -> query(context, latitude, longitude,
                NavStore.targetLat(context), NavStore.targetLon(context)));
    }

    private static void query(Context context, double fromLat, double fromLon,
                              double toLat, double toLon) {
        HttpURLConnection connection = null;
        try {
            double directDistance = GeoMath.distanceMeters(fromLat, fromLon, toLat, toLon);
            if (directDistance < 35.0) {
                NavStore.setObstacleResult(context, false, 0);
                return;
            }
            JSONObject body = new JSONObject();
            JSONArray locations = new JSONArray();
            locations.put(new JSONObject().put("lat", fromLat).put("lon", fromLon));
            locations.put(new JSONObject().put("lat", toLat).put("lon", toLon));
            body.put("locations", locations);
            body.put("costing", "pedestrian");
            body.put("units", "kilometers");
            body.put("directions_type", "maneuvers");

            connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(7_000);
            connection.setReadTimeout(9_000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-Client-Id", "app.michibiki.navigator");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) return;

            JSONObject result = new JSONObject(readAll(connection.getInputStream()));
            JSONObject trip = result.getJSONObject("trip");
            double routeMeters = trip.getJSONObject("summary").getDouble("length") * 1000.0;
            JSONArray maneuvers = trip.getJSONArray("legs").getJSONObject(0).getJSONArray("maneuvers");
            double routeHeading = firstUsefulHeading(maneuvers);
            double directHeading = GeoMath.bearingDegrees(fromLat, fromLon, toLat, toLon);
            double delta = signedDelta(directHeading, routeHeading);
            double absoluteDelta = Math.abs(delta);
            double excessMeters = routeMeters - directDistance;
            double ratio = routeMeters / Math.max(1.0, directDistance);

            // ±80° is the requested 160° forward scan. A route that initially
            // leaves that sector is also treated as a strong obstruction.
            boolean insideForwardScan = absoluteDelta <= 80.0;
            boolean detour = insideForwardScan
                    ? ratio >= 1.12 && absoluteDelta >= 22.0 && excessMeters >= 25.0
                    : excessMeters >= 40.0;
            int direction = detour ? (delta < 0.0 ? -1 : 1) : 0;
            if (!NavStore.hasTarget(context)
                    || GeoMath.distanceMeters(toLat, toLon,
                    NavStore.targetLat(context), NavStore.targetLon(context)) > 2.0) return;
            NavStore.setObstacleResult(context, detour, direction);
        } catch (Exception ignored) {
            // Keep the last fresh result on transient network or rate-limit errors.
        } finally {
            if (connection != null) connection.disconnect();
            requestRunning = false;
            notifyResult(context);
        }
    }

    private static double firstUsefulHeading(JSONArray maneuvers) throws Exception {
        for (int i = 0; i < maneuvers.length(); i++) {
            JSONObject maneuver = maneuvers.getJSONObject(i);
            if (maneuver.optDouble("length", 0.0) >= 0.015
                    && maneuver.has("bearing_after")) {
                return maneuver.getDouble("bearing_after");
            }
        }
        return maneuvers.getJSONObject(0).optDouble("bearing_after", 0.0);
    }

    private static double signedDelta(double from, double to) {
        return (to - from + 540.0) % 360.0 - 180.0;
    }

    private static String readAll(InputStream input) throws Exception {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line);
        }
        return text.toString();
    }

    private static void notifyResult(Context context) {
        NavWidgetProvider.updateAll(context);
        Listener current = listener;
        if (current != null) MAIN.post(current::onObstacleResult);
    }
}
