/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rocks.tbog.tblauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.db.ShortcutRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.utils.DebugInfo;
import rocks.tbog.tblauncher.utils.Utilities;

@RequiresApi(api = Build.VERSION_CODES.O)
public class PinShortcutConfirm extends Activity implements OnClickListener {
    private static final String TAG = "ShortcutConfirm";

    protected LauncherApps mLauncherApps;
    private EditText mShortcutName;
    private LauncherApps.PinItemRequest mRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        if (window != null) {
            window.setDimAmount(0.7f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        setContentView(R.layout.pin_shortcut_confirm);

        mLauncherApps = getSystemService(LauncherApps.class);

        findViewById(android.R.id.button1).setOnClickListener(this);
        findViewById(android.R.id.button2).setOnClickListener(this);

        mRequest = mLauncherApps.getPinItemRequest(getIntent());
        final ShortcutInfo shortcutInfo = mRequest.getShortcutInfo();
        if (shortcutInfo == null) {
            Log.e(TAG, "No shortcut info provided");
            finish();
            return;
        }

        // Label
        {
            mShortcutName = findViewById(R.id.shortcutName);
            String packageName = packageNameHeuristic(this, shortcutInfo);
            String appName = ShortcutUtil.getAppNameFromPackageName(this, packageName);
            CharSequence label = shortcutInfo.getLongLabel();
            if (label == null)
                label = shortcutInfo.getShortLabel();
            if (label == null)
                label = shortcutInfo.getPackage();
            if (!appName.isEmpty())
                mShortcutName.setText(getString(R.string.shortcut_with_appName, appName, label));
            else
                mShortcutName.setText(label);
        }

        if (prefs.getBoolean("pin-auto-confirm", false)) {
            acceptShortcut();
            finish();
            return;
        }

        // Description
        if (DebugInfo.widgetAdd(this)) {
            TextView description = findViewById(R.id.shortcutDetails);
            ComponentName activity = shortcutInfo.getActivity();
            String htmlString = String.format(
                    "<h1>Shortcut details:</h1>" +
                            "<b>Long label</b>: %s<br>" +
                            "<b>Short label</b>: %s<br>" +
                            "<b>Activity</b>: %s<br>" +
                            "<b>Publisher</b>: %s<br>" +
                            "<b>ID</b>: %s",

                    shortcutInfo.getLongLabel(),
                    shortcutInfo.getShortLabel(),
                    activity != null ? activity.flattenToShortString() : null,
                    shortcutInfo.getPackage(), // publisher app package
                    shortcutInfo.getId()
            );
            description.setText(Html.fromHtml(htmlString, Html.FROM_HTML_MODE_COMPACT));
            description.setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.shortcutDetails).setVisibility(View.GONE);
        }

        {
            View view = findViewById(R.id.image);
            TextView nameView = view.findViewById(android.R.id.text1);
            nameView.setVisibility(View.GONE);
            ImageView icon1 = view.findViewById(android.R.id.icon1);
            setIconsAsync(icon1, shortcutInfo, (ctx) -> mLauncherApps.getShortcutIconDrawable(shortcutInfo, 0));
        }

        {
            View view = findViewById(R.id.imageWithBadge);
            TextView nameView = view.findViewById(android.R.id.text1);
            nameView.setVisibility(View.GONE);
            ImageView icon1 = view.findViewById(android.R.id.icon1);
            setIconsAsync(icon1, shortcutInfo, (ctx) -> mLauncherApps.getShortcutBadgedIconDrawable(shortcutInfo, 0));
        }
    }

    private static void setIconsAsync(ImageView icon1, ShortcutInfo shortcutInfo, Utilities.GetDrawable getIcon) {
        new Utilities.AsyncSetDrawable(icon1) {
            Drawable appDrawable;

            @Override
            protected Drawable getDrawable(Context context) {
                appDrawable = ShortcutEntry.getAppDrawable(context, shortcutInfo.getId(), shortcutInfo.getPackage(), shortcutInfo, true);
                return getIcon.getDrawable(context);
            }

            @Override
            protected void onPostExecute(Drawable drawable) {
                ImageView icon1 = weakImage.get();
                super.onPostExecute(drawable);
                int drawFlags = EntryItem.FLAG_DRAW_ICON | EntryItem.FLAG_DRAW_ICON_BADGE;
                ShortcutEntry.setIcons(drawFlags, icon1, drawable, appDrawable);
            }
        }.executeOnExecutor(ResultViewHelper.EXECUTOR_LOAD_ICON);
    }

    @NonNull
    public static String packageNameHeuristic(@NonNull Context context, @NonNull ShortcutInfo shortcutInfo) {
        Intent intent = shortcutInfo.getIntent();
        ComponentName activity = intent != null ? intent.getComponent() : null;
        String packageName;
        if (activity == null) {
            // try to parse the ID to get the package name
            String id = shortcutInfo.getId();
            int schemePos = id.indexOf("://");
            int startPos = schemePos >= 0 ? schemePos + 3 : 0;
            int endPos = id.indexOf("/", startPos);
            if (endPos == -1)
                endPos = id.indexOf("#", startPos);
            if (endPos == -1)
                endPos = id.length();
            packageName = id.substring(startPos, endPos);
            String appName = ShortcutUtil.getAppNameFromPackageName(context, packageName);
            if (appName.isEmpty())
                packageName = null;
            if (packageName == null) {
                if (shortcutInfo.getActivity() != null)
                    packageName = shortcutInfo.getActivity().getPackageName();
                else
                    packageName = shortcutInfo.getPackage();
            }
        } else {
            packageName = activity.getPackageName();
        }
        return packageName;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case android.R.id.button1:
                acceptShortcut();
                finish();
                break;

            case android.R.id.button2:
                finish();
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    private void acceptShortcut() {
        final ShortcutInfo shortcutInfo = mRequest.getShortcutInfo();
        if (shortcutInfo == null) {
            Log.e(TAG, "shortcut info is null");
            return;
        }
        final boolean result = mRequest.accept();
        Log.i(TAG, "Accept returned: " + result);
        ShortcutRecord record = ShortcutUtil.createShortcutRecord(this, shortcutInfo, false);
        if (record != null) {
            if (mShortcutName.getText().length() > 0)
                record.displayName = mShortcutName.getText().toString();
            TBApplication.getApplication(this).getDataHandler().addShortcut(record);
        }
        mRequest = null;
    }
}