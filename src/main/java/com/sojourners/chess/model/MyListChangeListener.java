package com.sojourners.chess.model;

import com.sojourners.chess.controller.Controller;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

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
            for(int i = 0 ;i< originList.size();i++){
                int t = i;
                Platform.runLater(() -> {
                    ManualRecord originManualRecord = this.cb.getRecordTable().getItems().get(t);
                    ManualRecord newManualRecord = new ManualRecord(originManualRecord.getId(),originManualRecord.getName(),originList.get(t));
                    this.cb.getRecordTable().getItems().set(t,newManualRecord);
                });
            }
        }

    }
}
