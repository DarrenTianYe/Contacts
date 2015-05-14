/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contacts.list;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.app.LoaderManager.LoaderCallbacks;
import android.widget.Toast;
import android.util.Log;

import com.android.contacts.R;
import android.provider.ContactsContract.Contacts;
import com.android.contacts.ContactSaveService;
import com.sprd.contacts.group.GroupDetailFragmentSprd;
import com.android.contacts.activities.ContactSelectionActivity;
import com.android.contacts.common.util.AccountFilterUtil;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.Constants;
import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.common.list.ContactListAdapter;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.list.DefaultContactListAdapter;
import com.android.contacts.common.list.DirectoryListLoader;
import com.android.contacts.common.list.ShortcutIntentBuilder;
import com.android.contacts.common.list.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;
import com.android.contacts.group.GroupDetailFragment;
import com.sprd.contacts.list.OnContactMultiPickerActionListener;
import android.provider.ContactsContract.RawContacts;
import com.sprd.contacts.common.util.UniverseUtils;
import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;

/**
 * Fragment for the contact list used for browsing contacts (as compared to
 * picking a contact with one of the PICK or SHORTCUT intents).
 */
public class ContactPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter>
        implements OnShortcutIntentCreatedListener {

    private static final String KEY_EDIT_MODE = "editMode";
    private static final String KEY_CREATE_CONTACT_ENABLED = "createContactEnabled";
    private static final String KEY_SHORTCUT_REQUESTED = "shortcutRequested";

    private OnContactPickerActionListener mListener;
    private boolean mCreateContactEnabled;
    private boolean mEditMode;
    private boolean mShortcutRequested;

    public ContactPickerFragment() {
        /**
        * SPRD:
        * 
        *
        * Original Android code:
        * setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
        setQuickContactEnabled(false);
        setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_CONTACT_SHORTCUT);
        * 
        * @{
        */
        this(null);
        /**
        * @}
        */
    }

    public void setOnContactPickerActionListener(OnContactPickerActionListener listener) {
        mListener = listener;
    }

    public boolean isCreateContactEnabled() {
        return mCreateContactEnabled;
    }

    public void setCreateContactEnabled(boolean flag) {
        this.mCreateContactEnabled = flag;
    }

    public boolean isEditMode() {
        return mEditMode;
    }

    public void setEditMode(boolean flag) {
        mEditMode = flag;
    }

    public void setShortcutRequested(boolean flag) {
        mShortcutRequested = flag;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_EDIT_MODE, mEditMode);
        outState.putBoolean(KEY_CREATE_CONTACT_ENABLED, mCreateContactEnabled);
        outState.putBoolean(KEY_SHORTCUT_REQUESTED, mShortcutRequested);
        /**
        * SPRD:
        * 
        * @{
        */
        outState.putParcelable(KEY_FILTER, mFilter);
        outState.putString(KEY_GROUP_SELECTION, mAddGroupMemSelection);
        outState.putBoolean(KEY_STAR_SELECTION, mIsStarMemFlag);
        /**
        * @}
        */
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);

        if (savedState == null) {
            return;
        }

        mEditMode = savedState.getBoolean(KEY_EDIT_MODE);
        mCreateContactEnabled = savedState.getBoolean(KEY_CREATE_CONTACT_ENABLED);
        mShortcutRequested = savedState.getBoolean(KEY_SHORTCUT_REQUESTED);
        /**
        * SPRD:
        * 
        * @{
        */
        mFilter = (ContactListFilter)savedState.getParcelable(KEY_FILTER);
        mAddGroupMemSelection = savedState.getString(KEY_GROUP_SELECTION);
        mIsStarMemFlag = savedState.getBoolean(KEY_STAR_SELECTION);
        /**
        * @}
        */
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        if (mCreateContactEnabled) {
            getListView().addHeaderView(inflater.inflate(R.layout.create_new_contact, null, false));
        }
        /**
        * SPRD:
        * 
        * @{
        */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
           // mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);
            updateFilterHeaderView();
        }
        /**
        * @}
        */
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0 && mCreateContactEnabled) {
            mListener.onCreateNewContactAction();
        } else {
            super.onItemClick(parent, view, position, id);
        }
    }

    @Override
    protected void onItemClick(int position, long id) {
        Uri uri;
        if (isLegacyCompatibilityMode()) {
            uri = ((LegacyContactListAdapter)getAdapter()).getPersonUri(position);
        } else {
            uri = ((ContactListAdapter)getAdapter()).getContactUri(position);
        }
        if (mEditMode) {
            editContact(uri);
        } else  if (mShortcutRequested) {
            ShortcutIntentBuilder builder = new ShortcutIntentBuilder(getActivity(), this);
            builder.createContactShortcutIntent(uri);
        } else {
            pickContact(uri);
        }
    }

    public void createNewContact() {
        mListener.onCreateNewContactAction();
    }

    public void editContact(Uri contactUri) {
        mListener.onEditContactAction(contactUri);
    }

    public void pickContact(Uri uri) {
        mListener.onPickContactAction(uri);
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        if (!isLegacyCompatibilityMode()) {
            DefaultContactListAdapter adapter = new DefaultContactListAdapter(getActivity());
            adapter.setFilter(ContactListFilter.createFilterWithType(
                    ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
            adapter.setSectionHeaderDisplayEnabled(true);
            adapter.setDisplayPhotos(true);
            adapter.setQuickContactEnabled(false);
            return adapter;
        } else {
            LegacyContactListAdapter adapter = new LegacyContactListAdapter(getActivity());
            adapter.setSectionHeaderDisplayEnabled(false);
            adapter.setDisplayPhotos(false);
            return adapter;
        }
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();

        ContactEntryListAdapter adapter = getAdapter();
        /**
        * SPRD:
        * 
        * @{
        */
        if (!isSearchMode() && mFilter != null) {
            adapter.setFilter(mFilter);
        }
        /**
        * @}
        */

        // If "Create new contact" is shown, don't display the empty list UI
        adapter.setEmptyListEnabled(!isCreateContactEnabled());
        /**
        * SPRD:
        * 
        * @{
        */
        if (adapter instanceof DefaultContactListAdapter) {
            DefaultContactListAdapter defaultAdapter = (DefaultContactListAdapter) adapter;
            defaultAdapter.setAddGroupMemSelection(mAddGroupMemSelection);
            if(mIsStarMemFlag){
                defaultAdapter.setStarMemSelection();
            }
        }
        /**
        * @}
        */
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        /**
        * SPRD:
        * 
        *
        * Original Android code:
        * return inflater.inflate(R.layout.contact_picker_content, null);
        * 
        * @{
        */
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            return inflater.inflate(R.layout.contact_list_content_overlay, null);
        } else {
            return inflater.inflate(R.layout.contact_list_content, null);
        }
        /**
        * @}
        */
    }

    @Override
    public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
        mListener.onShortcutIntentCreated(shortcutIntent);
    }

    @Override
    public void onPickerResult(Intent data) {
        mListener.onPickContactAction(data.getData());
    }



    /**
    * SPRD:
    * 
    * @{
    */
    private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;
    private static final String KEY_FILTER = "mFilter";
    private static final String KEY_GROUP_SELECTION = "addGroupMemSelection";
    private static final String KEY_STAR_SELECTION = "addStarMemSelection";
    private static final int SHOW_TARGET_FRAGMENT_ID = 0;
    private static final int LOADER_RAW_CONTACT_ID = 1;
    public static final String MMS_MULTI_VCARD = "multiVcard";

    private boolean mLoaderStarted;
    private ContactListFilter mFilter;
    private View mAccountFilterHeader;
    private boolean mPermanentAccountFilter = false;
    private OnContactMultiPickerActionListener mMultiPickerListener;
    private static final String TAG = "ContactPickerFragment";
    private long[] memberToMove;
    private Long mTargetGroupId;
    private ContactListFilterController mContactListFilterController;
    private String mAddGroupMemSelection;
    private boolean mIsStarMemFlag = false;
    private ArrayList<String> mGroupIds;
    private ArrayList<String> mGroupTitles;

    private class FilterHeaderClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            if (!mPermanentAccountFilter) {
                AccountFilterUtil.startAccountFilterActivityForResult(
                        ContactPickerFragment.this, REQUEST_CODE_ACCOUNT_FILTER, getFilter());

            }
        }
    }

    private OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

    public ContactPickerFragment(Activity activity) {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
        setQuickContactEnabled(false);
        setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_CONTACT_SHORTCUT);
        if (activity != null) {
            mContactListFilterController = ContactListFilterController.getInstance(activity);
            mContactListFilterController.checkFilterValidity(false);
        }
    }

    public boolean isShortcutRequested() {
        return mShortcutRequested;
    }

    public void setAddGroupMemSelection(String selection) {
        mAddGroupMemSelection = selection;
    }

    public void setStarMemFlag(){
         mIsStarMemFlag = true;
    }

    @Override
    protected void prepareEmptyView() {
        super.prepareEmptyView();
        setEmptyText(R.string.listTotalAllContactsZero);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ACCOUNT_FILTER) {
            if (getActivity() != null) {
                final ContactListFilterController controller = ContactListFilterController
                        .getInstance(getActivity());
                AccountFilterUtil.handleAccountFilterResult(controller, resultCode, data);
            }
        }
    }

    public void setFilter(ContactListFilter filter) {
        if (filter == null) {
            return;
        }
        if (mFilter == null || !mFilter.toString().equals(filter.toString())) {
            mFilter = filter;
        }
        if (mLoaderStarted) {
            reloadData();
            updateFilterHeaderView();
        }
    }

	public void setPermanentFilter(ContactListFilter filter) {
		mPermanentAccountFilter = true;
		setFilter(filter);
	}

    public ContactListFilter getFilter() {
        return mFilter;
    }

    @Override
    protected void startLoading() {
        mLoaderStarted = true;
        super.startLoading();
    }

    @Override
    protected void setSearchMode(boolean flag) {
        super.setSearchMode(flag);
        updateFilterHeaderView();
    }

    private void updateFilterHeaderView() {
        final ContactListFilter filter = getFilter();
        if (mAccountFilterHeader == null || filter == null) {
            return;
        }
        final boolean shouldShowHeader = AccountFilterUtil.updateAccountFilterTitle(
                mAccountFilterHeader, filter, true, getListFilterId());

        if (Constants.DEBUG)
            Log.d(TAG, "isSearchMode:" + isSearchMode());
        if (shouldShowHeader && !isSearchMode()) {
            mAccountFilterHeader.setVisibility(View.VISIBLE);
        } else {
            mAccountFilterHeader.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMultiPickerSelected() {
        boolean setStarredFlag = getActivity().getIntent()
                .getBooleanExtra("setMulitStarred", false);
        String setMultiVcard = null;
        if (getActivity().getIntent().getExtras() != null) {
            setMultiVcard = getActivity().getIntent().getExtras().getString(MMS_MULTI_VCARD);
        }
        if (UniverseUtils.UNIVERSEUI_SUPPORT
                && (setStarredFlag == true || MMS_MULTI_VCARD.equals(setMultiVcard))) {
            ArrayList<String> ret = new ArrayList<String>();
            ArrayList<String> ret2 = new ArrayList<String>();
            ContactEntryListAdapter adapter = getAdapter();
            Set<Integer> checkedItems = adapter.getCheckedItems();
            for (Integer i : checkedItems) {
                Cursor c = (Cursor) adapter.getItem(i);
                if (c != null) {
                    String lookupKey = c.getString(c
                            .getColumnIndex(Contacts.LOOKUP_KEY));
                    ret.add(lookupKey);
                    ret2.add(c.getString(c.getColumnIndex(Contacts._ID)));
                }
            }
            if (ret.size() == 0) {
                Toast.makeText(getActivity(), R.string.toast_no_contact_selected,
                        Toast.LENGTH_SHORT).show();
                if (mMultiPickerListener != null) {
                    mMultiPickerListener.onCancel();
                } else {
                    Log.e(TAG, "mMultiPickerListener is null");
                }

            } else {
                if (mMultiPickerListener != null) {
                    if (Constants.DEBUG)
                        Log.d(TAG, "Contact add num from multiselect is " + ret.size());
                    mMultiPickerListener.onPickContactAction(ret, ret2);
                } else {
                    Log.e(TAG, "mMultiPickerListener is null");
                }

            }
        } else {
            ArrayList<String> ret = new ArrayList<String>();
            ArrayList<String> ret2 = new ArrayList<String>();
            ArrayList<Long> contactIds = new ArrayList<Long>();
            ContactEntryListAdapter adapter = getAdapter();
            Set<Integer> checkedItems = adapter.getCheckedItems();
            for (Integer i : checkedItems) {
                Cursor c = (Cursor) adapter.getItem(i);
                if (c != null) {
                    String lookupKey = c.getString(c.getColumnIndex(Contacts.LOOKUP_KEY));
                    Long contactId = c.getLong(c.getColumnIndex(Contacts._ID));
                    contactIds.add(contactId);
                    ret.add(lookupKey);
                    ret2.add(c.getString(c.getColumnIndex(Contacts._ID)));
                }
            }
            if (ret.size() == 0) {
                Toast.makeText(getActivity(),
                        R.string.toast_no_contact_selected, Toast.LENGTH_SHORT)
                        .show();
                if (mMultiPickerListener != null) {
                    mMultiPickerListener.onCancel();
                } else {
                    Log.e(TAG, "mMultiPickerListener is null");
                }
            } else {
                if (getActivity().getIntent().hasExtra("move_group_member")
                        || getActivity().getIntent().hasExtra("delete_group_member")) {
                    Bundle args = new Bundle();
                    if (contactIds != null && contactIds.size() > 0) {
                        long[] contactIdsToMove = new long[contactIds.size()];
                        for (int i = 0; i < contactIds.size(); i++) {
                            contactIdsToMove[i] = contactIds.get(i);
                        }
                        args.putLongArray("contact_Ids", contactIdsToMove);
                    }
                    getLoaderManager().restartLoader(LOADER_RAW_CONTACT_ID, args,
                            mSelectedGroupMemberListener);
                } else {
                    if (mMultiPickerListener != null) {
                        mMultiPickerListener.onPickContactAction(ret, ret2);
                    } else {
                        Log.e(TAG, "mMultiPickerListener is null");
                    }
                }

            }
        }
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mSelectedGroupMemberListener =
            new LoaderCallbacks<Cursor>() {
                ArrayList<Long> rawContactIds = new ArrayList<Long>();

                @Override
                public CursorLoader onCreateLoader(int id, Bundle args) {
                    CursorLoader ret = new CursorLoader(getActivity());
                    if (id == LOADER_RAW_CONTACT_ID) {
                        ret.setUri(RawContacts.CONTENT_URI);
                        ret.setProjection(new String[] {
                                RawContacts._ID, RawContacts.CONTACT_ID
                        });
                        long[] contactIds = args.getLongArray("contact_Ids");
                        StringBuilder sb = new StringBuilder();
                        boolean init = true;
                        for (long contactId : contactIds) {
                            if (!init) {
                                sb.append(",");
                            }
                            init = false;
                            sb.append(contactId);
                        }
                        ret.setSelection(RawContacts.CONTACT_ID + " in (" + sb.toString() + ")");
                    }
                    return ret;
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                    int id = loader.getId();
                    if (id == LOADER_RAW_CONTACT_ID) {
                        if (data != null && data.moveToFirst()) {
                            do {
                                long rawContactId = data.getLong(
                                        data.getColumnIndex(RawContacts._ID));
                                rawContactIds.add(rawContactId);
                            } while (data.moveToNext());
                            if (rawContactIds != null) {
                                memberToMove = new long[rawContactIds.size()];
                                for (int i = 0; i < rawContactIds.size(); i++) {
                                    memberToMove[i] = rawContactIds.get(i);
                                }
                            }
                            getLoaderManager().destroyLoader(LOADER_RAW_CONTACT_ID);
                            if (getActivity().getIntent().hasExtra("move_group_member")) {
                                mGroupIds = (ArrayList<String>) getActivity()
                                        .getIntent().getExtra(
                                                GroupDetailFragmentSprd.TARGET_GROUP_IDS);
                                mGroupTitles = (ArrayList<String>) getActivity()
                                        .getIntent().getExtra(
                                                GroupDetailFragmentSprd.TARGET_GROUP_TITLES);
                                if (mGroupIds != null && mGroupTitles != null) {
                                    Message message = mHandler.obtainMessage(SHOW_TARGET_FRAGMENT_ID);
                                    mHandler.sendMessage(message);                                }
                            } else if (getActivity().getIntent().hasExtra("delete_group_member")) {
                                /**
                                 * SPRD:
                                 *   add iLog
                                 *
                                 * Original Android code:
                                 *  
                                 * 
                                 * @{
                                 */
                                if (Log.isIloggable()) {
                                    Log.startPerfTracking(Constants.PERFORMANCE_TAG + String.format(
                                            ": Start deleting %d contacts from group", memberToMove.length));
                                }
                                 /**
                                 * @}
                                 */
                                getContext()
                                        .startService(
                                                ContactSaveService
                                                        .createGroupUpdateIntent(
                                                                getContext(),
                                                                (Long) (getActivity().getIntent()
                                                                        .getExtra("delete_group_member")),
                                                                null,
                                                                null,
                                                                memberToMove,
                                                                ContactSelectionActivity.class,
                                                                ContactSelectionActivity.MOVE_GROUP_COMPLETE));
                                getActivity().finish();

                            }

                        }
                    }
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    public void setOnContactMultiPickerActionListener(OnContactMultiPickerActionListener listener) {
        mMultiPickerListener = listener;
    }

    public static class ShowTargetGroupDialogFragment extends DialogFragment {
        static Long mTargetGroupId;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ArrayList<String> groupIds = getArguments().getStringArrayList("groupId");
            ArrayList<String> groupTitles = getArguments().getStringArrayList("groupTitle");
            final long[] member = getArguments().getLongArray("memberToMove");
            final Long[] ids = (Long[]) groupIds.toArray(new Long[groupIds.size()]);
            final String[] titles = (String[]) groupTitles.toArray(new String[groupTitles.size()]);

            if (ids.length > 0) {
                mTargetGroupId = Long.valueOf(ids[0]);
            }

            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.moveGroupMemberDialogTitle)
                    .setSingleChoiceItems(titles, 0,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which >= 0) {
                                        mTargetGroupId = ids[which];
                                    }
                                }
                            })
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (getActivity() != null) {
                                        Intent serviceIntent = ContactSaveService
                                                .createGroupMoveIntent(
                                                        getActivity(),
                                                        mTargetGroupId,
                                                        (Long) (getActivity()
                                                                .getIntent()
                                                                .getExtra(GroupDetailFragmentSprd.SRC_GROUP_ID)),
                                                        null,
                                                        member,
                                                        ContactSelectionActivity.class,
                                                        ContactSelectionActivity.MOVE_GROUP_COMPLETE);
                                        getActivity().startService(serviceIntent);

                                        getActivity().finish();
                                    }
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, null).create();
            return dialog;
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_TARGET_FRAGMENT_ID:
                    ShowTargetGroupDialogFragment showDialogFragment = new ShowTargetGroupDialogFragment();
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList("groupId", mGroupIds);
                    bundle.putStringArrayList("groupTitle", mGroupTitles);
                    bundle.putLongArray("memberToMove", memberToMove);
                    showDialogFragment.setArguments(bundle);
                    showDialogFragment.show(getFragmentManager(), null);
                    break;

                default:
                    break;
            }
        }
    };
    /**
    * @}
    */
}
