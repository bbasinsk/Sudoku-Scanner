package com.example.benbasinski.sudokuscanner.image;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.List;

public class ImageHelper {

    private final String TAG = "ImageHelper";

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


    public int[] matToArray(Mat digitImg) {
        int width = (int) digitImg.size().width;
        int height = (int) digitImg.size().height;
        int[] intArray = new int[width*height];

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                intArray[r*width + c] = (int) digitImg.get(r, c)[0];
            }
        }

        return intArray;
    }

    public Point[] findCorners(List<MatOfPoint> contours) {
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
        Point[] corners = new Point[4];

        // the four corners from top left going clockwise
        corners[0] = topLeft;
        corners[1] = topRight;
        corners[2] = botRight;
        corners[3] = botLeft;
        return corners;
    }

    public Mat[] divideMat(Mat squareCropped) {
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
        return arrayToMat(rect);
    }

    public Mat cropMat(Mat resizedGray, Point[] corners) {
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

        //    # compute the height of the new image, which will be the
        //    # maximum distance between the top-right and bottom-right
        //    # y-coordinates or the top-left and bottom-left y-coordinates
        //        heightA = np.sqrt(((tr[0] - br[0]) ** 2) + ((tr[1] - br[1]) ** 2))
        //        heightB = np.sqrt(((tl[0] - bl[0]) ** 2) + ((tl[1] - bl[1]) ** 2))
        //        maxHeight = max(int(heightA), int(heightB))
        Double heightA = Math.sqrt(Math.pow((tr.x - br.x), 2) + Math.pow((tr.y - br.y), 2));
        Double heightB = Math.sqrt(Math.pow((tl.x - bl.x), 2) + Math.pow((tl.y - bl.y), 2));
        int maxHeight = (int) Math.max(heightA, heightB);

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
}
