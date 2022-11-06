package com.sojourners.chess.board;

import com.sojourners.chess.util.PathUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

public class CustomBoardRender extends BaseBoardRender {

    private Image bgImage;
    private Map<Character, Image> map;

    public CustomBoardRender(Canvas canvas) {
        super(canvas);

        this.bgImage = new Image(PathUtils.getJarPath() + "/ui/board.png");

        map = new HashMap<>();
        map.put('r', new Image(PathUtils.getJarPath() + "/ui/br.png"));
        map.put('n', new Image(PathUtils.getJarPath() + "/ui/bn.png"));
        map.put('b', new Image(PathUtils.getJarPath() + "/ui/bb.png"));
        map.put('a', new Image(PathUtils.getJarPath() + "/ui/ba.png"));
        map.put('k', new Image(PathUtils.getJarPath() + "/ui/bk.png"));
        map.put('c', new Image(PathUtils.getJarPath() + "/ui/bc.png"));
        map.put('p', new Image(PathUtils.getJarPath() + "/ui/bp.png"));

        map.put('R', new Image(PathUtils.getJarPath() + "/ui/rr.png"));
        map.put('N', new Image(PathUtils.getJarPath() + "/ui/rn.png"));
        map.put('B', new Image(PathUtils.getJarPath() + "/ui/rb.png"));
        map.put('A', new Image(PathUtils.getJarPath() + "/ui/ra.png"));
        map.put('K', new Image(PathUtils.getJarPath() + "/ui/rk.png"));
        map.put('C', new Image(PathUtils.getJarPath() + "/ui/rc.png"));
        map.put('P', new Image(PathUtils.getJarPath() + "/ui/rp.png"));
    }

    @Override
    public void drawBackgroundImage(double width, double height) {
        gc.drawImage(bgImage, 0, 0, width, height);
    }

    @Override
    public void drawCenterText(int pos, int piece, ChessBoard.BoardSize style) {

    }

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
