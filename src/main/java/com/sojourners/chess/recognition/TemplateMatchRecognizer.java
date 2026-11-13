package com.sojourners.chess.recognition;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.util.Arrays;

/** 固定棋盘位置、缩放和棋子样式下的子图模板识别器。 */
public class TemplateMatchRecognizer {

    private static final int ROWS = 10;
    private static final int COLS = 9;
    private static final int SAMPLE_SIZE = 32;
    private static final int COARSE_SIZE = 8;
    // 采样网格覆盖正负 0.38 格距，核心半径约为 0.38 * sqrt(0.55) = 0.282 格距。
    private static final double CORE_RADIUS_SQUARED = 0.55;
    private static final double OUTER_SAMPLE_WEIGHT = 0.03;
    private static final double BOUND_EPSILON = 1e-6;
    private static final String PIECES = "rnbakcpRNBAKCP";
    private static final int LABEL_COUNT = PIECES.length() + 1;
    private static final double PIECE_THRESHOLD = 0.84;
    private static final double EMPTY_THRESHOLD = 0.75;
    private static final double MATCH_MARGIN = 0.045;
    private static final double DIRECT_MATCH_THRESHOLD = 0.97;
    private static final double CORE_MATCH_MARGIN = 0.08;
    private static final double HISTORY_MATCH_THRESHOLD = 0.995;
    private static final int HISTORY_CHANNEL_TOLERANCE = 8;
    private static final double BACKGROUND_THRESHOLD = 0.82;
    private static final double BACKGROUND_MARGIN = 0.10;
    // 扩展搜索只在常规匹配失败后触发，最大位移小于半格，防止追到相邻交点。
    private static final double MAX_SEARCH_FRACTION = 0.40;

    private final Rectangle recognizeRectangle;
    private final double pieceWidth, pieceHeight;
    private final Sampling sampling;
    private final Sampling coreSampling;

    // 模板构建完成后一次发布；识别的临时数据均为调用内局部变量。
    private volatile Templates templates;

    public TemplateMatchRecognizer(Rectangle recognizeRectangle) {
        if (recognizeRectangle == null || recognizeRectangle.width <= 0 || recognizeRectangle.height <= 0) {
            throw new IllegalArgumentException("棋盘范围和棋子大小必须有效且为正数");
        }
        this.recognizeRectangle = new Rectangle(recognizeRectangle);
        this.pieceWidth = recognizeRectangle.width / 8d;
        this.pieceHeight = recognizeRectangle.height / 9d;
        this.sampling = new Sampling(this.recognizeRectangle, pieceWidth, pieceHeight);
        this.coreSampling = new Sampling(this.recognizeRectangle, pieceWidth, pieceHeight, true);
    }

    /**
     * 从已知棋盘建立模板，同类棋子保留多个样本，空位也作为负模板。
     * recognizeRectangle 是图像内最外侧棋盘线的范围，宽、高分别跨 8、9 个格距；
     *
     * @throws IllegalArgumentException 图像、棋盘数组、标签或棋盘范围无效
     */
    public void initTemplate(BufferedImage img, char[][] board) {
        if (!validImage(img) || !validBoard(board)) {
            throw new IllegalArgumentException("图像必须包含棋盘交点，board 至少为 10 行 9 列");
        }
        PixelReader pixels = new PixelReader(img);
        Template[] samples = new Template[ROWS * COLS];
        double minPieceDeviation = Double.POSITIVE_INFINITY;
        double minEmptyMean = Double.POSITIVE_INFINITY;
        double maxEmptyMean = Double.NEGATIVE_INFINITY;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                char piece = board[row][col];
                int label = piece == ' ' || piece == '\0' ? 0 : PIECES.indexOf(piece) + 1;
                if (label == 0 && piece != ' ' && piece != '\0') {
                    throw new IllegalArgumentException("未知棋子标签: " + piece);
                }
                Feature feature = new Feature(sampling.count);
                sampling.read(pixels, row, col, 0, 0, feature);
                Feature core = new Feature(coreSampling.count);
                coreSampling.read(pixels, row, col, 0, 0, core);
                samples[row * COLS + col] = new Template(label, feature, core);
                if (label == 0) {
                    minEmptyMean = Math.min(minEmptyMean, feature.mean);
                    maxEmptyMean = Math.max(maxEmptyMean, feature.mean);
                } else {
                    minPieceDeviation = Math.min(minPieceDeviation, feature.deviation);
                }
            }
        }
        for (Template sample : samples) {
            for (Template other : samples) {
                if (sample.label != other.label && similarity(sample.feature, other.feature) > 0.94) {
                    sample.distinctive = false;
                    break;
                }
            }
        }
        templates = new Templates(samples, minPieceDeviation, minEmptyMean, maxEmptyMean);
    }

    /**
     * 匹配 90 个交点。未初始化或输入无效时返回 false，保持 board 不变。
     * 棋子优先只用字形核心和历史对齐结果确认，外围光圈变化不影响可靠的内部匹配。
     * 无法可靠识别的位置（如被动画遮挡）按空位处理，由上层判断可能的走棋路径。
     * 成功时一次写回全部结果，空位统一写为 ' '。允许裁切棋子外缘，但所有交点必须在图像内。
     */
    public boolean findChessBoard(BufferedImage img, char[][] board) {
        Templates current = templates;
        if (current == null || !validImage(img) || !validBoard(board)) {
            return false;
        }
        PixelReader pixels = new PixelReader(img);
        CachedFrame[] cachedFrames = current.cachedFrames;
        for (CachedFrame frame : cachedFrames) {
            if (frame.matches(pixels, 0, 0, pixels.width - 1, pixels.height - 1)) {
                writeBoard(frame.labels, board);
                return true;
            }
        }
        Feature feature = new Feature(sampling.count);
        Feature coreFeature = new Feature(coreSampling.count);
        Feature alignedCore = new Feature(coreSampling.count);
        double[] scores = new double[LABEL_COUNT];
        int[] offsetsX = new int[LABEL_COUNT], offsetsY = new int[LABEL_COUNT];
        char[] result = new char[ROWS * COLS];
        CellMatch[] confirmed = new CellMatch[ROWS * COLS];
        int stepX = Math.max(1, (int) Math.round(pieceWidth * 0.04));
        int stepY = Math.max(1, (int) Math.round(pieceHeight * 0.04));
        int maxRadiusX = (int) (recognizeRectangle.width / 8.0 * MAX_SEARCH_FRACTION / stepX) * stepX;
        int maxRadiusY = (int) (recognizeRectangle.height / 9.0 * MAX_SEARCH_FRACTION / stepY) * stepY;
        int initialRadiusX = Math.min(maxRadiusX,
                Math.max(stepX, (int) Math.ceil(pieceWidth * 0.20 / stepX) * stepX));
        int initialRadiusY = Math.min(maxRadiusY,
                Math.max(stepY, (int) Math.ceil(pieceHeight * 0.20 / stepY) * stepY));
        int expandX = Math.max(stepX, (int) Math.ceil(recognizeRectangle.width / 8.0 * 0.10 / stepX) * stepX);
        int expandY = Math.max(stepY, (int) Math.ceil(recognizeRectangle.height / 9.0 * 0.10 / stepY) * stepY);
        double[] backgroundErrors = new double[sampling.ringCount];
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = row * COLS + col;
                boolean cached = false;
                // 比较整个最大搜索足迹，而非仅中心采样点；偏移区中的遮挡也会使缓存失效。
                int left = Math.max(0, sampling.xs[col][0] - maxRadiusX);
                int right = Math.min(pixels.width - 1, sampling.xs[col][SAMPLE_SIZE - 1] + maxRadiusX + 1);
                int top = Math.max(0, sampling.ys[row][0] - maxRadiusY);
                int bottom = Math.min(pixels.height - 1, sampling.ys[row][SAMPLE_SIZE - 1] + maxRadiusY + 1);
                for (CachedFrame frame : cachedFrames) {
                    if (frame.matches(pixels, left, top, right, bottom)) {
                        result[index] = frame.labels[index];
                        confirmed[index] = frame.cells[index];
                        cached = true;
                        break;
                    }
                }
                if (cached) {
                    continue;
                }
                coreSampling.read(pixels, row, col, 0, 0, coreFeature);
                CellMatch previous = null;
                for (CachedFrame frame : cachedFrames) {
                    CellMatch candidate = frame.cells[index];
                    if (candidate == null || candidate == previous) {
                        continue;
                    }
                    previous = candidate;
                    Feature aligned = coreFeature;
                    if (candidate.dx != 0 || candidate.dy != 0) {
                        coreSampling.read(pixels, row, col, candidate.dx, candidate.dy, alignedCore);
                        aligned = alignedCore;
                    }
                    if (!candidate.matchesCore(pixels, aligned)
                            || !matchesHistoryFeature(aligned, candidate.core)) {
                        continue;
                    }
                    matchCore(aligned, current.samples, scores);
                    if (confidentLabel(scores) == candidate.label) {
                        result[index] = labelChar(candidate.label);
                        confirmed[index] = candidate;
                        cached = true;
                        break;
                    }
                }
                if (cached) {
                    continue;
                }
                matchCore(coreFeature, current.samples, scores);
                int coreLabel = confidentLabel(scores, DIRECT_MATCH_THRESHOLD,
                        DIRECT_MATCH_THRESHOLD, CORE_MATCH_MARGIN);
                if (coreLabel > 0) {
                    result[index] = labelChar(coreLabel);
                    confirmed[index] = rememberCore(pixels, row, col, coreLabel, 0, 0);
                    continue;
                }
                sampling.read(pixels, row, col, 0, 0, feature);
                Template original = current.samples[index];
                if (original.distinctive && similarity(feature, original.feature) >= 0.995) {
                    result[index] = labelChar(original.label);
                    continue;
                }
                double deviation = feature.deviation;
                double mean = feature.mean;
                Arrays.fill(scores, -Double.MAX_VALUE);
                Arrays.fill(offsetsX, 0);
                Arrays.fill(offsetsY, 0);
                match(feature, current.samples, scores, offsetsX, offsetsY, 0, 0, -1);
                int label = confidentLabel(scores);
                if (label <= 0 && bestPieceScore(scores) < 0.60
                        && (scores[0] > -Double.MAX_VALUE
                        && deviation < Math.min(10, current.minPieceDeviation * 0.45)
                        && mean >= current.minEmptyMean - 35 && mean <= current.maxEmptyMean + 35
                        || sampling.isBackground(feature, current.samples, backgroundErrors))) {
                    result[index] = ' ';
                    continue;
                }
                int radiusX = initialRadiusX, radiusY = initialRadiusY;
                int searchedX = 0, searchedY = 0;
                while (true) {
                    if (label < 0 || scores[label] < DIRECT_MATCH_THRESHOLD) {
                        for (int dy = -radiusY; dy <= radiusY; dy += stepY) {
                            for (int dx = -radiusX; dx <= radiusX; dx += stepX) {
                                if (Math.abs(dx) > searchedX || Math.abs(dy) > searchedY) {
                                    sampling.read(pixels, row, col, dx, dy, feature, false);
                                    match(feature, current.samples, scores, offsetsX, offsetsY, dx, dy, -1);
                                }
                            }
                        }
                        label = confidentLabel(scores);
                    }
                    if (label < 0 || scores[label] < DIRECT_MATCH_THRESHOLD) {
                        double[] coarseScores = scores.clone();
                        for (int candidate = 0; candidate < 3; candidate++) {
                            int target = 0;
                            for (int i = 1; i < LABEL_COUNT; i++) {
                                if (coarseScores[i] > coarseScores[target]) {
                                    target = i;
                                }
                            }
                            if (coarseScores[target] == -Double.MAX_VALUE) {
                                break;
                            }
                            coarseScores[target] = -Double.MAX_VALUE;
                            int fineX = Math.max(1, stepX / 2), fineY = Math.max(1, stepY / 2);
                            while (true) {
                                int centerX = offsetsX[target], centerY = offsetsY[target];
                                for (int dy = -1; dy <= 1; dy++) {
                                    for (int dx = -1; dx <= 1; dx++) {
                                        if (dx != 0 || dy != 0) {
                                            int x = centerX + dx * fineX, y = centerY + dy * fineY;
                                            if (Math.abs(x) > radiusX || Math.abs(y) > radiusY) {
                                                continue;
                                            }
                                            sampling.read(pixels, row, col, x, y, feature, false);
                                            match(feature, current.samples, scores, offsetsX, offsetsY, x, y, target);
                                        }
                                    }
                                }
                                if (fineX == 1 && fineY == 1) {
                                    break;
                                }
                                fineX = Math.max(1, fineX / 2);
                                fineY = Math.max(1, fineY / 2);
                            }
                        }
                        label = confidentLabel(scores);
                    }
                    boolean background = false;
                    if (label <= 0 && scores[0] > -Double.MAX_VALUE && bestPieceScore(scores) < 0.60
                            && deviation < Math.min(10, current.minPieceDeviation * 0.45)
                            && mean >= current.minEmptyMean - 35 && mean <= current.maxEmptyMean + 35) {
                        label = 0;
                        background = true;
                    }
                    if (label <= 0 && !background && bestPieceScore(scores) < 0.60) {
                        sampling.read(pixels, row, col, 0, 0, feature);
                        if (sampling.isBackground(feature, current.samples, backgroundErrors)) {
                            label = 0;
                            background = true;
                        }
                    }
                    boolean exhausted = radiusX == maxRadiusX && radiusY == maxRadiusY;
                    if (label >= 0 && (background || scores[label] >= DIRECT_MATCH_THRESHOLD || exhausted)) {
                        break;
                    }
                    if (exhausted) {
                        // 动画可能遮住棋子；仅将当前交点留空，继续识别其余位置。
                        label = 0;
                        break;
                    }
                    searchedX = radiusX;
                    searchedY = radiusY;
                    radiusX = Math.min(maxRadiusX, radiusX + expandX);
                    radiusY = Math.min(maxRadiusY, radiusY + expandY);
                }
                result[index] = labelChar(label);
                if (label > 0 && confidentLabel(scores) == label) {
                    int dx = offsetsX[label], dy = offsetsY[label];
                    CellMatch candidate = rememberCore(pixels, row, col, label, dx, dy);
                    matchCore(candidate.core, current.samples, scores);
                    if (confidentLabel(scores) == label) {
                        confirmed[index] = candidate;
                    }
                }
            }
        }
        CachedFrame completed = new CachedFrame(pixels, result, confirmed);
        CachedFrame[] latest = current.cachedFrames;
        current.cachedFrames = latest.length == 0 ? new CachedFrame[]{completed}
                : new CachedFrame[]{completed, latest[0]};
        writeBoard(result, board);
        return true;
    }

    private CellMatch rememberCore(PixelReader pixels, int row, int col, int label, int dx, int dy) {
        Feature core = new Feature(coreSampling.count);
        coreSampling.read(pixels, row, col, dx, dy, core);
        return new CellMatch(label, dx, dy, core, pixels.width, pixels.height);
    }

    private static void writeBoard(char[] result, char[][] board) {
        for (int row = 0; row < ROWS; row++) {
            System.arraycopy(result, row * COLS, board[row], 0, COLS);
        }
    }

    private boolean validImage(BufferedImage image) {
        return image != null && recognizeRectangle.x >= 0 && recognizeRectangle.y >= 0
                && (long) recognizeRectangle.x + recognizeRectangle.width < image.getWidth()
                && (long) recognizeRectangle.y + recognizeRectangle.height < image.getHeight();
    }

    private static boolean validBoard(char[][] board) {
        if (board == null || board.length < ROWS) {
            return false;
        }
        for (int row = 0; row < ROWS; row++) {
            if (board[row] == null || board[row].length < COLS) {
                return false;
            }
        }
        return true;
    }

    private static char labelChar(int label) {
        return label == 0 ? ' ' : PIECES.charAt(label - 1);
    }

    private static void match(Feature feature, Template[] samples, double[] scores,
                              int[] offsetsX, int[] offsetsY, int dx, int dy, int target) {
        for (Template sample : samples) {
            if (target >= 0 && sample.label != target) {
                continue;
            }
            double score = similarity(feature, sample.feature, scores[sample.label]);
            if (score > scores[sample.label]) {
                scores[sample.label] = score;
                offsetsX[sample.label] = dx;
                offsetsY[sample.label] = dy;
            }
        }
    }

    private static void matchCore(Feature feature, Template[] samples, double[] scores) {
        Arrays.fill(scores, -Double.MAX_VALUE);
        for (Template sample : samples) {
            double score = similarity(feature, sample.core, scores[sample.label]);
            if (score > scores[sample.label]) {
                scores[sample.label] = score;
            }
        }
    }

    private static int confidentLabel(double[] scores) {
        return confidentLabel(scores, PIECE_THRESHOLD, EMPTY_THRESHOLD, MATCH_MARGIN);
    }

    private static int confidentLabel(double[] scores, double pieceThreshold,
                                      double emptyThreshold, double margin) {
        int best = 0;
        double runnerUp = -Double.MAX_VALUE;
        for (int label = 1; label < scores.length; label++) {
            if (scores[label] > scores[best]) {
                runnerUp = scores[best];
                best = label;
            } else {
                runnerUp = Math.max(runnerUp, scores[label]);
            }
        }
        double threshold = best == 0 ? emptyThreshold : pieceThreshold;
        return scores[best] >= threshold && scores[best] - runnerUp >= margin ? best : -1;
    }

    private static double bestPieceScore(double[] scores) {
        double best = -Double.MAX_VALUE;
        for (int label = 1; label < scores.length; label++) {
            best = Math.max(best, scores[label]);
        }
        return best;
    }

    private static double similarity(Feature a, Feature b) {
        return similarity(a, b, -Double.MAX_VALUE);
    }

    private static boolean matchesHistoryFeature(Feature a, Feature b) {
        if (a.deviation < 2 || b.deviation < 2) {
            return similarity(a, b) >= HISTORY_MATCH_THRESHOLD;
        }
        double correlation = 0;
        for (int i = 0; i < a.values.length; i++) {
            correlation += a.values[i] * b.values[i];
        }
        return correlation >= HISTORY_MATCH_THRESHOLD;
    }

    private static double similarity(Feature a, Feature b, double minimum) {
        if (a.deviation < 2 || b.deviation < 2) {
            // 平坦子图不能做 NCC（方差为零），用亮度、色差和对比度比较。
            return 1 - Math.abs(a.mean - b.mean) / 90 - Math.abs(a.deviation - b.deviation) / 12
                    - Math.abs(a.redGreenMean - b.redGreenMean) / 90
                    - Math.abs(a.blueGreenMean - b.blueGreenMean) / 90;
        }
        double sensitivity = Math.min(6, Math.max(1, (a.mean + b.mean)
                / (4 * (a.deviation + b.deviation))));
        double colorPenalty = sensitivity * (2 * Math.abs(a.darkRG - b.darkRG)
                + Math.abs(a.darkBG - b.darkBG) + 0.5 * Math.abs(a.lightRG - b.lightRG)
                + 0.25 * Math.abs(a.lightBG - b.lightBG));
        double ratio = Math.min(a.deviation, b.deviation) / Math.max(a.deviation, b.deviation);
        double penalty = colorPenalty + Math.max(0, 0.5 - ratio) * 0.8;
        if (minimum > -Double.MAX_VALUE) {
            // 先比较严格上界；只跳过不可能改进本类别最高分的样本，不改变候选和阈值。
            if (a.norm * b.norm - penalty + BOUND_EPSILON <= minimum) {
                return -Double.MAX_VALUE;
            }
            double projected = 0;
            for (int i = 0; i < a.coarse.length; i++) {
                projected += a.coarse[i] * b.coarse[i];
            }
            if (projected + a.residualNorm * b.residualNorm - penalty + BOUND_EPSILON <= minimum) {
                return -Double.MAX_VALUE;
            }
        }
        double correlation = 0;
        for (int i = 0; i < a.values.length; i++) {
            correlation += a.values[i] * b.values[i];
        }
        return correlation - penalty;
    }

    private static final class Templates {
        final Template[] samples;
        volatile CachedFrame[] cachedFrames = new CachedFrame[0];
        final double minPieceDeviation, minEmptyMean, maxEmptyMean;

        Templates(Template[] samples, double minPieceDeviation, double minEmptyMean, double maxEmptyMean) {
            this.samples = samples;
            this.minPieceDeviation = minPieceDeviation;
            this.minEmptyMean = minEmptyMean;
            this.maxEmptyMean = maxEmptyMean;
        }
    }

    /** 只保留最近两张成功帧；重新初始化模板时随旧 Templates 一起失效。 */
    private static final class CachedFrame {
        final int width, height;
        final int[] rgb;
        final char[] labels;
        final CellMatch[] cells;

        CachedFrame(PixelReader pixels, char[] labels, CellMatch[] cells) {
            width = pixels.width;
            height = pixels.height;
            this.labels = labels;
            this.cells = cells;
            rgb = new int[Math.multiplyExact(width, height)];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    rgb[y * width + x] = pixels.rgb(x, y) & 0xffffff;
                }
            }
        }

        boolean matches(PixelReader pixels, int left, int top, int right, int bottom) {
            if (pixels.width != width || pixels.height != height) {
                return false;
            }
            for (int y = top; y <= bottom; y++) {
                int offset = y * width;
                for (int x = left; x <= right; x++) {
                    if (rgb[offset + x] != (pixels.rgb(x, y) & 0xffffff)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private static final class CellMatch {
        final int label, dx, dy;
        final Feature core;
        final int imageWidth, imageHeight;

        CellMatch(int label, int dx, int dy, Feature core, int imageWidth, int imageHeight) {
            this.label = label;
            this.dx = dx;
            this.dy = dy;
            this.core = core;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }

        boolean matchesCore(PixelReader pixels, Feature feature) {
            if (pixels.width != imageWidth || pixels.height != imageHeight) {
                return false;
            }
            for (int i = 0; i < core.colors.length; i++) {
                int before = core.colors[i], after = feature.colors[i];
                if (Math.abs((before >> 16 & 255) - (after >> 16 & 255)) > HISTORY_CHANNEL_TOLERANCE
                        || Math.abs((before >> 8 & 255) - (after >> 8 & 255)) > HISTORY_CHANNEL_TOLERANCE
                        || Math.abs((before & 255) - (after & 255)) > HISTORY_CHANNEL_TOLERANCE) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class Template {
        final int label;
        final Feature feature, core;
        boolean distinctive = true; // 仅初始化、发布前设置。

        Template(int label, Feature feature, Feature core) {
            this.label = label;
            this.feature = feature;
            this.core = core;
        }
    }

    private static final class Feature {
        final float[] values;
        final int[] colors;
        final double[] coarse = new double[COARSE_SIZE * COARSE_SIZE];
        double norm, residualNorm;
        double mean, deviation, redGreenMean, blueGreenMean, darkRG, darkBG, lightRG, lightBG;

        Feature(int size) {
            values = new float[size];
            colors = new int[size];
        }
    }

    /** 预计算交点和圆形掩码；椭圆内取样，降低圆环、阴影和相邻格的影响。 */
    private static final class Sampling {
        final int[][] xs = new int[COLS][SAMPLE_SIZE];
        final int[][] ys = new int[ROWS][SAMPLE_SIZE];
        final double[][] fx = new double[COLS][SAMPLE_SIZE];
        final double[][] fy = new double[ROWS][SAMPLE_SIZE];
        final int[] maskX = new int[SAMPLE_SIZE * SAMPLE_SIZE];
        final int[] maskY = new int[SAMPLE_SIZE * SAMPLE_SIZE];
        final double[] weights = new double[SAMPLE_SIZE * SAMPLE_SIZE];
        final double[] sqrtWeights = new double[SAMPLE_SIZE * SAMPLE_SIZE];
        final int[] ringIndices = new int[SAMPLE_SIZE * SAMPLE_SIZE];
        final int[] groups = new int[SAMPLE_SIZE * SAMPLE_SIZE];
        final double[] groupNorms = new double[COARSE_SIZE * COARSE_SIZE];
        int ringCount;
        int count;
        double weightSum;

        Sampling(Rectangle rectangle, double width, double height) {
            this(rectangle, width, height, false);
        }

        Sampling(Rectangle rectangle, double width, double height, boolean coreOnly) {
            for (int i = 0; i < SAMPLE_SIZE; i++) {
                double offset = (2.0 * i / (SAMPLE_SIZE - 1) - 1) * 0.38;
                for (int col = 0; col < COLS; col++) {
                    double x = rectangle.x + col * (rectangle.width / 8.0) + offset * width;
                    xs[col][i] = (int) Math.floor(x);
                    fx[col][i] = x - xs[col][i];
                }
                for (int row = 0; row < ROWS; row++) {
                    double y = rectangle.y + row * (rectangle.height / 9.0) + offset * height;
                    ys[row][i] = (int) Math.floor(y);
                    fy[row][i] = y - ys[row][i];
                }
            }
            for (int y = 0; y < SAMPLE_SIZE; y++) {
                for (int x = 0; x < SAMPLE_SIZE; x++) {
                    double nx = 2.0 * x / (SAMPLE_SIZE - 1) - 1;
                    double ny = 2.0 * y / (SAMPLE_SIZE - 1) - 1;
                    double radiusSquared = nx * nx + ny * ny;
                    if (radiusSquared <= (coreOnly ? CORE_RADIUS_SQUARED : 1)) {
                        maskX[count] = x;
                        maskY[count] = y;
                        double weight = radiusSquared <= CORE_RADIUS_SQUARED ? 1 : OUTER_SAMPLE_WEIGHT;
                        weights[count] = weight;
                        sqrtWeights[count] = Math.sqrt(weight);
                        int group = y / (SAMPLE_SIZE / COARSE_SIZE) * COARSE_SIZE
                                + x / (SAMPLE_SIZE / COARSE_SIZE);
                        groups[count] = group;
                        groupNorms[group]++;
                        if (radiusSquared >= 0.60) {
                            ringIndices[ringCount++] = count;
                        }
                        weightSum += weight;
                        count++;
                    }
                }
            }
            for (int i = 0; i < groupNorms.length; i++) {
                groupNorms[i] = groupNorms[i] == 0 ? 0 : 1 / Math.sqrt(groupNorms[i]);
            }
        }

        void read(PixelReader pixels, int row, int col, int dx, int dy, Feature feature) {
            read(pixels, row, col, dx, dy, feature, true);
        }

        void read(PixelReader pixels, int row, int col, int dx, int dy, Feature feature, boolean collectColors) {
            double sum = 0, squares = 0, sumRG = 0, sumBG = 0;
            for (int i = 0; i < count; i++) {
                int sx = maskX[i], sy = maskY[i];
                int x = xs[col][sx] + dx, y = ys[row][sy] + dy;
                int x0 = Math.max(0, Math.min(pixels.width - 1, x));
                int x1 = Math.max(0, Math.min(pixels.width - 1, x + 1));
                int y0 = Math.max(0, Math.min(pixels.height - 1, y));
                int y1 = Math.max(0, Math.min(pixels.height - 1, y + 1));
                int p00 = pixels.rgb(x0, y0), p10 = pixels.rgb(x1, y0);
                int p01 = pixels.rgb(x0, y1), p11 = pixels.rgb(x1, y1);
                double tx = fx[col][sx], ty = fy[row][sy];
                double r = interpolate(p00, p10, p01, p11, 16, tx, ty);
                double g = interpolate(p00, p10, p01, p11, 8, tx, ty);
                double b = interpolate(p00, p10, p01, p11, 0, tx, ty);
                double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
                double rg = r - g, bg = b - g, weight = weights[i];
                feature.values[i] = (float) luminance;
                if (collectColors) {
                    feature.colors[i] = ((int) Math.round(r) << 16) | ((int) Math.round(g) << 8)
                            | (int) Math.round(b);
                }
                pixels.sampleRG[i] = (float) rg;
                pixels.sampleBG[i] = (float) bg;
                pixels.sampleTotal[i] = (float) (r + g + b);
                sum += weight * luminance;
                squares += weight * luminance * luminance;
                sumRG += weight * rg;
                sumBG += weight * bg;
            }
            feature.mean = sum / weightSum;
            feature.redGreenMean = sumRG / weightSum;
            feature.blueGreenMean = sumBG / weightSum;
            double variance = Math.max(0, squares - sum * feature.mean);
            feature.deviation = Math.sqrt(variance / weightSum);
            double norm = variance > 1 ? 1 / Math.sqrt(variance) : 0;
            double darkRG = 0, darkBG = 0, darkTotal = 0, lightRG = 0, lightBG = 0, lightTotal = 0;
            double split = feature.deviation * 0.35;
            Arrays.fill(feature.coarse, 0);
            double normSquared = 0;
            for (int i = 0; i < count; i++) {
                double centered = feature.values[i] - feature.mean;
                double weight = weights[i];
                if (centered < -split) {
                    darkRG += pixels.sampleRG[i] * weight;
                    darkBG += pixels.sampleBG[i] * weight;
                    darkTotal += (pixels.sampleTotal[i] + 32) * weight;
                } else if (centered > split) {
                    lightRG += pixels.sampleRG[i] * weight;
                    lightBG += pixels.sampleBG[i] * weight;
                    lightTotal += (pixels.sampleTotal[i] + 32) * weight;
                }
                feature.values[i] = (float) ((feature.values[i] - feature.mean) * sqrtWeights[i] * norm);
                double value = feature.values[i];
                feature.coarse[groups[i]] += value;
                normSquared += value * value;
            }
            double projectedSquared = 0;
            for (int i = 0; i < feature.coarse.length; i++) {
                feature.coarse[i] *= groupNorms[i];
                projectedSquared += feature.coarse[i] * feature.coarse[i];
            }
            feature.norm = Math.sqrt(normSquared);
            feature.residualNorm = Math.sqrt(Math.max(0, normSquared - projectedSquared));
            feature.darkRG = darkTotal > 0 ? darkRG / darkTotal : 0;
            feature.darkBG = darkTotal > 0 ? darkBG / darkTotal : 0;
            feature.lightRG = lightTotal > 0 ? lightRG / lightTotal : 0;
            feature.lightBG = lightTotal > 0 ? lightBG / lightTotal : 0;
        }

        boolean isBackground(Feature feature, Template[] samples, double[] errors) {
            double empty = -Double.MAX_VALUE, piece = -Double.MAX_VALUE;
            for (Template sample : samples) {
                double score = backgroundSimilarity(feature, sample.feature, errors);
                if (sample.label == 0) {
                    empty = Math.max(empty, score);
                } else {
                    piece = Math.max(piece, score);
                }
            }
            return empty >= BACKGROUND_THRESHOLD && empty - piece >= BACKGROUND_MARGIN;
        }

        private double backgroundSimilarity(Feature a, Feature b, double[] errors) {
            for (int i = 0; i < ringCount; i++) {
                int p = a.colors[ringIndices[i]], q = b.colors[ringIndices[i]];
                errors[i] = (Math.abs((p >> 16 & 255) - (q >> 16 & 255))
                        + Math.abs((p >> 8 & 255) - (q >> 8 & 255))
                        + Math.abs((p & 255) - (q & 255))) / 3.0;
            }
            // 去掉少量棋盘线、提示点边缘或纹理离群值；棋子圆盘仍覆盖外围的大多数像素。
            Arrays.sort(errors);
            int retained = Math.max(1, (int) (ringCount * 0.75));
            double sum = 0;
            for (int i = 0; i < retained; i++) {
                sum += errors[i];
            }
            return 1 - sum / (retained * 80);
        }

        private static double interpolate(int p00, int p10, int p01, int p11, int shift, double x, double y) {
            double top = ((p00 >> shift) & 255) * (1 - x) + ((p10 >> shift) & 255) * x;
            double bottom = ((p01 >> shift) & 255) * (1 - x) + ((p11 >> shift) & 255) * x;
            return top + (bottom - top) * y;
        }
    }

    /** 常见截图格式直接读取栅格，不复制整张图片；保留子图的偏移、行步长和预乘 alpha 语义。 */
    private static final class PixelReader {
        final BufferedImage image;
        final int width, height;
        final float[] sampleRG = new float[SAMPLE_SIZE * SAMPLE_SIZE];
        final float[] sampleBG = new float[SAMPLE_SIZE * SAMPLE_SIZE];
        final float[] sampleTotal = new float[SAMPLE_SIZE * SAMPLE_SIZE];
        int[] ints;
        byte[] bytes;
        int base, stride, pixelStride, red, green, blue, alpha = -1;
        boolean bgr, premultiplied;

        PixelReader(BufferedImage image) {
            this.image = image;
            width = image.getWidth();
            height = image.getHeight();
            Raster raster = image.getRaster();
            int type = image.getType();
            if (type == BufferedImage.TYPE_INT_RGB || type == BufferedImage.TYPE_INT_ARGB
                    || type == BufferedImage.TYPE_INT_ARGB_PRE || type == BufferedImage.TYPE_INT_BGR) {
                DataBufferInt buffer = (DataBufferInt) raster.getDataBuffer();
                ints = buffer.getData();
                stride = ((SinglePixelPackedSampleModel) raster.getSampleModel()).getScanlineStride();
                base = buffer.getOffset() - raster.getSampleModelTranslateY() * stride
                        - raster.getSampleModelTranslateX();
                bgr = type == BufferedImage.TYPE_INT_BGR;
                premultiplied = type == BufferedImage.TYPE_INT_ARGB_PRE;
            } else if (type == BufferedImage.TYPE_3BYTE_BGR || type == BufferedImage.TYPE_4BYTE_ABGR
                    || type == BufferedImage.TYPE_4BYTE_ABGR_PRE) {
                DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
                if (buffer.getOffset() != 0 || buffer.getNumBanks() != 1) {
                    return;
                }
                ComponentSampleModel model = (ComponentSampleModel) raster.getSampleModel();
                bytes = buffer.getData();
                stride = model.getScanlineStride();
                pixelStride = model.getPixelStride();
                int[] bands = model.getBandOffsets();
                red = bands[0];
                green = bands[1];
                blue = bands[2];
                if (bands.length == 4) {
                    alpha = bands[3];
                }
                base = buffer.getOffset() - raster.getSampleModelTranslateY() * stride
                        - raster.getSampleModelTranslateX() * pixelStride;
                premultiplied = image.isAlphaPremultiplied();
            }
        }

        int rgb(int x, int y) {
            int rgb;
            if (ints != null) {
                rgb = ints[base + y * stride + x];
                if (bgr) {
                    return ((rgb & 255) << 16) | (rgb & 0xff00) | ((rgb >>> 16) & 255);
                }
            } else if (bytes != null) {
                int offset = base + y * stride + x * pixelStride;
                rgb = ((bytes[offset + red] & 255) << 16) | ((bytes[offset + green] & 255) << 8)
                        | (bytes[offset + blue] & 255);
                if (premultiplied) {
                    rgb |= (bytes[offset + alpha] & 255) << 24;
                }
            } else {
                return image.getRGB(x, y);
            }
            if (premultiplied) {
                int a = rgb >>> 24;
                if (a == 0) {
                    return 0;
                }
                if (a != 255) {
                    int r = (((rgb >>> 16) & 255) * 255 + a / 2) / a;
                    int g = (((rgb >>> 8) & 255) * 255 + a / 2) / a;
                    int b = ((rgb & 255) * 255 + a / 2) / a;
                    return (r << 16) | (g << 8) | b;
                }
            }
            return rgb;
        }
    }

}
