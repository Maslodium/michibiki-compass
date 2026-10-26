package app.michibiki.navigator;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import com.nothing.ketchum.Common;
import com.nothing.ketchum.Glyph;
import com.nothing.ketchum.GlyphMatrixManager;
import com.nothing.ketchum.GlyphToy;

public final class NavigationGlyphToyService extends Service implements SensorEventListener, LocationProvider.Listener {
    private final Handler renderHandler = new Handler(Looper.getMainLooper());
    private GlyphMatrixManager matrixManager;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private SystemLocationProvider locationProvider;
    private final TargetDirectionEngine targetDirection = new TargetDirectionEngine();
    private final AngleFilter northCompass = new AngleFilter(0.30, 25.0, 220.0);
    private boolean obstaclePreview;
    private boolean connected;
    private int matrixSize = 25;
    private int center = 12;

    private final Runnable renderLoop = new Runnable() {
        @Override public void run() {
            renderFrame();
            renderHandler.postDelayed(this, 100L);
        }
    };

    private final Handler eventHandler = new Handler(Looper.getMainLooper()) {
        @Override public void handleMessage(Message message) {
            if (message.what == GlyphToy.MSG_GLYPH_TOY) {
                Bundle data = message.getData();
                String event = data.getString(GlyphToy.MSG_GLYPH_TOY_DATA);
                if (GlyphToy.EVENT_CHANGE.equals(event)) {
                    obstaclePreview = !obstaclePreview;
                    renderFrame();
                } else if (GlyphToy.EVENT_AOD.equals(event)) {
                    renderFrame();
                }
            } else super.handleMessage(message);
        }
    };
    private final Messenger messenger = new Messenger(eventHandler);

    @Override public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        locationProvider = new SystemLocationProvider(this);
    }

    @Override public IBinder onBind(Intent intent) {
        matrixManager = GlyphMatrixManager.getInstance(getApplicationContext());
        matrixManager.init(new GlyphMatrixManager.Callback() {
            @Override public void onServiceConnected(ComponentName componentName) {
                String device;
                if (Common.is25111p()) device = Glyph.DEVICE_25111p;
                else if (Common.is23112()) device = Glyph.DEVICE_23112;
                else return;
                int detectedSize = Common.getDeviceMatrixLength();
                matrixSize = detectedSize > 0 ? detectedSize : (Common.is25111p() ? 13 : 25);
                center = matrixSize / 2;
                connected = matrixManager.register(device);
                if (!connected) return;
                startInputs();
                renderHandler.post(renderLoop);
            }

            @Override public void onServiceDisconnected(ComponentName componentName) {
                connected = false;
                stopInputs();
            }
        });
        return messenger.getBinder();
    }

    private void startInputs() {
        if (rotationSensor != null) sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationProvider.start(this);
        }
    }

    private void stopInputs() {
        renderHandler.removeCallbacks(renderLoop);
        sensorManager.unregisterListener(this);
        locationProvider.stop();
    }

    @Override public boolean onUnbind(Intent intent) {
        stopInputs();
        connected = false;
        if (matrixManager != null) {
            matrixManager.turnOff();
            matrixManager.unInit();
            matrixManager = null;
        }
        return false;
    }

    @Override public void onDestroy() {
        stopInputs();
        super.onDestroy();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float[] matrix = new float[9];
        float[] orientation = new float[3];
        SensorManager.getRotationMatrixFromVector(matrix, event.values);
        SensorManager.getOrientation(matrix, orientation);
        double rawHeading = GeoMath.normalize(Math.toDegrees(orientation[0]));
        northCompass.update(rawHeading, event.timestamp);
        targetDirection.onMagneticHeading(rawHeading, event.timestamp);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override public void onLocation(Location location) {
        targetDirection.onLocation(location);
        NavStore.setLocation(this, location);
    }
    @Override public void onProviderError(String message) {}

    private void renderFrame() {
        if (!connected || matrixManager == null) return;
        int[] frame = new int[matrixSize * matrixSize];
        if (!NavStore.hasTarget(this) || !NavStore.hasLocation(this) || !targetDirection.hasReference()) {
            drawX(frame, 255);
        } else {
            double bearing = GeoMath.bearingDegrees(NavStore.locationLat(this), NavStore.locationLon(this), NavStore.targetLat(this), NavStore.targetLon(this));
            double relative = targetDirection.relativeBearing(bearing);
            // Until obstacle geometry is supplied by a routing engine, use a
            // boundary perpendicular to the direct route. The renderer keeps
            // target and wall angles separate so real map geometry can replace it.
            if (obstaclePreview) drawObstacle(frame, relative, relative);
            else drawArrow(frame, relative, 255);
            if (northCompass.hasValue()) drawNorthStar(frame, -northCompass.value());
        }
        try { matrixManager.setMatrixFrame(frame); } catch (Exception ignored) {}
    }

    private void drawArrow(int[] frame, double degrees, int brightness) {
        double radians = Math.toRadians(degrees);
        // Local arrow geometry is balanced around (0,0), so (12,12) is the
        // actual visual and mathematical pivot at every angle.
        int[] tip = rotatePoint(0, -9, radians);
        int[] neckLeft = rotatePoint(-5, -3, radians);
        int[] neckRight = rotatePoint(5, -3, radians);
        int[] shaftTop = rotatePoint(0, -2, radians);
        int[] shaftBottom = rotatePoint(0, 6, radians);
        drawThickLine(frame, center + tip[0], center + tip[1], center + neckLeft[0], center + neckLeft[1], brightness);
        drawThickLine(frame, center + tip[0], center + tip[1], center + neckRight[0], center + neckRight[1], brightness);
        drawThickLine(frame, center + neckLeft[0], center + neckLeft[1], center + neckRight[0], center + neckRight[1], brightness);
        drawThickLine(frame, center + shaftTop[0], center + shaftTop[1], center + shaftBottom[0], center + shaftBottom[1], brightness);
        setPixel(frame, center, center, brightness);
    }

    private int[] rotatePoint(int x, int y, double radians) {
        double scale = center / 12.0;
        double sx = x * scale;
        double sy = y * scale;
        return new int[]{
                symmetricRound(sx * Math.cos(radians) - sy * Math.sin(radians)),
                symmetricRound(sx * Math.sin(radians) + sy * Math.cos(radians))
        };
    }

    private static int symmetricRound(double value) {
        return (int) Math.copySign(Math.floor(Math.abs(value) + 0.5), value);
    }

    private void drawObstacle(int[] frame, double targetDegrees, double wallNormalDegrees) {
        double targetRadians = Math.toRadians(targetDegrees);
        double wallRadians = Math.toRadians(wallNormalDegrees);

        // Keep the obstacle on the outer edge so it cannot cover the fork.
        // wallNormalDegrees controls its orientation independently.
        drawRotatedLine(frame, -6, -8, 6, -8, wallRadians, 255);
        drawRotatedLine(frame, -5, -7, 5, -7, wallRadians, 190);

        // Two compact, spatially separated detours. Unlike the old preview,
        // these are paths with small arrowheads, not three overlapping arrows.
        drawRotatedLine(frame, -1, 6, -5, 2, targetRadians, 255);
        drawRotatedLine(frame, -5, 2, -9, -2, targetRadians, 255);
        drawRotatedLine(frame, -9, -2, -6, -2, targetRadians, 255);
        drawRotatedLine(frame, -9, -2, -8, 1, targetRadians, 255);

        drawRotatedLine(frame, 1, 6, 5, 2, targetRadians, 255);
        drawRotatedLine(frame, 5, 2, 9, -2, targetRadians, 255);
        drawRotatedLine(frame, 9, -2, 6, -2, targetRadians, 255);
        drawRotatedLine(frame, 9, -2, 8, 1, targetRadians, 255);
    }

    private void drawRotatedLine(int[] frame, int x0, int y0, int x1, int y1,
                                 double radians, int brightness) {
        int[] start = rotatePoint(x0, y0, radians);
        int[] end = rotatePoint(x1, y1, radians);
        drawLine(frame, center + start[0], center + start[1],
                center + end[0], center + end[1], brightness);
    }

    private void drawNorthStar(int[] frame, double degrees) {
        double radians = Math.toRadians(degrees);
        int radius = Math.max(2, center - 1);
        int x = center + (int) Math.round(Math.sin(radians) * radius);
        int y = center - (int) Math.round(Math.cos(radians) * radius);
        // Four-ray star: a bright core plus one pixel in every cardinal direction.
        setPixel(frame, x, y, 255);
        setPixel(frame, x - 1, y, 210);
        setPixel(frame, x + 1, y, 210);
        setPixel(frame, x, y - 1, 210);
        setPixel(frame, x, y + 1, 210);
    }

    private void drawThickLine(int[] frame, int x0, int y0, int x1, int y1, int brightness) {
        drawLine(frame, x0, y0, x1, y1, brightness);
        if (matrixSize >= 20) {
            drawLine(frame, x0 - 1, y0, x1 - 1, y1, brightness);
            drawLine(frame, x0 + 1, y0, x1 + 1, y1, brightness);
        }
    }

    private void drawLine(int[] frame, int x0, int y0, int x1, int y1, int brightness) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            setPixel(frame, x0, y0, brightness);
            if (x0 == x1 && y0 == y1) break;
            int twice = 2 * error;
            if (twice >= dy) { error += dy; x0 += sx; }
            if (twice <= dx) { error += dx; y0 += sy; }
        }
    }

    private void setPixel(int[] frame, int x, int y, int brightness) {
        if (x >= 0 && x < matrixSize && y >= 0 && y < matrixSize) {
            frame[y * matrixSize + x] = Math.max(frame[y * matrixSize + x], brightness);
        }
    }

    private void drawX(int[] frame, int brightness) {
        int margin = Math.max(2, matrixSize / 5);
        int far = matrixSize - 1 - margin;
        drawLine(frame, margin, margin, far, far, brightness);
        drawLine(frame, far, margin, margin, far, brightness);
    }
}
