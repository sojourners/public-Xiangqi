package com.sojourners.chess.openbook;

import com.sojourners.chess.model.BookData;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public interface OpenBook {

    List<BookData> get(char[][] board, boolean redGo);

    List<BookData> get(String fenCode, boolean onlyFinalPhase);

    void close();

    default List<BookData> query(char[][] board, boolean redGo, MoveRule mr) {
        List<BookData> list = get(board, redGo);
        sort(list, mr);
        return list;
    }

    default List<BookData> query(String fenCode, boolean onlyFinalPhase, MoveRule mr) {
        List<BookData> list = get(fenCode, onlyFinalPhase);
        sort(list, mr);
        return list;
    }

    default void sort(List<BookData> list, MoveRule mr) {
        Collections.sort(list, new Comparator<BookData>() {
            private Random rd = new SecureRandom();
            @Override
            public int compare(BookData o1, BookData o2) {
                switch (mr) {
                    case BEST_SCORE: {
                        return o1.getScore() > o2.getScore() ? -1 : (o1.getScore() < o2.getScore() ? 1 : 0);
                    }
                    case BEST_WINRATE: {
                        return o1.getWinRate() > o2.getWinRate() ? -1 : (o1.getWinRate() < o2.getWinRate() ? 1 : 0);
                    }
                    case FULL_RANDOM: {
                        if (rd.nextInt(100) < 50) {
                            return 1;
                        } else {
                            return -1;
                        }
                    }
                    case POSITIVE_RANDOM: {
                        if (o1.getScore() > 0 && o2.getScore() > 0) {
                            if (rd.nextInt(100) < 50) {
                                return 1;
                            } else {
                                return -1;
                            }
                        } else {
                            return o1.getScore() > o2.getScore() ? -1 : (o1.getScore() < o2.getScore() ? 1 : 0);
                        }
                    }
                    default: {
                        return 0;
                    }
                }
            }
        });
    }

}
