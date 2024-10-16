package com.sojourners.chess.yolo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Yolo11Model extends Yolo5Model {

    float CONFIDENCE = 0.5f;

    @Override
    public String getModelPath() {
        return "model/yolov11n.onnx";
    }

    float[][][] processInput(BufferedImage image, float rate) {

        int destW = Math.round(image.getWidth() * rate);
        int destH = Math.round(image.getHeight() * rate);
        BufferedImage resizedImage = new BufferedImage(destW, destH, image.getType());
        Graphics2D g2d = resizedImage.createGraphics();
        // 改进的绘制参数设置
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); // 或者 VALUE_INTERPOLATION_BICUBIC
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(image, 0, 0, destW, destH, null);
//        g2d.drawImage(image.getScaledInstance(destW, destH, Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose();

        int resizedWidth = resizedImage.getWidth();
        int resizedHeight = resizedImage.getHeight();
        int leftMargin = (SIZE - resizedWidth) / 2, topMargin = (SIZE - resizedHeight) / 2;

        float[][][] arr = new float[3][SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (i >= topMargin && j >= leftMargin && i < topMargin + resizedHeight
                        && j < leftMargin + resizedWidth) {
                    int rgb = resizedImage.getRGB(j - leftMargin, i - topMargin);
                    Color color = new Color(rgb, true);
                    arr[0][i][j] = color.getRed() / 255.0f;
                    arr[1][i][j] = color.getGreen() / 255.0f;
                    arr[2][i][j] = color.getBlue() / 255.0f;
                } else {
                    arr[0][i][j] = 114.0f / 255;
                    arr[1][i][j] = 114.0f / 255;
                    arr[2][i][j] = 114.0f / 255;
                }
            }
        }
        return arr;
    }

    List<DetectResult> processOutput(float[] output, BufferedImage img, float rate) {
        List<DetectResult> list = new ArrayList<>();

        float xPadding = (SIZE - img.getWidth() * rate) / 2;
        float yPadding = (SIZE - img.getHeight() * rate) / 2;

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
                Rectangle rect = new Rectangle((xPos - xPadding) / rate, (yPos - yPadding) / rate, w / rate, h / rate);
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
