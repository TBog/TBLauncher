package rocks.tbog.tblauncher;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public class CustomIconDialog extends DialogFragment {
    private List<IconData> mIconData = new ArrayList<>();
    private IconData mSelected = null;
    private RecyclerView mIconGrid;
    private TextView mSearch;
    private OnDismissListener mOnDismissListener = null;
    private OnConfirmListener mOnConfirmListener = null;

    public interface OnDismissListener {
        void onDismiss(@NonNull CustomIconDialog dialog);
    }

    public interface OnConfirmListener {
        void onConfirm(@NonNull Drawable icon);
    }

    void setOnDismissListener(OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    void setOnConfirmListener(OnConfirmListener listener) {
        mOnConfirmListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, 0);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (mOnDismissListener != null)
            mOnDismissListener.onDismiss(this);
        super.onDismiss(dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.custom_icon, container, false);
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager.LayoutParams lp = getDialog().getWindow().getAttributes();
        lp.dimAmount = 0.5f;
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

        iconAdapter.setOnItemClickListener(new IconAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(IconAdapter adapter, View view, int position) {
                mSelected = adapter.getItem(position);
            }
        });

        // First param is number of columns and second param is orientation i.e Vertical or Horizontal
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(6, StaggeredGridLayoutManager.VERTICAL);
        mIconGrid.setLayoutManager(layoutManager);

        mSearch = view.findViewById(R.id.searchIcon);
        mSearch.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                // Auto left-trim text.
                if (s.length() > 0 && s.charAt(0) == ' ')
                    s.delete(0, 1);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearch.post(() -> refreshList());
            }
        });

        View button = view.findViewById(android.R.id.button1);
        button.setOnClickListener(v -> {
            if (mOnConfirmListener != null && mSelected != null) {
                Drawable drawable = mSelected.iconPack.getDrawable(mSelected.drawableInfo);
                if (drawable != null)
                    mOnConfirmListener.onConfirm(drawable);
            }
            dismiss();
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshList();
    }

    private void refreshList() {
        mIconData.clear();
        IconsHandler iconsHandler = TBApplication.getApplication(getActivity()).getIconsHandler();
        IconPack iconPack = iconsHandler.getCurrentIconPack();
        if (iconPack != null) {
            StringNormalizer.Result normalized = StringNormalizer.normalizeWithResult(mSearch.getText(), true);
            FuzzyScore fuzzyScore = new FuzzyScore(normalized.codePoints);
            Collection<IconPack.DrawableInfo> drawables = iconPack.getDrawableList();
            for (IconPack.DrawableInfo info : drawables) {
                if (fuzzyScore.match(info.drawableName).match)
                    mIconData.add(new IconData(iconPack, info));
            }
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
        private OnItemClickListener mOnItemClickListener = null;

        public interface OnItemClickListener {
            void onItemClick(IconAdapter adapter, View view, int position);
        }

        IconAdapter(List<IconData> icons) {
            mIcons = icons;
            setHasStableIds(true);
        }

        IconData getItem(int position) {
            return mIcons.get(position);
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            mOnItemClickListener = listener;
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

            holder.icon.clearAnimation();
            holder.icon.setScaleX(1.f);
            holder.icon.setScaleY(1.f);
            holder.icon.setImageDrawable(contact.getIcon());
            holder.icon.setOnClickListener(view -> {
                view.animate().scaleX(1.5f).scaleY(1.5f).start();
                if (mOnItemClickListener != null)
                    mOnItemClickListener.onItemClick(IconAdapter.this, view, position);
            });
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

                int parentCenterX = (gvr.width() / 2) + gvr.left;

                int parentCenterY = (gvr.height() / 2) + gvr.top;

                if (parentCenterY <= halfHeight) {
                    yOffset = -(halfHeight - parentCenterY);
                } else {
                    yOffset = (parentCenterY - halfHeight);
                }

                if (parentCenterX < halfWidth) {
                    xOffset = -(halfWidth - parentCenterX);
                }

                if (parentCenterX >= halfWidth) {
                    xOffset = parentCenterX - halfWidth;
                }
            }

            Toast toast = Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, xOffset, yOffset + v.getHeight());
            toast.show();
        }

        @Override
        public int getItemCount() {
            return mIcons.size();
        }

        @Override
        public long getItemId(int position) {
            return mIcons.get(position).hashCode();
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
