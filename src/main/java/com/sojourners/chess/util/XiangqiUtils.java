package com.sojourners.chess.util;

import com.sojourners.chess.board.ChessBoard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XiangqiUtils {

    public static final Map<Character, Character> cnMap = new HashMap<>(32);
    public static final Map<Character, String> map = new HashMap<>(32);

    static {
        cnMap.put('车', 'r');
        cnMap.put('马', 'n');
        cnMap.put('相', 'B');
        cnMap.put('象', 'b');
        cnMap.put('士', 'a');
        cnMap.put('仕', 'A');
        cnMap.put('将', 'k');
        cnMap.put('帅', 'K');
        cnMap.put('炮', 'C');
        cnMap.put('卒', 'p');
        cnMap.put('兵', 'P');

        cnMap.put('一', '１');
        cnMap.put('二', '２');
        cnMap.put('三', '３');
        cnMap.put('四', '４');
        cnMap.put('五', '５');
        cnMap.put('六', '６');
        cnMap.put('七', '７');
        cnMap.put('八', '８');
        cnMap.put('九', '９');

        map.put('r', "车");
        map.put('n', "马");
        map.put('b', "象");
        map.put('a', "士");
        map.put('k', "将");
        map.put('c', "炮");
        map.put('p', "卒");

        map.put('R', "车");
        map.put('N', "马");
        map.put('B', "相");
        map.put('A', "仕");
        map.put('K', "帅");
        map.put('C', "炮");
        map.put('P', "兵");

        map.put('１', "一");
        map.put('２', "二");
        map.put('３', "三");
        map.put('４', "四");
        map.put('５', "五");
        map.put('６', "六");
        map.put('７', "七");
        map.put('８', "八");
        map.put('９', "九");
    }

    public static boolean canGo(char[][] board, int x1, int y1, int x2, int y2) {
        if (board[x1][y1] == ' ' || board[x2][y2] != ' ' && isRed(board[x1][y1]) == isRed(board[x2][y2])) {
            return false;
        }
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
                int absX = Math.abs(x1 - x2), absY = Math.abs(y1 - y2);
                if (!(absX == 1 && absY == 2 || absX == 2 && absY == 1)) {
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
                int absX = Math.abs(x1 - x2), absY = Math.abs(y1 - y2);
                return absX == 1 && absY == 1;
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
        if ((y - 1 >= 0) && (isRed && (board[y - 1][x - 2] == 'n' && board[y - 1][x - 1] == ' ' || board[y - 1][x + 2] == 'n' && board[y - 1][x + 1] == ' ') || !isRed && (board[y - 1][x - 2] == 'N' && board[y - 1][x - 1] == ' ' || board[y - 1][x + 2] == 'N' && board[y - 1][x + 1] == ' '))) {
            return true;
        }
        if ((y + 1 < 10) && (isRed && (board[y + 1][x - 2] == 'n' && board[y + 1][x - 1] == ' ' || board[y + 1][x + 2] == 'n' && board[y + 1][x + 1] == ' ') || !isRed && (board[y + 1][x - 2] == 'N' && board[y + 1][x - 1] == ' ' || board[y + 1][x + 2] == 'N' && board[y + 1][x + 1] == ' '))) {
            return true;
        }

        if (rx == bx) {
            boolean f = true;
            for (int j = by + 1; j < ry; j++) {
                if (board[j][rx] != ' ') {
                    f = false;
                    break;
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

    public static void fenToBoard(char[][] board, String fenCode) {
        try {
            String[] arr = fenCode.split(" ")[0].split("/");
            if (XiangqiUtils.isReverse(fenCode)) {
                for (int i = 0; i < arr.length / 2; i++) {
                    String tmp = arr[i];
                    arr[i] = new StringBuffer(arr[arr.length - 1 - i]).reverse().toString();
                    arr[arr.length - 1 - i] = new StringBuffer(tmp).reverse().toString();
                }
            }
            for (int i = 0; i < 10; i++) {
                int p = 0;
                for (char c : arr[i].toCharArray()) {
                    if (c >= '1' && c <= '9') {
                        int count = c - '0';
                        while (count-- > 0) {
                            board[i][p++] = ' ';
                        }
                    } else {
                        board[i][p++] = c;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static char[][] fenToBoard(String fenCode) {
        char[][] board = new char[10][9];
        fenToBoard(board, fenCode);
        return board;
    }

    public static ChessBoard.Step translateCnMove(char[][] board, StringBuilder sb, String move) {
        if (StringUtils.isEmpty(move) || move.length() < 4) {
            sb.append(move);
            return null;
        }
        char a = move.charAt(0), b = move.charAt(1), c = move.charAt(2), d = move.charAt(3);
        if (b >= '1' && b <= '9') b = (char) (b - '1' + '１');
        if (d >= '1' && d <= '9') d = (char) (d - '1' + '１');
        char piece;
        int fromI = 0, fromJ = 0, toI = 0, toJ = 0;
        boolean isRed = map.containsValue(String.valueOf(d));
        // 普通棋子记法（如“象７退５”）在同一路有多个同类棋子时的候选起点
        List<Integer> candidates = null;
        if (a == '前' || a == '中' || a == '后') {
            piece = cnMap.get(b);
            piece = isRed ? Character.toUpperCase(piece) : Character.toLowerCase(piece);
            boolean isP = piece == 'p' || piece == 'P';
            List<Integer> tmp = new ArrayList<>();
            for (int j = 0; j < 9; j++) {
                int count = 0;
                tmp.clear();
                for (int i = 0; i < 10; i++) {
                    if (board[i][j] == piece) {
                        count++;
                        tmp.add(i);
                    }
                }
                if (!isP && count == 2 || isP && (count == 2 || count == 3)) {
                    fromJ = j;
                    if (a == '前') {
                        fromI = isRed ? tmp.getFirst() : tmp.getLast();
                    } else if (a == '中') {
                        fromI = tmp.get(1);
                    } else if (a == '后') {
                        fromI = isRed ? tmp.getLast() : tmp.getFirst();
                    }
                    break;
                }
            }
        } else if (a == '一' || a == '二' || a == '三' || a == '四' || a == '五') {
            piece = cnMap.get(b);
            piece = isRed ? Character.toUpperCase(piece) : Character.toLowerCase(piece);
            List<List<Integer>> tmp = new ArrayList<>();
            for (int j = 0; j < 9; j++) {
                List<Integer> list = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    if (board[i][j] == piece) {
                        list.add(i);
                    }
                }
                if (list.size() == 1) list.clear();
                tmp.add(list);
            }
            int n = cnMap.get(a) - '１' + 1;
            if (isRed) {
                for (int j = 8; j >= 0; j--) {
                    if (n > tmp.get(j).size()) {
                        n -= tmp.get(j).size();
                    } else {
                        fromJ = j;
                        fromI = tmp.get(j).get(n - 1);
                        break;
                    }
                }
            } else {
                for (int j = 0; j < 9; j++) {
                    if (n > tmp.get(j).size()) {
                        n -= tmp.get(j).size();
                    } else {
                        fromJ = j;
                        fromI = tmp.get(j).get(tmp.get(j).size() - n);
                        break;
                    }
                }
            }
        } else {
            piece = cnMap.get(a);
            piece = isRed ? Character.toUpperCase(piece) : Character.toLowerCase(piece);
            fromJ = isRed ? 8 - (cnMap.get(b) - '１') : b - '１';
            // 同一路可能有多个同类棋子，收集所有候选起点，稍后按可走性选出正确的一个
            candidates = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                if (board[i][fromJ] == piece) {
                    candidates.add(i);
                }
            }
            if (!candidates.isEmpty()) {
                fromI = candidates.get(0);
            }
        }
        int dist = (isRed ? cnMap.get(d) - '１' : d - '１') + 1;
        if (candidates == null || candidates.size() <= 1) {
            int[] target = computeTarget(fromI, fromJ, piece, c, dist, isRed);
            toI = target[0];
            toJ = target[1];
        } else {
            // 同一路有多个同类棋子：逐个试算终点，选出真正可走的那个起点。
            // 例如“象７退５”时 7 路可能有两枚象，底线的象再“退”会越界。
            boolean resolved = false;
            for (int cand : candidates) {
                int[] target = computeTarget(cand, fromJ, piece, c, dist, isRed);
                int tI = target[0], tJ = target[1];
                if (tI < 0 || tI > 9 || tJ < 0 || tJ > 8) {
                    continue;
                }
                if (canGo(board, cand, fromJ, tI, tJ)) {
                    fromI = cand;
                    toI = tI;
                    toJ = tJ;
                    resolved = true;
                    break;
                }
            }
            if (!resolved) {
                int[] target = computeTarget(fromI, fromJ, piece, c, dist, isRed);
                toI = target[0];
                toJ = target[1];
            }
        }
        sb.append(ChessBoard.stepForEngine(fromJ, fromI, toJ, toI));
        return new ChessBoard.Step(new ChessBoard.Point(fromJ, fromI), new ChessBoard.Point(toJ, toI));
    }

    private static int[] computeTarget(int fromI, int fromJ, char piece, char c, int dist, boolean isRed) {
        int toI = fromI, toJ = fromJ;
        if (c == '进' || c == '退') {
            if (piece == 'r' || piece == 'c' || piece == 'p' || piece == 'k' || piece == 'R' || piece == 'C' || piece == 'P' || piece == 'K') {
                toJ = fromJ;
                int g = c == '进' ? dist : -dist;
                toI = isRed ? fromI - g : fromI + g;
            } else if (piece == 'n' || piece == 'N') {
                toJ = isRed ? 8 - dist + 1 : dist - 1;
                int g = Math.abs(fromJ - toJ) == 1 ? 2 : 1;
                g = c == '进' ? g : -g;
                toI = isRed ? fromI - g : fromI + g;
            } else if (piece == 'b' || piece == 'B') {
                toJ = isRed ? 8 - dist + 1 : dist - 1;
                int g = c == '进' ? 2 : -2;
                toI = isRed ? fromI - g : fromI + g;
            } else if (piece == 'a' || piece == 'A') {
                toJ = isRed ? 8 - dist + 1 : dist - 1;
                int g = c == '进' ? 1 : -1;
                toI = isRed ? fromI - g : fromI + g;
            }
        } else if (c == '平') {
            toI = fromI;
            toJ = isRed ? 8 - dist + 1 : dist - 1;
        }
        return new int[]{toI, toJ};
    }

    public static ChessBoard.Step translate(char[][] board, StringBuilder sb, String move, boolean hasGo) {
        if (StringUtils.isEmpty(move) || move.length() < 4) {
            sb.append(move);
            return null;
        }
        char a = move.charAt(0), b = move.charAt(1), c = move.charAt(2), d = move.charAt(3);
        int fromI = 9 - Integer.parseInt(String.valueOf(b)), toI = 9 - Integer.parseInt(String.valueOf(d));
        int fromJ = a - 'a', toJ = c - 'a';
        char piece = hasGo ? board[toI][toJ] : board[fromI][fromJ];
        boolean isRed = XiangqiUtils.isRed(piece);
        String name = map.get(piece);
        String prefix = getSameFilePrefix(board, fromI, fromJ, toI, toJ, piece, isRed, hasGo);
        if (prefix != null) {
            sb.append(prefix).append(name);
        } else {
            sb.append(name);
            char pos = getPos(fromJ, isRed);
            sb.append(isRed ? map.get(pos) : pos);
        }
        if (fromI == toI && fromJ != toJ) {
            sb.append("平");
            char pos = getPos(toJ, isRed);
            sb.append(isRed ? map.get(pos) : pos);
        } else if (fromI != toI && fromJ == toJ) {
            if (isRed) {
                sb.append(fromI > toI ? "进" : "退");
            } else {
                sb.append(fromI < toI ? "进" : "退");
            }
            char pos = (char) ('０' + (Math.abs(fromI - toI)));
            sb.append(isRed ? map.get(pos) : pos);
        } else {
            if (isRed) {
                sb.append(fromI > toI ? "进" : "退");
            } else {
                sb.append(fromI < toI ? "进" : "退");
            }
            char pos = getPos(toJ, isRed);
            sb.append(isRed ? map.get(pos) : pos);
        }
        return new ChessBoard.Step(new ChessBoard.Point(fromJ, fromI), new ChessBoard.Point(toJ, toI));
    }

    private static char getPos(int j, boolean isRed) {
        if (isRed) {
            return (char) ('０' + 9 - j);
        } else {
            return (char) ('０' + j + 1);
        }
    }

    private static String getSameFilePrefix(char[][] board, int fromI, int fromJ, int toI, int toJ, char piece, boolean isRed, boolean hasGo) {
        if (piece == 'r' || piece == 'c' || piece == 'n' || piece == 'R' || piece == 'C' || piece == 'N') {
            for (int i = 0; i < fromI; i++) {
                if (board[i][fromJ] == piece && !(hasGo && i == toI && fromJ == toJ)) {
                    return isRed ? "后" : "前";
                }
            }
            for (int i = fromI + 1; i < 10; i++) {
                if (board[i][fromJ] == piece && !(hasGo && i == toI && fromJ == toJ)) {
                    return isRed ? "前" : "后";
                }
            }
            return null;
        } else if (piece == 'p' || piece == 'P') {
            int before = 0, after = 0;
            for (int i = 0; i < fromI; i++) {
                if (board[i][fromJ] == piece && !(hasGo && i == toI && fromJ == toJ)) {
                    before++;
                }
            }
            for (int i = fromI + 1; i < 10; i++) {
                if (board[i][fromJ] == piece && !(hasGo && i == toI && fromJ == toJ)) {
                    after++;
                }
            }
            if (before == 0 && after == 0) {
                return null;
            } else if (before + after >= 3) {
                return map.get((char)('０' + (isRed ? before : after) + 1));
            } else {
                int left = 0, right = 0;
                for (int j = 8; j > fromJ; j--) {
                    int count = 0;
                    for (int i = 0; i < 10; i++) {
                        if (board[i][j] == piece && !(hasGo && i == toI && j == toJ)) {
                            count++;
                        }
                    }
                    if (count > 1) {
                        right += count;
                        break;
                    }
                }
                for (int j = fromJ - 1; j >= 0; j--) {
                    int count = 0;
                    for (int i = 0; i < 10; i++) {
                        if (board[i][j] == piece && !(hasGo && i == toI && j == toJ)) {
                            count++;
                        }
                    }
                    if (count > 1) {
                        left += count;
                        break;
                    }
                }
                if (left == 0 && right == 0) {
                    if (before == 1 && after == 1) {
                        return "中";
                    } else if (after == 0) {
                        return isRed ? "后" : "前";
                    } else if (before == 0) {
                        return isRed ? "前" : "后";
                    }
                } else if (left > 0) {
                    return map.get((char)('０' + (isRed ? before : after + left) + 1));
                } else if (right > 0) {
                    return map.get((char)('０' + (isRed ? before + right : after) + 1));
                }
            }
        }
        return null;
    }
}
