package com.md.openxcapp;

import android.content.Context;
import android.graphics.Canvas;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;

/**
 * Created by ersinyildiz on 04/11/2017.
 */

public class MirroredConstraintLayout extends ConstraintLayout {
    public MirroredConstraintLayout(Context context) {
        super(context);
    }

    public MirroredConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MirroredConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        // Scale the canvas in reverse in the x-direction, pivoting on
        // the center of the view
        canvas.scale(-1f, 1f, getWidth() / 2f, getHeight() / 2f);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.save();
        // Scale the canvas in reverse in the x-direction, pivoting on
        // the center of the view
        canvas.scale(-1f, 1f, getWidth() / 2f, getHeight() / 2f);
        super.dispatchDraw(canvas);
        canvas.restore();
    }
}
