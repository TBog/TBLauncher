package rocks.tbog.tblauncher.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import rocks.tbog.tblauncher.result.ReversibleAdapterRecyclerLayoutManager;

public class RecyclerList extends RecyclerView {
    private static final String TAG = "list";

    public RecyclerList(@NonNull Context context) {
        this(context, null);
    }

    public RecyclerList(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerList(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void scrollToFirstItem() {
        final int adapterPos;
        if (getLayoutManager() instanceof ReversibleAdapterRecyclerLayoutManager) {
            ReversibleAdapterRecyclerLayoutManager lm = (ReversibleAdapterRecyclerLayoutManager) getLayoutManager();
            adapterPos = lm.isReverseAdapter() ? (getLayoutManager().getItemCount() - 1) : 0;
        } else {
            adapterPos = 0;
        }

        if (adapterPos >= 0) {
            Log.d(TAG, "scrollToPosition( " + adapterPos + " )");
            scrollToPosition(adapterPos);
        }
    }
}
