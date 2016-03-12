package com.fhactory.sketchracer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import org.opencv.core.MatOfPoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends AppCompatActivity {

    private ContourView contourView;
    private File photoFile;

    private static final int CAMERA_REQUEST_CODE = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        contourView = (ContourView) findViewById(R.id.track_contour_view);

        if(savedInstanceState == null) {
            /*Sketch sketch = new Sketch("/storage/emulated/0/photo.jpg");
            List<MatOfPoint> contours = sketch.computeContours();
            contourView.setPoints(contours);*/
            takePhoto();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cleanTempDir(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CAMERA_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                Sketch sketch = new Sketch(photoFile.getAbsolutePath());
                List<MatOfPoint> contours = sketch.computeContours();
                contourView.setPoints(contours);
            } else if(resultCode == RESULT_CANCELED) {
                new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.must_take_photo))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            takePhoto();
                        }
                    });
            } else {
                new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.unknown_error))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            takePhoto();
                        }
                    });
            }
        }
    }

    private void takePhoto() {
        //demander à prendre une photo
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = File.createTempFile(
                        "photo",  /* prefix */
                        ".jpg",         /* suffix */
                        getExternalFilesDir("temporary")      /* directory */
                );

                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));

                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            } catch (IOException e) {
                new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.photo_error))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            }
        } else {
            new AlertDialog.Builder(this)
                .setMessage(getString(R.string.photo_error))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        }
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

    public void use(View v) {
        ArrayList<Point> pts = contourView.getPoints();

        //keeping only a tiny part of the points
        for(int i = 0; i < pts.size(); i++) {
            for(int l = 0; l < 30; l++) {
                if(i < pts.size()) pts.remove(i);
            }
        }
        Log.d("PhotoActivity", "Now passing "+pts.size()+" points!");

        Intent i = new Intent(this, RaceActivity.class);
        i.putParcelableArrayListExtra(RaceActivity.EXTRA_CIRCUIT, pts);
        //RaceActivity.pts = pts;

        startActivity(i);
    }
}
