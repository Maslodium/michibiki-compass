package app.michibiki.navigator;

/** Circular low-pass filter with rejection of short-lived heading spikes. */
final class AngleFilter {
    private final double timeConstantSeconds;
    private final double baseJumpDegrees;
    private final double maxTurnDegreesPerSecond;
    private boolean initialized;
    private double value;
    private long lastNanos;
    private double pending;
    private int pendingSamples;

    AngleFilter(double timeConstantSeconds, double baseJumpDegrees, double maxTurnDegreesPerSecond) {
        this.timeConstantSeconds = timeConstantSeconds;
        this.baseJumpDegrees = baseJumpDegrees;
        this.maxTurnDegreesPerSecond = maxTurnDegreesPerSecond;
    }

    double update(double rawDegrees, long timestampNanos) {
        rawDegrees = GeoMath.normalize(rawDegrees);
        if (!initialized) {
            initialized = true;
            value = rawDegrees;
            lastNanos = timestampNanos;
            return value;
        }

        double dt = Math.max(0.001, Math.min(1.0, (timestampNanos - lastNanos) / 1_000_000_000.0));
        lastNanos = timestampNanos;
        double jump = Math.abs(shortestDelta(value, rawDegrees));
        double allowed = baseJumpDegrees + maxTurnDegreesPerSecond * dt;

        if (jump > allowed) {
            if (pendingSamples == 0 || Math.abs(shortestDelta(pending, rawDegrees)) > 12.0) {
                pending = rawDegrees;
                pendingSamples = 1;
            } else {
                pendingSamples++;
            }
            if (pendingSamples < 4) return value;
            rawDegrees = pending;
        }
        pendingSamples = 0;

        double alpha = 1.0 - Math.exp(-dt / timeConstantSeconds);
        value = GeoMath.normalize(value + shortestDelta(value, rawDegrees) * alpha);
        return value;
    }

    boolean hasValue() { return initialized; }
    double value() { return value; }

    static double shortestDelta(double from, double to) {
        return (to - from + 540.0) % 360.0 - 180.0;
    }
}
