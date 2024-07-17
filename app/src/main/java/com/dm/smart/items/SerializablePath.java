package com.dm.smart.items;

import android.graphics.Matrix;
import android.graphics.Path;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class SerializablePath extends Path implements Serializable {

    public final List<Action> actions = new LinkedList<>();

    public SerializablePath() {
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Create a temporary list to hold the actions
        List<Action> tempActions = new LinkedList<>(actions);

        // Clear the actions list
        actions.clear();

        // Iterate over the temporary list and perform the actions
        for (Action action : tempActions) {
            action.perform(this);
        }
    }

    @Override
    public void lineTo(float x, float y) {
        actions.add(new Line(x, y));
        super.lineTo(x, y);
    }

    @Override
    public void moveTo(float x, float y) {
        actions.add(new Move(x, y));
        super.moveTo(x, y);
    }

    @Override
    public void addPath(@NonNull Path src) {
        super.addPath(src);
        if (src instanceof SerializablePath) {
            actions.addAll(((SerializablePath) src).actions);
        }
    }

    @Override
    public void transform(@NonNull Matrix matrix) {
        List<Action> transformedActions = new LinkedList<>();

        for (Action action : actions) {
            if (action instanceof Move) {
                Move moveAction = (Move) action;
                float[] points = new float[]{moveAction.x, moveAction.y};
                matrix.mapPoints(points);
                transformedActions.add(new Move(points[0], points[1]));
            } else if (action instanceof Line) {
                Line lineAction = (Line) action;
                float[] points = new float[]{lineAction.x, lineAction.y};
                matrix.mapPoints(points);
                transformedActions.add(new Line(points[0], points[1]));
            }
        }

        actions.clear();
        actions.addAll(transformedActions);

        super.transform(matrix);
    }

    public interface Action extends Serializable {

        void perform(Path path);
    }

    public static final class Move implements Action {

        public float x;
        public float y;

        public Move(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void perform(Path path) {
            path.moveTo(x, y);
        }
    }

    public static final class Line implements Action {

        public float x;
        public float y;

        public Line(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void perform(Path path) {
            path.lineTo(x, y);
        }
    }
}