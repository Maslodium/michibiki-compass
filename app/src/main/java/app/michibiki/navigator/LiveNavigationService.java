package app.michibiki.navigator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.IBinder;
import android.os.SystemClock;

public final class LiveNavigationService extends Service implements LocationProvider.Listener, SensorEventListener {
    private static final String CHANNEL_ID = "live_navigation";
    private static final int NOTIFICATION_ID = 23;
    private SystemLocationProvider locationProvider;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private final AngleFilter headingFilter = new AngleFilter(0.16, 32.0, 360.0);
    private long lastWidgetUpdateMs;
    private long lastHeadingUpdateMs;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, "Live navigation", NotificationManager.IMPORTANCE_LOW));

        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 23, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Michibiki")
                .setContentText("Updating the navigation widget")
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);
        locationProvider = new SystemLocationProvider(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!NavStore.hasTarget(this) || !NavWidgetProvider.hasWidgets(this)
                || !NavStore.option(this, "live_widget", true)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        locationProvider.stop();
        locationProvider.start(this);
        if (rotationSensor != null) {
            sensorManager.unregisterListener(this);
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        return START_STICKY;
    }

    @Override public void onLocation(Location location) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastWidgetUpdateMs < 2000L) return;
        lastWidgetUpdateMs = now;
        NavStore.setLocation(this, location);
        ObstacleRouteDetector.maybeRefresh(this);
    }

    @Override public void onProviderError(String message) {
        NavWidgetProvider.updateAll(this);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float[] matrix = new float[9];
        float[] orientation = new float[3];
        SensorManager.getRotationMatrixFromVector(matrix, event.values);
        SensorManager.getOrientation(matrix, orientation);
        float heading = (float) headingFilter.update(
                GeoMath.normalize(Math.toDegrees(orientation[0])), event.timestamp);
        long now = SystemClock.elapsedRealtime();
        if (now - lastHeadingUpdateMs < 250L) return;
        lastHeadingUpdateMs = now;
        NavStore.setHeading(this, heading);
        NavWidgetProvider.updateAll(this);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override public void onDestroy() {
        if (locationProvider != null) locationProvider.stop();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
