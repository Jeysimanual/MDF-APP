package com.capstone.mdfeventmanagementsystem.Utilities;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.capstone.mdfeventmanagementsystem.R;

public class CircleViewDashDottedLine extends View {

    private Paint paint;
    private float circleRadius = 10f; // Radius for the circles

    public CircleViewDashDottedLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        paint = new Paint();
        paint.setStrokeWidth(4f); // Line thickness
        paint.setStyle(Paint.Style.STROKE); // Default to stroke for consistency
        paint.setPathEffect(new DashPathEffect(new float[]{25, 8}, 0)); // Dash pattern

        // Set default color to #88919D, override with custom attribute if provided
        int defaultColor = 0xFF88919D; // #88919D with full opacity
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ViewDashDottedLine, 0, 0);
        try {
            int color = a.getColor(R.styleable.ViewDashDottedLine_lineColor, defaultColor); // Use defaultColor as fallback
            paint.setColor(color);
        } finally {
            a.recycle();
        }

        // Ensure the view handles drawing
        setWillNotDraw(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = (int) (circleRadius * 2 + getPaddingTop() + getPaddingBottom());
        setMeasuredDimension(getMeasuredWidth(), height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerY = getHeight() / 2f;

        // Calculate drawing area with padding
        float startX = getPaddingLeft();
        float endX = getWidth() - getPaddingRight();
        float lineStartX = startX + circleRadius; // Start line after the start circle
        float lineEndX = endX - circleRadius; // End line before the end circle

        // Draw circles with stroke (hollow)
        paint.setStrokeWidth(6f); // Thicker stroke for circles
        canvas.drawCircle(startX, centerY, circleRadius, paint); // Start circle
        canvas.drawCircle(endX, centerY, circleRadius, paint); // End circle

        // Draw the dashed line
        paint.setStrokeWidth(4f); // Restore line thickness
        canvas.drawLine(lineStartX, centerY, lineEndX, centerY, paint);
    }
}