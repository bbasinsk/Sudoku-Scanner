package com.example.benbasinski.sudokuscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
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
    int[] predictions;

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

        predictions = new int[digitCrops.length];
        double[] confident = new double[digitCrops.length];

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

            confident[digitInd] = highestConfidence;

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
        Mat toShow = squareCrop;
        Bitmap bm = Bitmap.createBitmap(toShow.cols(), toShow.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(toShow, bm);

        // find the imageview and draw it!
        ImageView sudokuImg = (ImageView) findViewById(R.id.sudokuImg);
        sudokuImg.setImageBitmap(bm);
        sudokuImg.setAlpha(0.0f);


        EditText[] textViews = new EditText[81];

        for (int i = 0; i < predictions.length; i++) {
            String name = "digit"+(i+1);
            int id = getResources().getIdentifier(name, "id", getPackageName());
            textViews[i] = (EditText) findViewById(id);
            try {
                textViews[i].setText(String.valueOf(predictions[i]));
            } catch (Exception e) {
                Log.d("EXCEPTION", e.toString());
            }
            if (confident[i] < 0.95) {
                textViews[i].setBackgroundResource(R.color.colorAccent);
                textViews[i].setAlpha(0.3f);
            }

        }

        Button backButton = (Button) findViewById(R.id.buttonBack);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        Button showPuzzle = (Button) findViewById(R.id.buttonShowImg);
        showPuzzle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ImageView sudokuImg = (ImageView) findViewById(R.id.sudokuImg);
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    sudokuImg.setAlpha(0.9f);
                    return true;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    sudokuImg.setAlpha(0.0f);
                    return true;
                }
                return false;
            }
        });

    }

    public void getSolution(View view) {
        int[] digits = new int[81];
        EditText[] textViews = new EditText[81];
        for (int i = 0; i < predictions.length; i++) {
            String name = "digit"+(i+1);
            int id = getResources().getIdentifier(name, "id", getPackageName());
            textViews[i] = (EditText) findViewById(id);
            digits[i] = Integer.valueOf(textViews[i].getText().toString());
        }

        Intent intent = new Intent(InferenceActivity.this, SolutionActivity.class);
        Bundle extras = new Bundle();


        extras.putIntArray("DIGITS", digits);
        intent.putExtras(extras);
        startActivity(intent);
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
