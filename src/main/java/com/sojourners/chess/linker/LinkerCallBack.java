package com.sojourners.chess.linker;

public interface LinkerCallBack {

    void linkerInitChessBoard(String fenCode, boolean isReverse);

    char[][] getEngineBoard();

    boolean isThinking();

    boolean isWatchMode();

    void linkerMove(int x1, int y1, int x2, int y2);
}
