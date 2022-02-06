package rocks.tbog.tblauncher.customicon;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.collection.ArraySet;
import androidx.fragment.app.DialogFragment;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.github.dhaval2404.imagepicker.constant.ImageProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.drawable.DrawableUtils;
import rocks.tbog.tblauncher.handler.IconsHandler;
import rocks.tbog.tblauncher.icons.IconPackXML;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public class SystemPage extends CustomShapePage {
    private final ComponentName componentName;
    private final UserHandleCompat userHandle;

    SystemPage(CharSequence name, View view, ComponentName cn, UserHandleCompat uh) {
        super(name, view);
        componentName = cn;
        userHandle = uh;
    }

    @Override
    void setupView(@NonNull DialogFragment dialogFragment, @Nullable OnItemClickListener iconClickListener, @Nullable OnItemClickListener iconLongClickListener) {
        super.setupView(dialogFragment, iconClickListener, iconLongClickListener);

        addSystemIcons(dialogFragment.getContext(), mShapedIconAdapter);

        // this will call generateTextIcons
        //mLettersView.setText(pageName);
    }

    private void addSystemIcons(Context context, ShapedIconAdapter adapter) {
        ArraySet<Bitmap> dSet = new ArraySet<>(3);

        // add default icon
        {
            IconsHandler iconsHandler = TBApplication.getApplication(context).iconsHandler();
            Drawable drawable = iconsHandler.getDrawableIconForPackage(componentName, userHandle);

            //checkDuplicateDrawable(dSet, drawable);

            ShapedIconInfo iconInfo = new DefaultIconInfo(drawable);
            iconInfo.textId = R.string.default_icon;
            adapter.addItem(iconInfo);
        }

        // add getActivityIcon(componentName)
        {
            Drawable drawable = null;
            try {
                drawable = context.getPackageManager().getActivityIcon(componentName);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null) {
                if (checkDuplicateDrawable(dSet, drawable)) {
                    {
                        Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, mShape, mScale, mBackground);
                        addQuickOption(R.string.custom_icon_activity, shapedDrawable, drawable, adapter);
                    }
                    if (DrawableUtils.isAdaptiveIconDrawable(drawable)) {
                        Drawable noBackground = DrawableUtils.applyAdaptiveIconBackgroundShape(context, drawable, DrawableUtils.SHAPE_SQUARE, true);
                        Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, noBackground, mShape, mScale, mBackground);
                        addQuickOption(R.string.custom_icon_activity_adaptive_no_background, shapedDrawable, noBackground, adapter);
                    }
                }
            }
        }

        // add getApplicationIcon(packageName)
        {
            Drawable drawable = null;
            try {
                drawable = context.getPackageManager().getApplicationIcon(componentName.getPackageName());
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null) {
                if (checkDuplicateDrawable(dSet, drawable)) {
                    Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, mShape, mScale, mBackground);
                    addQuickOption(R.string.custom_icon_application, shapedDrawable, drawable, adapter);
                }
            }
        }

        // add Activity BadgedIcon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;
            List<LauncherActivityInfo> icons = launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle());
            for (LauncherActivityInfo info : icons) {
                Drawable drawable = info.getBadgedIcon(0);

                if (drawable != null) {
                    if (checkDuplicateDrawable(dSet, drawable)) {
                        Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, mShape, mScale, mBackground);
                        addQuickOption(R.string.custom_icon_badged, shapedDrawable, drawable, adapter);
                    }
                }
            }
        }

        // add icon picker
        {
            PickedIconInfo iconInfo = new PickedIconInfo(AppCompatResources.getDrawable(context, R.drawable.abc_vector_test), R.string.choose_icon);
            adapter.addItem(iconInfo);
        }
    }

    public static class PickedIconInfo extends ShapedIconInfo {
        @Nullable
        String text = null;

        public PickedIconInfo(Drawable icon, @StringRes int textId) {
            super(icon, icon);
            this.textId = textId;
        }

        public PickedIconInfo(Drawable icon, @Nullable String text) {
            super(icon, icon);
            this.text = text;
        }

        public PickedIconInfo(Drawable shaped, Drawable original, @Nullable String text) {
            super(shaped, original);
            this.text = text;
        }

        @Nullable
        CharSequence getText() {
            return text;
        }

        @Override
        protected ShapedIconInfo reshape(Context context, int shape, float scale, int background) {
            if (textId != 0)
                return this;
            Drawable drawable = DrawableUtils.applyIconMaskShape(context, originalDrawable, shape, scale, background);
            return new PickedIconInfo(drawable, originalDrawable, text);
        }

        public boolean launchPicker(@NonNull IconSelectDialog iconSelectDialog, @NonNull View v) {
            if (textId == 0)
                return false;
            Context ctx = v.getContext();
            int size = UISizes.dp2px(ctx, R.dimen.icon_size) * 2;
            ImagePicker
                .with(iconSelectDialog)
                .cropSquare()
                .provider(ImageProvider.GALLERY)
                .maxResultSize(size, size)
                .saveDir(new File(ctx.getCacheDir(), "ImagePicker"))
                .createIntent(intent -> {
                    iconSelectDialog.imagePickerResult.launch(intent);
                    return Unit.INSTANCE;
                });
            return true;
        }
    }

    private boolean checkDuplicateDrawable(ArraySet<Bitmap> set, Drawable drawable) {
        Bitmap b = null;
        if (drawable instanceof BitmapDrawable)
            b = ((BitmapDrawable) drawable).getBitmap();

        if (set.contains(b))
            return false;

        set.add(b);
        return true;
    }

    private static void addQuickOption(@StringRes int textId, Drawable shapedDrawable, Drawable drawable, ShapedIconAdapter adapter) {
        if (!(shapedDrawable instanceof BitmapDrawable))
            return;

        ShapedIconInfo iconInfo = new ShapedIconInfo(shapedDrawable, drawable);
        iconInfo.textId = textId;
        adapter.addItem(iconInfo);
    }

    public void loadIconPackIcons(List<Pair<String, String>> iconPacks) {
        if (iconPacks.isEmpty())
            return;
        final Context ctx = pageView.getContext();
        final ShapedIconInfo placeholderItem = new ShapedIconInfo(DrawableUtils.getProgressBarIndeterminate(ctx), null);
        placeholderItem.textId = R.string.icon_pack_loading;
        {
            mShapedIconAdapter.addItem(placeholderItem);
        }
        final ArrayList<NamedIconInfo> options = new ArrayList<>();
        Utilities.runAsync((t) -> {
            for (Pair<String, String> packInfo : iconPacks) {
                String packPackageName = packInfo.first;
                String packName = packInfo.second;
                Activity activity = Utilities.getActivity(pageView);
                if (activity != null) {
                    IconPackXML pack = TBApplication.iconPackCache(activity).getIconPack(packPackageName);
                    pack.load(activity.getPackageManager());
                    Drawable drawable = pack.getComponentDrawable(activity, componentName, userHandle);
                    if (drawable!=null) {
                        Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(activity, drawable, mShape, mScale, mBackground);
                        NamedIconInfo iconInfo = new NamedIconInfo(packName, shapedDrawable, drawable);
                        options.add(iconInfo);
                    }
                } else {
                    break;
                }
            }
        }, (t) -> {
            Activity activity = Utilities.getActivity(pageView);
            if (activity != null) {
                final ShapedIconAdapter adapter = mShapedIconAdapter;
                adapter.removeItem(placeholderItem);
                adapter.addItems(options);
            }
        });
    }
}
