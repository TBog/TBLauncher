package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.utils.ClipboardUtils;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.Utilities;

public final class CalculatorEntry extends SearchEntry {
    public static final String SCHEME = "calculator://";

    public CalculatorEntry(String query) {
        super(SCHEME + query);
        setName(query, false);
    }

    @Override
    public String getHistoryId() {
        // Search POJO should not appear in history
        return "";
    }

    @Override
    public int getResultLayout(int drawFlags) {
        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? R.layout.item_builtin :
                (Utilities.checkFlag(drawFlags, FLAG_DRAW_GRID) ? R.layout.item_grid :
                        R.layout.item_quick_list);
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        Context context = view.getContext();
        TextView nameView = view.findViewById(android.R.id.text1);
        nameView.setTextColor(UIColors.getResultTextColor(view.getContext()));
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME)) {
            String text = getName();
            int pos = text.indexOf("=");
            if (pos >= 0) {
                int color = UIColors.getResultHighlightColor(context);
                SpannableString enriched = new SpannableString(text);
                enriched.setSpan(
                        new ForegroundColorSpan(color),
                        pos + 1,
                        text.length(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                );
                nameView.setText(enriched);
            } else {
                nameView.setText(text);
            }
        } else {
            nameView.setVisibility(View.GONE);
        }

        ImageView appIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            ResultViewHelper.setIconColorFilter(appIcon, drawFlags);
            appIcon.setVisibility(View.VISIBLE);
            appIcon.setImageResource(R.drawable.ic_functions);
        } else {
            appIcon.setImageDrawable(null);
            appIcon.setVisibility(View.GONE);
        }

        ResultViewHelper.applyPreferences(drawFlags, nameView, appIcon);
    }

    @Override
    public void doLaunch(View v) {
        String text = getName();
        if (!text.isEmpty()) {
            String result = text.substring(text.indexOf("=") + 1).trim();
            Context context = v.getContext();
            ClipboardUtils.setClipboard(context, result);
            Toast.makeText(context, context.getString(R.string.copy_confirmation, result), Toast.LENGTH_SHORT).show();
        }
    }
}
