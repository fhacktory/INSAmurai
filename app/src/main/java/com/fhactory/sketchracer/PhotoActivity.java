package com.fhactory.sketchracer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

public class PhotoActivity extends AppCompatActivity {

    private ImageView imageView;
    private File photoFile;

    private static final int CAMERA_REQUEST_CODE = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        imageView = (ImageView) findViewById(R.id.track_image_view);

        if(savedInstanceState == null) {
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
                //setPic();
                Sketch sketch = new Sketch(photoFile.getAbsolutePath());
                sketch.compute_contures();
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

    /*private void setPic() {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoFile.getAbsolutePath(), bmOptions);
        double photoW = bmOptions.outWidth / 2048d;
        double photoH = bmOptions.outHeight / 2048d;

        // Determine how much to scale down the image
        int scaleFactor = Math.min((int)Math.ceil(photoW), (int)Math.ceil(photoH));

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath(), bmOptions);
        imageView.setImageBitmap(bitmap);
    }*/

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
}
