package app.michibiki.navigator;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.RemoteViews;
import java.util.Locale;

public final class NavWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) manager.updateAppWidget(id, build(context));
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, NavWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) manager.updateAppWidget(id, build(context));
    }

    private static RemoteViews build(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_nav);
        Intent main = new Intent(context, MainActivity.class);
        PendingIntent mainPending = PendingIntent.getActivity(context, 10, main, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, mainPending);

        Intent voice = new Intent(context, VoiceSearchActivity.class);
        PendingIntent voicePending = PendingIntent.getActivity(context, 11, voice, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_voice, voicePending);

        if (!NavStore.hasTarget(context)) {
            views.setTextViewText(R.id.widget_name, "No destination");
            views.setTextViewText(R.id.widget_distance, "—");
            views.setTextViewText(R.id.widget_status, "Tap to configure");
            views.setImageViewBitmap(R.id.widget_arrow, arrowBitmap(0, false));
            return views;
        }
        views.setTextViewText(R.id.widget_name, NavStore.targetName(context));
        if (!NavStore.hasLocation(context)) {
            views.setTextViewText(R.id.widget_distance, "—");
            views.setTextViewText(R.id.widget_status, "Waiting for location");
            views.setImageViewBitmap(R.id.widget_arrow, arrowBitmap(0, false));
            return views;
        }
        double bearing = GeoMath.bearingDegrees(NavStore.locationLat(context), NavStore.locationLon(context), NavStore.targetLat(context), NavStore.targetLon(context));
        double distance = GeoMath.distanceMeters(NavStore.locationLat(context), NavStore.locationLon(context), NavStore.targetLat(context), NavStore.targetLon(context));
        long ageSeconds = Math.max(0, (System.currentTimeMillis() - NavStore.locationTime(context)) / 1000L);
        views.setTextViewText(R.id.widget_distance, GeoMath.formatDistance(distance));
        views.setTextViewText(R.id.widget_status, String.format(Locale.getDefault(), "%.0f° · ±%.0f m · %d s", bearing, NavStore.locationAccuracy(context), ageSeconds));
        views.setImageViewBitmap(R.id.widget_arrow, arrowBitmap((float) bearing, true));
        return views;
    }

    private static Bitmap arrowBitmap(float degrees, boolean ready) {
        Bitmap bitmap = Bitmap.createBitmap(180, 180, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.GRAY);
        canvas.drawCircle(90, 90, 82, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(22f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("N", 90, 27, paint);
        if (!ready) return bitmap;
        canvas.rotate(degrees, 90, 90);
        Path path = new Path();
        path.moveTo(90, 20);
        path.lineTo(112, 104);
        path.lineTo(90, 88);
        path.lineTo(68, 104);
        path.close();
        canvas.drawPath(path, paint);
        return bitmap;
    }
}
