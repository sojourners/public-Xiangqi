package com.sojourners.chess.recognition;

import com.sojourners.chess.config.Properties;
import com.sojourners.chess.yolo.Yolo11Model;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ChessRecognizer {

    public static final double PADDING = 0.8d;

    private Yolo11Model aiModel;
    private TemplateMatchRecognizer templateMatchRecognizer;

    private int flag;

    public ChessRecognizer() {
        this.aiModel = new Yolo11Model();
    }

    public Rectangle findBoardPosition(BufferedImage img) {
        Rectangle pos = aiModel.findBoardPosition(img);
        if (pos == null) {
            return null;
        }

        double pieceWidth = pos.width / 8d, pieceHeight = pos.height / 9d;

        pos.x -= (int) (pieceWidth * PADDING);
        if (pos.x < 0) {
            pos.x = 0;
        }

        pos.y -= (int) (pieceHeight * PADDING);
        if (pos.y < 0) {
            pos.y = 0;
        }

        pos.width += (int) (pieceWidth * PADDING * 2);
        if (pos.x + pos.width > img.getWidth()) {
            pos.width = img.getWidth() - pos.x;
        }

        pos.height += (int) (pieceHeight * PADDING * 2);
        if (pos.y + pos.height > img.getHeight()) {
            pos.height = img.getHeight() - pos.y;
        }

        return pos;
    }

    public boolean initTemplateMathRecognizer(BufferedImage img) {
        char[][] board = new char[10][9];
        Rectangle pos = aiModel.findInitBoard(img, board);
        if (pos == null) {
            return false;
        }

        try {
            templateMatchRecognizer = new TemplateMatchRecognizer(pos);
            templateMatchRecognizer.initTemplate(img, board);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean findChessBoard(BufferedImage img, char[][] board) {
        boolean res;
//        long s = System.currentTimeMillis();
        if (Properties.getInstance().getRecognizeMode() == RecognizeMode.YOLO || flag-- > 0) {
            res = findChessBoardByYOLO(img, board);
        } else {
            res = findChessBoardByMixed(img, board);
        }
//        System.out.println(System.currentTimeMillis() - s);
        return res;
    }

    public boolean findChessBoardByYOLO(BufferedImage img, char[][] board) {
        return aiModel.findChessBoard(img, board);
    }

    public boolean findChessBoardByMixed(BufferedImage img, char[][] board) {
        return templateMatchRecognizer.findChessBoard(img, board);
    }

    public void setFlag(int f) {
        this.flag = f;
    }
}
