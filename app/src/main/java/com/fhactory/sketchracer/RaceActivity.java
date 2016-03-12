package com.fhactory.sketchracer;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

public class RaceActivity extends AppCompatActivity {

    public static final String EXTRA_CIRCUIT = "circuit";

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        gameView = (GameView) findViewById(R.id.race_view);

        ArrayList<Point> pts = getIntent().getParcelableArrayListExtra(EXTRA_CIRCUIT);
        Point[] ptsArray = new Point[pts.size()];
        pts.toArray(ptsArray);

        gameView.setCircuit(new Circuit(ptsArray));
    }
}
