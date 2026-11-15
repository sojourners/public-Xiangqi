package com.sojourners.chess.config;

import com.sojourners.chess.board.ChessBoard;
import com.sojourners.chess.enginee.Engine;
import com.sojourners.chess.model.EngineConfig;
import com.sojourners.chess.openbook.MoveRule;
import com.sojourners.chess.recognition.RecognizeMode;
import com.sojourners.chess.util.PathUtils;
import javafx.scene.text.Font;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Properties implements Serializable {

    private static final long serialVersionUID = -1410031608529065857L;

    public static final String DEFAULT_FIRST_STEP_COLOR = "#800080";
    public static final String DEFAULT_SECOND_STEP_COLOR = "#008000";
    public static final String DEFAULT_BRANCH_STEP_COLOR = "#FF2F00";
    public static final String DEFAULT_STEP_NUMBER_COLOR = "#FFFFFF";
    public static final double DEFAULT_STEP_OPACITY = 0.5d;
    public static final double DEFAULT_PIECE_SHADOW_ANGLE = 45d;
    public static final double DEFAULT_PIECE_SHADOW_LENGTH = 10d;
    public static final double DEFAULT_PIECE_SHADOW_SIZE = 95d;
    public static final String DEFAULT_CLOUD_BOOK_URL = "http://www.chessdb.cn/chessdb.php";

    private static Properties prop;

    private ChessBoard.BoardSize boardSize;
    private ChessBoard.BoardStyle boardStyle = ChessBoard.BoardStyle.DEFAULT;

    private boolean stepTip;

    private boolean stepSound;

    private boolean showNumber = true;

    private boolean topWindow = false;

    private int threadNum;

    private int hashSize;

    private String engineName;

    private List<EngineConfig> engineConfigList = new ArrayList<>();

    private Engine.AnalysisModel analysisModel;

    private long analysisValue;

    private double stageWidth;

    private double stageHeight;

    private double splitPos;
    private double splitPos2;
    private Double engineBookDivider;
    private Double manualChartDivider;

    private long linkScanTime;
    private int linkThreadNum;
    private boolean linkAnimation;
    private boolean linkShowInfo;
    private boolean linkBackMode;

    private List<String> openBookList;

    private Boolean localBookFirst;

    private Boolean useCloudBook;

    private String cloudBookUrl;

    private Boolean onlyCloudFinalPhase;

    private Integer cloudBookTimeout;

    private Integer offManualSteps;

    private MoveRule moveRule;

    private Boolean bookSwitch;

    private int engineDelayStart = 0;
    private int engineDelayEnd = 0;

    private int bookDelayStart = 0;
    private int bookDelayEnd = 0;

    private int mouseClickDelay = 2;
    private int mouseMoveDelay = 0;
    /*
     * 显示棋谱管理
     */
    private boolean showChessNotation = false;

    private String chessManualPath;

    private String autoSaveChessManualFormat;

    private String chessManualAutoSaveDirectory;
    private int chessManualPlaybackMillis;
    private int chessManualScoreMillis;
    private double chessManualCommentFontSize;

    private boolean manualTip = true;

    /*
     * 棋步提示颜色。使用包装类型保存透明度，以便读取旧版序列化配置时
     * 能区分“未配置”和用户明确选择的 0% 透明度。
     */
    private String firstStepColor;
    private Double firstStepOpacity;
    private String firstStepNumberColor;

    private String secondStepColor;
    private Double secondStepOpacity;
    private String secondStepNumberColor;

    private String branchStepColor;
    private Double branchStepOpacity;
    private String branchStepNumberColor;

    private ColorTheme colorTheme;

    private boolean pieceShadowEnabled = true;
    // 包装类型保留旧配置的默认值，同时允许用户明确设置为 0。
    private Double pieceShadowAngle;
    private Double pieceShadowLength;
    private Double pieceShadowSize;

    private ViewMode viewMode;

    private RecognizeMode recognizeMode = RecognizeMode.MIXED;

    private long animationConfirmDelay = 50L;

    private String customSkin = "红黑";

    public enum ViewMode {
        DEFAULT,
        TILED
    }

    public enum ColorTheme {
        LIGHT,
        DARK,
        EYE_GREEN,
        WARM_YELLOW
    }

    private Properties(ChessBoard.BoardSize boardSize, boolean stepTip,
                       int threadNum, int hashSize, String engineName, Engine.AnalysisModel analysisModel, long analysisValue,
                       boolean stepSound, double stageWidth, double stageHeight, double splitPos, double splitPos2,
                       long linkScanTime, int linkThreadNum, boolean linkAnimation, boolean linkShowInfo, boolean linkBackMode,
                       Boolean localBookFirst, Boolean useCloudBook, Boolean onlyCloudFinalPhase, Integer cloudBookTimeout, Integer offManualSteps,
                       MoveRule moveRule, Boolean bookSwitch, List<String> openBookList) {
        this.boardSize = boardSize;
        this.stepTip = stepTip;
        this.threadNum = threadNum;
        this.hashSize = hashSize;
        this.engineName = engineName;
        this.analysisModel = analysisModel;
        this.analysisValue = analysisValue;
        this.stepSound = stepSound;
        this.stageWidth = stageWidth;
        this.stageHeight = stageHeight;
        this.splitPos = splitPos;
        this.splitPos2 = splitPos2;
        this.linkScanTime = linkScanTime;
        this.linkThreadNum = linkThreadNum;
        this.linkAnimation = linkAnimation;
        this.linkShowInfo = linkShowInfo;
        this.linkBackMode = linkBackMode;
        this.localBookFirst = localBookFirst;
        this.useCloudBook = useCloudBook;
        this.onlyCloudFinalPhase = onlyCloudFinalPhase;
        this.cloudBookTimeout = cloudBookTimeout;
        this.offManualSteps = offManualSteps;
        this.moveRule = moveRule;
        this.bookSwitch = bookSwitch;
        this.openBookList = openBookList;
    }

    public static synchronized Properties getInstance() {
        if (prop == null) {
            String path = PathUtils.getJarPath() + "properties";
            File file = new File(path);
            if (file.exists()) {
                ObjectInputStream os = null;
                try {
                    os = new ObjectInputStream(new FileInputStream(file));
                    prop = (Properties) os.readObject();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (os != null)
                            os.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    List<EngineConfig> engineConfigList = new ArrayList<>();
                    prop = new Properties(ChessBoard.BoardSize.AUTOFIT_BOARD, true,
                            1, 16, "",
                            Engine.AnalysisModel.FIXED_TIME, 5000, true,
                            920, 737, 0.64, 0.6,
                            100, 2, true, true, true,
                            true, true, false, 2000, 9999,
                            MoveRule.BEST_SCORE, true, new ArrayList<>());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }

    public void save() {
        ObjectOutputStream os = null;
        try {
            String path = PathUtils.getJarPath() + "properties";
            File file = new File(path);
            os = new ObjectOutputStream(new FileOutputStream(file));
            os.writeObject(this);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null)
                    os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getCustomSkin() {
        return customSkin;
    }

    public void setCustomSkin(String customSkin) {
        this.customSkin = customSkin;
    }

    public ChessBoard.BoardStyle getBoardStyle() {
        return boardStyle;
    }

    public void setBoardStyle(ChessBoard.BoardStyle boardStyle) {
        this.boardStyle = boardStyle;
    }

    public int getEngineDelayStart() {
        return engineDelayStart;
    }

    public void setEngineDelayStart(int engineDelayStart) {
        this.engineDelayStart = engineDelayStart;
    }

    public int getEngineDelayEnd() {
        return engineDelayEnd;
    }

    public void setEngineDelayEnd(int engineDelayEnd) {
        this.engineDelayEnd = engineDelayEnd;
    }

    public int getBookDelayStart() {
        return bookDelayStart;
    }

    public void setBookDelayStart(int bookDelayStart) {
        this.bookDelayStart = bookDelayStart;
    }

    public int getBookDelayEnd() {
        return bookDelayEnd;
    }

    public void setBookDelayEnd(int bookDelayEnd) {
        this.bookDelayEnd = bookDelayEnd;
    }

    public int getMouseClickDelay() {
        return mouseClickDelay;
    }

    public void setMouseClickDelay(int mouseClickDelay) {
        this.mouseClickDelay = mouseClickDelay;
    }

    public int getMouseMoveDelay() {
        return mouseMoveDelay;
    }

    public void setMouseMoveDelay(int mouseMoveDelay) {
        this.mouseMoveDelay = mouseMoveDelay;
    }

    public List<String> getOpenBookList() {
        return openBookList;
    }

    public void setOpenBookList(List<String> openBookList) {
        this.openBookList = openBookList;
    }

    public Boolean getLocalBookFirst() {
        return localBookFirst;
    }

    public void setLocalBookFirst(Boolean localBookFirst) {
        this.localBookFirst = localBookFirst;
    }

    public Boolean getUseCloudBook() {
        return useCloudBook;
    }

    public void setUseCloudBook(Boolean useCloudBook) {
        this.useCloudBook = useCloudBook;
    }

    public String getCloudBookUrl() {
        return cloudBookUrl == null || cloudBookUrl.isBlank() ? DEFAULT_CLOUD_BOOK_URL : cloudBookUrl;
    }

    public void setCloudBookUrl(String cloudBookUrl) {
        this.cloudBookUrl = cloudBookUrl == null ? null : cloudBookUrl.trim();
    }

    public Boolean getOnlyCloudFinalPhase() {
        return onlyCloudFinalPhase;
    }

    public void setOnlyCloudFinalPhase(Boolean onlyCloudFinalPhase) {
        this.onlyCloudFinalPhase = onlyCloudFinalPhase;
    }

    public Integer getCloudBookTimeout() {
        return cloudBookTimeout;
    }

    public void setCloudBookTimeout(Integer cloudBookTimeout) {
        this.cloudBookTimeout = cloudBookTimeout;
    }

    public Integer getOffManualSteps() {
        return offManualSteps;
    }

    public void setOffManualSteps(Integer offManualSteps) {
        this.offManualSteps = offManualSteps;
    }

    public MoveRule getMoveRule() {
        return moveRule;
    }

    public void setMoveRule(MoveRule moveRule) {
        this.moveRule = moveRule;
    }

    public Boolean getBookSwitch() {
        return bookSwitch;
    }

    public void setBookSwitch(Boolean bookSwitch) {
        this.bookSwitch = bookSwitch;
    }

    public long getLinkScanTime() {
        return linkScanTime;
    }

    public void setLinkScanTime(long linkScanTime) {
        this.linkScanTime = linkScanTime;
    }

    public int getLinkThreadNum() {
        return linkThreadNum;
    }

    public void setLinkThreadNum(int linkThreadNum) {
        this.linkThreadNum = linkThreadNum;
    }

    public boolean isLinkAnimation() {
        return linkAnimation;
    }

    public void setLinkAnimation(boolean linkAnimation) {
        this.linkAnimation = linkAnimation;
    }

    public boolean isLinkShowInfo() {
        return linkShowInfo;
    }

    public void setLinkShowInfo(boolean linkShowInfo) {
        this.linkShowInfo = linkShowInfo;
    }

    public boolean isLinkBackMode() {
        return linkBackMode;
    }

    public void setLinkBackMode(boolean linkBackMode) {
        this.linkBackMode = linkBackMode;
    }

    public double getSplitPos2() {
        return splitPos2;
    }

    public void setSplitPos2(double splitPos2) {
        this.splitPos2 = splitPos2;
    }

    public double getEngineBookDivider() {
        return engineBookDivider == null ? 0.5 : engineBookDivider;
    }

    public void setEngineBookDivider(double engineBookDivider) {
        this.engineBookDivider = engineBookDivider;
    }

    public double getManualChartDivider() {
        return manualChartDivider == null ? 0.5 : manualChartDivider;
    }

    public void setManualChartDivider(double manualChartDivider) {
        this.manualChartDivider = manualChartDivider;
    }

    public double getStageWidth() {
        return stageWidth;
    }

    public void setStageWidth(double stageWidth) {
        this.stageWidth = stageWidth;
    }

    public double getStageHeight() {
        return stageHeight;
    }

    public void setStageHeight(double stageHeight) {
        this.stageHeight = stageHeight;
    }

    public double getSplitPos() {
        return splitPos;
    }

    public void setSplitPos(double splitPos) {
        this.splitPos = splitPos;
    }

    public boolean isStepSound() {
        return stepSound;
    }

    public void setStepSound(boolean stepSound) {
        this.stepSound = stepSound;
    }

    public Engine.AnalysisModel getAnalysisModel() {
        return analysisModel;
    }

    public void setAnalysisModel(Engine.AnalysisModel analysisModel) {
        this.analysisModel = analysisModel;
    }

    public long getAnalysisValue() {
        return analysisValue;
    }

    public void setAnalysisValue(long analysisValue) {
        this.analysisValue = analysisValue;
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public int getHashSize() {
        return hashSize;
    }

    public void setHashSize(int hashSize) {
        this.hashSize = hashSize;
    }

    public List<EngineConfig> getEngineConfigList() {
        return engineConfigList;
    }

    public void setEngineConfigList(List<EngineConfig> engineConfigList) {
        this.engineConfigList = engineConfigList;
    }

    public ChessBoard.BoardSize getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(ChessBoard.BoardSize boardSize) {
        this.boardSize = boardSize;
    }

    public boolean isStepTip() {
        return stepTip;
    }

    public void setStepTip(boolean stepTip) {
        this.stepTip = stepTip;
    }

    public boolean isShowNumber() {
        return showNumber;
    }

    public void setShowNumber(boolean showNumber) {
        this.showNumber = showNumber;
    }

    public boolean isTopWindow() {
        return topWindow;
    }

    public void setTopWindow(boolean topWindow) {
        this.topWindow = topWindow;
    }

    public boolean isShowChessNotation() {
        return showChessNotation;
    }

    public void setShowChessNotation(boolean showChessNotation) {
        this.showChessNotation = showChessNotation;
    }

    public String getChessManualPath() {
        return chessManualPath;
    }

    public void setChessManualPath(String chessManualPath) {
        this.chessManualPath = chessManualPath;
    }

    public String getAutoSaveChessManualFormat() {
        return switch (autoSaveChessManualFormat == null ? "" : autoSaveChessManualFormat) {
            case "txq", "pgn", "xqf", "cbr" -> autoSaveChessManualFormat;
            default -> "";
        };
    }

    public void setAutoSaveChessManualFormat(String autoSaveChessManualFormat) {
        this.autoSaveChessManualFormat = autoSaveChessManualFormat;
    }

    public String getChessManualAutoSaveDirectory() {
        return chessManualAutoSaveDirectory == null || chessManualAutoSaveDirectory.isBlank()
                ? new File(System.getProperty("user.home"), "TCHESS自动保存棋谱").getPath()
                : chessManualAutoSaveDirectory;
    }

    public void setChessManualAutoSaveDirectory(String chessManualAutoSaveDirectory) {
        this.chessManualAutoSaveDirectory = chessManualAutoSaveDirectory;
    }

    public int getChessManualPlaybackMillis() {
        return chessManualPlaybackMillis > 0 ? chessManualPlaybackMillis : 1000;
    }

    public void setChessManualPlaybackMillis(int chessManualPlaybackMillis) {
        this.chessManualPlaybackMillis = chessManualPlaybackMillis;
    }

    public int getChessManualScoreMillis() {
        return chessManualScoreMillis > 0 ? chessManualScoreMillis : 500;
    }

    public void setChessManualScoreMillis(int chessManualScoreMillis) {
        this.chessManualScoreMillis = chessManualScoreMillis;
    }

    public double getChessManualCommentFontSize() {
        return Double.isFinite(chessManualCommentFontSize) && chessManualCommentFontSize > 0
                ? chessManualCommentFontSize : Font.getDefault().getSize();
    }

    public void setChessManualCommentFontSize(double chessManualCommentFontSize) {
        this.chessManualCommentFontSize = chessManualCommentFontSize;
    }

    public boolean isManualTip() {
        return manualTip;
    }

    public void setManualTip(boolean manualTip) {
        this.manualTip = manualTip;
    }

    public String getFirstStepColor() {
        return colorOrDefault(firstStepColor, DEFAULT_FIRST_STEP_COLOR);
    }

    public void setFirstStepColor(String firstStepColor) {
        this.firstStepColor = firstStepColor;
    }

    public double getFirstStepOpacity() {
        return opacityOrDefault(firstStepOpacity);
    }

    public void setFirstStepOpacity(double firstStepOpacity) {
        this.firstStepOpacity = normalizeOpacity(firstStepOpacity);
    }

    public String getFirstStepNumberColor() {
        return colorOrDefault(firstStepNumberColor, DEFAULT_STEP_NUMBER_COLOR);
    }

    public void setFirstStepNumberColor(String firstStepNumberColor) {
        this.firstStepNumberColor = firstStepNumberColor;
    }

    public String getSecondStepColor() {
        return colorOrDefault(secondStepColor, DEFAULT_SECOND_STEP_COLOR);
    }

    public void setSecondStepColor(String secondStepColor) {
        this.secondStepColor = secondStepColor;
    }

    public double getSecondStepOpacity() {
        return opacityOrDefault(secondStepOpacity);
    }

    public void setSecondStepOpacity(double secondStepOpacity) {
        this.secondStepOpacity = normalizeOpacity(secondStepOpacity);
    }

    public String getSecondStepNumberColor() {
        return colorOrDefault(secondStepNumberColor, DEFAULT_STEP_NUMBER_COLOR);
    }

    public void setSecondStepNumberColor(String secondStepNumberColor) {
        this.secondStepNumberColor = secondStepNumberColor;
    }

    public String getBranchStepColor() {
        return colorOrDefault(branchStepColor, DEFAULT_BRANCH_STEP_COLOR);
    }

    public void setBranchStepColor(String branchStepColor) {
        this.branchStepColor = branchStepColor;
    }

    public double getBranchStepOpacity() {
        return opacityOrDefault(branchStepOpacity);
    }

    public void setBranchStepOpacity(double branchStepOpacity) {
        this.branchStepOpacity = normalizeOpacity(branchStepOpacity);
    }

    public String getBranchStepNumberColor() {
        return colorOrDefault(branchStepNumberColor, DEFAULT_STEP_NUMBER_COLOR);
    }

    public void setBranchStepNumberColor(String branchStepNumberColor) {
        this.branchStepNumberColor = branchStepNumberColor;
    }

    public ColorTheme getColorTheme() {
        return colorTheme == null ? ColorTheme.LIGHT : colorTheme;
    }

    public void setColorTheme(ColorTheme colorTheme) {
        this.colorTheme = colorTheme;
    }

    public boolean isPieceShadowEnabled() {
        return pieceShadowEnabled;
    }

    public void setPieceShadowEnabled(boolean pieceShadowEnabled) {
        this.pieceShadowEnabled = pieceShadowEnabled;
    }

    /** 阴影方向：0° 向右，顺时针增加。 */
    public double getPieceShadowAngle() {
        return normalizeShadowAngle(pieceShadowAngle == null ? DEFAULT_PIECE_SHADOW_ANGLE : pieceShadowAngle);
    }

    public void setPieceShadowAngle(double pieceShadowAngle) {
        this.pieceShadowAngle = normalizeShadowAngle(pieceShadowAngle);
    }

    /** 阴影长度占棋子直径的百分比，随棋盘缩放。 */
    public double getPieceShadowLength() {
        return normalizeShadowLength(pieceShadowLength == null ? DEFAULT_PIECE_SHADOW_LENGTH : pieceShadowLength);
    }

    public void setPieceShadowLength(double pieceShadowLength) {
        this.pieceShadowLength = normalizeShadowLength(pieceShadowLength);
    }

    /** 阴影圆直径占棋子直径的百分比，随棋盘缩放。 */
    public double getPieceShadowSize() {
        return normalizeShadowSize(pieceShadowSize == null ? DEFAULT_PIECE_SHADOW_SIZE : pieceShadowSize);
    }

    public void setPieceShadowSize(double pieceShadowSize) {
        this.pieceShadowSize = normalizeShadowSize(pieceShadowSize);
    }

    private double normalizeShadowAngle(double angle) {
        if (!Double.isFinite(angle)) {
            return DEFAULT_PIECE_SHADOW_ANGLE;
        }
        return (angle % 360d + 360d) % 360d;
    }

    private double normalizeShadowLength(double length) {
        if (!Double.isFinite(length)) {
            return DEFAULT_PIECE_SHADOW_LENGTH;
        }
        return Math.max(0d, Math.min(50d, length));
    }

    private double normalizeShadowSize(double size) {
        if (!Double.isFinite(size)) {
            return DEFAULT_PIECE_SHADOW_SIZE;
        }
        return Math.max(0d, Math.min(100d, size));
    }

    public ViewMode getViewMode() {
        return viewMode == null ? ViewMode.DEFAULT : viewMode;
    }

    public void setViewMode(ViewMode viewMode) {
        this.viewMode = viewMode;
    }

    public RecognizeMode getRecognizeMode() {
        return recognizeMode == null ? RecognizeMode.YOLO : recognizeMode;
    }

    public void setRecognizeMode(RecognizeMode recognizeMode) {
        this.recognizeMode = recognizeMode;
    }

    private String colorOrDefault(String color, String defaultColor) {
        return color == null || color.isBlank() ? defaultColor : color;
    }

    private double opacityOrDefault(Double opacity) {
        return opacity == null ? DEFAULT_STEP_OPACITY : normalizeOpacity(opacity);
    }

    private double normalizeOpacity(double opacity) {
        if (!Double.isFinite(opacity)) {
            return DEFAULT_STEP_OPACITY;
        }
        return Math.max(0d, Math.min(1d, opacity));
    }

    public long getAnimationConfirmDelay() {
        return animationConfirmDelay;
    }

    public void setAnimationConfirmDelay(long animationConfirmDelay) {
        this.animationConfirmDelay = animationConfirmDelay;
    }
}
