package com.fhactory.sketchrace;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class PhotoActivity extends AppCompatActivity {

    private ImageView imageView;

    private static final int CAMERA_REQUEST_CODE = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        imageView = (ImageView) findViewById(R.id.track_image_view);

        if(savedInstanceState == null) {
            takePhoto();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CAMERA_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(photo);
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
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }
}
