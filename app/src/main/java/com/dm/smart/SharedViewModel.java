package com.dm.smart;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {

    private boolean isSensationAddedFromDialog = false;

    private String activeCanvasFragmentTag;

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