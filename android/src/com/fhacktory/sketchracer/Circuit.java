package com.fhacktory.sketchracer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class Circuit {
    private static final int WIDTH = 40;

    private List<Point> outside, inside;

    private Point start = null;

    private int minX, minY, maxX, maxY;

    public Circuit(Point[] points) {
        inside = new ArrayList<>();
        outside = new ArrayList<>();

        for(int i = 0; i < points.length - 1; i++) {
            Point pt1 = points[i];
            Point pt2 = points[i+1 == points.length ? 0 : i+1];

            Point middle = new Point((pt1.x + pt2.x) / 2, (pt1.y + pt2.y) / 2);

            int vectorX = pt2.x - pt1.x;
            int vectorY = pt2.y - pt1.y;

            double normalX = - vectorY;
            double normalY = vectorX;

            double normeVecteurNormal = Math.sqrt(normalX*normalX + normalY*normalY);

            normalX /= normeVecteurNormal;
            normalY /= normeVecteurNormal;

            if(normeVecteurNormal != 0.0) {
                outside.add(new Point((int) (middle.x + normalX * WIDTH), (int) (middle.y + normalY * WIDTH)));
                inside.add(new Point((int) (middle.x - normalX * WIDTH), (int) (middle.y - normalY * WIDTH)));
            }
        }

        for(Point p : inside) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);

            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }
        for(Point p : outside) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);

            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }

        minX -= 5;
        minY -= 5;
        maxX += 5;
        maxY += 5;
    }

    private double getScale(View parent) {
        return Math.max((double) (maxX - minX) / parent.getWidth(), (double) (maxY - minY) / parent.getHeight());
    }
    private double getScale(Canvas c) {
        return Math.max((double) (maxX - minX) / c.getWidth(), (double) (maxY - minY) / c.getHeight());
    }

    public void setStart(Point start, View parent) {
        this.start = start;

        this.start.x *= getScale(parent);
        this.start.y *= getScale(parent);
        this.start.x += minX;
        this.start.y += minY;
    }

    public void drawOn(Canvas c, Paint paint) {
        paint.setColor(Color.BLACK);

        drawLine(c, paint, inside);
        drawLine(c, paint, outside);

        paint.setColor(Color.RED);

        if(start != null) {
            Point scaledStart = new Point((int)((start.x-minX)/getScale(c)), (int)((start.y-minY)/getScale(c)));

            int crossSize = 15;
            c.drawLine(scaledStart.x - crossSize, scaledStart.y - crossSize,
                    scaledStart.x + crossSize, scaledStart.y + crossSize, paint);
            c.drawLine(scaledStart.x - crossSize, scaledStart.y + crossSize,
                    scaledStart.x + crossSize, scaledStart.y - crossSize, paint);
        }
    }

    private void drawLine(Canvas c, Paint paint, List<Point> points) {
        double scale = getScale(c);

        float[] pts = new float[points.size() * 4];

        for(int i = 0; i < points.size(); i++) {
            Point pt1 = points.get(i);
            Point pt2 = points.get(i+1 == points.size() ? 0 : i+1);

            pts[4*i] = (float)((pt1.x-minX)/scale);
            pts[4*i+1] = (float)((pt1.y-minY)/scale);
            pts[4*i+2] = (float)((pt2.x-minX)/scale);
            pts[4*i+3] = (float)((pt2.y-minY)/scale);
        }

        c.drawLines(pts, paint);
    }

    public Point getStart() {
        return start;
    }
}
