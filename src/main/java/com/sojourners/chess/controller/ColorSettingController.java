package com.sojourners.chess.controller;

import com.sojourners.chess.board.PieceShadow;
import com.sojourners.chess.config.Properties;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.Locale;

public class ColorSettingController {

    @FXML
    private ColorPicker firstStepColor;
    @FXML
    private Spinner<Integer> firstStepOpacity;
    @FXML
    private ColorPicker firstStepNumberColor;

    @FXML
    private ColorPicker secondStepColor;
    @FXML
    private Spinner<Integer> secondStepOpacity;
    @FXML
    private ColorPicker secondStepNumberColor;

    @FXML
    private ColorPicker branchStepColor;
    @FXML
    private Spinner<Integer> branchStepOpacity;
    @FXML
    private ColorPicker branchStepNumberColor;

    @FXML
    private RadioButton lightTheme;
    @FXML
    private RadioButton darkTheme;
    @FXML
    private RadioButton eyeGreenTheme;
    @FXML
    private RadioButton warmYellowTheme;
    @FXML
    private ToggleGroup themeGroup;
    @FXML
    private CheckBox pieceShadowEnabled;
    @FXML
    private HBox pieceShadowControls;
    @FXML
    private Canvas pieceShadowDirection;
    @FXML
    private Spinner<Integer> pieceShadowAngle;
    @FXML
    private Slider pieceShadowLength;
    @FXML
    private Label pieceShadowLengthValue;
    @FXML
    private Slider pieceShadowSize;
    @FXML
    private Label pieceShadowSizeValue;
    @FXML
    private Button saveButton;

    private final Properties prop = Properties.getInstance();
    private boolean saved;

    @FXML
    public void initialize() {
        configureOpacity(firstStepOpacity, prop.getFirstStepOpacity());
        configureOpacity(secondStepOpacity, prop.getSecondStepOpacity());
        configureOpacity(branchStepOpacity, prop.getBranchStepOpacity());

        firstStepColor.setValue(Color.web(prop.getFirstStepColor()));
        firstStepNumberColor.setValue(Color.web(prop.getFirstStepNumberColor()));
        secondStepColor.setValue(Color.web(prop.getSecondStepColor()));
        secondStepNumberColor.setValue(Color.web(prop.getSecondStepNumberColor()));
        branchStepColor.setValue(Color.web(prop.getBranchStepColor()));
        branchStepNumberColor.setValue(Color.web(prop.getBranchStepNumberColor()));

        lightTheme.setUserData(Properties.ColorTheme.LIGHT);
        darkTheme.setUserData(Properties.ColorTheme.DARK);
        eyeGreenTheme.setUserData(Properties.ColorTheme.EYE_GREEN);
        warmYellowTheme.setUserData(Properties.ColorTheme.WARM_YELLOW);
        Properties.ColorTheme colorTheme = prop.getColorTheme();
        themeGroup.selectToggle(themeGroup.getToggles().stream()
                .filter(toggle -> toggle.getUserData() == colorTheme)
                .findFirst()
                .orElse(lightTheme));
        configurePieceShadow();
    }

    private void configurePieceShadow() {
        SpinnerValueFactory.IntegerSpinnerValueFactory angles =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 359,
                        Math.floorMod((int) Math.round(prop.getPieceShadowAngle()), 360));
        angles.setWrapAround(true);
        pieceShadowAngle.setValueFactory(angles);
        pieceShadowAngle.getEditor().setTextFormatter(new javafx.scene.control.TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,3}") ? change : null));
        pieceShadowAngle.getEditor().focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) {
                commitShadowAngle();
            }
        });
        pieceShadowAngle.getEditor().setOnAction(event -> commitShadowAngle());
        pieceShadowLength.setValue(prop.getPieceShadowLength());
        pieceShadowSize.setValue(prop.getPieceShadowSize());
        pieceShadowEnabled.setSelected(prop.isPieceShadowEnabled());
        pieceShadowControls.disableProperty().bind(pieceShadowEnabled.selectedProperty().not());
        pieceShadowControls.opacityProperty().bind(Bindings.when(pieceShadowEnabled.selectedProperty()).then(1d).otherwise(0.5d));
        pieceShadowLengthValue.textProperty().bind(Bindings.format("%.0f%%", pieceShadowLength.valueProperty()));
        pieceShadowSizeValue.textProperty().bind(Bindings.format("%.0f%%", pieceShadowSize.valueProperty()));

        pieceShadowAngle.getEditor().textProperty().addListener((observable, oldValue, value) -> drawShadowDirection());
        pieceShadowLength.valueProperty().addListener((observable, oldValue, value) -> drawShadowDirection());
        pieceShadowSize.valueProperty().addListener((observable, oldValue, value) -> drawShadowDirection());
        pieceShadowEnabled.selectedProperty().addListener((observable, oldValue, value) -> drawShadowDirection());
        pieceShadowDirection.setOnMousePressed(this::selectShadowDirection);
        pieceShadowDirection.setOnMouseDragged(this::selectShadowDirection);
        pieceShadowDirection.setCursor(Cursor.HAND);
        drawShadowDirection();
    }

    private int shadowAngle() {
        try {
            return Math.floorMod(Integer.parseInt(pieceShadowAngle.getEditor().getText()), 360);
        } catch (NumberFormatException e) {
            return pieceShadowAngle.getValue();
        }
    }

    private void commitShadowAngle() {
        int angle = shadowAngle();
        pieceShadowAngle.getValueFactory().setValue(angle);
        pieceShadowAngle.getEditor().setText(Integer.toString(angle));
    }

    private void selectShadowDirection(MouseEvent event) {
        double x = event.getX() - pieceShadowDirection.getWidth() / 2d;
        double y = event.getY() - pieceShadowDirection.getHeight() / 2d;
        if (Math.hypot(x, y) < 8d) {
            return;
        }
        int angle = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(y, x))), 360);
        pieceShadowAngle.getValueFactory().setValue(angle);
        // 即使角度未变，也用圆盘选择覆盖尚未提交的输入。
        pieceShadowAngle.getEditor().setText(Integer.toString(angle));
        event.consume();
    }

    private void drawShadowDirection() {
        GraphicsContext gc = pieceShadowDirection.getGraphicsContext2D();
        double cx = pieceShadowDirection.getWidth() / 2d;
        double cy = pieceShadowDirection.getHeight() / 2d;
        boolean dark = prop.getColorTheme() == Properties.ColorTheme.DARK;
        Color ink = Color.web(dark ? "#D9E0DB" : "#465149");
        Color accent = Color.web(dark ? "#92C3AC" : "#3E735A");
        gc.clearRect(0, 0, pieceShadowDirection.getWidth(), pieceShadowDirection.getHeight());
        gc.setFill(Color.web(dark ? "#465149" : "#EDE4CC"));
        gc.fillOval(cx - 51, cy - 51, 102, 102);
        gc.setStroke(ink);
        gc.setLineWidth(1);
        gc.strokeOval(cx - 52, cy - 52, 104, 104);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45d);
            gc.strokeLine(cx + Math.cos(angle) * 47, cy + Math.sin(angle) * 47,
                    cx + Math.cos(angle) * 51, cy + Math.sin(angle) * 51);
        }
        gc.setFont(Font.font(11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(ink);
        gc.fillText("右", cx + 64, cy);
        gc.fillText("下", cx, cy + 64);
        gc.fillText("左", cx - 64, cy);
        gc.fillText("上", cx, cy - 64);

        if (pieceShadowEnabled.isSelected()) {
            PieceShadow.draw(gc, cx, cy, 21, shadowAngle(), pieceShadowLength.getValue(), pieceShadowSize.getValue());
        }
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - 21, cy - 21, 42, 42);
        gc.setStroke(Color.web("#AD1A02"));
        gc.setLineWidth(2);
        gc.strokeOval(cx - 21, cy - 21, 42, 42);
        gc.setLineWidth(0.7);
        gc.strokeOval(cx - 17, cy - 17, 34, 34);
        gc.setFill(Color.web("#AD1A02"));
        gc.setFont(Font.font(24));
        gc.fillText("马", cx, cy);

        double angle = Math.toRadians(shadowAngle());
        double dx = Math.cos(angle);
        double dy = Math.sin(angle);
        gc.setFill(accent);
        gc.fillPolygon(new double[]{cx + dx * 54, cx + dx * 39 - dy * 5, cx + dx * 39 + dy * 5},
                new double[]{cy + dy * 54, cy + dy * 39 + dx * 5, cy + dy * 39 - dx * 5}, 3);
    }

    @FXML
    private void resetFirstStep() {
        firstStepColor.setValue(Color.web(Properties.DEFAULT_FIRST_STEP_COLOR));
        firstStepOpacity.getValueFactory().setValue(toPercent(Properties.DEFAULT_STEP_OPACITY));
        firstStepNumberColor.setValue(Color.web(Properties.DEFAULT_STEP_NUMBER_COLOR));
    }

    @FXML
    private void resetSecondStep() {
        secondStepColor.setValue(Color.web(Properties.DEFAULT_SECOND_STEP_COLOR));
        secondStepOpacity.getValueFactory().setValue(toPercent(Properties.DEFAULT_STEP_OPACITY));
        secondStepNumberColor.setValue(Color.web(Properties.DEFAULT_STEP_NUMBER_COLOR));
    }

    @FXML
    private void resetBranchStep() {
        branchStepColor.setValue(Color.web(Properties.DEFAULT_BRANCH_STEP_COLOR));
        branchStepOpacity.getValueFactory().setValue(toPercent(Properties.DEFAULT_STEP_OPACITY));
        branchStepNumberColor.setValue(Color.web(Properties.DEFAULT_STEP_NUMBER_COLOR));
    }

    @FXML
    private void resetTheme() {
        lightTheme.setSelected(true);
    }

    @FXML
    private void resetPieceShadow() {
        pieceShadowEnabled.setSelected(false);
        pieceShadowAngle.getValueFactory().setValue((int) Properties.DEFAULT_PIECE_SHADOW_ANGLE);
        pieceShadowAngle.getEditor().setText(Integer.toString((int) Properties.DEFAULT_PIECE_SHADOW_ANGLE));
        pieceShadowLength.setValue(Properties.DEFAULT_PIECE_SHADOW_LENGTH);
        pieceShadowSize.setValue(Properties.DEFAULT_PIECE_SHADOW_SIZE);
    }

    @FXML
    private void save() {
        prop.setFirstStepColor(toHex(firstStepColor.getValue()));
        prop.setFirstStepOpacity(toOpacity(firstStepOpacity));
        prop.setFirstStepNumberColor(toHex(firstStepNumberColor.getValue()));

        prop.setSecondStepColor(toHex(secondStepColor.getValue()));
        prop.setSecondStepOpacity(toOpacity(secondStepOpacity));
        prop.setSecondStepNumberColor(toHex(secondStepNumberColor.getValue()));

        prop.setBranchStepColor(toHex(branchStepColor.getValue()));
        prop.setBranchStepOpacity(toOpacity(branchStepOpacity));
        prop.setBranchStepNumberColor(toHex(branchStepNumberColor.getValue()));

        prop.setColorTheme((Properties.ColorTheme) themeGroup.getSelectedToggle().getUserData());
        commitShadowAngle();
        prop.setPieceShadowEnabled(pieceShadowEnabled.isSelected());
        prop.setPieceShadowAngle(pieceShadowAngle.getValue());
        prop.setPieceShadowLength(pieceShadowLength.getValue());
        prop.setPieceShadowSize(pieceShadowSize.getValue());
        prop.save();
        saved = true;
        close();
    }

    @FXML
    private void cancel() {
        close();
    }

    public boolean isSaved() {
        return saved;
    }

    private void configureOpacity(Spinner<Integer> spinner, double opacity) {
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, toPercent(opacity), 5));
        spinner.getEditor().setTextFormatter(new javafx.scene.control.TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,3}") ? change : null));
    }

    private int toPercent(double opacity) {
        return (int) Math.round(opacity * 100d);
    }

    private double toOpacity(Spinner<Integer> spinner) {
        try {
            int percent = Integer.parseInt(spinner.getEditor().getText());
            percent = Math.max(0, Math.min(100, percent));
            spinner.getValueFactory().setValue(percent);
            return percent / 100d;
        } catch (NumberFormatException e) {
            return spinner.getValue() / 100d;
        }
    }

    private String toHex(Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
