package com.sojourners.chess.util;

public class MathUtils {

    public static double calculateDistance(int x1, int y1, int x2, int y2) {

        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));

    }

    public static double calculateAngle(int x1, int y1, int x2, int y2) {
        double a = x1;
        double b = calculateDistance(x1, y1, x2, y2);
        double c = calculateDistance(0, y1, x2, y2);
        double pai = Math.acos((a * a + b * b - c * c)/(2.0 * a * b));
        double angle = Math.toDegrees(pai);
        if (y2 > y1) {
            angle = 360 - angle;
        }
        return angle;
    }

}
