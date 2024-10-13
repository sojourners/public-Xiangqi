package com.sojourners.chess.yolo;

import java.util.ArrayList;
import java.util.List;

public class Yolov11Model extends YoloModel {

    List<DetectResult> processOutput(float[] output, float rate) {
        List<DetectResult> list = new ArrayList<>();

        int sizeClasses = labels.length;
        int stride = 4 + sizeClasses;
        int size = output.length / stride;

        for(int i = 0; i < size; ++i) {
            int indexBase = i * stride;
            float maxClass = 0.0F;
            int maxIndex = 0;

            for(int c = 0; c < sizeClasses; ++c) {
                if (output[reshape(indexBase + c + 4, stride, size)] > maxClass) {
                    maxClass = output[reshape(indexBase + c + 4, stride, size)];
                    maxIndex = c;
                }
            }

            float score = maxClass;
            if (score > CONFIDENCE) {
                float xPos = output[reshape(indexBase, stride, size)];
                float yPos = output[reshape(indexBase + 1, stride, size)];
                float w = output[reshape(indexBase + 2, stride, size)];
                float h = output[reshape(indexBase + 3, stride, size)];
                Rectangle rect = new Rectangle(xPos / rate, yPos / rate, w / rate, h / rate);
                list.add(new DetectResult(labels[maxIndex], rect, score));
            }
        }

        return nms(list);
    }

    private int reshape(int i, int stride, int size) {
        int n = i / stride;
        int m = i % stride;
        return n + m * size;
    }
}
