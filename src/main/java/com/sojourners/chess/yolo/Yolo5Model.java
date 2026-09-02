package com.sojourners.chess.yolo;


import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.sojourners.chess.util.XiangqiUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

public class Yolo5Model extends OnnxModel {

    @Override
    public String getModelPath() {
        return "model/middle.onnx";
    }

    /**
     * 寻找棋盘范围，用于后续连线识别
     * @param img
     * @return
     */
    @Override
    public java.awt.Rectangle findBoardPosition(BufferedImage img) {
        try {
            if (img == null) {
                return null;
            }
            // 图像宽高的缩放比例
            List<DetectResult> results = this.predict(img);
            // 寻找棋盘
            java.awt.Rectangle pos = findBoardPosition(results, img);
            if (pos == null) {
                return null;
            }
            // 棋盘范围
            double pieceWidth = pos.width / 8d, pieceHeight = pos.height / 9d;
            pos.x -= pieceWidth * PADDING;
            if (pos.x < 0) {
                pos.x = 0;
            }
            pos.y -= pieceHeight * PADDING;
            if (pos.y < 0) {
                pos.y = 0;
            }
            pos.width += pieceWidth * PADDING * 2;
            if (pos.x + pos.width > img.getWidth()) {
                pos.width = img.getWidth() - pos.x;
            }
            pos.height += pieceHeight * PADDING * 2;
            if (pos.y + pos.height > img.getHeight()) {
                pos.height = img.getHeight() - pos.y;
            }
            return pos;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setBlankBoard(char[][] board) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                board[i][j] = ' ';
            }
        }
    }

    protected float PIECE_CONFIDENCE = 0.4f;

    protected float RETRY_PIECE_CONFIDENCE = 0.35f;

    protected float RECOVERY_PIECE_CONFIDENCE = 0.3f;

    /**
     * 上一次识别成功的棋盘位置，同一局内棋盘位置不变，可复用避免中局偏移歧义
     */
    private java.awt.Rectangle lastBoardPos;

    protected float getBoardConfidence() {
        return CONFIDENCE;
    }

    protected float getPieceConfidence() {
        return CONFIDENCE;
    }

    protected float getRetryPieceConfidence() {
        return RETRY_PIECE_CONFIDENCE;
    }

    protected float getRecoveryPieceConfidence() {
        return RECOVERY_PIECE_CONFIDENCE;
    }

    private java.awt.Rectangle findBoardPosition(List<DetectResult> results, BufferedImage img) {
        // 候选1：模型检测到的所有棋盘框（按面积从大到小），优先返回局面校验通过的位置
        List<java.awt.Rectangle> modelRects = findBoardsByModel(results);
        for (java.awt.Rectangle r : modelRects) {
            char[][] tmp = new char[10][9];
            setBlankBoard(tmp);
            fillBoard(results, r, tmp);
            if (XiangqiUtils.validateChessBoard(tmp)) {
                return r;
            }
        }
        // 候选2：模型未识别出棋盘时，用棋子位置反推棋盘范围（兼容JJ象棋等深色棋盘）
        List<java.awt.Rectangle> pieces = findBoardPositionByPieces(results, img);
        if (pieces != null) {
            for (java.awt.Rectangle r : pieces) {
                char[][] tmp = new char[10][9];
                setBlankBoard(tmp);
                fillBoard(results, r, tmp);
                if (XiangqiUtils.validateChessBoard(tmp)) {
                    return r;
                }
            }
            if (!pieces.isEmpty()) {
                return pieces.get(0);
            }
        }
        if (!modelRects.isEmpty()) {
            return modelRects.get(0);
        }
        return null;
    }

    /**
     * 模型检测到的所有棋盘框（类别0），按面积从大到小排序
     */
    private List<java.awt.Rectangle> findBoardsByModel(List<DetectResult> results) {
        List<java.awt.Rectangle> list = new ArrayList<>();
        for (DetectResult obj : results) {
            char label = obj.label;
            Rectangle bound = obj.rect;
            if (label == '0') {
                int w = (int) (bound.getWidth()), h = (int) (bound.getHeight());
                list.add(new java.awt.Rectangle((int) (bound.getX() - w / 2d),
                        (int) (bound.getY() - h / 2d), w, h));
            }
        }
        list.sort((a, b) -> Long.compare((long) b.width * b.height, (long) a.width * a.height));
        return list;
    }

    /**
     * 通过棋子位置推算棋盘范围（兼容模型未识别棋盘的情况，例如JJ象棋深色棋盘）
     */
    private List<java.awt.Rectangle> findBoardPositionByPieces(List<DetectResult> results, BufferedImage img) {
        if (results == null || results.size() < 8) {
            return null;
        }
        float tol = Math.max(6f, 0.015f * Math.max(img.getWidth(), img.getHeight()));
        List<Float> xs = new ArrayList<>();
        List<Float> ys = new ArrayList<>();
        for (DetectResult obj : results) {
            if (obj.label == '0') {
                continue;
            }
            xs.add(obj.rect.x);
            ys.add(obj.rect.y);
        }
        if (xs.size() < 8) {
            return null;
        }
        List<Float> ws = new ArrayList<>();
        List<Float> hs = new ArrayList<>();
        for (DetectResult obj : results) {
            if (obj.label == '0') {
                continue;
            }
            ws.add(obj.rect.width);
            hs.add(obj.rect.height);
        }
        GridFit fx = gridFit(xs, tol, median(ws), 8);
        GridFit fy = gridFit(ys, tol, median(hs), 9);
        if (fx == null || fy == null) {
            return null;
        }
        List<java.awt.Rectangle> rects = new ArrayList<>();
        int maxOx = 8 - fx.lastIndex;
        int maxOy = 9 - fy.lastIndex;
        for (int oy = 0; oy <= maxOy; oy++) {
            for (int ox = 0; ox <= maxOx; ox++) {
                int x = (int) (fx.base - (ox + 0.5f) * fx.unit);
                int y = (int) (fy.base - (oy + 0.5f) * fy.unit);
                int w = (int) (fx.unit * 9f);
                int h = (int) (fy.unit * 10f);
                if (w < 50 || h < 50 || w > img.getWidth() * 2 || h > img.getHeight() * 2) {
                    continue;
                }
                rects.add(new java.awt.Rectangle(x, y, w, h));
            }
        }
        return rects;
    }

    private GridFit gridFit(List<Float> vals, float tol, float pieceSize, int maxIndex) {
        Collections.sort(vals);
        List<Float> clusters = new ArrayList<>();
        for (float v : vals) {
            if (!clusters.isEmpty() && v - clusters.get(clusters.size() - 1) <= tol) {
                clusters.set(clusters.size() - 1, (clusters.get(clusters.size() - 1) + v) / 2f);
            } else {
                clusters.add(v);
            }
        }
        if (clusters.size() < 3) {
            return null;
        }
        // 在所有两两距离的整数分位中寻找最优单位间距，
        // 同时以棋子大小作为先验，避免隔行/隔列时把间距算成多倍
        float bestUnit = 0f;
        double bestScore = Double.MAX_VALUE;
        int n = clusters.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                float d = clusters.get(j) - clusters.get(i);
                for (int k = 1; k <= 9; k++) {
                    float u = d / k;
                    if (u < 5f || u > 200f) {
                        continue;
                    }
                    double score = 0;
                    for (int p = 1; p < n; p++) {
                        float diff = clusters.get(p) - clusters.get(p - 1);
                        int kk = Math.max(1, Math.round(diff / u));
                        score += Math.abs(diff - kk * u);
                    }
                    score += Math.abs(u - pieceSize);
                    if (score < bestScore) {
                        bestScore = score;
                        bestUnit = u;
                    }
                }
            }
        }
        if (bestUnit <= 0) {
            return null;
        }
        float unit = bestUnit;
        int[] idx = new int[clusters.size()];
        idx[0] = 0;
        for (int i = 1; i < clusters.size(); i++) {
            float d = clusters.get(i) - clusters.get(i - 1);
            idx[i] = idx[i - 1] + Math.max(1, Math.round(d / unit));
        }
        if (idx[clusters.size() - 1] > maxIndex) {
            return null;
        }
        float base = 0f;
        for (int i = 0; i < clusters.size(); i++) {
            base += clusters.get(i) - idx[i] * unit;
        }
        base /= clusters.size();
        return new GridFit(unit, base, idx[idx.length - 1]);
    }

    private float median(List<Float> vals) {
        if (vals == null || vals.isEmpty()) {
            return 60f;
        }
        Collections.sort(vals);
        int mid = vals.size() / 2;
        if (vals.size() % 2 == 1) {
            return vals.get(mid);
        }
        return (vals.get(mid - 1) + vals.get(mid)) / 2f;
    }

    private static class GridFit {
        float unit;
        float base;
        int lastIndex;
        GridFit(float unit, float base, int lastIndex) {
            this.unit = unit;
            this.base = base;
            this.lastIndex = lastIndex;
        }
    }

    /**
     * 根据图片识别棋子及其位置
     * @param img
     * @return
     */
    @Override
    public boolean findChessBoard(BufferedImage img, char[][] board) {
        try {
            if (img == null) {
                return false;
            }
            // 第一轮：原置信度识别
            List<DetectResult> results = this.predict(img);
            if (fillChessBoard(results, img, board)) {
                return true;
            }
            // 第二轮：降低棋子置信度重新识别（兼容JJ象棋等识别率较低的棋盘）
            List<DetectResult> results2 = this.predict(img, getBoardConfidence(), getRetryPieceConfidence());
            return fillChessBoard(results2, img, board);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean fillChessBoard(List<DetectResult> results, BufferedImage img, char[][] board) {
        // 候选1：同一局内棋盘位置不变，优先复用上一次识别成功的位置，避免中局偏移歧义
        if (lastBoardPos != null && lastBoardPos.x >= 0 && lastBoardPos.y >= 0
                && lastBoardPos.x + lastBoardPos.width <= img.getWidth()
                && lastBoardPos.y + lastBoardPos.height <= img.getHeight()) {
            setBlankBoard(board);
            fillBoard(results, lastBoardPos, board);
            if (XiangqiUtils.validateChessBoard(board)) {
                return true;
            }
        }
        // 候选2：模型检测到的棋盘框（从大到小逐一校验）
        List<java.awt.Rectangle> modelRects = findBoardsByModel(results);
        for (java.awt.Rectangle modelPos : modelRects) {
            setBlankBoard(board);
            fillBoard(results, modelPos, board);
            if (XiangqiUtils.validateChessBoard(board)) {
                lastBoardPos = modelPos;
                return true;
            }
        }
        // 候选3：棋子位置反推棋盘（含偏移搜索）
        List<java.awt.Rectangle> piecePos = findBoardPositionByPieces(results, img);
        if (piecePos != null) {
            for (java.awt.Rectangle r : piecePos) {
                setBlankBoard(board);
                fillBoard(results, r, board);
                if (XiangqiUtils.validateChessBoard(board)) {
                    lastBoardPos = r;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 少子补棋：在已识别出的合法局面基础上，用更低置信度重新识别，
     * 只把漏掉的棋子补进空格，补完后若仍合法且棋子数变多则接受。
     * @return 是否成功补棋
     */
    public boolean completeChessBoard(BufferedImage img, char[][] board) {
        try {
            if (img == null || board == null) {
                return false;
            }
            java.awt.Rectangle pos = lastBoardPos;
            if (pos == null) {
                return false;
            }
            List<DetectResult> results = this.predict(img, getBoardConfidence(), getRecoveryPieceConfidence());
            char[][] merged = new char[10][9];
            for (int i = 0; i < 10; i++) {
                System.arraycopy(board[i], 0, merged[i], 0, 9);
            }
            fillBoard(results, pos, merged, true);
            if (countPieces(merged) > countPieces(board) && XiangqiUtils.validateChessBoard(merged)) {
                for (int i = 0; i < 10; i++) {
                    System.arraycopy(merged[i], 0, board[i], 0, 9);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private int countPieces(char[][] board) {
        int n = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                if (board[i][j] != ' ') {
                    n++;
                }
            }
        }
        return n;
    }

    private void fillBoard(List<DetectResult> results, java.awt.Rectangle boardPos, char[][] board) {
        fillBoard(results, boardPos, board, false);
    }

    private void fillBoard(List<DetectResult> results, java.awt.Rectangle boardPos, char[][] board, boolean onlyEmpty) {
        int pieceWidth = boardPos.width / 8, pieceHeight = boardPos.height / 9;
        // 多棋盘场景：只填充中心点落在当前棋盘框内的棋子，避免把其它棋盘的棋子混入
        java.awt.Rectangle inner = new java.awt.Rectangle(
                boardPos.x - pieceWidth / 2,
                boardPos.y - pieceHeight / 2,
                boardPos.width + pieceWidth,
                boardPos.height + pieceHeight);
        // 再获取每个棋子及其位置
        for (DetectResult obj : results) {
            char label = obj.label;
            Rectangle bound = obj.rect;
            if (label != '0') {
                if (bound.x < inner.x || bound.x > inner.x + inner.width
                        || bound.y < inner.y || bound.y > inner.y + inner.height) {
                    continue;
                }
                int j = (int) ((bound.x - (boardPos.x - pieceWidth / 2)) / pieceWidth);
                int i = (int) ((bound.y - (boardPos.y - pieceHeight / 2)) / pieceHeight);
                if (i < 0 || i > 9 || j < 0 || j > 8) {
                    continue;
                }
                if (onlyEmpty && board[i][j] != ' ') {
                    continue;
                }
                board[i][j] = label;
            }
        }
    }

    private List<DetectResult> predict(BufferedImage image) throws OrtException {
        return predict(image, getBoardConfidence(), getPieceConfidence());
    }

    private List<DetectResult> predict(BufferedImage image, float boardConf, float pieceConf) throws OrtException {

        List<DetectResult> list = null;

        float rate = ((float) SIZE) / Math.max(image.getWidth(), image.getHeight());
        long s = System.currentTimeMillis();
        float[][][] inputData = processInput(image, rate);
        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, new float[][][][]{inputData})) {

            Map<String, OnnxTensor> container = new HashMap<>();
            container.put("images", inputTensor);
            try (OrtSession.Result results = session.run(container)) {

                for (Map.Entry<String, OnnxValue> r : results) {

                    OnnxValue resultValue = r.getValue();
                    OnnxTensor resultTensor = (OnnxTensor) resultValue;
                    float[] output = resultTensor.getFloatBuffer().array();

                    list = processOutput(output, image, rate, boardConf, pieceConf);
                }
            }
        }

//        System.gc();
        System.out.println(System.currentTimeMillis() - s);
        return list;
    }

    float[][][] processInput(BufferedImage image, float rate) {

        int destW = Math.round(image.getWidth() * rate);
        int destH = Math.round(image.getHeight() * rate);
        BufferedImage resizedImage = new BufferedImage(destW, destH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        // 改进的绘制参数设置
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); // 或者 VALUE_INTERPOLATION_BICUBIC
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(image, 0, 0, destW, destH, null);
        g2d.dispose();

        int resizedWidth = resizedImage.getWidth();
        int resizedHeight = resizedImage.getHeight();
        int leftMargin = 0, topMargin = 0;

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

    List<DetectResult> nms(List<DetectResult> list) {

        List<DetectResult> results = new ArrayList<>();

        for(int k = 0; k < 15; ++k) {
            PriorityQueue<DetectResult> pq = new PriorityQueue<>(50, (lhs, rhs) -> {
                return Double.compare(rhs.confidence, lhs.confidence);
            });
            Iterator var7 = list.iterator();

            while(var7.hasNext()) {
                DetectResult intermediateResult = (DetectResult) var7.next();
                if (intermediateResult.label == labels[k]) {
                    pq.add(intermediateResult);
                }
            }

            while(pq.size() > 0) {
                DetectResult[] a = new DetectResult[pq.size()];
                DetectResult[] detections = pq.toArray(a);

                results.add(detections[0]);

                pq.clear();

                for(int j = 1; j < detections.length; ++j) {
                    DetectResult detection = detections[j];
                    Rectangle location = detection.rect;
                    if (this.boxIou(detections[0].rect, location) < 0.45d) {
                        pq.add(detection);
                    }
                }
            }
        }

        return results;
    }
    private double boxIou(Rectangle a, Rectangle b) {
        return this.boxIntersection(a, b) / this.boxUnion(a, b);
    }

    private double boxUnion(Rectangle a, Rectangle b) {
        double i = this.boxIntersection(a, b);
        return a.getWidth() * a.getHeight() + b.getWidth() * b.getHeight() - i;
    }
    private double boxIntersection(Rectangle a, Rectangle b) {
        double w = this.overlap(a.getX(), a.getWidth(), b.getX(), b.getWidth());
        double h = this.overlap(a.getY(), a.getHeight(), b.getY(), b.getHeight());
        return w >= 0.0D && h >= 0.0D ? w * h : 0.0D;
    }
    private double overlap(double x1, double w1, double x2, double w2) {
        double l1 = x1 - w1 / 2.0D;
        double l2 = x2 - w2 / 2.0D;
        double left = Math.max(l1, l2);
        double r1 = x1 + w1 / 2.0D;
        double r2 = x2 + w2 / 2.0D;
        double right = Math.min(r1, r2);
        return right - left;
    }

    List<DetectResult> processOutput(float[] output, BufferedImage img, float rate) {
        return processOutput(output, img, rate, getBoardConfidence(), getPieceConfidence());
    }

    List<DetectResult> processOutput(float[] output, BufferedImage img, float rate, float boardConf, float pieceConf) {
        List<DetectResult> list = new ArrayList<>();

        int sizeClasses = labels.length;
        int stride = 5 + sizeClasses;
        int size = output.length / stride;

        for(int i = 0; i < size; ++i) {
            int indexBase = i * stride;
            float maxClass = 0.0F;
            int maxIndex = 0;

            for(int c = 0; c < sizeClasses; ++c) {
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
                Rectangle rect = new Rectangle(xPos / rate, yPos / rate, w / rate, h / rate);
                list.add(new DetectResult(labels[maxIndex], rect, score));
            }
        }

        return nms(list);
    }

    class DetectResult {
        char label;
        Rectangle rect;
        float confidence;
        public DetectResult(char label, Rectangle rect, float confidence) {
            this.label = label;
            this.rect = rect;
            this.confidence = confidence;
        }

        public char getLabel() {
            return label;
        }

        public void setLabel(char label) {
            this.label = label;
        }

        public Rectangle getRect() {
            return rect;
        }

        public void setRect(Rectangle rect) {
            this.rect = rect;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }
    }

    class Rectangle {
        float x;
        float y;
        float width;
        float height;

        public Rectangle(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public float getWidth() {
            return width;
        }

        public void setWidth(float width) {
            this.width = width;
        }

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }

        @Override
        public String toString() {
            return "Rectangle{" +
                    "x=" + x +
                    ", y=" + y +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }

}
