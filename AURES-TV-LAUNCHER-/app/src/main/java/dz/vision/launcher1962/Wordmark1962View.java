package dz.vision.launcher1962;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.View;

/**
 * Procedural 1962 Wordmark with subtle Algerian Crescent and Star.
 * 100% Vector Canvas rendering - zero bitmap memory overhead.
 */
public class Wordmark1962View extends View {

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint greenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path crescentPath = new Path();
    private final Path starPath = new Path();

    private static final int DZ_RED = 0xFFD21034;
    private static final int DZ_GREEN = 0xFF007A3D;

    public Wordmark1962View(Context context) {
        super(context);
        init();
    }

    private void init() {
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        textPaint.setTextSize(48f);

        subPaint.setColor(0xFFCCCCCC);
        subPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        subPaint.setTextSize(18f);

        redPaint.setColor(DZ_RED);
        redPaint.setStyle(Paint.Style.FILL);

        greenPaint.setColor(DZ_GREEN);
        greenPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Safe compact bounding box (approx 240dp x 70dp)
        int w = (int) (260 * getResources().getDisplayMetrics().density);
        int h = (int) (72 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;

        float startX = 12 * density;
        float baseTextY = 38 * density;

        // Draw "19" in white
        textPaint.setTextSize(36 * density);
        float w1 = textPaint.measureText("19");
        canvas.drawText("19", startX, baseTextY, textPaint);

        // Center emblem between 19 and 62: Red circle with white/green star & crescent
        float emblemCenterX = startX + w1 + 18 * density;
        float emblemCenterY = baseTextY - 12 * density;
        float emblemRadius = 13 * density;

        // Draw subtle Algerian red disc
        canvas.drawCircle(emblemCenterX, emblemCenterY, emblemRadius, redPaint);

        // Crescent cutout
        crescentPath.reset();
        crescentPath.addCircle(emblemCenterX - 1.5f * density, emblemCenterY, emblemRadius * 0.72f, Path.Direction.CW);
        Path innerCrescent = new Path();
        innerCrescent.addCircle(emblemCenterX + 2.5f * density, emblemCenterY, emblemRadius * 0.58f, Path.Direction.CW);
        crescentPath.op(innerCrescent, Path.Op.DIFFERENCE);

        Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whitePaint.setColor(Color.WHITE);
        canvas.drawPath(crescentPath, whitePaint);

        // 5-pointed star inside crescent
        drawStar(canvas, emblemCenterX + 2.5f * density, emblemCenterY, emblemRadius * 0.40f, whitePaint);

        // Draw "62" in white
        float w62Start = emblemCenterX + emblemRadius + 6 * density;
        canvas.drawText("62", w62Start, baseTextY, textPaint);

        // Small subtle green accent dot/stripe
        greenPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(startX, baseTextY + 5 * density, startX + 22 * density, baseTextY + 7 * density, greenPaint);

        // Subtitle "الجزائر دائماً وأبداً"
        subPaint.setTextSize(13 * density);
        canvas.drawText("الجزائر دائماً وأبداً", startX + 28 * density, baseTextY + 14 * density, subPaint);
    }

    private void drawStar(Canvas canvas, float cx, float cy, float radius, Paint paint) {
        starPath.reset();
        double innerRadius = radius * 0.42;
        double angle = -Math.PI / 2.0;
        double step = Math.PI / 5.0;

        starPath.moveTo((float) (cx + radius * Math.cos(angle)), (float) (cy + radius * Math.sin(angle)));
        for (int i = 0; i < 5; i++) {
            angle += step;
            starPath.lineTo((float) (cx + innerRadius * Math.cos(angle)), (float) (cy + innerRadius * Math.sin(angle)));
            angle += step;
            starPath.lineTo((float) (cx + radius * Math.cos(angle)), (float) (cy + radius * Math.sin(angle)));
        }
        starPath.close();
        canvas.drawPath(starPath, paint);
    }
}
