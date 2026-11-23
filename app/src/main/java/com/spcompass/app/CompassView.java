package com.spcompass.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CompassView extends View {
    private float heading = 0f;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setHeading(float heading) {
        this.heading = heading;
        invalidate();
    }

    private String bn(int n) {
        return MainActivity.toBn(n);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) / 2f - 8f;

        // Outer glass circle
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#EFF6FF"));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.parseColor("#2563EB"));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.parseColor("#BFDBFE"));
        canvas.drawCircle(cx, cy, r * 0.72f, paint);

        // Tick marks + degree labels, rotated by -heading so north stays up when heading=0
        canvas.save();
        canvas.rotate(-heading, cx, cy);
        textPaint.setColor(Color.parseColor("#1E3A8A"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int deg = 0; deg < 360; deg += 15) {
            double rad = Math.toRadians(deg);
            float x1 = cx + (float) (Math.sin(rad) * r * 0.92f);
            float y1 = cy - (float) (Math.cos(rad) * r * 0.92f);
            float x2 = cx + (float) (Math.sin(rad) * (deg % 45 == 0 ? r * 0.78f : r * 0.85f));
            float y2 = cy - (float) (Math.cos(rad) * (deg % 45 == 0 ? r * 0.78f : r * 0.85f));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(deg % 45 == 0 ? 5f : 2f);
            paint.setColor(deg == 0 ? Color.parseColor("#DC2626") : Color.parseColor("#1D4ED8"));
            canvas.drawLine(x1, y1, x2, y2, paint);
            if (deg % 45 == 0) {
                textPaint.setTextSize(r * 0.16f);
                textPaint.setFakeBoldText(true);
                float tx = cx + (float) (Math.sin(rad) * r * 0.62f);
                float ty = cy - (float) (Math.cos(rad) * r * 0.62f) + r * 0.06f;
                String label;
                if (deg == 0) label = "উ";
                else if (deg == 45) label = "উ-পূ";
                else if (deg == 90) label = "পূ";
                else if (deg == 135) label = "দ-পূ";
                else if (deg == 180) label = "দ";
                else if (deg == 225) label = "দ-প";
                else if (deg == 270) label = "প";
                else label = "উ-প";
                canvas.drawText(label, tx, ty, textPaint);
            }
        }
        canvas.restore();

        // Fixed needle pointing up (phone top = current heading direction)
        Path needle = new Path();
        needle.moveTo(cx, cy - r * 0.55f);
        needle.lineTo(cx - r * 0.07f, cy + r * 0.12f);
        needle.lineTo(cx + r * 0.07f, cy + r * 0.12f);
        needle.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#DC2626"));
        canvas.drawPath(needle, paint);
        Path tail = new Path();
        tail.moveTo(cx, cy + r * 0.55f);
        tail.lineTo(cx - r * 0.05f, cy - r * 0.05f);
        tail.lineTo(cx + r * 0.05f, cy - r * 0.05f);
        tail.close();
        paint.setColor(Color.parseColor("#93C5FD"));
        canvas.drawPath(tail, paint);

        // Centre hub
        paint.setColor(Color.parseColor("#1D4ED8"));
        canvas.drawCircle(cx, cy, r * 0.10f, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, r * 0.045f, paint);
    }
}
