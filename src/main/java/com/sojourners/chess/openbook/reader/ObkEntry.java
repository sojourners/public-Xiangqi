package com.sojourners.chess.openbook.reader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** One unmodified bhobk row. The key is an unsigned 64-bit value held in a Java long. */
public final class ObkEntry {
    private final long id, key;
    private final int move, score, win, draw, lost, valid, index;
    private final byte[] memo;
    private final Charset memoCharset;

    ObkEntry(long id, long key, int move, int score, int win, int draw,
             int lost, int valid, byte[] memo, int index, Charset memoCharset) {
        this.id = id;
        this.key = key;
        this.move = move;
        this.score = score;
        this.win = win;
        this.draw = draw;
        this.lost = lost;
        this.valid = valid;
        this.memo = memo == null ? null : memo.clone();
        this.index = index;
        this.memoCharset = memoCharset == null ? StandardCharsets.UTF_8 : memoCharset;
    }

    public long getId() { return id; }
    public long getKey() { return key; }
    public int getMove() { return move; }
    public int getScore() { return score; }
    public int getWin() { return win; }
    public int getDraw() { return draw; }
    public int getLost() { return lost; }
    public int getValid() { return valid; }
    public int getIndex() { return index; }
    public byte[] getMemo() { return memo == null ? null : memo.clone(); }
    /** TEXT uses the database encoding; BLOB defaults to UTF-8. Null remains null. */
    public String getMemoText() { return getMemoText(memoCharset); }
    /** Allows the caller to select a legacy encoding for BLOB comments. */
    public String getMemoText(Charset charset) {
        return memo == null ? null : new String(memo, charset);
    }
}
