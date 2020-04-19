package rocks.tbog.tblauncher;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomIconDialog extends DialogFragment {
    List<IconData> mIconData = new ArrayList<>();
    private RecyclerView mIconGrid;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_AppCompat_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.custom_icon, container, false);
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager.LayoutParams lp = getDialog().getWindow().getAttributes();
        lp.dimAmount = 0.4f;
        getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getDialog().setCanceledOnTouchOutside(true);

        return root;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mIconGrid = view.findViewById(R.id.iconGrid);
        IconAdapter iconAdapter = new IconAdapter(mIconData);
        mIconGrid.setAdapter(iconAdapter);

        // First param is number of columns and second param is orientation i.e Vertical or Horizontal
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(6, StaggeredGridLayoutManager.VERTICAL);
        mIconGrid.setLayoutManager(layoutManager);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshList();
    }

    private void refreshList() {
        IconsHandler iconsHandler = TBApplication.getApplication(getActivity()).getIconsHandler();
        IconPack iconPack = iconsHandler.getCurrentIconPack();
        if (iconPack == null)
            return;
        Collection<IconPack.DrawableInfo> drawables = iconPack.getDrawableList();
        mIconData.clear();
        for (IconPack.DrawableInfo info : drawables) {
            mIconData.add(new IconData(iconPack, info));
        }
        mIconGrid.getAdapter().notifyDataSetChanged();
    }

    static class IconData {
        final IconPack.DrawableInfo drawableInfo;
        final IconPack iconPack;

        IconData(IconPack iconPack, IconPack.DrawableInfo drawableInfo) {
            this.iconPack = iconPack;
            this.drawableInfo = drawableInfo;
        }

        Drawable getIcon() {
            return iconPack.getDrawable(drawableInfo);
        }
    }

    static class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {

        private final List<IconData> mIcons;

        IconAdapter(List<IconData> icons) {
            mIcons = icons;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            // Inflate the custom layout
            View contactView = inflater.inflate(R.layout.custom_icon_item, parent, false);

            // Return a new holder instance
            return new ViewHolder(contactView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // Get the data model based on position
            IconData contact = mIcons.get(position);

            holder.icon.setImageDrawable(contact.getIcon());
            holder.icon.setOnLongClickListener(v -> {
                displayToast(v, contact.drawableInfo.drawableName);
                return true;
            });
        }

        // v is the Button view that you want the Toast to appear above
        // and messageId is the id of your string resource for the message

        private void displayToast(View v, CharSequence message) {
            int xOffset = 0;
            int yOffset = 0;
            Rect gvr = new Rect();

            View parent = (View) v.getParent();
            int parentHeight = parent.getHeight();

            if (v.getGlobalVisibleRect(gvr)) {
                View root = v.getRootView();

                int halfWidth = root.getRight() / 2;
                int halfHeight = root.getBottom() / 2;

                int parentCenterX = ((gvr.right - gvr.left) / 2) + gvr.left;

                int parentCenterY = ((gvr.bottom - gvr.top) / 2) + gvr.top;

                if (parentCenterY <= halfHeight) {
                    yOffset = -(halfHeight - parentCenterY) - parentHeight;
                } else {
                    yOffset = (parentCenterY - halfHeight) - parentHeight;
                }

                if (parentCenterX < halfWidth) {
                    xOffset = -(halfWidth - parentCenterX);
                }

                if (parentCenterX >= halfWidth) {
                    xOffset = parentCenterX - halfWidth;
                }
            }

            Toast toast = Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, xOffset, yOffset);
            toast.show();
        }

        @Override
        public int getItemCount() {
            return mIcons.size();
        }

        // Provide a direct reference to each of the views within a data item
        // Used to cache the views within the item layout for fast access
        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;

            public ViewHolder(View itemView) {
                super(itemView);

                icon = itemView.findViewById(android.R.id.icon);
            }
        }
    }
}
