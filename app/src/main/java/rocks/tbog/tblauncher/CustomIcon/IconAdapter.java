package rocks.tbog.tblauncher.CustomIcon;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.result.ResultViewHelper;

class IconAdapter extends BaseAdapter {
    private final List<IconData> mIcons;
    private OnItemClickListener mOnItemClickListener = null;
    private OnItemClickListener mOnItemLongClickListener = null;

    public interface OnItemClickListener {
        void onItemClick(IconAdapter adapter, View view, int position);
    }

    IconAdapter(@NonNull List<IconData> objects) {
        mIcons = objects;
    }

    void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    void setOnItemLongClickListener(OnItemClickListener listener) {
        mOnItemLongClickListener = listener;
    }

    @Override
    public IconData getItem(int position) {
        return mIcons.get(position);
    }

    @Override
    public int getCount() {
        return mIcons.size();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_icon_item, parent, false);
        } else {
            view = convertView;
        }
        ViewHolder holder = view.getTag() instanceof ViewHolder ? (ViewHolder) view.getTag() : new ViewHolder(view);

        IconData content = getItem(position);
        holder.setContent(content);

        holder.icon.setOnClickListener(v -> {
            if (mOnItemClickListener != null)
                mOnItemClickListener.onItemClick(IconAdapter.this, v, position);
        });
        holder.icon.setOnLongClickListener(v -> {
            if (mOnItemLongClickListener != null)
                mOnItemLongClickListener.onItemClick(IconAdapter.this, v, position);
            return true;
        });

        return view;
    }

    static class ViewHolder {
        ImageView icon;
        ViewHolder.AsyncLoad loader = null;

        static class AsyncLoad extends AsyncTask<IconData, Void, Drawable> {
            WeakReference<ViewHolder> holder;

            AsyncLoad(ViewHolder holder) {
                this.holder = new WeakReference<>(holder);
            }

            @Override
            protected void onPreExecute() {
                ViewHolder h = holder.get();
                if (h == null || h.loader != this)
                    return;
                h.icon.setImageDrawable(null);
            }

            @Override
            protected Drawable doInBackground(IconData... iconData) {
                return iconData[0].getIcon();
            }

            @Override
            protected void onPostExecute(Drawable drawable) {
                ViewHolder h = holder.get();
                if (h == null || h.loader != this)
                    return;
                h.loader = null;
                h.icon.setImageDrawable(drawable);
                h.icon.setScaleX(0f);
                h.icon.setScaleY(0f);
                h.icon.setRotation((drawable.hashCode() & 1) == 1 ? 180f : -180f);
                h.icon.animate().scaleX(1f).scaleY(1f).rotation(0f).start();
            }
        }

        ViewHolder(View itemView) {
            itemView.setTag(this);
            icon = itemView.findViewById(android.R.id.icon);
        }

        public void setContent(IconData content) {
            if (loader != null)
                loader.cancel(false);
            loader = new ViewHolder.AsyncLoad(this);
            loader.executeOnExecutor(ResultViewHelper.EXECUTOR_LOAD_ICON, content);
        }
    }
}
