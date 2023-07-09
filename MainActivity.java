package com.example.colordetectionapp;
import android.content.res.AssetManager;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Size;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import android.widget.LinearLayout;
import java.util.HashMap;
import java.util.Map;
import android.os.AsyncTask;

public class MainActivity extends AppCompatActivity {
    private static final int request_code = 101;
    private List<ColorHSV> colorDataset;
    private String closestHueColor;


    // ...


    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    request_code);
        }
    }

    ImageView imageview;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.colordetectionapp.R.layout.activity1);

        loadColorDataset();
        if (!OpenCVLoader.initDebug()) {
            // OpenCV initialization failed
            Log.e("MainActivity", "OpenCV not loaded");
        } else {
            // OpenCV successfully loaded
            Log.d("MainActivity", "OpenCV loaded");
        }


        //colorDetector = new ColorDetector(colorList);
        button = findViewById(R.id.camera);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, request_code);

            }
        });


    }

    long startTime = System.currentTimeMillis();

    private void loadColorDataset() {
        colorDataset = new ArrayList<>();

        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("colors.csv");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String line;
            boolean isFirstLine = true;
            while ((line = bufferedReader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip the header row
                }
                String[] values = line.split(",");

                String colorName = values[1];
                String hexValue = values[2];
                int red = Integer.parseInt(values[3]);
                int green = Integer.parseInt(values[4]);
                int blue = Integer.parseInt(values[5]);
                float[] hsv = new float[3];
                Color.RGBToHSV(red, green, blue, hsv);

                Scalar hsvScalar = new Scalar(hsv[0], hsv[1], hsv[2]);
                ColorHSV color = new ColorHSV(colorName, hsvScalar);
                colorDataset.add(color);
            }

            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ColorHSV {
        private String name;
        private Scalar hsv;

        public ColorHSV(String name, Scalar hsv) {
            this.name = name;
            this.hsv = hsv;
        }

        public String getName() {
            return name;
        }

        public Scalar getHSV() {
            return hsv;
        }
    }

    long endTime = System.currentTimeMillis();
    long executionTime = endTime - startTime;

    //Log.d("ExecutionTime", "Execution Time: " + executionTime + " milliseconds");

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setContentView(R.layout.activity2);
        imageview = findViewById(R.id.display);

        Log.d("ColorDetection", "imageview: " + imageview);

        //TextView colorTextView = findViewById(R.id.resultTextView);

        if (requestCode == request_code) {
            Bitmap imgbitmap = (Bitmap) data.getExtras().get("data");
            imageview.setImageBitmap(imgbitmap);
            imageview.invalidate();
            imageview.requestLayout();

            new ColorDetectionTask().execute(imgbitmap);
        }
    }
    private class ColorDetectionTask extends AsyncTask<Bitmap, Void, String> {
        //private List<String> detectedColors = new ArrayList<>();
        private StringBuilder colorsText = new StringBuilder();
        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            Bitmap imgbitmap = bitmaps[0];
//            closestColor = findClosestColors(imgbitmap);
            return findClosestColor(imgbitmap);
        }

        protected void onPostExecute(String closestColor) {

            Log.d("ColorDetection", "Closest color: " + closestColor);
            TextView resultTextView = findViewById(R.id.resultTextView);
            resultTextView.setText("Detected Color:\n" + colorsText.toString().trim());
            resultTextView.invalidate();
            resultTextView.requestLayout();
//


        }

    List<String> detectedColors = new ArrayList<>();

    private String findClosestColor(Bitmap imgbitmap) {
        Log.d("ColorDetection", "findClosestColor method called");
        int width = imgbitmap.getWidth();
        int height = imgbitmap.getHeight();
        double minDistance = Double.MAX_VALUE;
        String closestColor = "";
        double minHueDistance = Double.MAX_VALUE;
        String closestHueColor = "";


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = imgbitmap.getPixel(x, y);

                // Extract HSV values from the pixel
                float[] hsv = new float[3];
                Color.RGBToHSV(Color.red(pixel), Color.green(pixel), Color.blue(pixel), hsv);

                Scalar hsvScalar = new Scalar(hsv[0], hsv[1], hsv[2]);
                // Convert Scalar objects to Mat


                // Find the closest color match
                for (ColorHSV color : colorDataset) {
                    Scalar colorHSV = color.getHSV();
                    double[] hsvValues = colorHSV.val;
                    double hueValue = colorHSV.val[0];
                    double hueDistance = Math.abs(hueValue - hsv[0]);
                    if (hueDistance < minHueDistance) {
                        minHueDistance = hueDistance;
                        closestHueColor = color.getName();
                        Log.d("ColorDetection", "New closest hue color found: " + closestHueColor);
                    }
                }
                for (ColorHSV color : colorDataset) {
                    Scalar colorHSV = color.getHSV();
                    double[] hsvValues = colorHSV.val;
                    Mat colorMat = new Mat(1, 1, CvType.CV_32FC3);
                    colorMat.put(0, 0, hsvValues[0], hsvValues[1], hsvValues[2]);
                    int colorMatRows = colorMat.rows();
                    int colorMatCols = colorMat.cols();
                    //Log.d("ColorDetection", "colorMat size: " + colorMatRows + "x" + colorMatCols);
                    Mat hsvMat = new Mat(1, 1, CvType.CV_32FC3);
                    hsvMat.put(0, 0, hsv[0], hsv[1], hsv[2]);
                    int hsvMatRows = hsvMat.rows();
                    int hsvMatCols = hsvMat.cols();
                   // Log.d("ColorDetection", "hsvMat size: " + hsvMatRows + "x" + hsvMatCols);
                    double distance = Core.norm(hsvMat, colorMat, Core.NORM_L2);

                    if (distance < minDistance) {
                        minDistance = distance;
                       closestColor = color.getName();
                        Log.d("ColorDetection", "New closest color found: " + closestColor);

                        if (!colorsText.toString().contains(closestColor)) {
                            colorsText.append(closestColor).append("\n");
                        }



                    }
                }
//                /
            }
        }

            return closestColor;

    }
}}


// Filter out the colors based on hue distance
//                if (!closestHueColor.equals(closestColor)) {
//                    // The colors do not match based on hue, so reset the closest color
//                    closestColor = "";
//                } else {
//                    for (ColorHSV color : colorDataset) {
//                        Scalar colorHSV = color.getHSV();
//                        double[] hsvValues = colorHSV.val;
//                        Mat colorMat = new Mat(1, 1, CvType.CV_32FC3);
//                        colorMat.put(0, 0, hsvValues[0], hsvValues[1], hsvValues[2]);
//                        int colorMatRows = colorMat.rows();
//                        int colorMatCols = colorMat.cols();
//                        //Log.d("ColorDetection", "colorMat size: " + colorMatRows + "x" + colorMatCols);
//                        Mat hsvMat = new Mat(1, 1, CvType.CV_32FC3);
//                        hsvMat.put(0, 0, hsv[0], hsv[1], hsv[2]);
//                        int hsvMatRows = hsvMat.rows();
//                        int hsvMatCols = hsvMat.cols();
//                        // Colors match based on hue, calculate the distance using HSV values
//                        double distance = Core.norm(hsvMat, colorMat, Core.NORM_L2);
//
//                        if (distance < minDistance) {
//                            minDistance = distance;
//                            closestColor = color.getName();
//                            Log.d("ColorDetection", "New closest color found2: " + closestColor);
//
//
//                        }
//                    }

//                }












