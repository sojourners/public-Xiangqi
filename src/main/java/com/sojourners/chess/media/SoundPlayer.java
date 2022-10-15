package com.sojourners.chess.media;

import com.sun.jna.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class SoundPlayer {
    private MediaPlayer pick;

    private MediaPlayer move;

    private MediaPlayer eat;

    private MediaPlayer check;

    private MediaPlayer over;

    public SoundPlayer(String pickSound, String moveSound, String eatSound, String checkSound, String overSound) {
        String protocol = Platform.isWindows() ? "file:/" : "file://";
        pick = new MediaPlayer(new Media(protocol + pickSound));
        move = new MediaPlayer(new Media(protocol + moveSound));
        eat = new MediaPlayer(new Media(protocol + eatSound));
        check = new MediaPlayer(new Media(protocol + checkSound));
        over = new MediaPlayer(new Media(protocol + overSound));
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
