package app.michibiki.navigator;

import java.util.Locale;

public final class GeoMath {
    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    private GeoMath() {}

    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1);
        double dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dl = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return normalize(Math.toDegrees(Math.atan2(y, x)));
    }

    public static double normalize(double value) {
        value %= 360.0;
        return value < 0 ? value + 360.0 : value;
    }

    public static String formatDistance(double meters) {
        if (meters < 1000) return String.format(Locale.getDefault(), "%.0f m", meters);
        if (meters < 10_000) return String.format(Locale.getDefault(), "%.1f km", meters / 1000.0);
        return String.format(Locale.getDefault(), "%.0f km", meters / 1000.0);
    }
}
