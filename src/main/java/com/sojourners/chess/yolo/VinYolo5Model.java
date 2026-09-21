package com.sojourners.chess.yolo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 复用 VinXiangQi（开源 YOLOv5 连线工具）训练的模型，用于识别 JJ象棋等深色棋盘。
 * 模型类别顺序与 Yolo5Model.labels 完全一致：n,b,a,k,r,c,p,R,N,A,K,B,C,P,0
 * 输入为标准 YOLOv5 letterbox：缩放后居中填充 114 灰色。
 */
public class VinYolo5Model extends Yolo5Model {

    // JJ象棋等深色棋盘识别率略低，阈值下调避免整帧漏检/漏子
    float BOARD_CONFIDENCE = 0.5f;
    float PIECE_CONFIDENCE = 0.5f;

    private float padX;
    private float padY;

    @Override
    public String getModelPath() {
        return "model/yolo5-vin.onnx";
    }

    @Override
    protected float getBoardConfidence() {
        return BOARD_CONFIDENCE;
    }

    @Override
    protected float getPieceConfidence() {
        return PIECE_CONFIDENCE;
    }

    @Override
    protected float getRetryPieceConfidence() {
        return 0.4f;
    }

    @Override
    protected float getRecoveryPieceConfidence() {
        return 0.35f;
    }

    @Override
    float[][][] processInput(BufferedImage image, float rate) {
        int destW = Math.round(image.getWidth() * rate);
        int destH = Math.round(image.getHeight() * rate);
        padX = (SIZE - destW) / 2f;
        padY = (SIZE - destH) / 2f;
        int leftMargin = (int) padX, topMargin = (int) padY;
        BufferedImage resizedImage = new BufferedImage(destW, destH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(image, 0, 0, destW, destH, null);
        g2d.dispose();
        float[][][] arr = new float[3][SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (i >= topMargin && j >= leftMargin && i < topMargin + destH && j < leftMargin + destW) {
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

    @Override
    List<DetectResult> processOutput(float[] output, BufferedImage img, float rate, float boardConf, float pieceConf) {
        List<DetectResult> list = new ArrayList<>();
        int sizeClasses = labels.length;
        int stride = 5 + sizeClasses;
        int size = output.length / stride;
        for (int i = 0; i < size; ++i) {
            int indexBase = i * stride;
            float maxClass = 0.0F;
            int maxIndex = 0;
            for (int c = 0; c < sizeClasses; ++c) {
                if (output[indexBase + c + 5] > maxClass) {
                    maxClass = output[indexBase + c + 5];
                    maxIndex = c;
                }
            }
            float score = maxClass * output[indexBase + 4];
            if (score > (labels[maxIndex] == '0' ? boardConf : pieceConf)) {
                float xPos = output[indexBase];
                float yPos = output[indexBase + 1];
                float w = output[indexBase + 2];
                float h = output[indexBase + 3];
                Rectangle rect = new Rectangle((xPos - padX) / rate, (yPos - padY) / rate, w / rate, h / rate);
                list.add(new DetectResult(labels[maxIndex], rect, score));
            }
        }
        return nms(list);
    }
}
