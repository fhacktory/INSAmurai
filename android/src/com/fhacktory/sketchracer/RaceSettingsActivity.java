package com.fhacktory.sketchracer;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;

import java.util.ArrayList;

public class RaceSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_CIRCUIT = "circuit";

    public static final String SAVE_TURNS = "save_turns";
    public static final String SAVE_CIRCUIT = "save_circuit";


    private CircuitView circuitView;

    private int turns = 3;

    private View pleaseTouch, go;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race_settings);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        circuitView = (CircuitView) findViewById(R.id.race_view);

        if(savedInstanceState == null) {
            ArrayList<Point> pts = getIntent().getParcelableArrayListExtra(EXTRA_CIRCUIT);
            Point[] ptsArray = new Point[pts.size()];
            pts.toArray(ptsArray);

            circuitView.setCircuit(new Circuit(ptsArray));
        } else {
            Circuit circ = savedInstanceState.getParcelable(SAVE_CIRCUIT);
            circuitView.setCircuit(circ);

            turns = savedInstanceState.getInt(SAVE_TURNS);
            if ((findViewById(R.id.turn_count)) != null) {
                ((Button) findViewById(R.id.turn_count)).setText(String.valueOf(turns));
            }
        }

        pleaseTouch = findViewById(R.id.please_touch);
        go = findViewById(R.id.go);
        if (go != null && circuitView.getCircuit().getStart() == null) {
            go.setVisibility(View.GONE);
        } else if(pleaseTouch != null) {
            pleaseTouch.setVisibility(View.GONE);
        }

        circuitView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(circuitView.setStart(new Point((int)event.getX(), (int)event.getY()))) {
                    pleaseTouch.setVisibility(View.GONE);
                    go.setVisibility(View.VISIBLE);
                }

                return true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(SAVE_CIRCUIT, circuitView.getCircuit());
        outState.putInt(SAVE_TURNS, turns);
    }

    public void changeTurns(View v) {
        final Button b = (Button) v;

        View pick = View.inflate(this, R.layout.number_picker, null);

        final NumberPicker numberPicker = (NumberPicker) pick.findViewById(R.id.numberPicker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(10);
        numberPicker.setValue(turns);
        numberPicker.setWrapSelectorWheel(false);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.turn_count))
                .setView(pick)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        turns = numberPicker.getValue();
                        b.setText(String.valueOf(turns));
                    }
                })
                .show();
    }



    public void cEstParti(View v) {
        Intent i = new Intent(this, AndroidLauncher.class);
        i.putExtra("circuit", circuitView.getCircuit());
        i.putExtra("turns", turns);
        startActivity(i);
    }
}
