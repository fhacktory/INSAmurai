package com.fhacktory.sketchracer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import org.opencv.core.MatOfPoint;

import java.util.ArrayList;
import java.util.List;

public class ContourView extends View {
    private List<Point> toDisplay = null;
    private Paint paint = new Paint();
    int minX, minY, maxX, maxY;

    public ContourView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPoints(List<Point> list) {
        toDisplay = list;

        updateMinMax();
        invalidate();
    }

    public void setMatOfPoints(MatOfPoint pointsToDraw) {
        //take the biggest MatOfPoint
        toDisplay = new ArrayList<>();
        org.opencv.core.Point[] pts = pointsToDraw.toArray();

        //keeping only a tiny part of the points
        for(int i = 0; i < pts.length; i += 30) {
            toDisplay.add(new Point((int)pts[i].x, (int)pts[i].y));
        }

        updateMinMax();
        invalidate();
    }

    private void updateMinMax() {
        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
        maxX = 0;
        maxY = 0;

        for(Point p : toDisplay) {
            minX = Math.min(minX, (int) p.x);
            minY = Math.min(minY, (int) p.y);

            maxX = Math.max(maxX, (int)p.x);
            maxY = Math.max(maxY, (int)p.y);
        }

        minX -= 5;
        minY -= 5;
        maxX += 5;
        maxY += 5;
    }

    @Override
    public void onDraw(Canvas c) {
        if(toDisplay == null) return;

        double scale = Math.max((double) (maxX - minX) / getWidth(), (double) (maxY - minY) / getHeight());

        System.out.println("Contour has size "+toDisplay.size());


        for(int i = 0; i < toDisplay.size(); i++) {
            int j = (i+1)%toDisplay.size();
            c.drawLine((float) ((toDisplay.get(i).x - minX) / scale), (float) ((toDisplay.get(i).y - minY) / scale),
                       (float) ((toDisplay.get(j).x - minX) / scale), (float) ((toDisplay.get(j).y - minY) / scale), paint);
        }
    }

    public boolean hasPoints() {
        return toDisplay != null;
    }

    public ArrayList<android.graphics.Point> getPoints() {
        if(toDisplay == null) return null;

        ArrayList<android.graphics.Point> pts = new ArrayList<>();

        double scale = Math.max((double) (maxX - minX) / 1000, (double) (maxY - minY) / 1000);

        for (Point point : toDisplay) {
            pts.add(new android.graphics.Point((int) ((point.x - minX) / scale), (int) ((point.y - minY) / scale)));
        }

        return pts;
    }
}
