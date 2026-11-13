package com.sojourners.chess;

import com.sojourners.chess.config.Properties;
import com.sojourners.chess.controller.ColorSettingController;
import com.sojourners.chess.controller.Controller;
import com.sojourners.chess.controller.EditChessBoardController;
import com.sojourners.chess.controller.LocalBookController;
import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.net.URL;

/**
 * 主窗口
 */
public class App extends Application {

    public static final String VERSION = "1.9";
    public static final String BUILT_ON = "20260801";

    private static Stage engineAdd;
    private static Stage engineSetting;
    private static Stage localBookSetting;
    private static Stage mainStage;
    private static Stage timeSetting;
    private static Stage bookSetting;
    private static Stage linkSetting;
    private static Stage editChessBoard;

    private static final String LIGHT_THEME = themeResource("/style/light-theme.css");
    private static final String DARK_THEME = themeResource("/style/dark-theme.css");

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/fxml/app.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("TCHESS  V" + VERSION);
        Scene scene = new Scene(root);
        applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/image/icon.png")));

        primaryStage.setOnCloseRequest(new EventHandler() {
            @Override
            public void handle(Event event) {
                Controller controller = fxmlLoader.getController();
                controller.exit();
            }
        });
        primaryStage.setOnShowing(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                Controller controller = fxmlLoader.getController();
                controller.initStage();
            }
        });

        mainStage = primaryStage;

        primaryStage.show();
    }

    public static void topWindow(boolean top) {
        mainStage.setAlwaysOnTop(top);
    }

    /**
     * 引擎管理对话框
     */
    public static void openEngineDialog() {
        engineSetting = createStage("/fxml/engineDialog.fxml");
        engineSetting.setTitle("引擎管理");
        engineSetting.initModality(Modality.APPLICATION_MODAL);
        engineSetting.initOwner(mainStage);

        engineSetting.showAndWait();
    }

    /**
     * 本地库管理对话框
     */
    public static boolean openLocalBookDialog() {
        localBookSetting = createStage("/fxml/localBook.fxml");
        localBookSetting.setTitle("本地库管理");
        localBookSetting.initModality(Modality.APPLICATION_MODAL);
        localBookSetting.initOwner(mainStage);

        localBookSetting.showAndWait();

        return LocalBookController.change;
    }

    /**
     * 添加引擎
     */
    public static void openEngineAdd() {
        engineAdd = createStage("/fxml/engineAdd.fxml");
        engineAdd.setTitle("添加引擎");
        engineAdd.initModality(Modality.APPLICATION_MODAL);
        engineAdd.initOwner(engineSetting);

        engineAdd.showAndWait();
    }
    public static void closeEngineAdd() {
        engineAdd.close();
    }

    /**
     * 时间设置
     */
    public static void openTimeSetting() {

        timeSetting = createStage("/fxml/timeSetting.fxml");
        timeSetting.setTitle("时间设置");
        timeSetting.initModality(Modality.APPLICATION_MODAL);
        timeSetting.initOwner(mainStage);

        timeSetting.showAndWait();
    }
    public static void closeTimeSetting() {
        timeSetting.close();
    }

    /**
     * 库招设置
     */
    public static void openBookSetting() {

        bookSetting = createStage("/fxml/bookSetting.fxml");
        bookSetting.setTitle("库招设置");
        bookSetting.initModality(Modality.APPLICATION_MODAL);
        bookSetting.initOwner(mainStage);

        bookSetting.showAndWait();
    }
    public static void closeBookSetting() {
        bookSetting.close();
    }

    /**
     * 连线设置
     */
    public static void openLinkSetting() {

        linkSetting = createStage("/fxml/linkSetting.fxml");
        linkSetting.setTitle("连线设置");
        linkSetting.initModality(Modality.APPLICATION_MODAL);
        linkSetting.initOwner(mainStage);

        linkSetting.showAndWait();
    }
    public static void closeLinkSetting() {
        linkSetting.close();
    }

    public static String openEditChessBoard(char[][] board, boolean redGo, boolean isReverse) {
        try {
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(App.class.getResource("/fxml/editChessBoard.fxml"));
            Parent pane = fxmlLoader.load();
            Scene scene = new Scene(pane);
            applyTheme(scene);
            stage.setScene(scene);

            editChessBoard = stage;
            editChessBoard.setTitle("编辑局面");
            editChessBoard.initModality(Modality.APPLICATION_MODAL);
            editChessBoard.initOwner(mainStage);

            EditChessBoardController controller = fxmlLoader.getController();
            controller.setBoard(board, isReverse);
            controller.setFirstMover(redGo);

            editChessBoard.showAndWait();
            return controller.getFenCode();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void closeEditChessBoard() {
        editChessBoard.close();
    }

    public static boolean openColorSetting() {
        try {
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(App.class.getResource("/fxml/colorSetting.fxml"));
            Parent pane = fxmlLoader.load();
            Scene scene = new Scene(pane);
            applyTheme(scene);
            stage.setScene(scene);
            stage.setTitle("主题配置");
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(mainStage);

            ColorSettingController controller = fxmlLoader.getController();
            stage.showAndWait();
            return controller.isSaved();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void refreshTheme() {
        for (Window window : Window.getWindows()) {
            if (window.getScene() != null) {
                applyTheme(window.getScene());
            }
        }
    }

    public static void applyTheme(Scene scene) {
        scene.getStylesheets().removeAll(LIGHT_THEME, DARK_THEME);
        Parent root = scene.getRoot();
        root.getStyleClass().removeAll("light-theme", "dark-theme", "eye-green-theme", "warm-yellow-theme");

        Properties.ColorTheme colorTheme = Properties.getInstance().getColorTheme();
        boolean dark = colorTheme == Properties.ColorTheme.DARK;
        String stylesheet = dark ? DARK_THEME : LIGHT_THEME;
        root.getStyleClass().add(dark ? "dark-theme" : "light-theme");
        switch (colorTheme) {
            case EYE_GREEN -> root.getStyleClass().add("eye-green-theme");
            case WARM_YELLOW -> root.getStyleClass().add("warm-yellow-theme");
            default -> {
            }
        }
        scene.getStylesheets().add(stylesheet);
        applyThemeToLocalStylesheets(root, stylesheet);
    }

    private static void applyThemeToLocalStylesheets(Parent parent, String theme) {
        boolean hasLocalStylesheet = parent.getStylesheets().stream()
                .anyMatch(stylesheet -> !LIGHT_THEME.equals(stylesheet) && !DARK_THEME.equals(stylesheet));
        parent.getStylesheets().removeAll(LIGHT_THEME, DARK_THEME);
        if (hasLocalStylesheet) {
            // 控件自身的样式表优先级高于 Scene 样式表，主题需要在其后加载。
            parent.getStylesheets().add(theme);
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Parent childParent) {
                applyThemeToLocalStylesheets(childParent, theme);
            }
        }
    }

    private static Stage createStage(String resource) {
        try {
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(App.class.getResource(resource));
            Parent pane = fxmlLoader.load();
            Scene scene = new Scene(pane);
            applyTheme(scene);
            stage.setScene(scene);
            return stage;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Stage getEngineAdd() {
        return engineAdd;
    }

    public static Stage getEngineDialog() {
        return engineSetting;
    }

    public static Stage getMainStage() {
        return mainStage;
    }

    public static Stage getLocalBookSetting() {
        return localBookSetting;
    }

    private static String themeResource(String resource) {
        URL url = App.class.getResource(resource);
        if (url == null) {
            throw new IllegalStateException("找不到主题样式文件: " + resource);
        }
        return url.toExternalForm();
    }
}
