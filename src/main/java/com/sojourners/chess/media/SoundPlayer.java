package com.sojourners.chess.media;

import com.sun.jna.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;

public class SoundPlayer {
    private MediaPlayer pick;

    private MediaPlayer move;

    private MediaPlayer eat;

    private MediaPlayer check;

    private MediaPlayer over;

    public SoundPlayer(String pickSound, String moveSound, String eatSound, String checkSound, String overSound) {
        pick = new MediaPlayer(new Media(new File(pickSound).toURI().toString()));
        move = new MediaPlayer(new Media(new File(moveSound).toURI().toString()));
        eat = new MediaPlayer(new Media(new File(eatSound).toURI().toString()));
        check = new MediaPlayer(new Media(new File(checkSound).toURI().toString()));
        over = new MediaPlayer(new Media(new File(overSound).toURI().toString()));
    }

    public void eat() {
        eat.seek(Duration.ZERO);
        eat.play();
    }

    public void pick() {
        pick.seek(Duration.ZERO);
        pick.play();
    }

    public void move() {
        move.seek(Duration.ZERO);
        move.play();
    }

    public void check() {
        check.seek(Duration.ZERO);
        check.play();
    }

    public void over() {
        over.seek(Duration.ZERO);
        over.play();
    }

}
