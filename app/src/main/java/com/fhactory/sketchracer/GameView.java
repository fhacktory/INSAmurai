package com.fhactory.sketchracer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
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

    @Override
    public void onDraw(Canvas c) {
        if(circ != null) circ.drawOn(c, paint);
    }
}
