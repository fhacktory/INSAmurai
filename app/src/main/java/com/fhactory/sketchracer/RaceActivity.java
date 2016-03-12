package com.fhactory.sketchracer;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

public class RaceActivity extends AppCompatActivity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        gameView = (GameView) findViewById(R.id.race_view);

        List<Point> ellipse = new ArrayList<>();
        for(double d = 0; d < 2 * Math.PI; d += 0.02) {
            ellipse.add(new Point((int)(1000*Math.cos(d)), (int)(600*Math.sin(d))));
        }

        Point[] array = new Point[ellipse.size()];
        ellipse.toArray(array);

        gameView.setCircuit(new Circuit(array));
    }
}
