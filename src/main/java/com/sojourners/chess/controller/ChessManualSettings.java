package com.sojourners.chess.controller;

import com.sojourners.chess.App;
import com.sojourners.chess.config.Properties;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class ChessManualSettings {

    private ChessManualSettings() {
    }

    public static boolean showAndWait(Window owner) {
        Properties properties = Properties.getInstance();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("棋谱设置");
        dialog.setResizable(true);

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(590);
        pane.setPadding(new Insets(8, 20, 10, 20));
        ButtonType save = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(save, new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE));

        TextField directory = new TextField(properties.getChessManualAutoSaveDirectory());
        directory.setMinSize(120, 30);
        Button browse = new Button("选择");
        browse.setMinSize(76, 30);
        browse.setOnAction(event -> chooseDirectory(directory, pane.getScene().getWindow()));
        HBox directoryRow = new HBox(10, directory, browse);
        directoryRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(directory, Priority.ALWAYS);
        GridPane.setHgrow(directoryRow, Priority.ALWAYS);

        TextField playbackMillis = numericField(Integer.toString(properties.getChessManualPlaybackMillis()));
        TextField scoreMillis = numericField(Integer.toString(properties.getChessManualScoreMillis()));
        TextField fontSize = numericField(Double.toString(properties.getChessManualCommentFontSize()));
        Label error = new Label();
        error.setVisible(false);
        error.managedProperty().bind(error.visibleProperty());

        GridPane form = new GridPane();
        form.setPadding(new Insets(16, 10, 12, 10));
        form.setHgap(24);
        form.setVgap(14);
        form.addRow(0, fieldLabel("棋谱自动保存位置", directory), directoryRow);
        form.addRow(1, fieldLabel("播放棋谱步时（毫秒）", playbackMillis), playbackMillis);
        form.addRow(2, fieldLabel("棋谱打分步时（毫秒）", scoreMillis), scoreMillis);
        form.addRow(3, fieldLabel("注解字体大小", fontSize), fontSize);
        form.add(error, 0, 4, 2, 1);
        pane.setContent(form);

        pane.lookupButton(save).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                Path saveDirectory = parseDirectory(directory.getText());
                int playbackDelay = parseDelay(playbackMillis.getText(), "播放棋谱步时");
                int scoreDelay = parseDelay(scoreMillis.getText(), "棋谱打分步时");
                double commentFontSize = parseFontSize(fontSize.getText());

                properties.setChessManualAutoSaveDirectory(saveDirectory.toString());
                properties.setChessManualPlaybackMillis(playbackDelay);
                properties.setChessManualScoreMillis(scoreDelay);
                properties.setChessManualCommentFontSize(commentFontSize);
                properties.save();
            } catch (IllegalArgumentException e) {
                event.consume();
                error.setText(e.getMessage());
                error.setVisible(true);
                pane.getScene().getWindow().sizeToScene();
            }
        });

        dialog.setOnShowing(event -> App.applyTheme(pane.getScene()));
        return dialog.showAndWait().filter(save::equals).isPresent();
    }

    private static TextField numericField(String value) {
        TextField field = new TextField(value);
        field.setPrefWidth(132);
        field.setMaxWidth(Region.USE_PREF_SIZE);
        field.setMinHeight(30);
        return field;
    }

    private static Label fieldLabel(String text, TextField field) {
        Label label = new Label(text);
        label.setLabelFor(field);
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    private static void chooseDirectory(TextField field, Window owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择棋谱自动保存目录");
        File initialDirectory = new File(field.getText()).getAbsoluteFile();
        while (initialDirectory != null && !initialDirectory.isDirectory()) {
            initialDirectory = initialDirectory.getParentFile();
        }
        chooser.setInitialDirectory(initialDirectory);
        File selectedDirectory = chooser.showDialog(owner);
        if (selectedDirectory != null) {
            field.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private static Path parseDirectory(String text) {
        if (text.isBlank()) {
            throw new IllegalArgumentException("请输入棋谱自动保存目录。");
        }
        Path directory;
        try {
            directory = Path.of(text).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("棋谱自动保存目录无效。", e);
        }

        // 目录可以尚未创建，但已有的上级路径必须是目录。
        Path existingPath = directory;
        while (existingPath != null && Files.notExists(existingPath)) {
            existingPath = existingPath.getParent();
        }
        if (existingPath != null && !Files.isDirectory(existingPath)) {
            throw new IllegalArgumentException("棋谱自动保存目录无效。");
        }
        return directory;
    }

    private static int parseDelay(String text, String label) {
        try {
            int delay = Integer.parseInt(text.trim());
            if (delay > 0) {
                return delay;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException(label + "必须是正整数。");
    }

    private static double parseFontSize(String text) {
        try {
            double size = Double.parseDouble(text.trim());
            if (Double.isFinite(size) && size > 0) {
                return size;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException("注解字体大小必须是正数。");
    }
}
