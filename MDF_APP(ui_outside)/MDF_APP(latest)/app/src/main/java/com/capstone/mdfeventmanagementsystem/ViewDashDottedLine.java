package com.capstone.mdfeventmanagementsystem;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ViewDashDottedLine extends View {

    private Paint paint;

    public ViewDashDottedLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        paint = new Paint();
        paint.setStrokeWidth(6f); // Line thickness
        paint.setStyle(Paint.Style.STROKE);
        paint.setPathEffect(new DashPathEffect(new float[]{25, 8}, 0)); // Dash pattern

        // Read custom attributes
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ViewDashDottedLine, 0, 0);
        try {
            int color = a.getColor(R.styleable.ViewDashDottedLine_lineColor, 0xFF3A862D); // Default color
            paint.setColor(color);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw a horizontal dashed line across the width of the view
        canvas.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2, paint);
    }
}
