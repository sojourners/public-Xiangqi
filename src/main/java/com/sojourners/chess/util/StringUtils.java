package com.sojourners.chess.util;

public class StringUtils {

    public static boolean isDigit(String str) {
        return str.matches("^-?\\d+$");
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isPositiveInt(String str) {
        return str.matches("^[1-9]\\d*$");
    }

    public static boolean isNonNegativeInt(String str) {
        return "0".equals(str) || isPositiveInt(str);
    }

    /**
     * 是否为正数（支持小数）
     */
    public static boolean isPositiveNumber(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        try {
            return Double.parseDouble(str) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
