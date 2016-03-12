package com.fhactory.sketchracer;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs; // imread, imwrite, etc


/**
 * Created by Afkid on 3/12/2016.
 */
public class Sketch {

    static{ System.loadLibrary("opencv_java3"); }

    private String path;

    public Sketch(String path)
    {
        this.path = path;
    }

    public void compute_contures() {
        // load opencv library
        Mat image = Imgcodecs.imread(path, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        Mat original = image.clone();

        Imgproc.threshold(image,image,100,128,Imgproc.THRESH_BINARY_INV);

        double[] center = {(double)image.width()/2,(double)image.height()/2};
        Point image_center = new Point(center);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy  = new Mat();

        Imgproc.findContours(image,contours,hierarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for(int i=0;i<contours.size();++i)
        {
            System.out.println("This is our "+ i + "contour");
        }
    }
}
