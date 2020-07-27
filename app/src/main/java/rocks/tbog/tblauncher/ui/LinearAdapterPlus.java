package rocks.tbog.tblauncher.ui;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

/**
 * Add callbacks to the LinearAdapter
 */
public class LinearAdapterPlus extends LinearAdapter {
    public interface BindCallback {
        /**
         * @param view the view to bind with
         * @return true if super.bindView should not be called
         */
        boolean bindView(View view);
    }

    public static class ItemStringIcon extends ItemString implements BindCallback {
        @NonNull
        final Drawable icon;

        public ItemStringIcon(@NonNull String string, @NonNull Drawable icon) {
            super(string);
            this.icon = icon;
        }

        @Override
        public int getLayoutResource() {
            return android.R.layout.activity_list_item;
        }

        @Override
        public boolean bindView(View view) {
            ImageView image = view.findViewById(android.R.id.icon);
            image.setImageDrawable(icon);
            return false;
        }
    }


    @Override
    protected void bindView(View view, MenuItem item) {
        if (item instanceof BindCallback && ((BindCallback) item).bindView(view))
            return;
        super.bindView(view, item);
    }
}
