package rocks.tbog.tblauncher.utils;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

/**
 * http://groups.google.com/group/android-developers/browse_thread/thread/9e215c83c3819953
 * http://gskinner.com/blog/archives/2007/12/colormatrix_cla.html
 * https://medium.com/mobile-app-development-publication/android-image-color-change-with-colormatrix-e927d7fb6eb4
 */
public class ColorFilterHelper {

    private static final float[] DELTA_INDEX = {
            0f, 0.01f, 0.02f, 0.04f, 0.05f, 0.06f, 0.07f, 0.08f, 0.1f, 0.11f,
            0.12f, 0.14f, 0.15f, 0.16f, 0.17f, 0.18f, 0.20f, 0.21f, 0.22f, 0.24f,
            0.25f, 0.27f, 0.28f, 0.30f, 0.32f, 0.34f, 0.36f, 0.38f, 0.40f, 0.42f,
            0.44f, 0.46f, 0.48f, 0.5f, 0.53f, 0.56f, 0.59f, 0.62f, 0.65f, 0.68f,
            0.71f, 0.74f, 0.77f, 0.80f, 0.83f, 0.86f, 0.89f, 0.92f, 0.95f, 0.98f,
            1.0f, 1.06f, 1.12f, 1.18f, 1.24f, 1.30f, 1.36f, 1.42f, 1.48f, 1.54f,
            1.60f, 1.66f, 1.72f, 1.78f, 1.84f, 1.90f, 1.96f, 2.0f, 2.12f, 2.25f,
            2.37f, 2.50f, 2.62f, 2.75f, 2.87f, 3.0f, 3.2f, 3.4f, 3.6f, 3.8f,
            4.0f, 4.3f, 4.7f, 4.9f, 5.0f, 5.5f, 6.0f, 6.5f, 6.8f, 7.0f,
            7.3f, 7.5f, 7.8f, 8.0f, 8.4f, 8.7f, 9.0f, 9.4f, 9.6f, 9.8f,
            10.f
    };

    /**
     * @param cm     color matrix to alter
     * @param amount of hue
     */
    public static void adjustHue(ColorMatrix cm, int amount) {
        float value = clampValue(amount, 180);
        if (value == 0f) {
            return;
        }

        double rad = Math.toRadians(value);
        float cosVal = (float) Math.cos(rad);
        float sinVal = (float) Math.sin(rad);
        float R = 0.2125f;
        float G = 0.7154f;
        float B = 0.0721f;
        float[] mat = new float[]{
                R + cosVal * (1 - R) + sinVal * (-R), G + cosVal * (-G) + sinVal * (-G), B + cosVal * (-B) + sinVal * (1 - B), 0, 0,
                R + cosVal * (-R) + sinVal * (0.143f), G + cosVal * (1 - G) + sinVal * (0.140f), B + cosVal * (-B) + sinVal * (-0.283f), 0, 0,
                R + cosVal * (-R) + sinVal * (-(1 - R)), G + cosVal * (-G) + sinVal * (G), B + cosVal * (1 - B) + sinVal * (B), 0, 0,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 0f, 1f};
        cm.postConcat(new ColorMatrix(mat));
    }

    public static void adjustBrightness(ColorMatrix cm, int amount) {
        int value = clampValue(amount, 100);
        if (value == 0) {
            return;
        }
        // convert from -100..100 to -255..255
        value = value * 255 / 100;
        float[] mat = new float[]{
                1, 0, 0, 0, value,
                0, 1, 0, 0, value,
                0, 0, 1, 0, value,
                0, 0, 0, 1, 0,
                0, 0, 0, 0, 1
        };
        cm.postConcat(new ColorMatrix(mat));
    }

    public static void adjustContrast(ColorMatrix cm, int amount) {
        int value = clampValue(amount, 100);
        float x;
        if (value < 0) {
            x = 127.5f + value / 100f * 127.5f;
        } else {
            x = 127.5f + DELTA_INDEX[value] * 127.5f;
        }

        float c = x / 127.5f;
        float b = .5f * (127.5f - x);
        float[] mat = new float[]{
                c, 0, 0, 0, b,
                0, c, 0, 0, b,
                0, 0, c, 0, b,
                0, 0, 0, 1, 0,
                0, 0, 0, 0, 1
        };
        cm.postConcat(new ColorMatrix(mat));
    }

    public static void adjustSaturation(ColorMatrix cm, int amount) {
        int value = clampValue(amount, 100);
        if (value == 0) {
            return;
        }

        final float x = 1f + ((value > 0) ? 3f * value / 100f : value / 100f);
        final float inv = 1f - x;
        final float R = 0.3086f * inv;
        final float G = 0.6094f * inv;
        final float B = 0.0820f * inv;
        float[] mat = new float[]{
                R + x, G, B, 0, 0,
                R, G + x, B, 0, 0,
                R, G, B + x, 0, 0,
                0, 0, 0, 1, 0,
                0, 0, 0, 0, 1
        };
        cm.postConcat(new ColorMatrix(mat));
    }

    public static void adjustScale(ColorMatrix cm, int r, int g, int b, int a) {
        final float R = getChannelScale(r);
        final float G = getChannelScale(g);
        final float B = getChannelScale(b);
        final float A = getChannelScale(a);
        float[] mat = new float[]{
                R, 0, 0, 0, -255f * (R - 1f) * .5f,
                0, G, 0, 0, -255f * (G - 1f) * .5f,
                0, 0, B, 0, -255f * (B - 1f) * .5f,
                0, 0, 0, A, -255f * (A - 1f) * .5f,
                0, 0, 0, 0, 1
        };
        cm.postConcat(new ColorMatrix(mat));
    }

    private static float getChannelScale(int scale) {
        float s = clampValue(scale, 200);
        return s / 100f + 1f;
    }

    // make sure values are within the specified range, hue has a limit of 180, others are 100:
    private static int clampValue(int val, int limit) {
        return Math.min(limit, Math.max(-limit, val));
    }

    public static ColorFilter adjustColor(int brightness, int contrast, int saturation, int hue) {
        ColorMatrix cm = new ColorMatrix();
        adjustHue(cm, hue);
        adjustContrast(cm, contrast);
        adjustBrightness(cm, brightness);
        adjustSaturation(cm, saturation);

        return new ColorMatrixColorFilter(cm);
    }
}