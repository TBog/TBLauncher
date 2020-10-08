package rocks.tbog.tblauncher.CustomIcon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.entry.StaticEntry;

public class StaticEntryPage extends PageAdapter.Page {
    StaticEntry mStaticEntry;

    StaticEntryPage(CharSequence name, View view, StaticEntry staticEntry) {
        super(name, view);
        mStaticEntry = staticEntry;
    }

    @Override
    void setupView(@NonNull Context context, @Nullable OnItemClickListener iconClickListener) {
        ProgressBar mIconLoadingBar = pageView.findViewById(R.id.iconLoadingBar);
        mIconLoadingBar.setVisibility(View.GONE);

        TextView textView = pageView.findViewById(android.R.id.text1);
        textView.setVisibility(View.GONE);

        TextView mSearch = pageView.findViewById(R.id.search);
        mSearch.setVisibility(View.GONE);

        GridView mGridView = pageView.findViewById(R.id.iconGrid);
        SystemPage.SystemPageAdapter adapter = new SystemPage.SystemPageAdapter(SystemPage.SystemPageViewHolder.class, iconClickListener);
        mGridView.setAdapter(adapter);

        // add default icon
        {
            Drawable drawable = mStaticEntry.getDefaultDrawable(context);

            SystemPage.SystemIconInfo iconInfo = new SystemPage.DefaultIconInfo();
            iconInfo.iconDrawable = drawable;
            iconInfo.textId = R.string.default_icon;
            adapter.addItem(iconInfo);
        }
    }
}
