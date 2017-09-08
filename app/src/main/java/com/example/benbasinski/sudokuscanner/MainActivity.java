package com.example.benbasinski.sudokuscanner;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.benbasinski.sudokuscanner.image.ImageHelper;
import com.example.benbasinski.sudokuscanner.model.ImageInference;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    private Mat inputImgCol;
    private Mat inputImgGray;
    private Mat resizedGray;

    private Mat imgToReturn;
    private Point[] corners;

    private CameraBridgeViewBase mOpenCvCameraView;

    private ImageInference imageInference;
    private ImageHelper imageHelper;

    static {
        if(!OpenCVLoader.initDebug()) {
            Log.d("MAIN", "OPENCV NOT LOADED");
        } else {
            Log.d("MAIN", "OPENCV LOADED!!!!");
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        // Set up camera
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        //set up the inference model
        imageInference = new ImageInference(getApplicationContext());

        imageHelper = new ImageHelper();


        final Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                setContentView(R.layout.activity_result);

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
                        textViews[i].setText(predictions[i] + "");
                    } catch (Exception e) {
                        Log.d("EXCEPTION", e.toString());
                    }

                }

            }
        });

    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        inputImgGray = new Mat();
        inputImgCol = new Mat();
        imgToReturn = new Mat();
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //Garbage collect
        System.gc();

        //Init mats
        Mat resizedCol = new Mat();
        Mat blurred = new Mat();
        Mat thresh = new Mat();
        Mat hierarchy = new Mat();
        inputImgGray = new Mat();
        resizedGray = new Mat();

        //get input frame
        inputImgCol = inputFrame.rgba();

        //get input size
        Size inputSize = inputImgCol.size();

        //get grey img
        Imgproc.cvtColor(inputImgCol, inputImgGray, Imgproc.COLOR_BGRA2GRAY);


        Imgproc.resize(inputImgGray, resizedGray, new Size(600, 400));
        Imgproc.resize(inputImgCol, resizedCol, new Size(600, 400));


        Imgproc.GaussianBlur(resizedGray, blurred, new Size(5, 5) ,0 ,0);


        Imgproc.adaptiveThreshold(blurred, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 2);


        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //find corners of biggest rectangle in image (sudoku puzzle)
        corners = imageHelper.findCorners(contours);


        // Apply lines around the puzzle
        if (corners[0] == null || corners[1] == null || corners[2] == null || corners[3] == null) {
            Log.d(TAG, "No sudoku puzzle found");
        } else {
            Imgproc.line(resizedCol, corners[0], corners[1], new Scalar(0,255,0), 3);
            Imgproc.line(resizedCol, corners[1], corners[2], new Scalar(0,255,0), 3);
            Imgproc.line(resizedCol, corners[2], corners[3], new Scalar(0,255,0), 3);
            Imgproc.line(resizedCol, corners[3], corners[0], new Scalar(0,255,0), 3);
        }

        //   Mat imgToReturn = new Mat();
        Imgproc.resize(resizedCol, imgToReturn, inputSize);

        resizedCol.release();
        blurred.release();
        thresh.release();
        hierarchy.release();

        //rotate
        return imageHelper.rotateMat(imgToReturn);
    }


}
