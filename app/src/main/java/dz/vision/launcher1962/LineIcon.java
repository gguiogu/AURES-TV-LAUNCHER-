package dz.vision.launcher1962;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Pure vector line icons on 240x240 grid.
 * Zero bitmap memory footprint.
 */
public final class LineIcon extends Drawable {
    public static final int[] TV = {
            0, 30, 60, 1, 210, 60, 1, 210, 160, 1, 30, 160, 2,
            0, 90, 160, 1, 70, 200, 0, 150, 160, 1, 170, 200,
            0, 60, 200, 1, 180, 200
    };

    public static final int[] SETTINGS = {
            0, 40, 70, 1, 140, 70, 0, 180, 70, 1, 200, 70,
            0, 40, 170, 1, 60, 170, 0, 100, 170, 1, 200, 170,
            3, 160, 70, 20, 3, 80, 170, 20
    };

    public static final int[] APPS = {
            3, 75, 75, 24, 3, 165, 75, 24, 3, 75, 165, 24, 3, 165, 165, 24
    };

    public static final int[] WIFI = {
            4, 120, 195, 135, 225, 90, 4, 120, 195, 92, 225, 90,
            4, 120, 195, 50, 225, 90, 3, 120, 195, 5
    };

    private final int[] ops;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF arc = new RectF();

    public LineIcon(int[] ops, int color) {
        this.ops = ops;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(color);
    }

    @Override
    public void draw(Canvas c) {
        if (ops == null) return;
        float s = getBounds().width() / 240f;
        paint.setStrokeWidth(16 * s);
        path.reset();
        for (int i = 0; i < ops.length; ) {
            switch (ops[i]) {
                case 0: path.moveTo(ops[i + 1] * s, ops[i + 2] * s); i += 3; break;
                case 1: path.lineTo(ops[i + 1] * s, ops[i + 2] * s); i += 3; break;
                case 2: path.close(); i += 1; break;
                case 3: path.addCircle(ops[i + 1] * s, ops[i + 2] * s, ops[i + 3] * s, Path.Direction.CW); i += 4; break;
                default:
                    float cx = ops[i + 1] * s, cy = ops[i + 2] * s, r = ops[i + 3] * s;
                    arc.set(cx - r, cy - r, cx + r, cy + r);
                    path.addArc(arc, ops[i + 4], ops[i + 5]);
                    i += 6;
            }
        }
        c.save();
        c.translate(getBounds().left, getBounds().top);
        c.drawPath(path, paint);
        c.restore();
    }

    @Override public void setAlpha(int a) { paint.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
