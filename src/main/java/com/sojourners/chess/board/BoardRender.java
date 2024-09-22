package com.sojourners.chess.board;


import javafx.scene.paint.Color;

public interface BoardRender {

    void drawBackgroundImage(double width, double height);

    void drawBoardLine(int pos, int padding, int piece, boolean isReverse, ChessBoard.BoardSize style);

    void drawCenterText(int pos, int piece, ChessBoard.BoardSize style);

    void drawStepRemark(int pos, int piece, int x, int y, boolean isPrevStep, boolean isReverse, ChessBoard.BoardSize style);

    void drawPieces(int pos, int piece, char[][] board, boolean isReverse, ChessBoard.BoardSize style);

    void drawStepTips(int pos, int piece, int x1, int y1, int x2, int y2, boolean isReverse, boolean isFirst);

    void setAutoPieceSize(int size);

    Color getBackgroundColor();
}
