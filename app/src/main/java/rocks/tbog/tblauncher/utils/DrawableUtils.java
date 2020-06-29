package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.R;

public class DrawableUtils {

    public static final int SHAPE_SYSTEM = 0;
    private static final int SHAPE_CIRCLE = 1;
    private static final int SHAPE_SQUARE = 2;
    private static final int SHAPE_SQUIRCLE = 3;
    private static final int SHAPE_ROUND_RECT = 4;
    private static final int SHAPE_TEARDROP_BR = 5;
    private static final int SHAPE_TEARDROP_BL = 6;
    private static final int SHAPE_TEARDROP_TL = 7;
    private static final int SHAPE_TEARDROP_TR = 8;
    private static final int SHAPE_TEARDROP_RND = 9;

    private static final Paint PAINT = new Paint();
    private static final Path PATH = new Path();

    // https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap

    @NonNull
    public static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
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
    public static Drawable applyIconMaskShape(Context ctx, Drawable icon, boolean fitInside) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String shapePref = prefs.getString("adaptive-shape", String.valueOf(SHAPE_SYSTEM));
        int shape = Integer.parseInt(shapePref);
        return applyIconMaskShape(ctx, icon, shape, fitInside);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Drawable applyIconMaskShape(Context ctx, Drawable icon, int shape, boolean fitInside) {
        if (shape == SHAPE_SYSTEM)
            return icon;
        if (shape == SHAPE_TEARDROP_RND)
            shape = SHAPE_TEARDROP_BR + (icon.hashCode() % 4);

        Bitmap outputBitmap;
        Canvas outputCanvas;
        final Paint outputPaint = PAINT;
        outputPaint.reset();
        outputPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        if (icon instanceof AdaptiveIconDrawable) {
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
            bgDrawable.setBounds(-layerOffset, -layerOffset, iconSize + layerOffset, iconSize + layerOffset);
            bgDrawable.draw(iconCanvas);

            fgDrawable.setBounds(-layerOffset, -layerOffset, iconSize + layerOffset, iconSize + layerOffset);
            fgDrawable.draw(iconCanvas);

            outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = new Canvas(outputBitmap);

            outputPaint.setShader(new BitmapShader(iconBitmap, TileMode.CLAMP, TileMode.CLAMP));
            setIconShape(outputCanvas, outputPaint, shape);
            outputPaint.setShader(null);
        }
        // If icon is not adaptive, put it in a white canvas to make it have a unified shape
        else if (icon != null) {
            // Shrink icon fit inside the shape
            int iconSize;
            int iconOffset = 0;
            if (fitInside) {
                float marginPercent = 0.2071f;  // (sqrt(2)-1)/2 to make a square fit in a circle
                iconSize = Math.round((1f + 2f * marginPercent) * icon.getIntrinsicHeight());
                iconOffset = Math.round(marginPercent * icon.getIntrinsicHeight());
            } else {
                // we don't have antialiasing when clipping so we make the icon bigger and let the View downscale
                    iconSize = 2 * ctx.getResources().getDimensionPixelSize(R.dimen.icon_height);
            }

            outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = new Canvas(outputBitmap);
            outputPaint.setColor(Color.WHITE);

            // Shrink icon so that it fits the shape
            int bottomRightCorner = iconSize - iconOffset;
            icon.setBounds(iconOffset, iconOffset, bottomRightCorner, bottomRightCorner);

            setIconShape(outputCanvas, outputPaint, shape);
            icon.draw(outputCanvas);
        } else {
            int iconSize = ctx.getResources().getDimensionPixelSize(R.dimen.icon_height);

            outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = new Canvas(outputBitmap);
            outputPaint.setColor(0xFF000000);

            setIconShape(outputCanvas, outputPaint, shape);
        }
        return new BitmapDrawable(ctx.getResources(), outputBitmap);
    }

    /**
     * Set the shape of adaptive icons
     *
     * @param shape type of shape: DrawableUtils.SHAPE_*
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void setIconShape(Canvas canvas, Paint paint, int shape) {
        int iconSize = canvas.getHeight();
        Path path = PATH;
        path.rewind();

        switch (shape) {
            case SHAPE_CIRCLE: {
                int radius = iconSize / 2;
                canvas.drawCircle(radius, radius, radius, paint);

                path.addCircle(radius, radius, radius, Path.Direction.CCW);
                break;
            }
            case SHAPE_SQUIRCLE: {
                int h = iconSize / 2;
                float c = iconSize / 2.333f;
                path.moveTo(h, 0f);
                path.cubicTo(h + c, 0, iconSize, h - c, iconSize, h);
                path.cubicTo(iconSize, h + c, h + c, iconSize, h, iconSize);
                path.cubicTo(h - c, iconSize, 0, h + c, 0, h);
                path.cubicTo(0, h - c, h-c, 0, h, 0);
                path.close();

                canvas.drawPath(path, paint);
                break;
            }
            case SHAPE_SQUARE:
                canvas.drawRect(0f, 0f, iconSize, iconSize, paint);

                path.addRect(0f, 0f, iconSize, iconSize, Path.Direction.CCW);
                break;
            case SHAPE_ROUND_RECT:
                canvas.drawRoundRect(0f, 0f, iconSize, iconSize, iconSize / 8f, iconSize / 12f, paint);

                path.addRoundRect(0f, 0f, iconSize, iconSize, iconSize / 8f, iconSize / 12f, Path.Direction.CCW);
                break;
            case SHAPE_TEARDROP_RND: // this is handled before we get here
            case SHAPE_TEARDROP_BR:
                path.addArc(0, 0, iconSize, iconSize, 90, 270);
                path.lineTo(iconSize, iconSize * 0.70f);
                path.arcTo(iconSize * 0.70f, iconSize * 0.70f, iconSize, iconSize, 0, 90, false);
                path.close();

                canvas.drawPath(path, paint);
                break;
            case SHAPE_TEARDROP_BL:
                path.addArc(0, 0, iconSize, iconSize, 180, 270);
                path.lineTo(iconSize * .3f, iconSize);
                path.arcTo(0, iconSize * .7f, iconSize * .3f, iconSize, 90, 90, false);
                path.close();

                canvas.drawPath(path, paint);
                break;
            case SHAPE_TEARDROP_TL:
                path.addArc(0, 0, iconSize, iconSize, 270, 270);
                path.lineTo(0, iconSize * .3f);
                path.arcTo(0, 0, iconSize * .3f, iconSize * .3f, 180, 90, false);
                path.close();

                canvas.drawPath(path, paint);
                break;
            case SHAPE_TEARDROP_TR:
                path.addArc(0, 0, iconSize, iconSize, 0, 270);
                path.lineTo(iconSize * .7f, 0);
                path.arcTo(iconSize * .7f, 0, iconSize, iconSize * .3f, 270, 90, false);
                path.close();

                canvas.drawPath(path, paint);
                break;
        }
        // make sure we don't draw outside the shape
        canvas.clipPath(path);
    }
}