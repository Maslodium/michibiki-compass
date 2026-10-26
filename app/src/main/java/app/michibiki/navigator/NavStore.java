package app.michibiki.navigator;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

public final class NavStore {
    private static final String PREFS = "navigation";

    private NavStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void setTarget(Context context, double latitude, double longitude, String name) {
        prefs(context).edit()
                .putBoolean("has_target", true)
                .putLong("target_lat", Double.doubleToRawLongBits(latitude))
                .putLong("target_lon", Double.doubleToRawLongBits(longitude))
                .putString("target_name", name == null || name.isBlank() ? "Selected point" : name)
                .apply();
        NavWidgetProvider.updateAll(context);
        NavWidgetProvider.startLiveUpdates(context);
    }

    public static boolean hasTarget(Context context) { return prefs(context).getBoolean("has_target", false); }
    public static double targetLat(Context context) { return Double.longBitsToDouble(prefs(context).getLong("target_lat", 0)); }
    public static double targetLon(Context context) { return Double.longBitsToDouble(prefs(context).getLong("target_lon", 0)); }
    public static String targetName(Context context) { return prefs(context).getString("target_name", "Selected point"); }

    public static void setLocation(Context context, Location location) {
        prefs(context).edit()
                .putBoolean("has_location", true)
                .putLong("location_lat", Double.doubleToRawLongBits(location.getLatitude()))
                .putLong("location_lon", Double.doubleToRawLongBits(location.getLongitude()))
                .putFloat("location_accuracy", location.hasAccuracy() ? location.getAccuracy() : -1f)
                .putLong("location_time", location.getTime())
                .apply();
        NavWidgetProvider.updateAll(context);
    }

    public static boolean hasLocation(Context context) { return prefs(context).getBoolean("has_location", false); }
    public static double locationLat(Context context) { return Double.longBitsToDouble(prefs(context).getLong("location_lat", 0)); }
    public static double locationLon(Context context) { return Double.longBitsToDouble(prefs(context).getLong("location_lon", 0)); }
    public static float locationAccuracy(Context context) { return prefs(context).getFloat("location_accuracy", -1f); }
    public static long locationTime(Context context) { return prefs(context).getLong("location_time", 0L); }

    public static void setHeading(Context context, float heading) {
        prefs(context).edit()
                .putBoolean("has_heading", true)
                .putFloat("heading", heading)
                .apply();
    }

    public static boolean hasHeading(Context context) { return prefs(context).getBoolean("has_heading", false); }
    public static float heading(Context context) { return prefs(context).getFloat("heading", 0f); }

    public static boolean option(Context context, String key, boolean fallback) {
        return prefs(context).getBoolean(key, fallback);
    }

    public static void setOption(Context context, String key, boolean value) {
        prefs(context).edit().putBoolean(key, value).apply();
    }

    public static int intOption(Context context, String key, int fallback) {
        return prefs(context).getInt(key, fallback);
    }

    public static void setIntOption(Context context, String key, int value) {
        prefs(context).edit().putInt(key, value).apply();
    }
}
