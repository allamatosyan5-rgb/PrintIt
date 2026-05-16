package alla.matosyan.printit;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class DragAndScaleListener implements View.OnTouchListener {

    private float dX, dY;
    private GestureDetector gestureDetector;
    private View targetView;

    public DragAndScaleListener(Context context) {
        this.gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (targetView != null) {
                    targetView.performClick();
                }
                return true;
            }
        });
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        this.targetView = view;

        gestureDetector.onTouchEvent(event);

        view.bringToFront();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                dX = view.getX() - event.getRawX();
                dY = view.getY() - event.getRawY();
                break;

            case MotionEvent.ACTION_MOVE:
                float newX = event.getRawX() + dX;
                float newY = event.getRawY() + dY;

                View parent = (View) view.getParent();
                if (parent != null) {
                    float minX = 0;
                    float maxX = parent.getWidth() - view.getWidth();
                    float minY = 0;
                    float maxY = parent.getHeight() - view.getHeight();

                    if (newX < minX) newX = minX;
                    if (newX > maxX) newX = maxX;
                    if (newY < minY) newY = minY;
                    if (newY > maxY) newY = maxY;
                }
                view.setX(newX);
                view.setY(newY);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }
}