package com.fhacktory.sketchracer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

public class GameView extends View {
    private Circuit circ;

    private Paint paint = new Paint();

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setStrokeWidth(5);
        paint.setAntiAlias(true);
    }

    public void setCircuit(Circuit circ) {
        this.circ = circ;
        invalidate();
    }

    public boolean setStart(Point start) {
        boolean res;
        if(circ != null) {
            res = circ.setStart(start, this);
        } else {
            res = false;
        }
        invalidate();
        return res;
    }

    public Point getStart() {
        return circ.getStart();
    }

    public Circuit getCircuit() {
        return circ;
    }

    @Override
    public void onDraw(Canvas c) {
        if(circ != null) circ.drawOn(c, paint);
    }
}