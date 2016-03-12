package com.fhactory.sketchracer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class ContourView extends View {
    private MatOfPoint toDisplay = null;
    private Paint paint = new Paint();
    int minX, minY, maxX, maxY;

    public ContourView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPoints(List<MatOfPoint> pointsToDraw) {
        //take the biggest MatOfPoint
        MatOfPoint biggest = pointsToDraw.get(0);
        for(MatOfPoint candidate : pointsToDraw) {
            if(biggest.toArray().length < candidate.toArray().length)
                biggest = candidate;
        }

        toDisplay = biggest;

        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
        maxX = 0;
        maxY = 0;

        for(Point p : toDisplay.toArray()) {
            minX = Math.min(minX, (int) p.x);
            minY = Math.min(minY, (int) p.y);

            maxX = Math.max(maxX, (int)p.x);
            maxY = Math.max(maxY, (int)p.y);
        }

        invalidate();
    }

    @Override
    public void onDraw(Canvas c) {
        if(toDisplay == null) return;

        double scale = Math.max((double) (maxX - minX) / getWidth(), (double) (maxY - minY) / getHeight());

        System.out.println("Contour has size "+toDisplay.size());

        Point[] points = toDisplay.toArray();

        for(int i = 0; i < points.length - 2; i++) {
            c.drawLine((float) ((points[i].x - minX) / scale), (float) ((points[i].y - minY) / scale),
                       (float) ((points[i+1].x - minX) / scale), (float) ((points[i+1].y - minY) / scale), paint);
        }
    }
}
