package com.sojourners.chess.menu;

import com.sojourners.chess.config.Properties;
import com.sojourners.chess.enginee.Engine;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;

public class BoardContextMenu extends ContextMenu {

    /** * 单例 */
    private static volatile BoardContextMenu INSTANCE = null;

    /** * 私有构造函数 */
    private BoardContextMenu() {
        MenuItem editMenuItem = new MenuItem("编辑局面");
        getItems().add(editMenuItem);
        getItems().add(new SeparatorMenuItem());

        MenuItem copyMenuItem = new MenuItem("复制局面");
        MenuItem pasteMenuItem = new MenuItem("粘贴局面");
        getItems().addAll(copyMenuItem, pasteMenuItem);
        getItems().add(new SeparatorMenuItem());

        Menu timeMenu = new Menu("对局时间");
        MenuItem timeOf01 = new MenuItem("0.1s");
        MenuItem timeOf03 = new MenuItem("0.3s");
        MenuItem timeOf05 = new MenuItem("0.5s");
        MenuItem timeOf1 = new MenuItem("1s");
        MenuItem timeOf2 = new MenuItem("2s");
        MenuItem timeOf3 = new MenuItem("3s");
        MenuItem timeOf5 = new MenuItem("5s");
        MenuItem timeOf10 = new MenuItem("10s");
        MenuItem timeOf15 = new MenuItem("15s");
        timeMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String time = ((MenuItem) event.getTarget()).getText();
                if (!"对局时间".equals(time)) {
                    time = time.substring(0, time.length() - 1);
                    long t = (long) (Double.parseDouble(time) * 1000);
                    Properties prop = Properties.getInstance();
                    prop.setAnalysisModel(Engine.AnalysisModel.FIXED_TIME);
                    prop.setAnalysisValue(t);
                }
            }
        });
        timeMenu.getItems().addAll(timeOf01, timeOf03, timeOf05, timeOf1, timeOf2, timeOf3, timeOf5, timeOf10, timeOf15);
        getItems().add(timeMenu);
        getItems().add(new SeparatorMenuItem());

        MenuItem switchMenuItem = new MenuItem("交换行棋方");
        getItems().add(switchMenuItem);
    }

    /** * 获取实例 * @return GlobalMenu */
    public static BoardContextMenu getInstance() {
        if (INSTANCE == null) {
            synchronized (BoardContextMenu.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BoardContextMenu();
                }
            }
        }
        return INSTANCE;
    }
}
