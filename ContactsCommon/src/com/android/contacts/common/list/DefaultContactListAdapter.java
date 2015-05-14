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
package com.android.contacts.common.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.SearchSnippetColumns;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import neolink.telephony.PrivateManager;
import neolink.telephony.PrivateMode;
import com.android.contacts.common.preference.ContactsPreferences;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.AccountTypeManager;
import java.util.ArrayList;
import java.util.List;

/**
 * A cursor adapter for the {@link ContactsContract.Contacts#CONTENT_TYPE} content type.
 */
public class DefaultContactListAdapter extends ContactListAdapter {

    public static final char SNIPPET_START_MATCH = '\u0001';
    public static final char SNIPPET_END_MATCH = '\u0001';
    public static final String SNIPPET_ELLIPSIS = "\u2026";
    public static final int SNIPPET_MAX_TOKENS = 5;

    public static final String SNIPPET_ARGS = SNIPPET_START_MATCH + "," + SNIPPET_END_MATCH + ","
            + SNIPPET_ELLIPSIS + "," + SNIPPET_MAX_TOKENS;
	private static final String TAG = "DefaultContactListAdapter";
	/**
	 * add by xuhong.tian
	 */
	public int mMode=PrivateMode.MODE_UNKNOWN;
	private  PrivateManager mManager;

    public DefaultContactListAdapter(Context context) {
        super(context);
		mManager=new PrivateManager(context);
        
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        if (loader instanceof ProfileAndContactsLoader) {
            ((ProfileAndContactsLoader) loader).setLoadProfile(shouldIncludeProfile());
        }

        ContactListFilter filter = getFilter();
        mMode=mManager.getMode();
        Log.d(TAG, "isSearchMode()=="+isSearchMode()+"getSearchMode="+"filter=="+filter.filterType+filter.accountName+filter.accountType);
        if (isSearchMode()) {
            String query = getQueryString();
            if (query == null) {
                query = "";
            }
            query = query.trim();
            if (TextUtils.isEmpty(query)) {
                // Regardless of the directory, we don't want anything returned,
                // so let's just send a "nothing" query to the local directory.
                loader.setUri(Contacts.CONTENT_URI);
                loader.setProjection(getProjection(false));
                loader.setSelection("0");
            } else {
                Builder builder = Contacts.CONTENT_FILTER_URI.buildUpon();
                builder.appendPath(query);      // Builder will encode the query
                builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(directoryId));
                if (directoryId != Directory.DEFAULT && directoryId != Directory.LOCAL_INVISIBLE) {
                    builder.appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                            String.valueOf(getDirectoryResultLimit(getDirectoryById(directoryId))));
                }
                builder.appendQueryParameter(SearchSnippetColumns.SNIPPET_ARGS_PARAM_KEY,
                        SNIPPET_ARGS);
                builder.appendQueryParameter(SearchSnippetColumns.DEFERRED_SNIPPETING_KEY,"1");
                loader.setUri(builder.build());
                loader.setProjection(getProjection(true));
                /**
                * SPRD:
                * 
                * @{
                */
                configureSelection(loader, directoryId, filter, true);
                /**
                * @}
                */
            }
        } else {
            configureUri(loader, directoryId, filter);
            loader.setProjection(getProjection(false));
            configureSelection(loader, directoryId, filter);
        }

        String sortOrder;
        if (getSortOrder() == ContactsContract.Preferences.SORT_ORDER_PRIMARY) {
            sortOrder = Contacts.SORT_KEY_PRIMARY;
        } else {
            sortOrder = Contacts.SORT_KEY_ALTERNATIVE;
        }

        loader.setSortOrder(sortOrder);
    }

    protected void configureUri(CursorLoader loader, long directoryId, ContactListFilter filter) {
        Uri uri = Contacts.CONTENT_URI;
        if (filter != null && filter.filterType == ContactListFilter.FILTER_TYPE_SINGLE_CONTACT) {
            String lookupKey = getSelectedContactLookupKey();
            if (lookupKey != null) {
                uri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey);
            } else {
                uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, getSelectedContactId());
            }
        }

        if (directoryId == Directory.DEFAULT && isSectionHeaderDisplayEnabled()) {
            uri = ContactListAdapter.buildSectionIndexerUri(uri);
        }

        // The "All accounts" filter is the same as the entire contents of Directory.DEFAULT
        if (filter != null
                && filter.filterType != ContactListFilter.FILTER_TYPE_CUSTOM
                && filter.filterType != ContactListFilter.FILTER_TYPE_SINGLE_CONTACT) {
            final Uri.Builder builder = uri.buildUpon();
            builder.appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(Directory.DEFAULT));
            if (filter.filterType == ContactListFilter.FILTER_TYPE_ACCOUNT) {
                filter.addAccountQueryParameterToUrl(builder);
            }
            uri = builder.build();
        }

        loader.setUri(uri);
    }

    /**
    * SPRD:
    *   Defer the action to make the window properly repaint.
    *
    * Original Android code:
    *     private void configureSelection(
            CursorLoader loader, long directoryId, ContactListFilter filter) {
    * 
    * @{
    */
    private void configureSelection(
            CursorLoader loader, long directoryId, ContactListFilter filter) {
        configureSelection(loader, directoryId, filter, false);
    }
    /**
    * @}
    */
    /**
    * SPRD:
    *  add isSearchMode parameter
    *
    * Original Android code:
    * private void configureSelection(
            CursorLoader loader, long directoryId, ContactListFilter filter)
    * 
    * @{
    */

    private void configureSelection(
            CursorLoader loader, long directoryId, ContactListFilter filter, boolean isSearchMode) {
        /**
         * @}
         */
        if (filter == null) {
            return;
        }

        if (directoryId != Directory.DEFAULT) {
            return;
        }

        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<String>();

        switch (filter.filterType) {
            case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS: {
//                // We have already added directory=0 to the URI, which takes care of this
//                // filter
//                /**
//                * SPRD:
//                * 
//                * @{
//                */
//                // sunway: add additional filter according to AccountTypeManager
//                // state, those account not "syncable" will not be shown in
//                // ALL_ACCOUNT;
//                AccountTypeManager am = AccountTypeManager.getInstance(getContext());
//                List<AccountWithDataSet> accounts = am.getAccounts(false);
//
//                selection.append(
//                        " EXISTS ("
//                                + "SELECT DISTINCT " + RawContacts.CONTACT_ID
//                                + " FROM view_raw_contacts"
//                                + " WHERE ( " + "view_contacts." + Contacts._ID + "="
//                                + RawContacts.CONTACT_ID
//                                + "  AND (("
//                                + RawContacts.ACCOUNT_TYPE + " is NULL "
//                                + " AND " + RawContacts.ACCOUNT_NAME + " is NULL )");
//                for (AccountWithDataSet account : accounts) {
//                    selection.append(" or ( " + RawContacts.ACCOUNT_TYPE + "=?" +
//                            " AND " + RawContacts.ACCOUNT_NAME + "=?" +
//                            ") "
//                            );
//                    selectionArgs.add(account.type);
//                    selectionArgs.add(account.name);
//                }
//                selection.append(")))");
//                if (isCustomFilterForPhoneNumbersOnly()) {
//                    selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
//                }
//                /**
//                * @}
//                */
            	
                selection.append(Contacts.CONTACT_IMPORT_TYPE+" = "+String.valueOf(RawContacts.CONTACTS_BY_LOCAL));
                selection.append(" OR " + Contacts.CONTACAT_MODE_TYPE + " = "+String.valueOf(mMode));
            	
                break;
            }
            case ContactListFilter.FILTER_TYPE_SINGLE_CONTACT: {
                // We have already added the lookup key to the URI, which takes care of this
                // filter
                break;
            }
            case ContactListFilter.FILTER_TYPE_STARRED: {
                selection.append(Contacts.STARRED + "!=0");
                break;
            }
            case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY: {
                selection.append(Contacts.HAS_PHONE_NUMBER + "=1");
                break;
            }
            case ContactListFilter.FILTER_TYPE_CUSTOM: {
                selection.append(Contacts.IN_VISIBLE_GROUP + "=1");
                if (isCustomFilterForPhoneNumbersOnly()) {
                    selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
                }
                break;
            }
            /**
            * SPRD:
            * 
            * @{
            */
            case ContactListFilter.FILTER_TYPE_ACCOUNTS: {
                // TODO: avoid the use of private API
                selection.append(
                        " EXISTS ("
                                + "SELECT DISTINCT " + RawContacts.CONTACT_ID
                                + " FROM view_raw_contacts WHERE ( "
                                + "view_contacts." + Contacts._ID + "=" + RawContacts.CONTACT_ID
                                + " AND (");
                boolean init = true;

                if (filter.accounts.size() == 0) {
                    selection.append("(" + RawContacts.ACCOUNT_TYPE + "=\"" + NO_ACCOUNT + "\"");
                    selection.append(" AND ");
                    selection.append(RawContacts.ACCOUNT_NAME + "=\"" + NO_ACCOUNT
                            + "\")");
                } else {
                    for (AccountWithDataSet account : filter.accounts) {
                        if (!init) {
                            selection.append(" OR ");
                        }
                        init = false;
                        selection.append("(" + RawContacts.ACCOUNT_TYPE + "=\"" + account.type
                                + "\"");
                        selection.append(" AND ");
                        selection.append(RawContacts.ACCOUNT_NAME + "=\"" + account.name + "\")");
                    }
                }
                if (isCustomFilterForPhoneNumbersOnly()) {
                    selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
                }
                if(mIsStarMemSelection){
                    selection.append(" AND " + Contacts.STARRED + "=0");
                }
                selection.append(")))");
                break;
            }
            /**
            * @}
            */
            case ContactListFilter.FILTER_TYPE_ACCOUNT: {
                // We use query parameters for account filter, so no selection to add here.
                /**
                * SPRD:
                * 
                * @{
                */
                if (isSearchMode) {
                    selection.append(
                            " EXISTS ("
                                    + "SELECT DISTINCT " + RawContacts.CONTACT_ID
                                    + " FROM view_raw_contacts"
                                    + " WHERE ( " + "view_contacts." + Contacts._ID + "="
                                    + RawContacts.CONTACT_ID + " AND " + RawContacts.ACCOUNT_TYPE
                                    + "=?"
                                    + " AND " + RawContacts.ACCOUNT_NAME + "=? )");
                    selectionArgs.add(filter.accountType);
                    selectionArgs.add(filter.accountName);
                    selection.append(")");
                }
                if (isCustomFilterForPhoneNumbersOnly()) {
                    if (isSearchMode) {
                        selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
                    } else {
                        selection.append(Contacts.HAS_PHONE_NUMBER + "=1");
                    }
                }
                if (mAddGroupMemSelection != null) {
                    if (isSearchMode) {
                        selection.append(" AND " + Contacts._ID);
                    } else {
                        if (isCustomFilterForPhoneNumbersOnly()) {
                            selection.append(" AND " + Contacts._ID);
                        } else {
                            selection.append(Contacts._ID);
                        }
                    }
                    selection.append(" NOT IN (" + mAddGroupMemSelection + ")");
                }
                /**
                * @}
                */
                break;
            }
            /**
            * SPRD:
            * 
            * @{
            */
            case ContactListFilter.FILTER_TYPE_GROUP_MEMBER: {
                Long groupId = filter.groupId;
                selection.append(
                        Contacts._ID + " IN ("
                                + "SELECT " + RawContacts.CONTACT_ID
                                + " FROM " + "raw_contacts"
                                + " WHERE " + "raw_contacts._id" + " IN "
                                + "(SELECT " + "data." + Data.RAW_CONTACT_ID
                                + " FROM " + "data "
                                + "JOIN mimetypes ON (data.mimetype_id = mimetypes._id)"
                                + " WHERE " + Data.MIMETYPE + "='"
                                + GroupMembership.CONTENT_ITEM_TYPE
                                + "' AND " + GroupMembership.GROUP_ROW_ID +
                                "=?)");
                selection.append(" AND " + RawContacts.DELETED + "=0 )");
                selectionArgs.add(String.valueOf(filter.groupId));
            }

            /**
             * add by xuhong.tian
             */
            case ContactListFilter.FILTER_TYPE_PRIVATECONTACTS: {
            	Log.d(TAG, "mMode=="+mMode);
            	selection.append(Contacts.CONTACAT_MODE_TYPE + " = "+String.valueOf(mMode));
            }
            /**
            * @}
            */
        }
        loader.setSelection(selection.toString());
        loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        final ContactListItemView view = (ContactListItemView)itemView;

        view.setHighlightedPrefix(isSearchMode() ? getUpperCaseQueryString() : null);

        if (isSelectionVisible()) {
            view.setActivated(isSelectedContact(partition, cursor));
        }

        bindSectionHeaderAndDivider(view, position, cursor);

        if (isQuickContactEnabled()) {
            /**
            * SPRD:
            * 
            *
            * Original Android code:
            * bindQuickContact(view, partition, cursor, ContactQuery.CONTACT_PHOTO_ID,
                    ContactQuery.CONTACT_PHOTO_URI, ContactQuery.CONTACT_ID,
                    ContactQuery.CONTACT_LOOKUP_KEY);
            * 
            * @{
            */
            bindQuickContact(view, partition, cursor, ContactQuery.CONTACT_PHOTO_ID,
                    ContactQuery.CONTACT_PHOTO_URI, ContactQuery.CONTACT_ID,
                    ContactQuery.CONTACT_LOOKUP_KEY, ContactQuery.CONTACT_DISPLAY_ACCOUNT_TYPE,
                    ContactQuery.CONTACT_DISPLAY_ACCOUNT_NAME);
            /**
            * @}
            */
        } else {
            if (getDisplayPhotos()) {
                bindPhoto(view, partition, cursor);
            }
        }

        bindName(view, cursor);
        /**
        * SPRD:
        *
        * @{
        */
        if (isMultiPickerSupported()) {
            bindCheckbox(view, getRealPosition(partition, position));
        }
        /**
        * @}
        */
        bindPresenceAndStatusMessage(view, cursor);

        if (isSearchMode()) {
            bindSearchSnippet(view, cursor);
        } else {
            view.setSnippet(null);
        }
    }

    private boolean isCustomFilterForPhoneNumbersOnly() {
        // TODO: this flag should not be stored in shared prefs.  It needs to be in the db.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean(ContactsPreferences.PREF_DISPLAY_ONLY_PHONES,
                ContactsPreferences.PREF_DISPLAY_ONLY_PHONES_DEFAULT);
    }



    /**
    * SPRD:
    * 
    * @{
    */
    public static final String NO_ACCOUNT = "sprd.com.android.account.null";
    private String mAddGroupMemSelection;
    private boolean mIsStarMemSelection = false;

    public void setAddGroupMemSelection(String selection) {
        mAddGroupMemSelection = selection;
    }

    public void setStarMemSelection(){
        mIsStarMemSelection = true;
    }
    /**
    * @}
    */
}
