package com.sojourners.chess.util;

import java.util.HashMap;
import java.util.Map;

public class XiangqiUtils {


    public static boolean canGo(char[][] board, int x1, int y1, int x2, int y2) {
        switch (board[x1][y1]) {
            case 'r':
            case 'R': {
                if (x1 != x2 && y1 != y2) {
                    return false;
                } else if (x1 == x2) {
                    int start = y1 > y2 ? y2 : y1;
                    int end = y1 > y2 ? y1 : y2;
                    for (int i = start + 1; i < end; i++) {
                        if (board[x1][i] != ' ') {
                            return false;
                        }
                    }
                    return true;
                } else {
                    int start = x1 > x2 ? x2 : x1;
                    int end = x1 > x2 ? x1 : x2;
                    for (int i = start + 1; i < end; i++) {
                        if (board[i][y1] != ' ') {
                            return false;
                        }
                    }
                    return true;
                }
            }
            case 'n':
            case 'N': {
                int absX = Math.abs(x1 - x2), abxY = Math.abs(y1 - y2);
                if (!(absX == 1 && abxY == 2 || absX == 2 && abxY == 1)) {
                    return false;
                }
                char c;
                if (absX == 2) {
                    if (x2 > x1) {
                        c = board[x1 + 1][y1];
                    } else {
                        c = board[x1 - 1][y1];
                    }
                } else {
                    if (y2 > y1) {
                        c = board[x1][y1 + 1];
                    } else {
                        c = board[x1][y1 - 1];
                    }
                }
                return c == ' ';
            }
            case 'b':
            case 'B': {
                boolean isRed = isRed(board[x1][y1]);
                if (isRed && x2 < 5 || !isRed && x2 > 4) {
                    return false;
                }
                int absX = Math.abs(x1 - x2), abxY = Math.abs(y1 - y2);
                if (absX != 2 || abxY != 2) {
                    return false;
                }
                return board[(x1 + x2) / 2][(y1 + y2) / 2] == ' ';
            }
            case 'a':
            case 'A': {
                if (y2 < 3 || y2 > 5) {
                    return false;
                }
                boolean isRed = isRed(board[x1][y1]);
                if (isRed && x2 < 7 || !isRed && x2 > 2) {
                    return false;
                }
                int absX = Math.abs(x1 - x2), abxY = Math.abs(y1 - y2);
                return absX == 1 && abxY == 1;
            }
            case 'k':
            case 'K': {
                if (y2 < 3 || y2 > 5) {
                    return false;
                }
                boolean isRed = isRed(board[x1][y1]);
                if (isRed && x2 < 7 || !isRed && x2 > 2) {
                    return false;
                }
                int absX = Math.abs(x1 - x2), abxY = Math.abs(y1 - y2);
                return absX == 1 && abxY == 0 || absX == 0 && abxY == 1;
            }
            case 'c':
            case 'C': {
                if (x1 != x2 && y1 != y2) {
                    return false;
                } else if (x1 == x2) {
                    int start = y1 > y2 ? y2 : y1;
                    int end = y1 > y2 ? y1 : y2;
                    int count = 0;
                    for (int i = start + 1; i < end; i++) {
                        if (board[x1][i] != ' ') {
                            count++;
                        }
                    }
                    return count == 1 && board[x2][y2] != ' ' || count == 0 && board[x2][y2] == ' ';
                } else {
                    int start = x1 > x2 ? x2 : x1;
                    int end = x1 > x2 ? x1 : x2;
                    int count = 0;
                    for (int i = start + 1; i < end; i++) {
                        if (board[i][y1] != ' ') {
                            count++;
                        }
                    }
                    return count == 1 && board[x2][y2] != ' ' || count == 0 && board[x2][y2] == ' ';
                }
            }
            case 'p': {
                if (x1 < 5) {
                    return (y1 == y2) && (x2 - x1) == 1;
                } else {
                    return (y1 == y2) && (x2 - x1) == 1 || (x1 == x2) && Math.abs(y1 - y2) == 1;
                }
            }
            case 'P': {
                if (x1 >= 5) {
                    return (y1 == y2) && (x2 - x1) == -1;
                } else {
                    return (y1 == y2) && (x2 - x1) == -1 || (x1 == x2) && Math.abs(y1 - y2) == 1;
                }
            }
            default:
                return false;
        }

    }

    public static boolean isJiang(char[][] board, boolean isRed) {
        int bx = 0, by = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 3; j < 6; j++) {
                if (board[i][j] == 'k') {
                    bx = j;
                    by = i;
                    break;
                }
            }
        }
        int rx = 0, ry = 0;
        for (int i = 7; i < 10; i++) {
            for (int j = 3; j < 6; j++) {
                if (board[i][j] == 'K') {
                    rx = j;
                    ry = i;
                    break;
                }
            }
        }

        int x = isRed ? rx : bx, y = isRed ? ry : by;
        if (x == 0 && y == 0) {
            return false;
        }
        boolean searchPao = false;
        for (int i = x - 1; i >=0; i--) {
            if (!searchPao) {
                if (i == x - 1) {
                    if (isRed && board[y][i] == 'p' || !isRed && board[y][i] == 'P') {
                        return true;
                    }
                }
                if (isRed && board[y][i] == 'r' || !isRed && board[y][i] == 'R') {
                    return true;
                }
                if (board[y][i] != ' ') {
                    searchPao = true;
                }
            } else {
                if (isRed && board[y][i] == 'c' || !isRed && board[y][i] == 'C') {
                    return true;
                }
                if (board[y][i] != ' ') {
                    break;
                }
            }
        }
        searchPao = false;
        for (int i = x + 1; i < 9; i++) {
            if (!searchPao) {
                if (i == x + 1) {
                    if (isRed && board[y][i] == 'p' || !isRed && board[y][i] == 'P') {
                        return true;
                    }
                }
                if (isRed && board[y][i] == 'r' || !isRed && board[y][i] == 'R') {
                    return true;
                }
                if (board[y][i] != ' ') {
                    searchPao = true;
                }
            } else {
                if (isRed && board[y][i] == 'c' || !isRed && board[y][i] == 'C') {
                    return true;
                }
                if (board[y][i] != ' ') {
                    break;
                }
            }
        }
        searchPao = false;
        for (int j = y - 1; j >= 0; j--) {
            if (!searchPao) {
                if (j == y - 1) {
                    if (isRed && board[j][x] == 'p') {
                        return true;
                    }
                }
                if (isRed && board[j][x] == 'r' || !isRed && board[j][x] == 'R') {
                    return true;
                }
                if (board[j][x] != ' ') {
                    searchPao = true;
                }
            } else {
                if (isRed && board[j][x] == 'c' || !isRed && board[j][x] == 'C') {
                    return true;
                }
                if (board[j][x] != ' ') {
                    break;
                }
            }
        }
        searchPao = false;
        for (int j = y + 1; j < 10; j++) {
            if (!searchPao) {
                if (j == y + 1) {
                    if (!isRed && board[j][x] == 'P') {
                        return true;
                    }
                }
                if (isRed && board[j][x] == 'r' || !isRed && board[j][x] == 'R') {
                    return true;
                }
                if (board[j][x] != ' ') {
                    searchPao = true;
                }
            } else {
                if (isRed && board[j][x] == 'c' || !isRed && board[j][x] == 'C') {
                    return true;
                }
                if (board[j][x] != ' ') {
                    break;
                }
            }
        }

        if ((y - 2 >= 0) && (isRed && (board[y - 2][x + 1] == 'n' && board[y - 1][x + 1] == ' ' || board[y - 2][x - 1] == 'n' && board[y - 1][x - 1] == ' ') || !isRed && (board[y - 2][x + 1] == 'N' && board[y - 1][x + 1] == ' ' || board[y - 2][x - 1] == 'N' && board[y - 1][x - 1] == ' '))) {
            return true;
        }
        if ((y + 2 < 10) && (isRed && (board[y + 2][x + 1] == 'n' && board[y + 1][x + 1] == ' ' || board[y + 2][x - 1] == 'n' && board[y + 1][x - 1] == ' ') || !isRed && (board[y + 2][x + 1] == 'N' && board[y + 1][x + 1] == ' ' || board[y + 2][x - 1] == 'N' && board[y + 1][x - 1] == ' '))) {
            return true;
        }
        if ((y - 1 >= 0 && y + 1 < 10) && (isRed && (board[y - 1][x - 2] == 'n' && board[y - 1][x - 1] == ' ' || y + 1 < 10 && board[y + 1][x - 2] == 'n' && board[y + 1][x - 1] == ' ') || !isRed && (y - 1 >=0 && board[y - 1][x - 2] == 'N' && board[y - 1][x - 1] == ' ' || board[y + 1][x - 2] == 'N' && board[y + 1][x - 1] == ' '))) {
            return true;
        }
        if ((y - 1 >= 0 && y + 1 < 10) && (isRed && (board[y - 1][x + 2] == 'n' && board[y - 1][x + 1] == ' ' || y + 1 < 10 && board[y + 1][x + 2] == 'n' && board[y + 1][x + 1] == ' ') || !isRed && (y - 1 >=0 && board[y - 1][x + 2] == 'N' && board[y - 1][x + 1] == ' ' || board[y + 1][x + 2] == 'N' && board[y + 1][x + 1] == ' '))) {
            return true;
        }

        if (rx == bx) {
            boolean f = true;
            for (int j = by + 1; j < ry; j++) {
                if (board[j][rx] != ' ') {
                    f = false;
                }
            }
            if (f) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSha(char[][] board, boolean isRed) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (!isRed && (board[i][j] >= 'a' && board[i][j] <= 'z') || isRed && (board[i][j] >= 'A' && board[i][j] <= 'Z')) {
                    if (jieJiang(board, isRed, j, i)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    private static boolean jieJiang(char[][] board, boolean isRed, int x, int y) {
        switch (board[y][x]) {
            case 'k':
            case 'K': {
                if (x - 1 >= 3 && (board[y][x - 1] == ' ' || isRed(board[y][x - 1]) != isRed)) {
                    if (jieJiang(board, isRed, x, y, x - 1, y)) {
                        return true;
                    }
                }
                if (x + 1 <= 5 && (board[y][x + 1] == ' ' || isRed(board[y][x + 1]) != isRed)) {
                    if (jieJiang(board, isRed, x, y, x + 1, y)) {
                        return true;
                    }
                }
                if ((isRed && y - 1 >= 7 || !isRed && y - 1 >= 0) && (board[y - 1][x] == ' ' || isRed(board[y - 1][x]) != isRed)) {
                    if (jieJiang(board, isRed, x, y, x, y - 1)) {
                        return true;
                    }
                }
                if ((isRed && y + 1 <= 9 || !isRed && y + 1 <= 2) && (board[y + 1][x] == ' ' || isRed(board[y + 1][x]) != isRed)) {
                    if (jieJiang(board, isRed, x, y, x, y + 1)) {
                        return true;
                    }
                }
                return false;
            }
            case 'c':
            case 'C': {
                boolean f = false;
                for (int i = y, j = x - 1; j >= 0; j--) {
                    if (!f) {
                        if (board[i][j] == ' ') {
                            if (jieJiang(board, isRed, x, y, j, i)) {
                                return true;
                            }
                        } else {
                            f = true;
                            continue;
                        }
                    } else {
                        if (board[i][j] != ' ') {
                            if (isRed(board[i][j]) != isRed) {
                                if (jieJiang(board, isRed, x, y, j, i)) {
                                    return true;
                                }
                            }
                            break;
                        }
                    }
                }
                f = false;
                for (int i = y, j = x + 1; j < 9; j++) {
                    if (!f) {
                        if (board[i][j] == ' ') {
                            if (jieJiang(board, isRed, x, y, j, i)) {
                                return true;
                            }
                        } else {
                            f = true;
                            continue;
                        }
                    } else {
                        if (board[i][j] != ' ') {
                            if (isRed(board[i][j]) != isRed) {
                                if (jieJiang(board, isRed, x, y, j, i)) {
                                    return true;
                                }
                            }
                            break;
                        }
                    }
                }
                f = false;
                for (int i = y - 1, j = x; i >= 0; i--) {
                    if (!f) {
                        if (board[i][j] == ' ') {
                            if (jieJiang(board, isRed, x, y, j, i)) {
                                return true;
                            }
                        } else {
                            f = true;
                            continue;
                        }
                    } else {
                        if (board[i][j] != ' ') {
                            if (isRed(board[i][j]) != isRed) {
                                if (jieJiang(board, isRed, x, y, j, i)) {
                                    return true;
                                }
                            }
                            break;
                        }
                    }
                }
                f = false;
                for (int i = y + 1, j = x; i < 10; i++) {
                    if (!f) {
                        if (board[i][j] == ' ') {
                            if (jieJiang(board, isRed, x, y, j, i)) {
                                return true;
                            }
                        } else {
                            f = true;
                            continue;
                        }
                    } else {
                        if (board[i][j] != ' ') {
                            if (isRed(board[i][j]) != isRed) {
                                if (jieJiang(board, isRed, x, y, j, i)) {
                                    return true;
                                }
                            }
                            break;
                        }
                    }
                }
                return false;
            }
            case 'p':
            case 'P': {
                if (isRed && y - 1 >= 0 || !isRed && y + 1 <= 9) {
                    int i = isRed ? y - 1 : y + 1;
                    if ((board[i][x] == ' ' || isRed(board[i][x]) != isRed) && jieJiang(board, isRed, x, y, x, i)) {
                        return true;
                    }
                }
                if (isRed && y <= 4 && x - 1 >= 0 || !isRed && y >= 5 && x - 1 >= 0) {
                    if ((board[y][x - 1] == ' ' || isRed(board[y][x - 1]) != isRed) && jieJiang(board, isRed, x, y, x - 1, y)) {
                        return true;
                    }
                }
                if (isRed && y <= 4 && x + 1 <= 8 || !isRed && y >= 5 && x + 1 <= 8) {
                    if ((board[y][x + 1] == ' ' || isRed(board[y][x + 1]) != isRed) && jieJiang(board, isRed, x, y, x + 1, y)) {
                        return true;
                    }
                }
                return false;
            }
            case 'r':
            case 'R': {
                for (int i = y, j = x - 1; j >= 0; j--) {
                    if (board[i][j] == ' ' || isRed(board[i][j]) != isRed) {
                        if (jieJiang(board, isRed, x, y, j, i)) {
                            return true;
                        }
                    } else {
                        break;
                    }
                }
                for (int i = y, j = x + 1; j < 9; j++) {
                    if (board[i][j] == ' ' || isRed(board[i][j]) != isRed) {
                        if (jieJiang(board, isRed, x, y, j, i)) {
                            return true;
                        }
                    } else {
                        break;
                    }
                }
                for (int i = y - 1, j = x; i >= 0; i--) {
                    if (board[i][j] == ' ' || isRed(board[i][j]) != isRed) {
                        if (jieJiang(board, isRed, x, y, j, i)) {
                            return true;
                        }
                    } else {
                        break;
                    }
                }
                for (int i = y + 1, j = x; i < 10; i++) {
                    if (board[i][j] == ' ' || isRed(board[i][j]) != isRed) {
                        if (jieJiang(board, isRed, x, y, j, i)) {
                            return true;
                        }
                    } else {
                        break;
                    }
                }
                return false;
            }
            case 'n':
            case 'N': {
                if (x - 2 >= 0 && board[y][x - 1] == ' ') {
                    if (y - 1 >= 0 && (board[y - 1][x - 2] == ' ' || isRed(board[y - 1][x - 2]) != isRed)) {
                        if (jieJiang(board, isRed, x, y, x - 2, y - 1))
                            return true;
                    }
                    if (y + 1 <= 9 && (board[y + 1][x - 2] == ' ' || isRed(board[y + 1][x - 2]) != isRed)) {
                        if (jieJiang(board, isRed, x, y, x - 2, y + 1))
                            return true;
                    }
                }
                if (x + 2 <= 8 && board[y][x + 1] == ' ') {
                    if (y - 1 >= 0 && (board[y - 1][x + 2] == ' ' || isRed(board[y - 1][x + 2]) != isRed)) {
                        if (jieJiang(board, isRed, x, y, x + 2, y - 1))
                            return true;
                    }
                    if (y + 1 <= 9 && (board[y + 1][x + 2] == ' ' || isRed(board[y + 1][x + 2]) != isRed)) {
                        if (jieJiang(board, isRed, x, y, x + 2, y + 1))
                            return true;
                    }
                }
                if (y - 2 >= 0 && board[y - 1][x] == ' ') {
                    if (x - 1 >= 0 && (board[y - 2][x - 1] == ' ' || isRed(board[y - 2][x - 1]) != isRed)) {
                        if (jieJiang(board, isRed, x, y, x - 1, y - 2))
                            return true;
                    }
                    if (x + 1 <= 8 && (board[y - 2][x + 1] == ' ' || isRed(board[y - 2][x + 1]) != isRed)) {
                        if (jieJiang(board, isRed, x, y, x + 1, y - 2))
                            return true;
                    }
                }
                if (y + 2 <= 9 && board[y + 1][x] == ' ') {
                    if (x - 1 >= 0 && (board[y + 2][x - 1] == ' ' || isRed(board[y + 2][x - 1]) != isRed)) {
                        if (jieJiang(board, isRed, x, y, x - 1, y + 2))
                            return true;
                    }
                    if (x + 1 <= 8 && (board[y + 2][x + 1] == ' ' || isRed(board[y + 2][x + 1]) != isRed)) {
                        if (jieJiang(board, isRed, x, y, x + 1, y + 2))
                            return true;
                    }
                }
                return false;
            }
            case 'b':
            case 'B': {
                if (x - 2 >= 0 && y - 2 >= 0 && board[y - 1][x - 1] == ' ' && (isRed && y - 2 >= 5 || !isRed)) {
                    if (board[y - 2][x - 2] == ' ' || isRed(board[y - 2][x - 2]) != isRed) {
                        if (jieJiang(board, isRed, x, y, x - 2, y - 2))
                            return true;
                    }
                }
                if (x - 2 >= 0 && y + 2 <= 9 && board[y + 1][x - 1] == ' ' && (!isRed && y + 2 <= 4 || isRed)) {
                    if (board[y + 2][x - 2] == ' ' || isRed(board[y + 2][x - 2]) != isRed) {
                        if (jieJiang(board, isRed, x, y, x - 2, y + 2))
                            return true;
                    }
                }
                if (x + 2 <= 8 && y - 2 >= 0 && board[y - 1][x + 1] == ' ' && (isRed && y - 2 >= 5 || !isRed)) {
                    if (board[y - 2][x + 2] == ' ' || isRed(board[y - 2][x + 2]) != isRed) {
                        if (jieJiang(board, isRed, x, y, x + 2, y - 2))
                            return true;
                    }
                }
                if (x + 2 <= 8 && y + 2 <= 9 && board[y + 1][x + 1] == ' ' && (!isRed && y + 2 <= 4 || isRed)) {
                    if (board[y + 2][x + 2] == ' ' || isRed(board[y + 2][x + 2]) != isRed) {
                        if (jieJiang(board, isRed, x, y, x + 2, y + 2))
                            return true;
                    }
                }
                return false;
            }
            case 'a':
            case 'A': {
                if (x - 1 >= 3 && y - 1 >= (isRed ? 7 : 0) && (board[y - 1][x - 1] == ' ' || isRed(board[y - 1][x - 1]) != isRed)) {
                    if (jieJiang(board, isRed, x, y, x - 1, y - 1))
                        return true;
                }
                if (x - 1 >= 3 && y + 1 <= (isRed ? 9 : 2) && (board[y + 1][x - 1] == ' ' || isRed(board[y + 1][x - 1]) != isRed)) {
                    if (jieJiang(board, isRed, x, y, x - 1, y + 1))
                        return true;
                }
                if (x + 1 <= 5 && y - 1 >= (isRed ? 7 : 0) && (board[y - 1][x + 1] == ' ' || isRed(board[y - 1][x + 1]) != isRed)) {
                    if (jieJiang(board, isRed, x, y, x + 1, y - 1))
                        return true;
                }
                if (x + 1 <= 5 && y + 1 <= (isRed ? 9 : 2) && (board[y + 1][x + 1] == ' ' || isRed(board[y + 1][x + 1]) != isRed)) {
                    if (jieJiang(board, isRed, x, y, x + 1, y + 1))
                        return true;
                }
                return false;
            }

            default:
                return false;
        }
    }
    private static boolean jieJiang(char[][] board, boolean isRed, int x1, int y1, int x2, int y2) {
        char tmp = board[y2][x2];
        board[y2][x2] = board[y1][x1];
        board[y1][x1] = ' ';
        boolean result = isJiang(board, isRed);
        board[y1][x1] = board[y2][x2];
        board[y2][x2] = tmp;
        return !result;
    }
    public static boolean isRed(char c) {
        return c >= 'A' && c <= 'Z';
    }

    public static boolean validateChessBoard(char[][] board) {
        Map<Character, Integer> map = new HashMap<>(32);
        // 校验棋子位置是否合法
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                if ((board[i][j] == 'k' || board[i][j] == 'K') && ((i > 2 && i < 7) || j < 3 || j > 5)) {
                    return false;
                }
                if ((board[i][j] == 'b' || board[i][j] == 'B') &&
                        ((i != 0 && i != 2 && i != 4 && i != 5 && i != 7 && i != 9)
                                || (j != 0 && j != 2 && j != 4 && j != 6 && j != 8))) {
                    return false;
                }
                if ((board[i][j] == 'a' || board[i][j] == 'A') && ((i > 2 && i < 7) || j < 3 || j > 5 || (i <= 2 && (i + j) % 2 == 0) || (i >= 7 && (i + j) % 2 == 1))) {
                    return false;
                }
                if (board[i][j] != ' ') {
                    if (map.containsKey(board[i][j])) {
                        map.put(board[i][j], map.get(board[i][j]) + 1);
                    } else {
                        map.put(board[i][j], 1);
                    }
                }
            }
        }
        // 校验棋子数量是否合法
        for (Map.Entry<Character, Integer> entry : map.entrySet()) {
            char key = entry.getKey();
            Integer value = entry.getValue();
            if (key == 'r' || key == 'R' || key == 'b' || key == 'B' || key == 'a' || key == 'A' || key == 'c' || key == 'C' || key == 'n' || key == 'N') {
                if (value > 2) {
                    return false;
                }
            }
            if (key == 'p' || key == 'P') {
                if (value > 5) {
                    return false;
                }
            }
        }
        if (map.get('k') == null || map.get('k') != 1 || map.get('K') == null || map.get('K') != 1) {
            return false;
        }
        return true;
    }

    public static boolean isReverse(String fenCode) {
        int rIndex = fenCode.indexOf('K');
        int bIndex = fenCode.indexOf('k');
        if (rIndex == -1 && bIndex == -1) {
            return false;
        } else if (rIndex == -1 || bIndex == -1) {
            int xIndex = 0;
            for (int i = 0; i < 5; i++) {
                xIndex = fenCode.indexOf('/', xIndex + 1);
            }
            return rIndex == -1 ? bIndex > xIndex : rIndex < xIndex;
        } else if (rIndex != -1 && bIndex != -1) {
            return rIndex < bIndex;
        }
        return false;
    }
}
