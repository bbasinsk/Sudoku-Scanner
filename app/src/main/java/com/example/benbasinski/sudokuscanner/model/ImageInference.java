package com.example.benbasinski.sudokuscanner.model;

import android.content.Context;
import android.content.res.AssetManager;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class ImageInference {
    static {
        System.loadLibrary("tensorflow_inference");
    }

    private static ImageInference imageInferenceInstance;
    private TensorFlowInferenceInterface inferenceInterface;
    private static final String MODEL_FILE = "file:///android_asset/opt_digits_w_drop.pb";
    private static final String INPUT_NODE = "conv2d_1_input";
    private static final String[] OUTPUT_NODES = {"dense_3/Softmax"};
    private static final String OUTPUT_NODE = "dense_3/Softmax";
    private static final long[] INPUT_SIZE = {1, 33, 33, 1};
    private static final int OUTPUT_SIZE = 10;

    public static ImageInference getInstance(final Context context)
    {
        if (imageInferenceInstance == null)
        {
            imageInferenceInstance = new ImageInference(context);
        }
        return imageInferenceInstance;
    }

    public ImageInference(final Context context) {
        AssetManager assetManager = context.getAssets();
        inferenceInterface = new TensorFlowInferenceInterface(assetManager, MODEL_FILE);
    }

    public float[] getImageProb(float[] input_img)
    {
        float[] result = new float[OUTPUT_SIZE];    // 10 choices
        inferenceInterface.feed(INPUT_NODE, input_img, INPUT_SIZE);
        inferenceInterface.run(OUTPUT_NODES);
        inferenceInterface.fetch(OUTPUT_NODE,result);

        return result;
    }
}