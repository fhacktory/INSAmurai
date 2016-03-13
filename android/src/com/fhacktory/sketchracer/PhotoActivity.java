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
import android.view.View;
import android.view.WindowManager;

import org.opencv.core.MatOfPoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends AppCompatActivity {

    private static final String SAVE_POINTS = "save_points";

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

        if(savedInstanceState == null || savedInstanceState.getParcelableArrayList(SAVE_POINTS) == null) {
            takePhoto();
        } else {
            Log.d("PhotoActivity", "Restoring instance state!");
            List<Point> savedPoints = savedInstanceState.getParcelableArrayList(SAVE_POINTS);
            contourView.setPoints(savedPoints);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d("PhotoActivity", "Saving instance state!");

        if(contourView.hasPoints()) {
            outState.putParcelableArrayList(SAVE_POINTS, contourView.getPoints());
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
                if(!contourView.hasPoints()) {
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.error))
                            .setMessage(getString(R.string.must_take_photo))
                            .setPositiveButton(getString(R.string.retry), takePhoto)
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
                    .setMessage(getString(R.string.photo_error))
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
                ProgressDialog.show(this, getString(R.string.processing), getString(R.string.search_contour), true, false);

        new Thread() {
            @Override public void run() {
                Sketch sketch = new Sketch(photoFile.getAbsolutePath());
                List<MatOfPoint> contours = sketch.computeContours();

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
                            contourView.setMatOfPoints(finalBiggest);
                            pd.dismiss();
                            cleanTempDir(PhotoActivity.this);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                            cleanTempDir(PhotoActivity.this);

                            if(!contourView.hasPoints()) {
                                new AlertDialog.Builder(PhotoActivity.this)
                                        .setTitle(getString(R.string.error))
                                        .setMessage(getString(R.string.no_contour))
                                        .setPositiveButton(getString(R.string.retry), takePhoto)
                                        .setCancelable(false)
                                        .show();
                            } else {
                                new AlertDialog.Builder(PhotoActivity.this)
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

    public void use(View v) {
        ArrayList<Point> pts = contourView.getPoints();

        Log.d("PhotoActivity", "Now passing "+pts.size()+" points!");

        Intent i = new Intent(this, AndroidLauncher.class);//RaceActivity.class
        i.putParcelableArrayListExtra(RaceActivity.EXTRA_CIRCUIT, pts);
        //RaceActivity.pts = pts;

        startActivity(i);
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

}
