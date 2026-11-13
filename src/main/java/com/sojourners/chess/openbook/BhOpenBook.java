package com.sojourners.chess.openbook;

import com.sojourners.chess.model.BookData;
import com.sojourners.chess.openbook.reader.ObkEntry;
import com.sojourners.chess.openbook.reader.ObkReader;
import com.sojourners.chess.util.ZobristUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BhOpenBook implements OpenBook {

    private final ObkReader reader;

    private final String name;

    public BhOpenBook(String bookPath) throws SQLException {
        try {
            this.reader = ObkReader.open(Paths.get(bookPath));
        } catch (IOException e) {
            throw new SQLException("Cannot open OBK book: " + bookPath, e);
        }
        this.name = new File(bookPath).getName();
    }

    @Override
    public List<BookData> get(char[][] board, boolean redGo) {

        long zobrist = ZobristUtils.getZobristFromBoard(board, redGo, false);
        List<BookData> results = get(zobrist, false);

        zobrist = ZobristUtils.getZobristFromBoard(board, redGo, true);
        results.addAll(get(zobrist, true));

        return results;
    }

    private List<BookData> get(long zobrist, boolean leftRightSwap) {
        List<BookData> results = new ArrayList<>();

        try {
            for (ObkEntry entry : reader.find(zobrist)) {
                if (entry.getValid() != 1) {
                    continue;
                }
                BookData bd = new BookData();
                bd.setScore(entry.getScore());
                bd.setWinNum(entry.getWin());
                bd.setDrawNum(entry.getDraw());
                bd.setLoseNum(entry.getLost());
                long total = (long) bd.getWinNum() + bd.getDrawNum() + bd.getLoseNum();
                int winRate = total == 0 ? 0 : (int) (10000 * (bd.getWinNum() + bd.getDrawNum() / 2.0d) / total);
                bd.setWinRate(winRate / 100d);
                bd.setNote(entry.getMemoText());
                bd.setMove(ZobristUtils.getMoveFromVmove(entry.getMove(), leftRightSwap));

                bd.setSource(this.name);
                results.add(bd);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return results;
    }

    @Override
    public List<BookData> get(String fenCode, boolean onlyFinalPhase) {
        return null;
    }

    @Override
    public void close() {
        try {
            this.reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
