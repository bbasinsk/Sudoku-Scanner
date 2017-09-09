package com.example.benbasinski.sudokuscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.benbasinski.sudokuscanner.image.ImageHelper;
import com.example.benbasinski.sudokuscanner.model.ImageInference;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class InferenceActivity extends AppCompatActivity {

    private static final String TAG = "INFERENCE";

    ImageHelper imageHelper;
    ImageInference imageInference;
    Mat resizedGray;
    Point[] corners;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inference);


        Bundle extras = getIntent().getExtras();


        //Convert extra resizedBm to usable Mat
        String path = extras.getString("IMG_PATH");
        Bitmap resizedBm = loadImageFromStorage(path);
        resizedGray = new Mat();
        Utils.bitmapToMat(resizedBm, resizedGray);


        //Convert extra to usable points
        double[] c = extras.getDoubleArray("CORNERS");
        corners = new Point[4];
        if (c != null && c.length >= 8) {
            for (int i = 0; i < corners.length; i++) {
                corners[i] = new Point(c[i*2], c[i*2+1]);
            }
        }


        //set up the inference model
        imageInference = new ImageInference(getApplicationContext());
        imageHelper = new ImageHelper();

        //Save Image by Button Click
        Mat cropped = imageHelper.cropMat(resizedGray, corners);

        Mat squareCrop = new Mat();
        Imgproc.resize(cropped, squareCrop, new Size(297, 313));

        squareCrop = squareCrop.rowRange(16, (int) squareCrop.size().height);
        //297x297

        Mat[] digitCrops = imageHelper.divideMat(squareCrop);

        int[] predictions = new int[digitCrops.length];

        for (int digitInd = 0; digitInd < digitCrops.length; digitInd++) {
            // Convert img to array


            int[] inputIntImgArray = imageHelper.matToArray(digitCrops[digitInd]);
            float[] inputFloatImgArray = new float[inputIntImgArray.length];
            for (int i = 0; i < inputIntImgArray.length; i++) {
                inputFloatImgArray[i] = (float) inputIntImgArray[i] / 255;
            }

            float[] results = imageInference.getImageProb(inputFloatImgArray);

            float highestConfidence = 0;
            for (int i = 0; i < results.length; i++) {
                if (results[i] > highestConfidence) {
                    predictions[digitInd] = i;
                    highestConfidence = results[i];
                }
            }

            Log.d("CLASSIFIER_RESULTS - " + digitInd,
                    "\n0: " + results[0] +
                            "\n1: " + results[1] +
                            "\n2: " + results[2] +
                            "\n3: " + results[3] +
                            "\n4: " + results[4] +
                            "\n5: " + results[5] +
                            "\n6: " + results[6] +
                            "\n7: " + results[7] +
                            "\n8: " + results[8] +
                            "\n9: " + results[9]);
        }

        // convert to bitmap:
        Mat toShow = digitCrops[2];
        Bitmap bm = Bitmap.createBitmap(toShow.cols(), toShow.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(toShow, bm);

        // find the imageview and draw it!
        ImageView iv = (ImageView) findViewById(R.id.imageView1);
        iv.setImageBitmap(bm);


        TextView[] textViews = new TextView[81];

        for (int i = 0; i < predictions.length; i++) {
            String name = "textView"+(i+1);
            int id = getResources().getIdentifier(name, "id", getPackageName());
            textViews[i] = (TextView) findViewById(id);
            try {
                textViews[i].setText(String.valueOf(predictions[i]));
            } catch (Exception e) {
                Log.d("EXCEPTION", e.toString());
            }

        }


        Button backButton = (Button) findViewById(R.id.buttonBack);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent backIntent = new Intent(InferenceActivity.this, CameraPreviewActivity.class);
                startActivity(backIntent);
            }
        });


    }

    private Bitmap loadImageFromStorage(String path)
    {
        try {
            File f=new File(path, "img.jpg");
            return BitmapFactory.decodeStream(new FileInputStream(f));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
    }

}