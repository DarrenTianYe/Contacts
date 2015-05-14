/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.view.View.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;
import android.content.Context;
import java.util.Set;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.common.list.DirectoryListLoader;
import com.android.contacts.common.util.AccountFilterUtil;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.list.ContactListFilterController.ContactListFilterListener;
import com.sprd.contacts.list.OnEmailAddressMultiPickerActionListener;

import java.util.HashMap;

/**
 * Fragment containing an email list for picking.
 */
public class EmailAddressPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter>
        implements OnItemLongClickListener {
    private OnEmailAddressPickerActionListener mListener;

    public EmailAddressPickerFragment() {
        setQuickContactEnabled(false);
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_DATA_SHORTCUT);
        /**
        * SPRD:
        * 
        * @{
        */
        setHasOptionsMenu(true);
        /**
        * @}
        */
    }

    public void setOnEmailAddressPickerActionListener(OnEmailAddressPickerActionListener listener) {
        mListener = listener;
    }

    @Override
    protected void onItemClick(int position, long id) {
        EmailAddressListAdapter adapter = (EmailAddressListAdapter)getAdapter();
        pickEmailAddress(adapter.getDataUri(position));
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        EmailAddressListAdapter adapter = new EmailAddressListAdapter(getActivity());
        adapter.setSectionHeaderDisplayEnabled(true);
        adapter.setDisplayPhotos(true);
        return adapter;
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, null);
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);

        /**
        * SPRD:
        * 
        * @{
        */
        mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);
        mListView.setOnItemLongClickListener(this);
        updateFilterHeaderView();
        /**
        * @}
        */
        setVisibleScrollbarEnabled(!isLegacyCompatibilityMode());
    }




    /**
    * SPRD:
    * 
    * @{
    */
    private static final String TAG = EmailAddressPickerFragment.class.getSimpleName();

    private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;
    private OnEmailAddressMultiPickerActionListener mMultiPickerListener;
    private String mShortcutAction;

    private ContactListFilter mFilter;

    private View mAccountFilterHeader;
    /**
     * Lives as ListView's header and is shown when
     * {@link #mAccountFilterHeader} is set to View.GONE.
     */

    private static final String KEY_FILTER = "filter";

    /** true if the loader has started at least once. */
    private boolean mLoaderStarted;

    private ContactListItemView.PhotoPosition mPhotoPosition =
            ContactListItemView.getDefaultPhotoPosition(false);

    private class FilterHeaderClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(
                    EmailAddressPickerFragment.this, REQUEST_CODE_ACCOUNT_FILTER, getFilter());
        }
    }

    private OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

    private void pickEmailAddress(Uri uri) {
        mListener.onPickEmailAddressAction(uri);
    }

    public void setOnEmailAddressMultiPickerActionListener(
            OnEmailAddressMultiPickerActionListener listener) {
        this.mMultiPickerListener = listener;
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

        if (shouldShowHeader && !isSearchMode()) {
            mAccountFilterHeader.setVisibility(View.VISIBLE);
        } else {
            mAccountFilterHeader.setVisibility(View.GONE);
        }
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);

        if (savedState == null) {
            return;
        }

        mFilter = savedState.getParcelable(KEY_FILTER);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_FILTER, mFilter);
    }

    @Override
    protected void startLoading() {
        mLoaderStarted = true;
        super.startLoading();
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();

        final ContactEntryListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }

        if (!isSearchMode() && mFilter != null) {
            adapter.setFilter(mFilter);
        }
    }

    @Override
    protected void prepareEmptyView() {
        super.prepareEmptyView();
        setEmptyText(R.string.listTotalEmailContactsZero);
    }

    @Override
    public void onPickerResult(Intent data) {
        mListener.onPickEmailAddressAction(data.getData());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ACCOUNT_FILTER) {
            if (getActivity() != null) {
                final ContactListFilterController controller = ContactListFilterController
                        .getInstance(getActivity());
                controller.addListener(new ContactListFilterListener() {
                    public void onContactListFilterChanged() {
                        setFilter(controller.getFilter());
                    }
                });
                AccountFilterUtil.handleAccountFilterResult(controller, resultCode, data);
            } else {
                Log.e(TAG, "getActivity() returns null during Fragment#onActivityResult()");
            }
        }
    }

    public ContactListFilter getFilter() {
        return mFilter;
    }

    public void setFilter(ContactListFilter filter) {
        if (filter == null) {
            return;
        }
        mFilter = filter;
        if (mLoaderStarted) {
            reloadData();
        }
        updateFilterHeaderView();
    }

    private void hideSoftKeyboard() {
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager)
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mListView.getWindowToken(), 0);
    }

    @Override
    public int getListFilterId() {
        return R.string.list_filter_emails;
    }

    @Override
    public void onMultiPickerSelected() {
        HashMap<String, String> ret = new HashMap<String, String>();
        ContactEntryListAdapter adapter = getAdapter();
        Set<Integer> checkedItems = adapter.getCheckedItems();
        for (Integer i : checkedItems) {
            Cursor c = (Cursor) adapter.getItem(i);
            String num = c.getString(c.getColumnIndex(Email.ADDRESS));
            String name = c.getString(c.getColumnIndex(Email.DISPLAY_NAME_PRIMARY));
            ret.put(num, name);
        }
        if (ret.size() == 0) {
            Toast.makeText(getActivity(), R.string.toast_no_contact_selected,
                    Toast.LENGTH_SHORT).show();
        }
        mMultiPickerListener.onPickEmailAddressAction(ret);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        return false;
    }
    /**
    * @}
    */
}
