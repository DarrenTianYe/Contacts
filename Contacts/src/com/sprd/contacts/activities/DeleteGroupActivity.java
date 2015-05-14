package com.sprd.contacts.activities;

import java.util.ArrayList;

import com.android.contacts.ContactSaveService;
import com.android.contacts.GroupListLoader;
import com.sprd.contacts.group.GroupDeleteListAdapter;
import com.android.contacts.R;
import android.app.ActionBar;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.content.CursorLoader;

public class DeleteGroupActivity extends ListActivity implements
        OnItemClickListener, OnClickListener {

    private static final String TAG = DeleteGroupActivity.class.getSimpleName();
    public static final String GROUP_PHOTO_URI = "group_photo_uri";
    private GroupDeleteListAdapter mAdapter;
    private Button mBtnOk;
    private Button mBtnAll;
    private boolean mIsSelectAll = false;
    private ArrayList<Long> mSelectedGroups = new ArrayList<Long>();
    private ListView mListView;
    final private int LOADER_GROUPS = 1;
    private Cursor mGroupListCursor;
    private Context mContext;
    private Uri mSelectedGroupUri;
    private LinearLayout mFooter;
    private TextView mSelectAllGroup;
    private CheckBox mSelectAllGroupCb;
    private Button mDoneMenuItem;
    private RelativeLayout mSelectGroups;
    private Bundle mPhotoUriBundle;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mContext = this;

        // TODO: Create Intent Resolver to handle the different ways users can
        // get to this list.
        // TODO: Handle search or key down
        setContentView(R.layout.group_delete_activity);
        mSelectGroups = (RelativeLayout) findViewById(R.id.select_group);
        mSelectAllGroup = (TextView) findViewById(R.id.selece_all_group);
        mSelectAllGroupCb = (CheckBox) findViewById(R.id.select_group_cb);
        mSelectAllGroupCb.setOnClickListener(this);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP
                    | ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setTitle(R.string.menu_delete_group);
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = layoutInflater.inflate(
                    R.layout.editor_custom_action_bar_overlay, null);
            mDoneMenuItem = (Button) customActionBarView
                    .findViewById(R.id.save_menu_item_button);
            mDoneMenuItem.setEnabled(false);
            mDoneMenuItem.setTextColor(Color.GRAY);
            mDoneMenuItem.setText(R.string.menu_done);
            mDoneMenuItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    GroupDeleteDialogFragment dialog = new GroupDeleteDialogFragment();
                    Bundle args = new Bundle();
                    long[] selectGroups = new long[mSelectedGroups.size()];
                    for (int i = 0; i < selectGroups.length; i++) {
                        selectGroups[i] = mSelectedGroups.get(i);
                    }
                    args.putLongArray("selectedGroup", selectGroups);
                    dialog.setArguments(args);
                    dialog.show(getFragmentManager(), null);
                }
            });
            Button cancelMenuItem = (Button) customActionBarView
                    .findViewById(R.id.cancel_menu_item_button);
            cancelMenuItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DeleteGroupActivity.this.finish();
                }
            });
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                    | ActionBar.DISPLAY_SHOW_TITLE
                    | ActionBar.DISPLAY_HOME_AS_UP);
            actionBar.setCustomView(customActionBarView,
                    new ActionBar.LayoutParams(
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_VERTICAL | Gravity.END));

        }

        getLoaderManager()
                .initLoader(LOADER_GROUPS, null, mGroupLoaderListener);
        mPhotoUriBundle = getIntent().getBundleExtra(GROUP_PHOTO_URI);
        mAdapter = new GroupDeleteListAdapter(mContext);
        mAdapter.setPhotoUri(mPhotoUriBundle);
        mListView = getListView();
        mListView.setItemsCanFocus(false);
        mListView.setOnItemClickListener(this);
        setListAdapter(mAdapter);
        //View dividerView = findViewById(R.id.divider);
        //dividerView.setBackgroundDrawable(mListView.getDivider());
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderListener = new LoaderCallbacks<Cursor>() {
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return new GroupListLoader(mContext, true);
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

    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        // TODO Auto-generated method stub
        if (mAdapter.isChecked(position)) {
            if (mIsSelectAll = true) {
                mIsSelectAll = false;
                mSelectAllGroup.setText(R.string.select_all_contacts);
                mSelectAllGroupCb.setChecked(false);
            }
            mAdapter.setChecked(position, false);
            mSelectedGroups.remove(mAdapter.getItem(position).getGroupId());
            if (mSelectedGroups.size() == 0) {
                mDoneMenuItem.setEnabled(false);
                mDoneMenuItem.setTextColor(Color.GRAY);
            }
        } else {
            mDoneMenuItem.setEnabled(true);
            mDoneMenuItem.setTextColor(Color.WHITE);
            mSelectedGroups.add(mAdapter.getItem(position).getGroupId());
            if (mSelectedGroups.size() == mAdapter.getCount()) {
                mIsSelectAll = true;
                mSelectAllGroup.setText(R.string.cancel_select_all_contacts);
                mSelectAllGroupCb.setChecked(true);
            }
            mAdapter.setChecked(position, true);
        }
        mAdapter.notifyDataSetChanged();
    }

    public void onClick(View v) {
        // TODO Auto-generated method stub
        View mView;
        if (v == mSelectAllGroupCb) {
            if (mIsSelectAll == true) {
                mIsSelectAll = false;
                mSelectAllGroup.setText(R.string.select_all_contacts);
                mSelectedGroups.clear();
                mAdapter.checkAll(false);
                mDoneMenuItem.setEnabled(false);
                mDoneMenuItem.setTextColor(Color.GRAY);
            } else {
                mIsSelectAll = true;
                mSelectAllGroup.setText(R.string.cancel_select_all_contacts);
                mSelectedGroups.clear();
                for (int i = 0; i < mAdapter.getCount(); i++) {
                    mSelectedGroups.add(mAdapter.getItem(i).getGroupId());
                }
                mAdapter.checkAll(true);
                mDoneMenuItem.setEnabled(true);
                mDoneMenuItem.setTextColor(Color.WHITE);
            }
            mAdapter.notifyDataSetChanged();
        }
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

	public static class GroupDeleteDialogFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(
					this.getActivity())
					.setTitle(R.string.menu_delete_group)
					.setMessage(R.string.delete_group_dialog_commit)
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
								    final long[] selectedGroups = (long[])getArguments()
                                            .getLongArray("selectedGroup");
									for (int i = 0; i < selectedGroups.length; i++) {
                                        getActivity().startService(
                                                ContactSaveService.createGroupDeletionIntent(
                                                        getActivity(), selectedGroups[i]));
									}
									getActivity().finish();
								}
							});
			return builder.create();
		}
	}
}
