package app.michibiki.navigator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public final class CompassView extends View {
    private static final int TARGET_RED = Color.rgb(255, 35, 48);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float relativeBearing;
    private float heading;
    private boolean ready;
    private boolean detourMode;

    public CompassView(Context context) { super(context); }
    public CompassView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setNavigation(float relativeBearing, float heading, boolean ready) {
        this.relativeBearing = relativeBearing;
        this.heading = heading;
        this.ready = ready;
        invalidate();
    }

    public void setDetourMode(boolean enabled) {
        detourMode = enabled;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - dp(15);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeMiter(8f);
        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, radius, paint);

        drawNorthMarker(canvas, cx, cy, radius);
        drawPositionRings(canvas, cx, cy, radius);

        if (!ready) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(dp(14));
            paint.setColor(Ui.MUTED);
            canvas.drawText("Choose a destination", cx, cy + radius * 0.56f, paint);
            return;
        }

        canvas.save();
        canvas.rotate(relativeBearing, cx, cy);
        drawTargetArrow(canvas, cx, cy, radius);
        if (detourMode) drawDetourArrows(canvas, cx, cy, radius);
        canvas.restore();
    }

    private void drawNorthMarker(Canvas canvas, float cx, float cy, float radius) {
        canvas.save();
        canvas.rotate(-heading, cx, cy);
        float inset = dp(14);
        RectF arcBounds = new RectF(cx - radius + inset, cy - radius + inset,
                cx + radius - inset, cy + radius - inset);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(Color.WHITE);
        canvas.drawArc(arcBounds, -116f, 52f, false, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(dp(18));
        canvas.drawText("N", cx, cy - radius + dp(36), paint);
        canvas.restore();
    }

    private void drawTargetArrow(Canvas canvas, float cx, float cy, float radius) {
        float tipY = cy - radius * 0.55f;
        float footY = cy - radius * 0.27f;
        float halfWidth = radius * 0.13f;
        float serif = radius * 0.09f;
        Path target = new Path();
        target.moveTo(cx - halfWidth + serif, footY - serif * 0.45f);
        target.lineTo(cx - halfWidth, footY);
        target.lineTo(cx, tipY);
        target.lineTo(cx + halfWidth, footY);
        target.lineTo(cx + halfWidth - serif, footY - serif * 0.45f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(5));
        paint.setColor(TARGET_RED);
        canvas.drawPath(target, paint);
    }

    private void drawDetourArrows(Canvas canvas, float cx, float cy, float radius) {
        float offset = radius * 0.48f;
        // Compact copies of the main A-shaped arrow, rotated toward each bypass.
        float halfHeight = radius * 0.09f;
        float depth = radius * 0.13f;
        float serif = radius * 0.09f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(4));
        paint.setColor(Color.WHITE);

        Path left = new Path();
        left.moveTo(cx - offset + depth - serif * 0.35f, cy - halfHeight + serif);
        left.lineTo(cx - offset + depth, cy - halfHeight);
        left.lineTo(cx - offset, cy);
        left.lineTo(cx - offset + depth, cy + halfHeight);
        left.lineTo(cx - offset + depth - serif * 0.35f, cy + halfHeight - serif);
        canvas.drawPath(left, paint);

        Path right = new Path();
        right.moveTo(cx + offset - depth + serif * 0.35f, cy - halfHeight + serif);
        right.lineTo(cx + offset - depth, cy - halfHeight);
        right.lineTo(cx + offset, cy);
        right.lineTo(cx + offset - depth, cy + halfHeight);
        right.lineTo(cx + offset - depth + serif * 0.35f, cy + halfHeight - serif);
        canvas.drawPath(right, paint);
    }

    private void drawPositionRings(Canvas canvas, float cx, float cy, float radius) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, radius * 0.13f, paint);
        paint.setStrokeWidth(dp(5));
        canvas.drawCircle(cx, cy, radius * 0.20f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(cx, cy, radius * 0.08f, paint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
