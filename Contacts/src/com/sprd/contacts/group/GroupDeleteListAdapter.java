
package com.sprd.contacts.group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Groups;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import com.android.contacts.R;

import com.android.contacts.GroupListLoader;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.group.GroupListItem;
import com.android.internal.util.Objects;

public class GroupDeleteListAdapter extends BaseAdapter {

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final AccountTypeManager mAccountTypeManager;

    private Cursor mCursor;

    private boolean mSelectionVisible;
    private Uri mSelectedGroupUri;
    private boolean mSelectMode = false;
    private static final int LOADER_METADATA = 0;
    private static final int LOADER_MEMBERS = 1;
    private Bundle mPhotoUriBundle;

    private ContactPhotoManager mPhotoManager;
    private Set<Long> mCheckedItems = new HashSet<Long>();

    public GroupDeleteListAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mAccountTypeManager = AccountTypeManager.getInstance(mContext);
        mPhotoManager = ContactPhotoManager.getInstance(mContext);
    }

    public void setCursor(Cursor cursor) {
        mCursor = cursor;

        // If there's no selected group already and the cursor is valid, then by
        // default, select the
        // first group
        if (mSelectedGroupUri == null && cursor != null && cursor.getCount() > 0) {
            GroupListItem firstItem = getItem(0);
            long groupId = (firstItem == null) ? 0 : firstItem.getGroupId();
            mSelectedGroupUri = getGroupUriFromId(groupId);
        }

        notifyDataSetChanged();
    }

    public int getSelectedGroupPosition() {
        if (mSelectedGroupUri == null || mCursor == null || mCursor.getCount() == 0) {
            return -1;
        }

        int index = 0;
        mCursor.moveToPosition(-1);
        while (mCursor.moveToNext()) {
            long groupId = mCursor.getLong(GroupListLoader.GROUP_ID);
            Uri uri = getGroupUriFromId(groupId);
            if (mSelectedGroupUri.equals(uri)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public void setSelectionVisible(boolean flag) {
        mSelectionVisible = flag;
    }

    public void setSelectedGroup(Uri groupUri) {
        mSelectedGroupUri = groupUri;
    }

    private boolean isSelectedGroup(Uri groupUri) {
        return mSelectedGroupUri != null && mSelectedGroupUri.equals(groupUri);
    }

    public Uri getSelectedGroup() {
        return mSelectedGroupUri;
    }

    @Override
    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public GroupListItem getItem(int position) {
        if (mCursor == null || mCursor.isClosed() || !mCursor.moveToPosition(position)) {
            return null;
        }
        String accountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
        String accountType = mCursor.getString(GroupListLoader.ACCOUNT_TYPE);
        String dataSet = mCursor.getString(GroupListLoader.DATA_SET);
        long groupId = mCursor.getLong(GroupListLoader.GROUP_ID);
        String title = mCursor.getString(GroupListLoader.TITLE);
        int memberCount = mCursor.getInt(GroupListLoader.MEMBER_COUNT);

        // Figure out if this is the first group for this account name / account
        // type pair by
        // checking the previous entry. This is to determine whether or not we
        // need to display an
        // account header in this item.
        int previousIndex = position - 1;
        boolean isFirstGroupInAccount = true;
        if (previousIndex >= 0 && mCursor.moveToPosition(previousIndex)) {
            String previousGroupAccountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
            String previousGroupAccountType = mCursor.getString(GroupListLoader.ACCOUNT_TYPE);
            String previousGroupDataSet = mCursor.getString(GroupListLoader.DATA_SET);

            if (accountName.equals(previousGroupAccountName) &&
                    accountType.equals(previousGroupAccountType) &&
                    Objects.equal(dataSet, previousGroupDataSet)) {
                isFirstGroupInAccount = false;
            }
        }

        return new GroupListItem(accountName, accountType, dataSet, groupId, title,
                isFirstGroupInAccount, memberCount,0,null,0,0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final GroupListItem entry = getItem(position);
        View result;
        final GroupListItemViewCache viewCache;
        if (convertView != null) {
            result = convertView;
            viewCache = (GroupListItemViewCache) result.getTag();
        } else {
            result = LayoutInflater.from(mContext).inflate(R.layout.group_delete_list_item, null);
            viewCache = new GroupListItemViewCache(result);
            result.setTag(viewCache);
        }
        // Bind the group data
        final Uri groupUri = getGroupUriFromId(entry.getGroupId());
        String memberCountString = mContext.getResources().getQuantityString(
                R.plurals.group_list_num_contacts_in_group, entry.getMemberCount(),
                entry.getMemberCount());
        viewCache.setUri(groupUri);
        viewCache.groupTitle.setText(entry.getTitle());
        viewCache.groupMemberCount.setText(memberCountString);
        viewCache.select.setChecked(isChecked(position));

        if (mSelectionVisible) {
            result.setActivated(isSelectedGroup(groupUri));
        }
        ArrayList<Uri> photoUri;
        ArrayList<HashMap<String, Object>> memList = new ArrayList<HashMap<String, Object>>();
        Long groupid = entry.getGroupId();
        photoUri = mPhotoUriBundle == null ? null : (ArrayList<Uri>) mPhotoUriBundle.get(groupid
                .toString());
        if (photoUri != null) {
            for (int i = 0; i < photoUri.size(); i++) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("group_member_photo", photoUri.get(i));
                memList.add(map);
            }
        }
        while (memList.size() < 4) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("group_member_photo",
                    R.drawable.ic_contact_picture_holo_light);
            memList.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(mContext, memList,
                R.layout.group_photo_grid_layout,
                new String[] {
                        "group_member_photo"
                },
                new int[] {
                        R.id.group_member_photo
                });
        viewCache.groupMember.setAdapter(adapter);

        return result;
    }

    public void setPhotoUri(Bundle bundle) {
        mPhotoUriBundle = bundle;
    }

    private static Uri getGroupUriFromId(long groupId) {
        return ContentUris.withAppendedId(Groups.CONTENT_URI, groupId);
    }

    public boolean isChecked(int position) {
        return mCheckedItems.contains(getItem(position).getGroupId());
    }

    public void setChecked(int position, boolean checked) {
        long groupId = getItem(position).getGroupId();
        if (checked) {
            mCheckedItems.add(groupId);
        } else {
            mCheckedItems.remove(groupId);
        }
    }

    public void checkAll(boolean checked) {
        if (checked) {
            int count = getCount();
            for (int i = 0; i < count; ++i) {
                long groupId = getItem(i).getGroupId();
                mCheckedItems.add(groupId);
            }
        } else {
            mCheckedItems.clear();
        }
    }

    /**
     * Cache of the children views of a contact detail entry represented by a
     * {@link GroupListItem}
     */
    public static class GroupListItemViewCache {
        public final TextView groupTitle;
        public final TextView groupMemberCount;
        public final CheckBox select;
        public final GridView groupMember;
        public final ImageView image;
        private Uri mUri;

        public GroupListItemViewCache(View view) {
            groupTitle = (TextView) view.findViewById(R.id.label);
            groupMemberCount = (TextView) view.findViewById(R.id.count);
            select = (CheckBox) view.findViewById(R.id.select_checkbox);
            groupMember = (GridView) view.findViewById(R.id.group_member);
            image = (ImageView) view.findViewById(R.id.img);
        }

        public void setUri(Uri uri) {
            mUri = uri;
        }

        public Uri getUri() {
            return mUri;
        }
    }

}
