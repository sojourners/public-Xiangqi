package com.sojourners.chess.model;

import com.sojourners.chess.controller.Controller;
import com.sojourners.chess.util.DialogUtils;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

public class MyListChangeListener implements ListChangeListener<Integer> {

    private Controller cb;
    private int flag;

    public MyListChangeListener(Controller cb,Integer flag){
        this.cb = cb;
        this.flag = flag;
    }

    @Override
    public void onChanged(Change<? extends Integer> change) {
        ObservableList<? extends Integer> originList = change.getList();
        if(originList.size() >= flag){
            Platform.runLater(() ->{
                DialogUtils.showWarningDialog("提示", "复盘结束");
            });
            for(int i = 0 ;i< originList.size();i++){
                int t = i;
                Platform.runLater(() -> {
                    ManualRecord originManualRecord = this.cb.getRecordTable().getItems().get(t);
                    ManualRecord newManualRecord = new ManualRecord(originManualRecord.getId(),originManualRecord.getName(),originList.get(t));
                    this.cb.getRecordTable().getItems().set(t,newManualRecord);

                    // 趋势图
                    if(t == 0){
                        this.cb.initLineChart();
                    }
                    this.cb.getLineChartSeries().getData().add(new XYChart.Data<>(t, originList.get(t) > 1000 ? 1000 : (originList.get(t) < -1000 ? -1000 : originList.get(t))));
                });
            }
        }

    }
}