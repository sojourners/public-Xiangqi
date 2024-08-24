package com.sojourners.chess.board;

import com.sojourners.chess.util.PathUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CustomBoardRender extends BaseBoardRender {

    private Image bgImage;
    private Map<Character, Image> map;

    public CustomBoardRender(Canvas canvas) {
        super(canvas);

        this.bgImage = new Image(new File(PathUtils.getJarPath() + "/ui/board.png").toURI().toString());

        map = new HashMap<>();
        map.put('r', new Image(new File(PathUtils.getJarPath() + "/ui/br.png").toURI().toString()));
        map.put('n', new Image(new File(PathUtils.getJarPath() + "/ui/bn.png").toURI().toString()));
        map.put('b', new Image(new File(PathUtils.getJarPath() + "/ui/bb.png").toURI().toString()));
        map.put('a', new Image(new File(PathUtils.getJarPath() + "/ui/ba.png").toURI().toString()));
        map.put('k', new Image(new File(PathUtils.getJarPath() + "/ui/bk.png").toURI().toString()));
        map.put('c', new Image(new File(PathUtils.getJarPath() + "/ui/bc.png").toURI().toString()));
        map.put('p', new Image(new File(PathUtils.getJarPath() + "/ui/bp.png").toURI().toString()));

        map.put('R', new Image(new File(PathUtils.getJarPath() + "/ui/rr.png").toURI().toString()));
        map.put('N', new Image(new File(PathUtils.getJarPath() + "/ui/rn.png").toURI().toString()));
        map.put('B', new Image(new File(PathUtils.getJarPath() + "/ui/rb.png").toURI().toString()));
        map.put('A', new Image(new File(PathUtils.getJarPath() + "/ui/ra.png").toURI().toString()));
        map.put('K', new Image(new File(PathUtils.getJarPath() + "/ui/rk.png").toURI().toString()));
        map.put('C', new Image(new File(PathUtils.getJarPath() + "/ui/rc.png").toURI().toString()));
        map.put('P', new Image(new File(PathUtils.getJarPath() + "/ui/rp.png").toURI().toString()));
    }

    @Override
    public void drawBackgroundImage(double width, double height) {
        gc.drawImage(bgImage, 0, 0, width, height);
    }

    @Override
    public void drawCenterText(int pos, int piece, ChessBoard.BoardSize style) {

    }

//    @Override
//    public void drawBoardLine(int pos, int padding, int piece, boolean isReverse, ChessBoard.BoardSize style) {
//
//    }

    @Override
    public void drawPieces(int pos, int piece, char[][] board, boolean isReverse, ChessBoard.BoardSize style) {
        // 绘制棋子
        int r = (piece - piece / 16) / 2;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                Image img = map.get(board[i][j]);
                if (img != null) {
                    int x = pos + piece * j;
                    int y = pos + piece * getReverseY(i, isReverse);
                    gc.drawImage(img, x - r, y - r, 2 * r, 2 * r);
                }
            }
        }
    }
}
