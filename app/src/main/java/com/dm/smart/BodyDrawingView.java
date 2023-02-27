package com.dm.smart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class BodyDrawingView extends View {

    private final List<Step> steps = new ArrayList<>();
    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleDetector;
    private final Rect mBGRect = new Rect();
    private final Paint snapshotPaint = new Paint();
    public Bitmap snapshot = null;
    Canvas drawImageCanvas;
    private Point mBGSizeZoomed;
    private Rect mBGZoomedRect;
    private Matrix mZoomingMatrix, mInvertMatrix;
    private float mZoomingScale = 1.0f, minZoomingScale = 1.0f, maxZoomingScale = 7.0f;
    private Bitmap freshSnapshot = null;
    private Path freshPath = null;
    private Paint freshPaint = null;
    Bitmap backgroundImage = null;
    private Bitmap maskImage = null;
    private int intensity = -1;
    private CanvasFragment.Brush brush = null;

    public BodyDrawingView(Context context) {
        this(context, null);
    }

    public BodyDrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mGestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {
            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                moveXY(-v, -v1);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {
            }

            @Override
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                return false;
            }
        });

        mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float tmpZoomingScale = mZoomingScale * detector.getScaleFactor();
                if (tmpZoomingScale >= minZoomingScale && tmpZoomingScale <= maxZoomingScale) {
                    mZoomingMatrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(),
                            detector.getFocusX(), detector.getFocusY());
                }
                setBGImageZoomingLocation();
                invalidate();
                return true;
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        if (isInEditMode()) {
            super.onSizeChanged(w, h, old_w, old_h);
            return;
        }
        if (w > 0 & h > 0) {
            setBGImageZooming();
            setZoomingBounds();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (isInEditMode()) {
            super.onDraw(canvas);
            return;
        }

        // Draw background
        if (backgroundImage != null) {
            canvas.drawBitmap(backgroundImage, mBGRect, mBGZoomedRect, null);
        }

        if (snapshot != null) {
            canvas.drawBitmap(snapshot, mBGRect, mBGZoomedRect, snapshotPaint);
        }

        // draw freshSnapshot and path currently being drawn
        if (freshSnapshot != null) {
            canvas.drawBitmap(freshSnapshot, mBGRect, mBGZoomedRect, null);
        }
        if (freshPath != null) {
            canvas.drawPath(freshPath, freshPaint);
        }
    }

    // After lifting the pen this method draws the step to the snapshot
    private void drawStep(Step step) {
        if (freshSnapshot == null) {
            freshSnapshot = Bitmap.createBitmap(mBGRect.width(), mBGRect.height(),
                    Bitmap.Config.ARGB_8888);
            drawImageCanvas = new Canvas(freshSnapshot);
        }
        Bitmap currentSnapshot = Bitmap.createBitmap(mBGRect.width(), mBGRect.height(),
                Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(currentSnapshot);
        Paint paint = new Paint(step.brush.paint);
        paint.setStrokeWidth(step.brush.thickness);
        if (step.brush.type.equals("erase")) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            drawImageCanvas.drawPath(step.path, paint);
        } else {
            paint.setColor(intensity);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
            tempCanvas.drawPath(step.path, paint);
            if (!step.brush.title.equals(getResources().getString(R.string.brush_out))) {
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                tempCanvas.drawBitmap(maskImage, 0, 0, paint);
            }
        }
        drawImageCanvas.drawBitmap(currentSnapshot, 0, 0, null);
        snapshot = freshSnapshot;
    }

    public void setBGImage(Bitmap backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public void setMaskImage(Bitmap maskImage) {
        this.maskImage = maskImage;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public void setBrush(CanvasFragment.Brush brush) {
        this.brush = brush;
    }

    public void undoLastStep() {
        if (steps.size() > 0) {
            steps.remove(steps.size() - 1);
            if (freshSnapshot != null) {
                freshSnapshot.recycle();
                freshSnapshot = null;
            }
            for (Step step : steps) {
                drawStep(step);
            }
            invalidate();
        }
    }

    private void setBGImageZooming() {

        int imgW = backgroundImage.getWidth();
        int imgH = backgroundImage.getHeight();
        RectF imgRect = new RectF(0, 0, imgW, imgH);
        int viewW = this.getWidth();
        int viewH = this.getHeight();
        RectF viewRect = new RectF(0, 0, viewW, viewH);
        imgRect.round(mBGRect);
        Matrix matrix = new Matrix();
        matrix.setRectToRect(imgRect, viewRect, Matrix.ScaleToFit.CENTER);
        mZoomingMatrix = matrix;
        setBGImageZoomingLocation();
        if (mBGSizeZoomed.x > 0 & mBGSizeZoomed.y > 0) {
            invalidate();
        }
    }

    private void setBGImageZoomingLocation() {
        Matrix matrix = mZoomingMatrix;
        mInvertMatrix = new Matrix();
        matrix.invert(mInvertMatrix);
        int imgW = backgroundImage.getWidth();
        int imgH = backgroundImage.getHeight();
        mZoomingScale = mZoomingMatrix.mapRadius(1f) - mZoomingMatrix.mapRadius(0f);
        mBGSizeZoomed = new Point((int) (imgW * mZoomingScale), (int) (imgH * mZoomingScale));
        RectF BGZoomedRect = new RectF();
        matrix.mapRect(BGZoomedRect, new RectF(mBGRect));
        mBGZoomedRect = new Rect();
        BGZoomedRect.round(mBGZoomedRect);
    }

    private void setZoomingBounds() {
        int imgW = backgroundImage.getWidth();
        int imgH = backgroundImage.getHeight();
        RectF imgRect = new RectF(0, 0, imgW, imgH);
        int viewW = this.getWidth();
        int viewH = this.getHeight();
        RectF viewRect = new RectF(0, 0, viewW, viewH);
        Matrix matrix = new Matrix();
        matrix.setRectToRect(imgRect, viewRect, Matrix.ScaleToFit.CENTER);
        minZoomingScale = matrix.mapRadius(1f) - matrix.mapRadius(0f);
        maxZoomingScale = 7 * minZoomingScale;
    }

    private void moveXY(float transX, float transY) {
        float newX = mBGZoomedRect.left + transX;
        float newY = mBGZoomedRect.top + transY;

        boolean widthBiggerThanScreen = mBGZoomedRect.width() > getWidth();
        boolean heightBiggerThanScreen = mBGZoomedRect.height() > getHeight();

        boolean passLeftEdge = (newX > 0 && widthBiggerThanScreen) || (newX < 0 && !widthBiggerThanScreen);
        boolean passRightEdge = (newX + mBGZoomedRect.width() < getWidth() && widthBiggerThanScreen) ||
                (newX + mBGZoomedRect.width() > getWidth() && !widthBiggerThanScreen);
        boolean passTopEdge = (newY > 0 && heightBiggerThanScreen) || (newY < 0 && !heightBiggerThanScreen);
        boolean passBottomEdge = (newY + mBGZoomedRect.height() < getHeight() && heightBiggerThanScreen) ||
                (newY + mBGZoomedRect.height() > getHeight() && !heightBiggerThanScreen);

        if (passLeftEdge && passRightEdge) {
            transX = 0;
        } else if (passLeftEdge) {
            transX = -mBGZoomedRect.left;
        } else if (passRightEdge) {
            transX = getWidth() - (mBGZoomedRect.left + mBGZoomedRect.width());
        }

        if (passTopEdge && passBottomEdge) {
            transY = 0;
        } else if (passTopEdge) {
            transY = -mBGZoomedRect.top;
        } else if (passBottomEdge) {
            transY = getHeight() - (mBGZoomedRect.top + mBGZoomedRect.height());
        }

        mZoomingMatrix.postTranslate(transX, transY);
        setBGImageZoomingLocation();
        invalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        boolean isPen = event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS;

        if (!isPen) {
            mScaleDetector.onTouchEvent(event);
            if (!mScaleDetector.isInProgress()) {
                mGestureDetector.onTouchEvent(event);
            }
            return true;
        }
        if (intensity == -1) {
            showToast(getResources().getString(R.string.toast_select_intensity));
            return true;
        }

        if (event.getAction() != MotionEvent.ACTION_DOWN &&
                event.getAction() != MotionEvent.ACTION_MOVE &&
                event.getAction() != MotionEvent.ACTION_UP) {
            freshPath = null;
            invalidate();
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        if (brush.drawByMove) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                freshPath = new Path();
                freshPath.moveTo(x, y);
                freshPath.lineTo(x, y);

                freshPaint = new Paint(brush.paint);
                freshPaint.setStyle(Paint.Style.STROKE);
                freshPaint.setStrokeWidth(mZoomingScale * brush.thickness);
                freshPaint.setColor(brush.type.equals("erase") ? Color.WHITE
                        : intensity);

            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                // Get all points from event.getHistoricalX/Y for a smoother draw;
                // There is an issue here:
                // Attempt to invoke virtual method 'void android.graphics.Path.lineTo(float, float)'
                // on a null object reference
                int histPointsAmount = event.getHistorySize();
                for (int i = 0; i < histPointsAmount; i++) {
                    freshPath.lineTo(event.getHistoricalX(i), event.getHistoricalY(i));
                }
                freshPath.lineTo(x, y);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                freshPath.lineTo(x, y);

                Step step = new Step();
                step.brush = brush;
                step.path = new Path(freshPath);
                freshPath = null;
                step.path.transform(mInvertMatrix);
                drawStep(step);
            }
        } else {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                float[] pts = new float[]{x, y};
                mInvertMatrix.mapPoints(pts);

                Step step = new Step();
                step.brush = brush;
            }
        }
        invalidate();
        return true;
    }

    public void showToast(String text_to_show) {
        Toast toast = new Toast(getContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        LayoutInflater inflater_toast = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View toast_view = inflater_toast.inflate(R.layout.toast_bordered,
                this.findViewById(R.id.toast_layout));
        TextView text = toast_view.findViewById(R.id.toast_text);
        text.setText(text_to_show);
        toast.setView(toast_view);
        toast.show();
    }


    private class Step {
        CanvasFragment.Brush brush;
        Path path;

        Step() {
            steps.add(this);
        }
    }
}