package com.sojourners.chess.util;

public final class ScoreScale {

    private static final double FIRST_SEGMENT_SCORE = 333;
    private static final double SECOND_SEGMENT_SCORE = 900;
    private static final double MAX_SCORE = 9999;

    private ScoreScale() {
    }

    public static double toAxisValue(double score) {
        double magnitude = Math.min(Math.abs(score), MAX_SCORE);
        double position;
        if (magnitude <= FIRST_SEGMENT_SCORE) {
            position = magnitude / FIRST_SEGMENT_SCORE;
        } else if (magnitude <= SECOND_SEGMENT_SCORE) {
            position = 1 + (magnitude - FIRST_SEGMENT_SCORE) / (SECOND_SEGMENT_SCORE - FIRST_SEGMENT_SCORE);
        } else {
            position = 2 + (magnitude - SECOND_SEGMENT_SCORE) / (MAX_SCORE - SECOND_SEGMENT_SCORE);
        }
        return Math.copySign(position, score);
    }

    public static double toScore(double axisValue) {
        double position = Math.min(Math.abs(axisValue), 3);
        double score;
        if (position <= 1) {
            score = position * FIRST_SEGMENT_SCORE;
        } else if (position <= 2) {
            score = FIRST_SEGMENT_SCORE + (position - 1) * (SECOND_SEGMENT_SCORE - FIRST_SEGMENT_SCORE);
        } else {
            score = SECOND_SEGMENT_SCORE + (position - 2) * (MAX_SCORE - SECOND_SEGMENT_SCORE);
        }
        return Math.copySign(score, axisValue);
    }
}
