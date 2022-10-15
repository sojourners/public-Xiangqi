package com.sojourners.chess.openbook;

import com.sojourners.chess.model.BookData;
import com.sojourners.chess.util.ZobristUtils;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BhOpenBook implements OpenBook {

    private Connection connection;

    private String name;

    public BhOpenBook(String bookPath) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + bookPath);
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

        String sql;
        if (zobrist < 0) {
            double zobristDouble = Double.longBitsToDouble(zobrist);
            sql = "SELECT * FROM bhobk WHERE cast(vkey as double) = " + zobristDouble + " and vvalid = 1;";
        } else {
            sql = "SELECT * FROM bhobk WHERE cast(vkey as integer) = " + zobrist + " and vvalid = 1;";
        }

        try (Statement stmt = this.connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                BookData bd = new BookData();
                bd.setScore(rs.getInt("vscore"));
                bd.setWinNum(rs.getInt("vwin"));
                bd.setDrawNum(rs.getInt("vdraw"));
                bd.setLoseNum(rs.getInt("vlost"));
                int winRate = (int) (10000 * (bd.getWinNum() + bd.getDrawNum() / 2.0d) / (bd.getWinNum() + bd.getDrawNum() + bd.getLoseNum()));
                bd.setWinRate(winRate / 100d);
                bd.setNote(rs.getString("vmemo"));
                int vmove = rs.getInt("vmove");
                bd.setMove(ZobristUtils.getMoveFromVmove(vmove, leftRightSwap));

                bd.setSource(this.name);
                results.add(bd);
            }

        } catch (SQLException e) {
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
            this.connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
