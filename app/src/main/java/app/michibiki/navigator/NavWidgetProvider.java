package app.michibiki.navigator;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.RemoteViews;
import java.util.Locale;

public final class NavWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_REFRESH = "app.michibiki.navigator.WIDGET_REFRESH";

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) manager.updateAppWidget(id, build(context));
        startLiveUpdates(context);
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            updateAll(context);
            startLiveUpdates(context);
        }
    }

    @Override public void onDisabled(Context context) {
        context.stopService(new Intent(context, LiveNavigationService.class));
        super.onDisabled(context);
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, NavWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) manager.updateAppWidget(id, build(context));
    }

    static boolean hasWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, NavWidgetProvider.class);
        return manager.getAppWidgetIds(component).length > 0;
    }

    static void startLiveUpdates(Context context) {
        if (!hasWidgets(context) || !NavStore.hasTarget(context)
                || !NavStore.option(context, "live_widget", true)) return;
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try {
            context.startForegroundService(new Intent(context, LiveNavigationService.class));
        } catch (RuntimeException ignored) {
            // Android may reject a background start. A user tap or reopening the
            // app retries from an allowed context.
        }
    }

    private static RemoteViews build(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_nav);
        boolean detourMode = NavStore.option(context, "detour_mode", false);
        Intent main = new Intent(context, MainActivity.class);
        PendingIntent mainPending = PendingIntent.getActivity(context, 10, main, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, mainPending);

        Intent voice = new Intent(context, VoiceSearchActivity.class);
        PendingIntent voicePending = PendingIntent.getActivity(context, 11, voice, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_voice, voicePending);

        Intent refresh = new Intent(context, NavWidgetProvider.class).setAction(ACTION_REFRESH);
        PendingIntent refreshPending = PendingIntent.getBroadcast(context, 12, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending);

        if (!NavStore.hasTarget(context)) {
            views.setTextViewText(R.id.widget_name, "No destination");
            views.setTextViewText(R.id.widget_distance, "—");
            views.setTextViewText(R.id.widget_status, "Tap to configure");
            views.setImageViewBitmap(R.id.widget_arrow, compassBitmap(0, false, detourMode));
            return views;
        }
        views.setTextViewText(R.id.widget_name, NavStore.targetName(context));
        if (!NavStore.hasLocation(context)) {
            views.setTextViewText(R.id.widget_distance, "—");
            views.setTextViewText(R.id.widget_status, "Waiting for location");
            views.setImageViewBitmap(R.id.widget_arrow, compassBitmap(0, false, detourMode));
            return views;
        }
        double bearing = GeoMath.bearingDegrees(NavStore.locationLat(context), NavStore.locationLon(context), NavStore.targetLat(context), NavStore.targetLon(context));
        double distance = GeoMath.distanceMeters(NavStore.locationLat(context), NavStore.locationLon(context), NavStore.targetLat(context), NavStore.targetLon(context));
        long ageSeconds = Math.max(0, (System.currentTimeMillis() - NavStore.locationTime(context)) / 1000L);
        views.setTextViewText(R.id.widget_distance, GeoMath.formatDistance(distance));
        views.setTextViewText(R.id.widget_status, String.format(Locale.getDefault(), "%.0f° · ±%.0f m · %d s", bearing, NavStore.locationAccuracy(context), ageSeconds));
        views.setImageViewBitmap(R.id.widget_arrow, compassBitmap((float) bearing, true, detourMode));
        return views;
    }

    private static Bitmap compassBitmap(float degrees, boolean ready, boolean detourMode) {
        Bitmap bitmap = Bitmap.createBitmap(180, 180, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeMiter(8f);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(90, 90, 82, paint);

        android.graphics.RectF northArc = new android.graphics.RectF(20, 20, 160, 160);
        paint.setStrokeWidth(4f);
        canvas.drawArc(northArc, -116f, 52f, false, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(20f);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("N", 90, 45, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        canvas.drawCircle(90, 90, 17, paint);
        canvas.drawCircle(90, 90, 11, paint);
        if (!ready) return bitmap;

        canvas.rotate(degrees, 90, 90);
        Path target = new Path();
        target.moveTo(74, 61);
        target.lineTo(90, 22);
        target.lineTo(106, 61);
        target.moveTo(74, 61);
        target.lineTo(82, 58);
        target.moveTo(106, 61);
        target.lineTo(98, 58);
        paint.setColor(Color.rgb(255, 35, 48));
        paint.setStrokeWidth(7f);
        canvas.drawPath(target, paint);

        if (detourMode) {
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(6f);
            Path left = new Path();
            left.moveTo(55, 75);
            left.lineTo(35, 90);
            left.lineTo(55, 105);
            left.moveTo(55, 75);
            left.lineTo(52, 82);
            left.moveTo(55, 105);
            left.lineTo(52, 98);
            canvas.drawPath(left, paint);

            Path right = new Path();
            right.moveTo(125, 75);
            right.lineTo(145, 90);
            right.lineTo(125, 105);
            right.moveTo(125, 75);
            right.lineTo(128, 82);
            right.moveTo(125, 105);
            right.lineTo(128, 98);
            canvas.drawPath(right, paint);
        }
        return bitmap;
    }
}
