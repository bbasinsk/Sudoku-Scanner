package com.example.benbasinski.sudokuscanner;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.example.benbasinski.sudokuscanner.image.ImageHelper;
import com.example.benbasinski.sudokuscanner.model.ImageInference;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraPreviewActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

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

    public void getInference(View view) {
        Intent intent = new Intent(CameraPreviewActivity.this, InferenceActivity.class);
        Bundle extras = new Bundle();

        Mat grayCrop = new Mat(resizedGray.rows(), resizedGray.cols(), CvType.CV_8UC1);
        resizedGray.convertTo(grayCrop, CvType.CV_8UC1);

        Bitmap bm = Bitmap.createBitmap(grayCrop.cols(), grayCrop.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(grayCrop, bm);
        String path = saveToInternalStorage(bm);

        double[] c = new double[8];
        for (int i = 0; i < 4; i++) {
            c[2*i] = corners[i].x;
            c[2*i+1] = corners[i].y;
        }

        extras.putString("IMG_PATH", path);
        extras.putDoubleArray("CORNERS", c);
        intent.putExtras(extras);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        // Set up camera
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera_preview);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
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

        ImageHelper imageHelper = new ImageHelper();

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
        return imgToReturn;
    }

    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"img.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }
}
