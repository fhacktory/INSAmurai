package com.fhactory.sketchracer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import java.util.ArrayList;

public class ContourView extends View {
    private MatOfPoint toDisplay = null;
    private Paint paint = new Paint();
    int minX, minY, maxX, maxY;

    public ContourView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPoints(MatOfPoint pointsToDraw) {
        //take the biggest MatOfPoint
        toDisplay  = pointsToDraw;

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

        for(int i = 0; i < points.length; i++) {
            int j = (i+1)%points.length;
            c.drawLine((float) ((points[i].x - minX) / scale), (float) ((points[i].y - minY) / scale),
                       (float) ((points[j].x - minX) / scale), (float) ((points[j].y - minY) / scale), paint);
        }
    }

    public boolean hasPoints() {
        return toDisplay != null;
    }

    public ArrayList<android.graphics.Point> getPoints() {
        if(toDisplay == null) return null;

        ArrayList<android.graphics.Point> pts = new ArrayList<>();

        double scale = Math.max((double) (maxX - minX) / 1000, (double) (maxY - minY) / 1000);

        Point[] points = toDisplay.toArray();
        for (Point point : points) {
            pts.add(new android.graphics.Point((int) ((point.x - minX) / scale), (int) ((point.y - minY) / scale)));
        }

        return pts;
    }
}
