package rocks.tbog.tblauncher.icons;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.core.content.res.ResourcesCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import rocks.tbog.tblauncher.drawable.DrawableUtils;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public class IconPackXML implements IconPack<DrawableInfo> {
    private final static String TAG = IconPackXML.class.getSimpleName();
    private final Map<String, ArraySet<DrawableInfo>> drawablesByComponent = new ArrayMap<>(0);
    private final LinkedHashSet<DrawableInfo> drawableList = new LinkedHashSet<>(0);
    // instance of a resource object of an icon pack
    private Resources packResources;
    // package name of the icons pack
    @NonNull
    private final String iconPackPackageName;
    // list of back images available on an icons pack
    private final ArrayList<DrawableInfo> backImages = new ArrayList<>();
    // bitmap mask of an icons pack
    private DrawableInfo maskImage = null;
    // front image of an icons pack
    private DrawableInfo frontImage = null;
    // scale factor of an icons pack
    private float factor = 1.0f;

    private final Random random = new Random();
    private final Matrix matScale = new Matrix();

    private boolean loaded;

    public IconPackXML(@NonNull String packageName) {
        iconPackPackageName = packageName;
        loaded = false;
    }

    @Override
    public synchronized boolean isLoaded() {
        return loaded;
    }

    @Override
    public synchronized void load(PackageManager packageManager) {
        if (loaded)
            return;
        try {
            packResources = packageManager.getResourcesForApplication(iconPackPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "get icon pack resources" + iconPackPackageName, e);
        }

        parseAppFilterXML();
        loaded = true;
    }

    public synchronized void loadDrawables(PackageManager packageManager) {
        if (!loaded)
            load(packageManager);
        try {
            packResources = packageManager.getResourcesForApplication(iconPackPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "get icon pack resources" + iconPackPackageName, e);
        }

        parseDrawableXML();
    }

    public boolean hasMask() {
        return maskImage != null;
    }

    @NonNull
    @Override
    public Collection<DrawableInfo> getDrawableList() {
        return Collections.unmodifiableCollection(drawableList);
    }

    @Override
    @Nullable
    public DrawableInfo getComponentDrawable(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandleCompat userHandle) {
        return getComponentDrawable(componentName.toString());
    }

    @Override
    public boolean isComponentDynamic(@NonNull ComponentName componentName) {
        return getCalendarDrawable(componentName.toString()) != null;
    }

    @Nullable
    private CalendarDrawable getCalendarDrawable(@Nullable String componentName) {
        ArraySet<DrawableInfo> drawables = drawablesByComponent.get(componentName);
        if (drawables != null)
            for (DrawableInfo info : drawables)
                if (info instanceof CalendarDrawable)
                    return (CalendarDrawable) info;
        return null;
    }

    @Nullable
    public DrawableInfo getComponentDrawable(String componentName) {
        CalendarDrawable calendar = getCalendarDrawable(componentName);
        if (calendar != null)
            return calendar;
        ArraySet<DrawableInfo> drawables = drawablesByComponent.get(componentName);
        return drawables != null ? drawables.valueAt(0) : null;
    }

    @Nullable
    @Override
    public Drawable getDrawable(@Nullable DrawableInfo drawableInfo) {
        if (drawableInfo instanceof SimpleDrawable) {
            SimpleDrawable sd = (SimpleDrawable) drawableInfo;
            try {
                return ResourcesCompat.getDrawable(packResources, sd.getResourceId(), null);
            } catch (Resources.NotFoundException ignored) {
            }
        } else if (drawableInfo instanceof CalendarDrawable) {
            CalendarDrawable cd = (CalendarDrawable) drawableInfo;
            // The first day of the month has value 1.
            int dayOfMonthIdx = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1;
            try {
                return ResourcesCompat.getDrawable(packResources, cd.getDayDrawable(dayOfMonthIdx), null);
            } catch (Resources.NotFoundException ignored) {
            }
        }
        return null;
    }

    @NonNull
    private Bitmap getBitmap(@NonNull DrawableInfo drawableInfo) {
        Drawable drawable = getDrawable(drawableInfo);
        return Utilities.drawableToBitmap(drawable);
    }

    @NonNull
    @Override
    public Drawable applyBackgroundAndMask(@NonNull Context ctx, @NonNull Drawable systemIcon, boolean fitInside) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (systemIcon instanceof AdaptiveIconDrawable)
                systemIcon = DrawableUtils.applyIconMaskShape(ctx, systemIcon, DrawableUtils.SHAPE_SQUARE, fitInside);
        }

        if (systemIcon instanceof BitmapDrawable) {
            return generateBitmap((BitmapDrawable) systemIcon);
        }

        Bitmap bitmap;
        if (systemIcon.getIntrinsicWidth() <= 0 || systemIcon.getIntrinsicHeight() <= 0)
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        else
            bitmap = Bitmap.createBitmap(systemIcon.getIntrinsicWidth(), systemIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        systemIcon.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        systemIcon.draw(new Canvas(bitmap));
        return generateBitmap(new BitmapDrawable(ctx.getResources(), bitmap));
    }

    @NonNull
    private BitmapDrawable generateBitmap(@NonNull BitmapDrawable defaultBitmap) {

        // if no support images in the icon pack return the bitmap itself
        if (backImages.size() == 0) {
            return defaultBitmap;
        }

        // select a random background image
        int backImageInd = random.nextInt(backImages.size());
        Bitmap backImage = getBitmap(backImages.get(backImageInd));
        int w = backImage.getWidth();
        int h = backImage.getHeight();

        // create a bitmap for the result
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.setDensity(Bitmap.DENSITY_NONE);

        // draw the background first
        canvas.drawBitmap(backImage, 0, 0, null);

        // scale original icon
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(defaultBitmap.getBitmap(), (int) (w * factor), (int) (h * factor), false);
        scaledBitmap.setDensity(Bitmap.DENSITY_NONE);

        int offsetLeft = (w - scaledBitmap.getWidth()) / 2;
        int offsetTop = (h - scaledBitmap.getHeight()) / 2;
        if (maskImage != null) {
            // draw the scaled bitmap with mask
            Bitmap mask = getBitmap(maskImage);

            // paint the bitmap with mask into the result
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas.drawBitmap(scaledBitmap, offsetLeft, offsetTop, null);
            matScale.setScale(w / (float) mask.getWidth(), h / (float) mask.getHeight());
            canvas.drawBitmap(mask, matScale, paint);
            paint.setXfermode(null);
        } else { // draw the scaled bitmap without mask
            canvas.drawBitmap(scaledBitmap, offsetLeft, offsetTop, null);
        }

        // paint the front
        if (frontImage != null) {
            canvas.drawBitmap(getBitmap(frontImage), 0, 0, null);
        }

        return new BitmapDrawable(packResources, result);
    }

    @SuppressLint("DiscouragedApi")
    private void parseDrawableXML() {
        XmlPullParser xpp = null;
        // search drawable.xml into icons pack apk resource folder
        int drawableXmlId = packResources.getIdentifier("drawable", "xml", iconPackPackageName);
        if (drawableXmlId > 0) {
            xpp = packResources.getXml(drawableXmlId);
        }
        if (xpp == null)
            return;
        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    int attrCount = xpp.getAttributeCount();
                    switch (xpp.getName()) {
                        case "item":
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if (attrName.equals("drawable")) {
                                    String drawableName = xpp.getAttributeValue(attrIdx);
                                    if (!drawableList.contains(new DrawableInfo(drawableName))) {
                                        int drawableId = packResources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                                        if (drawableId != 0) {
                                            DrawableInfo drawableInfo = new SimpleDrawable(drawableName, drawableId);
                                            drawableList.add(drawableInfo);
                                        }
                                    }
                                }
                            }
                            break;
                        case "category":
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "parsing drawable.xml", e);
        }

    }

    @SuppressLint("DiscouragedApi")
    @Nullable
    private XmlPullParser findAppFilterXml() throws XmlPullParserException {
        // search appfilter.xml in icon pack's apk resource folder for xml files
        int appFilterIdXml = packResources.getIdentifier("appfilter", "xml", iconPackPackageName);
        if (appFilterIdXml > 0) {
            return packResources.getXml(appFilterIdXml);
        }

        // search appfilter.xml in icon pack's apk resource folder for raw files (supporting icon pack studio)
        int appFilterIdRaw = packResources.getIdentifier("appfilter", "raw", iconPackPackageName);
        if (appFilterIdRaw > 0) {
            InputStream input = packResources.openRawResource(appFilterIdRaw);
            XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = xppf.newPullParser();
            xpp.setInput(input, "UTF-8");
            return xpp;
        }
        return null;
    }

    @SuppressLint("DiscouragedApi")
    private void parseAppFilterXML() {
        if (packResources == null)
            return;

        Map<String, CalendarDrawable> calendarDrawablesByPrefix = new ArrayMap<>(0);
        Map<String, Integer> drawableCache = drawableList.stream()
            .filter(d -> (d instanceof SimpleDrawable))
            .collect(Collectors.toMap(DrawableInfo::getDrawableName, d -> ((SimpleDrawable) d).getResourceId()));

        try {
            XmlPullParser xpp = findAppFilterXml();
            if (xpp != null) {
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        String componentName = null;
                        String drawableName = null;
                        int drawableId;

                        switch (xpp.getName()) {
                            //parse <iconback> xml tags used as background of generated icons
                            case "iconback":
                                for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                    if (xpp.getAttributeName(i).startsWith("img")) {
                                        drawableName = xpp.getAttributeValue(i);
                                        drawableId = packResources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                                        if (drawableId != 0)
                                            backImages.add(new SimpleDrawable(drawableName, drawableId));
//                                    Bitmap iconback = loadBitmap(drawableName);
//                                    if (iconback != null) {
//                                        backImages.add(iconback);
//                                    }
                                    }
                                }
                                break;
                            //parse <iconmask> xml tags used as mask of generated icons
                            case "iconmask":
                                if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                    drawableName = xpp.getAttributeValue(0);
                                    drawableId = packResources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                                    if (drawableId != 0)
                                        maskImage = new SimpleDrawable(drawableName, drawableId);
                                    //maskImage = loadBitmap(drawableName);
                                }
                                break;
                            //parse <iconupon> xml tags used as front image of generated icons
                            case "iconupon":
                                if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                    drawableName = xpp.getAttributeValue(0);
                                    drawableId = packResources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                                    if (drawableId != 0)
                                        frontImage = new SimpleDrawable(drawableName, drawableId);
                                    //frontImage = loadBitmap(drawableName);
                                }
                                break;
                            //parse <scale> xml tags used as scale factor of original bitmap icon
                            case "scale":
                                if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("factor"))
                                    factor = Float.parseFloat(xpp.getAttributeValue(0));
                                break;
                            //parse <item> xml tags for custom icons
                            case "item":
                                for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                    if (xpp.getAttributeName(i).equals("component")) {
                                        componentName = xpp.getAttributeValue(i);
                                    } else if (xpp.getAttributeName(i).equals("drawable")) {
                                        drawableName = xpp.getAttributeValue(i);
                                    }
                                }

                                if (drawableName != null && componentName != null) {
                                    Integer cache = drawableCache.get(drawableName);
                                    if (cache != null)
                                        drawableId = cache;
                                    else {
                                        drawableId = packResources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                                        drawableCache.put(drawableName, drawableId);
                                    }
                                    if (drawableId != 0) {
                                        DrawableInfo drawableInfo = new SimpleDrawable(drawableName, drawableId);
                                        drawableList.add(drawableInfo);
                                        ArraySet<DrawableInfo> infoSet = drawablesByComponent.get(componentName);
                                        if (infoSet == null)
                                            drawablesByComponent.put(componentName, infoSet = new ArraySet<>(1));
                                        infoSet.add(drawableInfo);
                                    }
                                    //else {
                                    //    if (componentName == null)
                                    //        componentName = "`null`";
                                    //    Log.w(TAG, "Drawable `" + drawableName + "` for " + componentName + " not found");
                                    //}
                                }
                                break;
                            case "calendar":
                                String prefix = null;

                                for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                    if (xpp.getAttributeName(i).equals("component")) {
                                        componentName = xpp.getAttributeValue(i);
                                    } else if (xpp.getAttributeName(i).equals("prefix")) {
                                        prefix = xpp.getAttributeValue(i);
                                    }
                                }

                                if (prefix != null && componentName != null) {
                                    CalendarDrawable calendarDrawable = calendarDrawablesByPrefix.get(prefix);
                                    if (calendarDrawable == null) {
                                        calendarDrawable = new CalendarDrawable(prefix + "1..31");
                                        calendarDrawablesByPrefix.put(prefix, calendarDrawable);

                                        // build calendar drawable
                                        for (int day = 0; day < 31; day += 1) {
                                            drawableName = prefix + (1 + day);
                                            drawableId = packResources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                                            if (drawableId == 0)
                                                Log.w(TAG, "Calendar drawable `" + drawableName + "` for " + componentName + " not found");
                                            calendarDrawable.setDayDrawable(day, drawableId);
                                        }
                                    }

                                    ArraySet<DrawableInfo> infoSet = drawablesByComponent.get(componentName);
                                    if (infoSet == null)
                                        drawablesByComponent.put(componentName, infoSet = new ArraySet<>(1));
                                    infoSet.add(calendarDrawable);
                                }
                                break;
                            default:
                                // ignore
                                break;
                        }
                    }
                    eventType = xpp.next();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing appfilter.xml " + e);
        }
    }


    @NonNull
    @Override
    public String getPackPackageName() {
        return iconPackPackageName;
    }

}
