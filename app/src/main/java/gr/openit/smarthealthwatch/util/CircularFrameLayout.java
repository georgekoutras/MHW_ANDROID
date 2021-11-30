package gr.openit.smarthealthwatch.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CircularFrameLayout extends FrameLayout {


    public CircularFrameLayout(@NonNull Context context) {
        super(context);
    }

    public CircularFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CircularFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private Path path;
    private boolean circle = false;

    public void setCircle(boolean circle) {
        this.circle = circle;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        if(circle) {
            this.path = new Path();
            //TODO experiment with these values to find the correct size ratio for the circle
            this.path.addCircle(width / 2.0f, height / 2.0f, width / 2.0f, Path.Direction.CW);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (this.path != null && circle) {
            canvas.clipPath(this.path);
        }
        super.dispatchDraw(canvas);
    }
}
