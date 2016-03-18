package com.fhacktory.sketchracer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class Circuit implements Parcelable {
    public static final int WIDTH = 40;

    private static final int NOISE_POINT_REMOVE_THRESHOLD = 20;

    private ArrayList<Point> outside, inside;

    public final static double EPS = 1e-10;

    private Point start = null;

    private int minX, minY, maxX, maxY;
    private boolean dirty;

    public Circuit(Point[] points) {
        int width = WIDTH;

        //try generating the circuit as necessary, until it is possible to solve all the intersections
        //(making it narrower at each try)
        do {
            Log.i("Circuit", "I try building it with a width="+width);

            inside = new ArrayList<>();
            outside = new ArrayList<>();

            for (int i = 0; i < points.length - 1; i++) {
                Point pt1 = points[i];
                Point pt2 = points[i + 1 == points.length ? 0 : i + 1];

                Point middle = new Point((pt1.x + pt2.x) / 2, (pt1.y + pt2.y) / 2);

                int vectorX = pt2.x - pt1.x;
                int vectorY = pt2.y - pt1.y;

                double normalX = -vectorY;
                double normalY = vectorX;

                double normeVecteurNormal = Math.sqrt(normalX * normalX + normalY * normalY);

                normalX /= normeVecteurNormal;
                normalY /= normeVecteurNormal;

                if (normeVecteurNormal != 0.0) {
                    outside.add(new Point((int) (middle.x + normalX * width), (int) (middle.y + normalY * width)));
                    inside.add(new Point((int) (middle.x - normalX * width), (int) (middle.y - normalY * width)));
                }
            }

            for (Point p : inside) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);

                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }
            for (Point p : outside) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);

                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }
            minX -= 5;
            minY -= 5;
            maxX += 5;
            maxY += 5;

            Intersect();

            width--;
        } while(dirty);

        //scale the whole circuit for it to be at the requested width
        double ratio = (double) WIDTH / width;
        for(Point p : inside) {
            p.x *= ratio;
            p.y *= ratio;
        }
        for(Point p : outside) {
            p.x *= ratio;
            p.y *= ratio;
        }
        minX *= ratio;
        minY *= ratio;
        maxX *= ratio;
        maxY *= ratio;
    }

    public boolean isDirty() {
        return dirty;
    }

    private void Intersect(){
        dirty = false;

        int n = inside.size();

        for(int i=0;i<n;++i) {
            for(int j=0;j<i-1;++j) {
                if(SegementIntersection(inside.get(i), inside.get((i - 1+n)%n), inside.get(j), inside.get((j - 1 + n)%n))) {
                    double t = FindIntersection(inside.get(i),inside.get((i-1+n)%n),inside.get(j),inside.get((j-1+n)%n));
                    int ex = (int)(inside.get(i).x + (inside.get(i-1).x-inside.get(i).x)*t);
                    int ey = (int)(inside.get(i).y + (inside.get(i-1).y-inside.get(i).y)*t);
                    if(j+n-i+1>i-1-j)
                    {
                        Log.d("Circuit", "I am removing "+(i-1-j)+" points apparently [inside]");
                        if(i-1-j > NOISE_POINT_REMOVE_THRESHOLD) {
                            Log.d("Circuit", "This is too much! Don't do that!");
                            dirty = true;
                            return;
                        } else {
                            for (int k = j; k <= i - 1; ++k)
                                inside.get(k).set(ex, ey);
                        }
                    } else {
                        Log.d("Circuit", "I am removing "+(j+n-i)+" points apparently [inside]");
                        if(j+n-i > NOISE_POINT_REMOVE_THRESHOLD) {
                            Log.d("Circuit", "This is too much! Don't do that!");
                            dirty = true;
                            return;
                        } else {
                            for(int k=i;k<=j+n;++k)
                                inside.get(k%n).set(ex,ey);
                        }
                    }
                    break;
                }

            }
        }

        n = outside.size();
        for(int i=0;i<n;++i) {
            for(int j=0;j<i-1;++j) {
                if(SegementIntersection(outside.get(i), outside.get((i - 1 + n) % n), outside.get(j), outside.get((j - 1 + n) % n))) {
                    double t = FindIntersection(outside.get(i),outside.get((i-1+n)%n),outside.get(j),outside.get((j-1+n)%n));
                    int ex = (int)(outside.get(i).x + (outside.get(i-1).x-outside.get(i).x)*t);
                    int ey = (int)(outside.get(i).y + (outside.get(i-1).y-outside.get(i).y)*t);
                    boolean bool1 = Math.abs(outside.get(i).x-outside.get((i-1+n)%n).x)>=2.0 * Math.abs(outside.get(i).x-ex);
                    boolean bool2 = Math.abs(outside.get(j).x-outside.get((j-1+n)%n).x)>=2.0 * Math.abs(outside.get(j).x - ex);
                    if(j+n-i+1>i-1-j)
                    {
                        Log.d("Circuit", "I am removing "+(i-1-j)+" points apparently [outside]");
                        if(i-1-j > NOISE_POINT_REMOVE_THRESHOLD) {
                            Log.d("Circuit", "This is too much! Don't do that!");
                            dirty = true;
                            return;
                        } else {
                            for (int k = j; k <= i - 1; ++k)
                                outside.get(k).set(ex, ey);
                        }
                    } else {
                        Log.d("Circuit", "I am removing "+(j+n-i)+" points apparently [outside]");
                        if(j+n-i > NOISE_POINT_REMOVE_THRESHOLD) {
                            Log.d("Circuit", "This is too much! Don't do that!");
                            dirty = true;
                            return;
                        } else {
                            for (int k = i; k <= j + n; ++k)
                                outside.get(k % n).set(ex, ey);
                        }
                    }
                    break;
                }
            }
        }
    }

    protected Circuit(Parcel in) {
        outside = in.createTypedArrayList(Point.CREATOR);
        inside = in.createTypedArrayList(Point.CREATOR);
        start = in.readParcelable(Point.class.getClassLoader());
        minX = in.readInt();
        minY = in.readInt();
        maxX = in.readInt();
        maxY = in.readInt();
        dirty = in.readInt() == 1;
    }

    public static final Creator<Circuit> CREATOR = new Creator<Circuit>() {
        @Override
        public Circuit createFromParcel(Parcel in) {
            return new Circuit(in);
        }

        @Override
        public Circuit[] newArray(int size) {
            return new Circuit[size];
        }
    };

    private double getScale(View parent) {
        return Math.max((double) (maxX - minX) / parent.getWidth(), (double) (maxY - minY) / parent.getHeight());
    }
    private double getScale(Canvas c) {
        return Math.max((double) (maxX - minX) / c.getWidth(), (double) (maxY - minY) / c.getHeight());
    }

    public boolean setStart(Point start, View parent) {
        int decX = parent.getWidth() / 2 - (int)((maxX - minX)/getScale(parent)) / 2;
        int decY = parent.getHeight() / 2 -(int)((maxY - minY)/getScale(parent)) / 2;

        start.x -= decX;
        start.y -= decY;
        start.x *= getScale(parent);
        start.y *= getScale(parent);
        start.x += minX;
        start.y += minY;

        if(PointInsideCircuit(start)) {
            this.start = start;
            return true;
        } else {
            return false;
        }
    }

    public void drawOn(Canvas c, Paint paint) {
        paint.setColor(Color.BLACK);

        drawLine(c, paint, inside);
        drawLine(c, paint, outside);

        paint.setColor(Color.RED);

        int decX = c.getWidth() / 2 - (int)((maxX - minX)/getScale(c)) / 2;
        int decY = c.getHeight() / 2 -(int)((maxY - minY)/getScale(c)) / 2;

        if(start != null) {
            Point scaledStart = new Point((int)((start.x-minX)/getScale(c) + decX), (int)((start.y-minY)/getScale(c) + decY));

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

        int decX = c.getWidth() / 2 - (int)((maxX - minX)/scale) / 2;
        int decY = c.getHeight() / 2 -(int)((maxY - minY)/scale) / 2;

        for(int i = 0; i < points.size(); i++) {
            Point pt1 = points.get(i);
            Point pt2 = points.get(i+1 == points.size() ? 0 : i+1);

            pts[4*i] = (float)((pt1.x-minX)/scale + decX);
            pts[4*i+1] = (float)((pt1.y-minY)/scale + decY);
            pts[4*i+2] = (float)((pt2.x-minX)/scale + decX);
            pts[4*i+3] = (float)((pt2.y-minY)/scale + decY);
        }

        c.drawLines(pts, paint);
    }

    public Point getStart() {
        return start;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(outside);
        dest.writeTypedList(inside);
        dest.writeParcelable(start, flags);
        dest.writeInt(minX);
        dest.writeInt(minY);
        dest.writeInt(maxX);
        dest.writeInt(maxY);
        dest.writeInt(dirty ? 1 : 0);
    }


    private boolean SegementIntersection(Point a, Point b, Point c, Point d){
        if (cross(new Point(d.x-a.x,d.y-a.y),new Point(b.x-a.x,b.y-a.y)) *
                cross(new Point(c.x-a.x,c.y-a.y), new Point(b.x-a.x,b.y-a.y)) > -EPS)
            return false;
        if (cross(new Point(a.x-c.x,a.y-c.y), new Point(d.x-c.x,d.y-c.y)) *
                cross(new Point(b.x-c.x,b.y-c.y), new Point(d.x-c.x,d.y-c.y)) > -EPS)
            return false;
        return true;
    }

    private double FindIntersection(Point a,Point b, Point c,Point d){
        double cross1 = cross(new Point(c.x-a.x,c.y-a.y), new Point(d.x-c.x,d.y-c.y));
        double cross2 = cross(new Point(b.x-a.x,b.y-a.y),new Point(d.x-c.x,d.y-c.y));
        if(cross1<EPS  || cross2<EPS)
            return 0;
        return cross1/cross2;

    }

    public boolean PointInsideCircuit(Point x){
        if(PointInPolygon(outside,x) && !PointOnPolygon(inside,x) && !( PointInPolygon(inside,x)))
            return true;
        return false;
    }

    private boolean PointInPolygon(ArrayList<Point> p, Point q){
        boolean c = false;
        for(int i=0;i<p.size();++i){
            int j = (i+1)%p.size();
            if ((p.get(i).y <= q.y && q.y < p.get(j).y ||
                    p.get(j).y <= q.y && q.y < p.get(i).y) &&
                    q.x < p.get(i).x + (p.get(j).x - p.get(i).x) * (q.y - p.get(i).y) / (p.get(j).y - p.get(i).y))
                c = !c;
        }
        return c;
    }

    private boolean PointOnPolygon(ArrayList<Point> p, Point q){
        for(int i=0;i<p.size();++i){
            if(dist2(ProjectPointSegment(p.get(i),p.get((i+1)%p.size()),q),q)<EPS)
                return true;
        }
        return false;
    }

    private Point ProjectPointSegment(Point a, Point b, Point c){
        double r = dot(b,b,a);
        if(Math.abs(r)<EPS) return a;
        r = dot(c,b,a)/r;
        if(r<0) return a;
        if(r>1) return b;
        return new Point((int)(a.x+(b.x-a.x)*r),(int)(a.y+(b.y-a.y)*r));
    }

    private double cross(Point p,Point q) { return p.x*q.y-p.y*q.x; }
    private double dot(Point p, Point q, Point a){ return (p.x-a.x)*(q.x-a.x)+(p.y-a.y)*(q.y-a.y);}
    private double dist2(Point p, Point q)   { return (p.x-q.x)*(p.x-q.x)+(p.y-q.y)*(p.y-q.y); }

    public List<Point> getInside() {
        return inside;
    }

    public List<Point> getOutside() {
        return outside;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

}
