package com.fhacktory.sketchracer;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;

import org.opencv.core.MatOfPoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RaceSettingsActivity extends AppCompatActivity {

    private DialogInterface.OnClickListener finish = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            finish();
        }
    };

    private DialogInterface.OnClickListener takePhoto = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            takePhoto();
        }
    };

    private File photoFile;

    private static final int CAMERA_REQUEST_CODE = 12345;

    public static final String EXTRA_CIRCUIT = "circuit";

    public static final String SAVE_TURNS = "save_turns";
    public static final String SAVE_CIRCUIT = "save_circuit";


    private CircuitView circuitView;

    private int turns = 1;

    private View pleaseTouch, go;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race_settings);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        circuitView = (CircuitView) findViewById(R.id.race_view);

        if(savedInstanceState == null || savedInstanceState.getParcelable(SAVE_CIRCUIT) == null) {
            takePhoto();
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
        if (go != null && (circuitView.getCircuit() == null || circuitView.getCircuit().getStart() == null)) {
            go.setVisibility(View.GONE);
        } else if(pleaseTouch != null) {
            pleaseTouch.setVisibility(View.GONE);
        }

        circuitView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (circuitView.setStart(new Point((int) event.getX(), (int) event.getY()))) {
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

    /*** From PhotoActivity ***/
    @Override
    protected void onDestroy() {
        super.onDestroy();

        cleanTempDir(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CAMERA_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                if(data != null) {
                    String path = null;
                    if (data.getData().getScheme().equals("content"))
                        path = getRealPathFromURI(data.getData());
                    else if (data.getData().getScheme().equals("file"))
                        path = data.getData().getPath();

                    if(path == null) {
                        new AlertDialog.Builder(this)
                                .setMessage(getString(R.string.unsupported))
                                .setPositiveButton(getString(R.string.retry), takePhoto)
                                .setNegativeButton(getString(R.string.exit), finish)
                                .setCancelable(false)
                                .show();

                        return;
                    }
                    photoFile = new File(path);
                }

                scanPhoto();
            } else if(resultCode == RESULT_CANCELED) {
                if(circuitView.getCircuit() == null) {
                    new AlertDialog.Builder(this)
                            .setMessage(getString(R.string.must_take_photo))
                            .setPositiveButton(getString(R.string.ok), takePhoto)
                            .setNegativeButton(getString(R.string.exit), finish)
                            .setCancelable(false)
                            .show();
                }
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.unknown_error))
                        .setPositiveButton(getString(R.string.retry), takePhoto)
                        .setNegativeButton(getString(R.string.exit), finish)
                        .setCancelable(false)
                        .show();
            }
        }
    }

    private void takePhoto() {
        try {
            photoFile = File.createTempFile(
                    "photo",  /* prefix */
                    ".jpg",         /* suffix */
                    getExternalFilesDir("temporary")      /* directory */
            );
        } catch (IOException e) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.error))
                    .setMessage(getString(R.string.unknown_error))
                    .setPositiveButton(getString(R.string.ok), finish)
                    .setCancelable(false)
                    .show();
        }

        Uri outputFileUri = Uri.fromFile(photoFile);

        // Get image from camera
        final List<Intent> cameraIntents = new ArrayList<>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for(ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }

        // Get image from gallery
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Chooser of filesystem options
        final Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.select_picture));

        // Add the camera options
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

        startActivityForResult(chooserIntent, CAMERA_REQUEST_CODE);
    }

    private void scanPhoto() {
        final ProgressDialog pd =
                ProgressDialog.show(this, "", getString(R.string.search_contour), true, false);

        new Thread() {
            @Override public void run() {
                while(photoFile.length() == 0) {
                    Log.d("PhotoActivity", "Waiting for file...");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                ContourUtils contourUtils = new ContourUtils(photoFile.getAbsolutePath());
                List<MatOfPoint> contours = contourUtils.computeContours();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.setMessage(getString(R.string.contour_selecting));
                    }
                });

                if(contours.size() != 0) {
                    MatOfPoint biggest = contours.get(0);
                    for (MatOfPoint candidate : contours) {
                        if (biggest.toArray().length < candidate.toArray().length)
                            biggest = candidate;
                    }
                    final MatOfPoint finalBiggest = biggest;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ArrayList<Point> listPoints = getPoints(finalBiggest);
                            Point[] tablPoints = new Point[listPoints.size()];
                            listPoints.toArray(tablPoints);

                            circuitView.setCircuit(new Circuit(tablPoints));
                            pd.dismiss();
                            cleanTempDir(RaceSettingsActivity.this);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                            cleanTempDir(RaceSettingsActivity.this);

                            if(circuitView.getCircuit() == null) {
                                new AlertDialog.Builder(RaceSettingsActivity.this)
                                        .setTitle(getString(R.string.error))
                                        .setMessage(getString(R.string.no_contour))
                                        .setPositiveButton(getString(R.string.retry), takePhoto)
                                        .setCancelable(false)
                                        .show();
                            } else {
                                new AlertDialog.Builder(RaceSettingsActivity.this)
                                        .setTitle(getString(R.string.error))
                                        .setMessage(getString(R.string.no_contour))
                                        .setPositiveButton(getString(R.string.cancel), takePhoto)
                                        .setPositiveButton(getString(R.string.retry), takePhoto)
                                        .show();
                            }
                        }
                    });
                }
            }
        }.start();
    }

    public static void cleanTempDir(Context c) {
        File directory = c.getExternalFilesDir("temporary");
        if (directory != null) {
            File[] list = directory.listFiles();
            for(File f : list) {
                if(f.delete()) {
                    Log.d("CleanTempDir", f.getName()+" deleted");
                } else {
                    Log.e("CleanTempDir", f.getName()+" not deleted!");
                }
            }
        }
    }

    public void retry(View v) {
        takePhoto();
    }

    //gets the real path from a content:// URI
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    /*** From ContourView ***/
    public ArrayList<android.graphics.Point> getPoints(MatOfPoint matOfPoint) {
        //take the biggest MatOfPoint
        ArrayList<Point> toDisplay = new ArrayList<>();
        org.opencv.core.Point[] pts = matOfPoint.toArray();

        //keeping only a tiny part of the points
        for(int i = 0; i < pts.length; i += 30) {
            toDisplay.add(new Point((int)pts[i].x, (int)pts[i].y));
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = 0;
        int maxY = 0;

        for(Point p : toDisplay) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);

            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }

        ArrayList<android.graphics.Point> ptsRet = new ArrayList<>();

        double scale = Math.max((double) (maxX - minX) / 1000, (double) (maxY - minY) / 1000);

        for (Point point : toDisplay) {
            ptsRet.add(new android.graphics.Point((int) ((point.x - minX) / scale), (int) ((point.y - minY) / scale)));
        }

        return ptsRet;
    }
}
