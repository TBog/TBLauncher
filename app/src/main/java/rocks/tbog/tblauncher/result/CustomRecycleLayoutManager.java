package rocks.tbog.tblauncher.result;

import androidx.recyclerview.widget.RecyclerView;

public class CustomRecycleLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = "CRLM";

    // Reusable int array to be passed to method calls that mutate it in order to "return" two ints.
    // This should only be used used transiently and should not be used to retain any state over
    // time.
    private final int[] mReusableIntPair = new int[2];

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return null;
    }
}
