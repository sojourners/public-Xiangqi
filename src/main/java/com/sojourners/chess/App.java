package com.sojourners.chess;

import com.sojourners.chess.controller.Controller;
import com.sojourners.chess.controller.EditChessBoardController;
import com.sojourners.chess.controller.LocalBookController;
import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * 主窗口
 */
public class App extends Application {

    public static final String VERSION = "1.5";
    public static final String BUILT_ON = "20240925";

    private static Stage engineAdd;
    private static Stage engineSetting;
    private static Stage localBookSetting;
    private static Stage mainStage;
    private static Stage timeSetting;
    private static Stage bookSetting;
    private static Stage linkSetting;
    private static Stage editChessBoard;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/fxml/app.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("中国象棋 V" + VERSION);
        primaryStage.setScene(new Scene(root));
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


    public static String openEditChessBoard(char[][] board, boolean redGo, boolean isReverse) {
        try {
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(App.class.getResource("/fxml/editChessBoard.fxml"));
            Parent pane = fxmlLoader.load();
            stage.setScene(new Scene(pane));

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

    private static Stage createStage(String resource) {
        try {
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(App.class.getResource(resource));
            Parent pane = fxmlLoader.load();
            stage.setScene(new Scene(pane));
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
}
