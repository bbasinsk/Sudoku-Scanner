package com.example.benbasinski.opencvtest;

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

import com.example.benbasinski.opencvtest.model.ImageInference;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    private Mat inputImgCol;
    private Mat inputImgGray;
    private Mat resizedGray;

    private Mat imgToReturn;
    private Size inputSize;
    private Point[] corners;

    private CameraBridgeViewBase mOpenCvCameraView;

    private ImageInference imageInference;

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


        final Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                setContentView(R.layout.activity_result);

                //Save Image by Button Click
                Mat cropped = cropMat(resizedGray, corners);

                int[] crop = matToArray(cropped);

                Mat squareCrop = new Mat();
                Imgproc.resize(cropped, squareCrop, new Size(297, 313));

                squareCrop = squareCrop.rowRange(16, (int) squareCrop.size().height);
                //297x297

                Mat[] digitCrops = divideMat(squareCrop);

                int[] predictions = new int[digitCrops.length];

                for (int digitInd = 0; digitInd < digitCrops.length; digitInd++) {
                    // Convert img to array


                    int[] inputIntImgArray = matToArray(digitCrops[digitInd]);
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

//        final Button buttonBack = (Button) findViewById(R.id.buttonBack);
//
//        buttonBack.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                setContentView(R.layout.activity_result);
//            }
//        });

    }

    private Mat cropMat(Mat resizedGray, Point[] corners) {
//        # initialzie a list of coordinates that will be ordered
//    # such that the first entry in the list is the top-left,
//    # the second entry is the top-right, the third is the
//    # bottom-right, and the fourth is the bottom-left

        Mat rect = orderPoints(corners);

//        # obtain a consistent order of the points and unpack them
//    # individually

        Point tl = corners[0];
        Point tr = corners[1];
        Point br = corners[2];
        Point bl = corners[3];



//    # compute the width of the new image, which will be the
//    # maximum distance between bottom-right and bottom-left
//    # x-coordiates or the top-right and top-left x-coordinates
        int widthA = (int) Math.sqrt(Math.pow((br.x - bl.x), 2) + Math.pow((br.y - bl.y), 2));
        int widthB = (int) Math.sqrt(Math.pow((tr.x - tl.x), 2) + Math.pow((tr.y - tl.y), 2));
        int maxWidth = Math.max(widthA, widthB);
//
//    # compute the height of the new image, which will be the
//    # maximum distance between the top-right and bottom-right
//    # y-coordinates or the top-left and bottom-left y-coordinates
//        heightA = np.sqrt(((tr[0] - br[0]) ** 2) + ((tr[1] - br[1]) ** 2))
//        heightB = np.sqrt(((tl[0] - bl[0]) ** 2) + ((tl[1] - bl[1]) ** 2))
//        maxHeight = max(int(heightA), int(heightB))
        Double heightA = Math.sqrt(Math.pow((tr.x - br.x), 2) + Math.pow((tr.y - br.y), 2));
        Double heightB = Math.sqrt(Math.pow((tl.x - bl.x), 2) + Math.pow((tl.y - bl.y), 2));
        int maxHeight = (int) Math.max(heightA, heightB);
//
//    # now that we have the dimensions of the new image, construct
//    # the set of destination points to obtain a "birds eye view",
//    # (i.e. top-down view) of the image, again specifying points
//    # in the top-left, top-right, bottom-right, and bottom-left
//    # order

        double[][] dst = new double[4][2];
        dst[0] = new double[]{0,0};
        dst[1] = new double[]{maxWidth - 1, 0};
        dst[2] = new double[]{maxWidth - 1, maxHeight -1};
        dst[3] = new double[]{0, maxHeight - 1};
        Mat dest = arrayToMat(dst);

//    # compute the perspective transform matrix and then apply it
        Mat M = Imgproc.getPerspectiveTransform(rect, dest);

        Mat cropped = new Mat();
        Imgproc.warpPerspective(resizedGray, cropped, M, new Size(maxWidth, maxHeight));

        return cropped;
    }

    private Mat orderPoints(Point[] corners) {
//    initialzie a list of coordinates that will be ordered
//    such that the first entry in the list is the top-left,
//    the second entry is the top-right, the third is the
//    bottom-right, and the fourth is the bottom-left

        double[][] rect = new double[4][2];

//    # the top-left point will have the smallest sum, whereas
//    # the bottom-right point will have the largest sum

        double[] sums = new double[]{corners[0].x + corners[0].y,
                                     corners[1].x + corners[1].y,
                                     corners[2].x + corners[2].y,
                                     corners[3].x + corners[3].y};
        double maxSum = sums[0];
        double minSum = sums[0];
        int maxInd = 0;
        int minInd = 0;
        for (int i = 0; i < sums.length; i++) {
            if (sums[i] > maxSum) {
                maxSum = sums[i];
                maxInd = i;
            }
            if (sums[i] < minSum) {
                minSum = sums[i];
                minInd = i;
            }
        }

        //top left
        rect[0] = new double[]{corners[minInd].x, corners[minInd].y};
        //bottom right
        rect[2] = new double[]{corners[maxInd].x, corners[maxInd].y};

//    # now, compute the difference between the points, the
//    # top-right point will have the smallest difference,
//    # whereas the bottom-left will have the largest difference


        double[] diff = new double[]{corners[0].y - corners[0].x,
                corners[1].y - corners[1].x,
                corners[2].y - corners[2].x,
                corners[3].y - corners[3].x};
        double maxDiff = diff[0];
        double minDiff = diff[0];
        int maxIndD = 0;
        int minIndD = 0;
        for (int i = 0; i < diff.length; i++) {
            if (diff[i] > maxDiff) {
                maxDiff = diff[i];
                maxIndD = i;
            }
            if (diff[i] < minDiff) {
                minDiff = diff[i];
                minIndD = i;
            }
        }

        // top right
        rect[1] = new double[]{corners[minIndD].x, corners[minIndD].y};
        // bottom left
        rect[3] = new double[]{corners[maxIndD].x, corners[maxIndD].y};


//    # return the ordered coordinates
        Mat toRet = arrayToMat(rect);
        return toRet;
    }

    private Mat[] divideMat(Mat squareCropped) {
        Mat[] digitMats = new Mat[81];

//        int height = squareCropped.height()/9;
//        int width = squareCropped.width()/9;

        int height = 33;
        int width = 33;


        for (int i = 0; i < 9; i++) { //rows
            for (int j = 0; j < 9; j++) {  //cols
                int index = i * 9 + j;

                Mat row = squareCropped.rowRange(i*height, i*height + height);
                Mat digit = row.colRange(j*width, j*width + width);

                digitMats[index] = digit;
            }
        }

        return digitMats;
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
        inputSize = inputImgCol.size();

        //get grey img
        Imgproc.cvtColor(inputImgCol, inputImgGray, Imgproc.COLOR_BGRA2GRAY);


        Imgproc.resize(inputImgGray, resizedGray, new Size(600, 400));
        Imgproc.resize(inputImgCol, resizedCol, new Size(600, 400));


        Imgproc.GaussianBlur(resizedGray, blurred, new Size(5, 5) ,0 ,0);


        Imgproc.adaptiveThreshold(blurred, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 2);


        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //find corners of biggest rectangle in image (sudoku puzzle)
        corners = findCorners(contours);


        // Apply lines around the puzzle
        if (corners[0] == null || corners[1] == null || corners[2] == null || corners[3] == null) {
            Log.d(TAG, "No sudoku puzzle found");
        } else {
            Imgproc.line(resizedCol, corners[0], corners[1], new Scalar(0,255,0), 3);
            Imgproc.line(resizedCol, corners[1], corners[2], new Scalar(0,255,0), 3);
            Imgproc.line(resizedCol, corners[2], corners[3], new Scalar(0,255,0), 3);
            Imgproc.line(resizedCol, corners[3], corners[0], new Scalar(0,255,0), 3);
        }

//        Mat imgToReturn = new Mat();
        Imgproc.resize(resizedCol, imgToReturn, inputSize);

        return imgToReturn;
    }

    private Point[] findCorners(List<MatOfPoint> contours) {
        // look for the largest square in image
        Double maxArea = -99999.9;
        MatOfPoint2f biggest = new MatOfPoint2f();

        for (int i = 0; i < contours.size(); i++) {
            Double area = Imgproc.contourArea(contours.get(i));
            if (area > 100) {
                MatOfPoint2f  cont2f = new MatOfPoint2f(contours.get(i).toArray());

                Double peri = Imgproc.arcLength(cont2f, true);

                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(cont2f, approx, 0.02 * peri, true);

                if (area > maxArea && approx.toArray().length == 4) {

                    biggest = approx;
                    maxArea = area;
                }
            }
        }

        Mat square = biggest;
        // Init corners
        Point topLeft = null;
        Point topRight = null;
        Point botLeft = null;
        Point botRight = null;

        // calculate the center of the square
        try {
            Moments M = Imgproc.moments(square);
            int cx = (int) M.get_m10() / (int) M.get_m00();
            int cy = (int) M.get_m01() / (int) M.get_m00();



            for (int i = 0; i < 4; i++) {
                // calculate the difference between the center
                // of the square and the current point

                Point p = biggest.toArray()[i];
                double dx = p.x - cx;
                double dy = p.y - cy;

                if (dx < 0 && dy < 0) {
                    topLeft = new Point(p.x, p.y);
                } else if (dx > 0 && 0 > dy) {
                    topRight = new Point(p.x, p.y);
                } else if (dx > 0 && dy > 0) {
                    botRight = new Point(p.x, p.y);
                } else if (dx < 0 && 0 < dy) {
                    botLeft = new Point(p.x, p.y);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Can't find puzzle");
        }
        corners = new Point[4];

        // the four corners from top left going clockwise
        corners[0] = topLeft;
        corners[1] = topRight;
        corners[2] = botRight;
        corners[3] = botLeft;
        return corners;
    }

    private Mat arrayToMat(double[][] array) {
        int rows = array.length;
        int cols = array[0].length;
        Mat toRet = new Mat(rows, cols, CvType.CV_32FC1);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                toRet.put(r, c, array[r][c]);
            }
        }
        return toRet;
    }


    private int[] matToArray(Mat digitImg) {
//        String sdcard = Environment.getExternalStorageDirectory().getPath();
//
//        File imgFile = new File(sdcard + "/digit2test.jpg");

        int width = (int) digitImg.size().width;
        int height = (int) digitImg.size().height;
        int[] intArray = new int[width*height];

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                intArray[r*width + c] = (int) digitImg.get(r, c)[0];
            }
        }

        return intArray;

//        if(imgFile.exists()){
//            Bitmap digit = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//            int x = digit.getWidth();
//            int y = digit.getHeight();
//            int[] intArray = new int[x * y];
//            digit.getPixels(intArray, 0, x, 0, 0, x, y);
//
//            for (int i = 0; i < x * y; i++) {
//                int p = intArray[i];
//
//                int newVal = p & 0xff;
//                intArray[i] = newVal;
//            }
//
//            return intArray;
//        } else {
//            Log.d("LOADING_TEST_FILE", "File not found");
//            return new int[0];
//        }
    }
}
