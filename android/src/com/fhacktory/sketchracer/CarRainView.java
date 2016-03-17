package com.fhacktory.sketchracer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Small easter egg referring to a private joke between the developers during the hackathon...
 * When calling enable(), cars fall down from the top of the view
 */
public class CarRainView extends View {

    /**
     * Time waiting between each frame.
     */
    private static final int REFRESH_RATE = 10;
    /**
     * Number of pixels added to car's y coordinate per frame.
     */
    private static final int CAR_STEP = 10;
    /**
     * Number of frames to wait before spawning a new car.
     */
    private static final int NEW_CAR_THRESHOLD = 10;


    private int carCounter = NEW_CAR_THRESHOLD;

    private ArrayList<Point> carPositions = new ArrayList<>();

    private Bitmap car;

    private Runnable refresh = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };

    public CarRainView(Context context, AttributeSet attrs) {
        super(context, attrs);

        try {
            InputStream bitmap = context.getAssets().open("car.png");
            car = BitmapFactory.decodeStream(bitmap);
        } catch (IOException e) {
            Log.wtf("CarRainView", "But where is the car I'm supposed to make rain?");
        }
    }

    public void enable() {
        Log.i("CarRainView", "Now making cars rain! Yay!");

        setVisibility(View.VISIBLE);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Log.i("CarRainView", "Gets destroyed, stop the rain");

        getHandler().removeCallbacks(refresh);
    }

    @Override
    public void onDraw(Canvas c) {
        //don't care making cars rain if the view is invisible
        if(getVisibility() == View.VISIBLE) {
            Log.v("CarRainView", "Cars are still raining out there");

            //clean screen
            c.drawARGB(0, 0, 0, 0);

            for(int i = 0; i < carPositions.size(); i++) {
                Point p = carPositions.get(i);

                p.y += CAR_STEP;

                if((p.y > getHeight())) {
                    carPositions.remove(i);
                    i--;
                    Log.d("CarRainView", "Killed a car, now has "+carPositions.size()+" of these");
                } else {
                    c.drawBitmap(car, p.x, p.y, null);
                }
            }

            //spawn a new car every now and then
            if(--carCounter < 0) {
                Point s = new Point(
                        (int) (Math.random() * (getWidth() - car.getWidth())),
                        - car.getHeight());
                carPositions.add(s);

                Log.d("CarRainView", "Spawned a car, now has " + carPositions.size() + " of these");
                Log.d("CarRainView", "Its coordinates are: "+s.x+", "+s.y);

                carCounter = NEW_CAR_THRESHOLD;
            }

            getHandler().removeCallbacks(refresh);
            getHandler().postDelayed(refresh, REFRESH_RATE);
        }
    }
}
