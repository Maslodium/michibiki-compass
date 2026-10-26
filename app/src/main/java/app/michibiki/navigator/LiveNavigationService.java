package app.michibiki.navigator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.SystemClock;

public final class LiveNavigationService extends Service implements LocationProvider.Listener {
    private static final String CHANNEL_ID = "live_navigation";
    private static final int NOTIFICATION_ID = 23;
    private SystemLocationProvider locationProvider;
    private long lastWidgetUpdateMs;

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
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!NavStore.hasTarget(this) || !NavWidgetProvider.hasWidgets(this)
                || !NavStore.option(this, "live_widget", true)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        locationProvider.stop();
        locationProvider.start(this);
        return START_STICKY;
    }

    @Override public void onLocation(Location location) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastWidgetUpdateMs < 2000L) return;
        lastWidgetUpdateMs = now;
        NavStore.setLocation(this, location);
    }

    @Override public void onProviderError(String message) {
        NavWidgetProvider.updateAll(this);
    }

    @Override public void onDestroy() {
        if (locationProvider != null) locationProvider.stop();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
