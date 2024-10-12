package com.sojourners.chess.model;

import com.sojourners.chess.board.ChessBoard;

import java.util.List;

/**
 * 思考细节数据显示
 */
public class ThinkData {

    private Integer depth;

    private Integer score;

    private Integer mate;

    private Long nps;

    private Long time;

    private List<String> detail;

    private String title;

    private String body;

    private Boolean isValid;

    public ThinkData() {

    }

    public void generate(boolean redGo, boolean isReverse, ChessBoard board) {
        // 生成title
        StringBuilder sb = new StringBuilder();
        sb.append("深度: ").append(depth).append("  ");
        boolean f = false;
        if (score == null) {
            sb.append("绝杀: ");
            score = mate;
            f = true;
        } else {
            sb.append("分数: ");
        }
        if (redGo && isReverse || !redGo && !isReverse) {
            score = -score;
        }
        sb.append(score).append(f ? "步  " : "  ");
        sb.append("NPS: ").append(nps / 1000).append("K  ");
        sb.append("时间: ").append(String.format("%.1fs", time / 1000D));
        title = sb.toString();
        // 生成body
        body = board.translate(detail);
        // 是否有效（处理分析模式下null数据）
        isValid = !body.contains("null");
    }

    public Boolean getValid() {
        return isValid;
    }

    public void setValid(Boolean valid) {
        isValid = valid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Integer getMate() {
        return mate;
    }

    public void setMate(Integer mate) {
        this.mate = mate;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Long getNps() {
        return nps;
    }

    public void setNps(Long nps) {
        this.nps = nps;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public List<String> getDetail() {
        return detail;
    }

    public void setDetail(List<String> detail) {
        this.detail = detail;
    }
}
