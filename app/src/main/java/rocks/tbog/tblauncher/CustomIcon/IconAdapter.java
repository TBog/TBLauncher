package rocks.tbog.tblauncher.CustomIcon;

import androidx.annotation.NonNull;

import java.util.List;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

class IconAdapter extends ViewHolderListAdapter<IconData, IconViewHolder> {

    IconAdapter(@NonNull List<IconData> objects) {
        super(IconViewHolder.class, R.layout.custom_icon_item, objects);
    }
}
