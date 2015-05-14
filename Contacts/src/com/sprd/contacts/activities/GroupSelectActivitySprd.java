
package com.sprd.contacts.activities;

import java.util.ArrayList;

import com.android.contacts.ContactSaveService;
import com.android.contacts.GroupListLoader;
import com.sprd.contacts.group.GroupSelectListAdapterSprd;

import com.android.contacts.R;
import android.app.ActionBar;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class GroupSelectActivitySprd extends ListActivity implements OnItemClickListener,
        OnClickListener {
    private Context mContext;
    private static final String TAG = GroupSelectActivitySprd.class.getSimpleName();
    private GroupSelectListAdapterSprd mAdapter;
    private Button mBtnOk;
    private Button mBtnAll;
    private boolean mIsSelectAll = false;
    private ArrayList<Long> mSelectedGroups = new ArrayList<Long>();
    private ListView mListView;
    final private int LOADER_GROUPS = 1;
    private Cursor mGroupListCursor;
    private ArrayList<Long> mOldGroupIdList = new ArrayList<Long>();
    private Uri mSelectedGroupUri;
    private long mRawContactId;
    private long[] memberToAddArray = new long[1];
    private long[] memberToRemoveArray = new long[1];
    private static final String ACTION_SAVE_COMPLETED = "update group complete";
    private TextView mEmptyTextView;
    private String mAccountType;
    private boolean changedState = true;
    private boolean unchangedState = false;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mContext = this;
        setContentView(R.layout.group_select_activity_new_ui);
        final Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            mRawContactId = bundle.getLong("RawContactId", -1);
            mAccountType = bundle.getString("AccountType");
            // Sometimes the account type passed from external is null, we
            // should ignore
            // this request.
            if (TextUtils.isEmpty(mAccountType)) {
                finish();
                return;
            }
            mOldGroupIdList = (ArrayList<Long>) bundle.getSerializable("ExistingGroup");
            if (mOldGroupIdList != null) {
                for (int i = 0; i < mOldGroupIdList.size(); i++) {
                    mSelectedGroups.add(mOldGroupIdList.get(i));
                }
            }
            if (mRawContactId != -1) {
                memberToAddArray[0] = mRawContactId;
                memberToRemoveArray[0] = mRawContactId;
            }

        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {

            LayoutInflater inflater = (LayoutInflater) getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = inflater.inflate(R.layout.group_select_custom_action_bar,
                    null);
            Button doneMenuItem = (Button) customActionBarView
                    .findViewById(R.id.done_menu_item_button);
            doneMenuItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSelectedGroups != null) {
                        for (int i = 0; i < mSelectedGroups.size(); i++) {
                            unchangedState = false;
                            if (mOldGroupIdList != null) {
                                for (int j = 0; j < mOldGroupIdList.size(); j++) {
                                    if (mOldGroupIdList.get(j) == mSelectedGroups.get(i)) {
                                        unchangedState = true;
                                        break;
                                    }
                                }
                            }
                            if (!unchangedState) {
                                GroupSelectActivitySprd.this.startService(ContactSaveService
                                        .createGroupUpdateIntent(
                                                GroupSelectActivitySprd.this,
                                                mSelectedGroups.get(i), null, memberToAddArray,
                                                null,
                                                GroupSelectActivitySprd.this.getClass(),
                                                GroupSelectActivitySprd.ACTION_SAVE_COMPLETED));
                                finish();
                            } else {
                                finish();
                            }
                        }
                    } else {
                        finish();
                    }

                    if (mOldGroupIdList != null) {
                        for (int i = 0; i < mOldGroupIdList.size(); i++) {
                            changedState = true;
                            for (int j = 0; j < mSelectedGroups.size(); j++) {
                                if (mOldGroupIdList.get(i) == mSelectedGroups.get(j)) {
                                    changedState = false;
                                    break;
                                }
                            }
                            if (changedState) {
                                GroupSelectActivitySprd.this.startService(ContactSaveService
                                        .createGroupUpdateIntent(
                                                GroupSelectActivitySprd.this,
                                                mOldGroupIdList.get(i), null, null,
                                                memberToRemoveArray,
                                                GroupSelectActivitySprd.this.getClass(),
                                                GroupSelectActivitySprd.ACTION_SAVE_COMPLETED));
                                finish();
                            } else {
                                finish();
                            }
                        }
                    } else {
                        finish();
                    }
                }
            });
            Button cancelMenuItem = (Button) customActionBarView
                    .findViewById(R.id.cancel_menu_item_button);
            cancelMenuItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    onBackPressed();
                }
            });
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                    | ActionBar.DISPLAY_SHOW_TITLE
                    | ActionBar.DISPLAY_HOME_AS_UP);
            actionBar.setCustomView(customActionBarView,
                    new ActionBar.LayoutParams(
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            actionBar.setTitle(R.string.select_group);
        }
        getLoaderManager().initLoader(LOADER_GROUPS, null, mGroupLoaderListener);
        mAdapter = new GroupSelectListAdapterSprd(mContext, mOldGroupIdList);
        if (mOldGroupIdList != null) {
            mAdapter.setChecked(mOldGroupIdList, true);
        }
        mAdapter.notifyDataSetChanged();
        mListView = getListView();
        mListView.setItemsCanFocus(false);
        mListView.setOnItemClickListener(this);
        setListAdapter(mAdapter);
        mEmptyTextView = (TextView) findViewById(R.id.empty);
        mListView.setEmptyView(mEmptyTextView);

        if (mAdapter.getCount() == 0) {
            mEmptyTextView.setText(R.string.noGroups);
        } else {
            mEmptyTextView.setText(null);
        }
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderListener =
            new LoaderCallbacks<Cursor>() {
                public CursorLoader onCreateLoader(int id, Bundle args) {
                    return new GroupListLoader(mContext, mAccountType);
                }

                public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                    mGroupListCursor = data;
                    bindGroupList();
                }

                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    private void bindGroupList() {
        if (mGroupListCursor == null) {
            return;
        }
        mAdapter.setCursor(mGroupListCursor);
        if (mGroupListCursor == null) {
            return;
        }
        mAdapter.setCursor(mGroupListCursor);

    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        // TODO Auto-generated method stub
        if (mAdapter.isChecked(position)) {
            mAdapter.setChecked(position, false);
            // mSelectedGroups.remove(mAdapter.getItem(position).getGroupId());
            if (mSelectedGroups.contains(mAdapter.getItem(position).getGroupId())) {
                mSelectedGroups.remove(mAdapter.getItem(position).getGroupId());
            }
        } else {
            mSelectedGroups.add(mAdapter.getItem(position).getGroupId());
            mAdapter.setChecked(position, true);
        }
        mAdapter.notifyDataSetChanged();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return false;
    }

}
