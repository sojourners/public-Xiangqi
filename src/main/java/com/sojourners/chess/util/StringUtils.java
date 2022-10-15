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
}
