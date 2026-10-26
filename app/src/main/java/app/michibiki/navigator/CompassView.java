package app.michibiki.navigator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public final class CompassView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float relativeBearing;
    private float heading;
    private boolean ready;

    public CompassView(Context context) { super(context); }
    public CompassView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setNavigation(float relativeBearing, float heading, boolean ready) {
        this.relativeBearing = relativeBearing;
        this.heading = heading;
        this.ready = ready;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - Ui.dp(getContext(), 18);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Ui.dp(getContext(), 1));
        paint.setColor(Color.rgb(58, 58, 58));
        canvas.drawCircle(cx, cy, radius, paint);

        // The compass rose follows the physical world. North is therefore
        // drawn at -heading, instead of being pinned to the top of the screen.
        canvas.save();
        canvas.rotate(-heading, cx, cy);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Ui.dp(getContext(), 18));
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText("N", cx, cy - radius + Ui.dp(getContext(), 23), paint);
        paint.setColor(Color.rgb(110, 110, 110));
        paint.setStrokeWidth(Ui.dp(getContext(), 2));
        for (int angle = 30; angle < 360; angle += 30) {
            canvas.save();
            canvas.rotate(angle, cx, cy);
            canvas.drawLine(cx, cy - radius, cx, cy - radius + Ui.dp(getContext(), angle % 90 == 0 ? 12 : 7), paint);
            canvas.restore();
        }
        canvas.restore();

        if (!ready) {
            paint.setTextSize(Ui.dp(getContext(), 15));
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            paint.setColor(Ui.MUTED);
            canvas.drawText("Choose a destination", cx, cy + Ui.dp(getContext(), 5), paint);
            return;
        }

        canvas.save();
        canvas.rotate(relativeBearing, cx, cy);
        float tip = cy - radius * 0.62f;
        float shoulder = cy - radius * 0.16f;
        float stemBottom = cy + radius * 0.28f;
        float headHalf = radius * 0.20f;
        float stemHalf = radius * 0.055f;
        Path arrow = new Path();
        arrow.moveTo(cx, tip);
        arrow.lineTo(cx + headHalf, shoulder);
        arrow.lineTo(cx + stemHalf, shoulder);
        arrow.lineTo(cx + stemHalf, stemBottom);
        arrow.lineTo(cx - stemHalf, stemBottom);
        arrow.lineTo(cx - stemHalf, shoulder);
        arrow.lineTo(cx - headHalf, shoulder);
        arrow.close();
        paint.setColor(Color.WHITE);
        canvas.drawPath(arrow, paint);
        paint.setColor(Ui.BLACK);
        canvas.drawCircle(cx, cy, Ui.dp(getContext(), 4), paint);
        canvas.restore();
    }
}
