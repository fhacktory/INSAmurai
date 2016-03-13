package com.fhacktory.sketchracer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;

public class Circuit {
    private static final int WIDTH = 40;

    private List<Point> outside, inside;

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

    public void drawOn(Canvas c, Paint paint) {
        drawLine(c, paint, inside);
        drawLine(c, paint, outside);
    }

    private void drawLine(Canvas c, Paint paint, List<Point> points) {
        double scale = Math.max((double) (maxX - minX) / c.getWidth(), (double) (maxY - minY) / c.getHeight());

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
}
