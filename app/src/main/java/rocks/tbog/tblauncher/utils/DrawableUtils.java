package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.Shader.TileMode;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

public class DrawableUtils {

    private static final int SHAPE_SYSTEM = 0;
    private static final int SHAPE_CIRCLE = 1;
    private static final int SHAPE_SQUARE = 2;
    private static final int SHAPE_SQUIRCLE = 3;
    private static final int SHAPE_TEARDROP = 4;

    // https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap

    static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Handle adaptive icons for compatible devices
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Drawable applyIconMaskShape(Context ctx, Drawable icon) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        int shape = prefs.getInt("icons-shape", SHAPE_TEARDROP);
        if (shape == SHAPE_SYSTEM)
            return icon;

        Bitmap outputBitmap;
        Canvas outputCanvas;
        Paint outputPaint;

        if(icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) icon;
            Drawable bgDrawable = adaptiveIcon.getBackground();
            Drawable fgDrawable = adaptiveIcon.getForeground();

            int layerSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108f, ctx.getResources().getDisplayMetrics()));
            int iconSize = Math.round(layerSize / (1 + 2 * AdaptiveIconDrawable.getExtraInsetFraction()));
            int layerOffset = (layerSize - iconSize) / 2;

            // Create a bitmap of the icon to use it as the shader of the outputBitmap
            Bitmap iconBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            Canvas iconCanvas = new Canvas(iconBitmap);

            // Stretch adaptive layers because they are 108dp and the icon size is 48dp
            bgDrawable.setBounds(-layerOffset, -layerOffset, iconSize+layerOffset, iconSize+layerOffset);
            bgDrawable.draw(iconCanvas);

            fgDrawable.setBounds(-layerOffset, -layerOffset, iconSize+layerOffset, iconSize+layerOffset);
            fgDrawable.draw(iconCanvas);

            outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = new Canvas(outputBitmap);
            outputPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            outputPaint.setShader(new BitmapShader(iconBitmap, TileMode.CLAMP, TileMode.CLAMP));

            setIconShape(outputCanvas, outputPaint, shape);
        }
        // If icon is not adaptive, put it in a white canvas to make it have a unified shape
        else {
            int iconSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, ctx.getResources().getDisplayMetrics()));

            outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = new Canvas(outputBitmap);
            outputPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            outputPaint.setColor(Color.WHITE);

            // Shrink icon to 70% of its size so that it fits the shape
            int topLeftCorner = Math.round(0.15f*iconSize);
            int bottomRightCorner = Math.round(0.85f*iconSize);
            icon.setBounds(topLeftCorner, topLeftCorner, bottomRightCorner, bottomRightCorner);

            setIconShape(outputCanvas, outputPaint, shape);
            icon.draw(outputCanvas);
        }
        return new BitmapDrawable(ctx.getResources(), outputBitmap);
    }

    /**
     * Set the shape of adaptive icons
     * @param shape type of shape: DrawableUtils.SHAPE_*
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void setIconShape(Canvas canvas, Paint paint, int shape) {
        int iconSize = canvas.getHeight();

        switch (shape) {
            case SHAPE_SQUIRCLE: {
                // |x|^n + |y|^n = |r|^n with n = 3
                int radius = iconSize / 2;
                double radiusToPow = radius * radius * radius;

                Path path = new Path();
                path.moveTo(-radius, 0);
                for (int x = -radius; x <= radius; x++)
                    path.lineTo(x, ((float) Math.cbrt(radiusToPow - Math.abs(x * x * x))));
                for (int x = radius; x >= -radius; x--)
                    path.lineTo(x, ((float) -Math.cbrt(radiusToPow - Math.abs(x * x * x))));
                path.close();

                Matrix matrix = new Matrix();
                matrix.postTranslate(radius, radius);
                path.transform(matrix);

                canvas.drawPath(path, paint);
                break;
            }
            case SHAPE_SQUARE:
                canvas.drawRoundRect(0f, 0f, iconSize, iconSize, iconSize / 8f, iconSize / 12f, paint);
                break;
            case SHAPE_CIRCLE: {
                int radius = iconSize / 2;
                canvas.drawCircle(radius, radius, radius, paint);
                break;
            }
            case SHAPE_TEARDROP: {
                Path path = new Path();
                path.addArc(0, 0, iconSize, iconSize, 90, 270);
                path.lineTo(iconSize, iconSize * 0.70f);
                path.arcTo(iconSize * 0.70f, iconSize * 0.70f, iconSize, iconSize, 0, 90, false);
                path.close();

                canvas.drawPath(path, paint);
                break;
            }
        }
    }
}
