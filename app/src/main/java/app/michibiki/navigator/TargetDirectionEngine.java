package app.michibiki.navigator;

import android.location.Location;
import android.os.SystemClock;

/**
 * Chooses and stabilizes the reference used by the destination arrow.
 * This is deliberately independent from the compass rose shown on screen.
 */
final class TargetDirectionEngine {
    enum Source { UNAVAILABLE, STABILIZED_COMPASS, GPS_COURSE }

    private final AngleFilter magnetic = new AngleFilter(0.16, 32.0, 360.0);
    private final AngleFilter course = new AngleFilter(0.55, 35.0, 180.0);
    private long gpsCourseValidUntil;

    void onMagneticHeading(double degrees, long timestampNanos) {
        magnetic.update(degrees, timestampNanos);
    }

    void onLocation(Location location) {
        boolean accurateEnough = !location.hasAccuracy() || location.getAccuracy() <= 35f;
        boolean bearingAccurate = !location.hasBearingAccuracy()
                || location.getBearingAccuracyDegrees() <= 35f;
        if (accurateEnough && bearingAccurate && location.hasSpeed()
                && location.getSpeed() >= 1.2f && location.hasBearing()) {
            course.update(location.getBearing(), SystemClock.elapsedRealtimeNanos());
            gpsCourseValidUntil = SystemClock.elapsedRealtime() + 4_000L;
        }
    }

    boolean hasReference() {
        return source() != Source.UNAVAILABLE;
    }

    double relativeBearing(double absoluteBearing) {
        Source source = source();
        if (source == Source.GPS_COURSE) return GeoMath.normalize(absoluteBearing - course.value());
        if (source == Source.STABILIZED_COMPASS) return GeoMath.normalize(absoluteBearing - magnetic.value());
        return absoluteBearing;
    }

    Source source() {
        if (course.hasValue() && SystemClock.elapsedRealtime() <= gpsCourseValidUntil) return Source.GPS_COURSE;
        if (magnetic.hasValue()) return Source.STABILIZED_COMPASS;
        return Source.UNAVAILABLE;
    }

    String sourceLabel() {
        return switch (source()) {
            case GPS_COURSE -> "GPS course";
            case STABILIZED_COMPASS -> "stabilized compass";
            default -> "direction unavailable";
        };
    }
}
