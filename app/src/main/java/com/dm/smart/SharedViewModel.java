package com.dm.smart;

import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {

    private boolean isSensationAddedFromDialog = false;

    private String activeCanvasFragmentTag;

    private final MutableLiveData<Pair<Integer, Integer>> colorAndIndex = new MutableLiveData<>();

    public void setColorAndIndex(int color, int index) {
        this.colorAndIndex.setValue(new Pair<>(color, index));
    }

    public LiveData<Pair<Integer, Integer>> getColorAndIndex() {
        return colorAndIndex;
    }


    public boolean isSensationAddedFromDialog() {
        return isSensationAddedFromDialog;
    }

    public void setSensationAddedFromDialog(boolean sensationAddedFromDialog) {
        isSensationAddedFromDialog = sensationAddedFromDialog;
    }

    public String getActiveCanvasFragmentTag() {
        return activeCanvasFragmentTag;
    }

    public void setActiveCanvasFragmentTag(String activeCanvasFragmentTag) {
        this.activeCanvasFragmentTag = activeCanvasFragmentTag;
    }

    private final MutableLiveData<String> sensation = new MutableLiveData<>();

    public void selectSensation(String input) {
        sensation.setValue(input);
    }

    public LiveData<String> getSensation() {
        return sensation;
    }
}