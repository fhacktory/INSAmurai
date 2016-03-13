package com.fhacktory.sketchracer;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;

import java.util.ArrayList;

public class RaceActivity extends AppCompatActivity {

    public static final String EXTRA_CIRCUIT = "circuit";

    private CircuitView circuitView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race_settings);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        circuitView = (CircuitView) findViewById(R.id.race_view);

        ArrayList<Point> pts = getIntent().getParcelableArrayListExtra(EXTRA_CIRCUIT);
        Point[] ptsArray = new Point[pts.size()];
        pts.toArray(ptsArray);

        circuitView.setCircuit(new Circuit(ptsArray));
    }
}
