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

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.ContactCounts;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.provider.ContactsContract.RawContacts;
import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.list.ContactListFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * A cursor adapter for the {@link Email#CONTENT_TYPE} content type.
 */
/**
* SPRD:
* 
*
* Original Android code:
* public class EmailAddressListAdapter extends ContactEntryListAdapter {
* 
* @{
*/
public class EmailAddressListAdapter extends ContactEntryListAdapter {

/**
* @}
*/

    protected static class EmailQuery {
        private static final String[] PROJECTION_PRIMARY = new String[] {
            Email._ID,                       // 0
            Email.TYPE,                      // 1
            Email.LABEL,                     // 2
            Email.DATA,                      // 3
            Email.PHOTO_ID,                  // 4
            Email.DISPLAY_NAME_PRIMARY,      // 5
            /**
            * SPRD:
            * 
            * @{
            */
            Contacts.DISPLAY_ACCOUNT_TYPE,
            Contacts.DISPLAY_ACCOUNT_NAME,
            /**
            * @}
            */
        };

        private static final String[] PROJECTION_ALTERNATIVE = new String[] {
            Email._ID,                       // 0
            Email.TYPE,                      // 1
            Email.LABEL,                     // 2
            Email.DATA,                      // 3
            Email.PHOTO_ID,                  // 4
            Email.DISPLAY_NAME_ALTERNATIVE,  // 5
            /**
            * SPRD:
            * 
            * @{
            */
            Contacts.DISPLAY_ACCOUNT_TYPE,
            Contacts.DISPLAY_ACCOUNT_NAME,
            /**
            * @}
            */
        };

        public static final int EMAIL_ID           = 0;
        public static final int EMAIL_TYPE         = 1;
        public static final int EMAIL_LABEL        = 2;
        public static final int EMAIL_ADDRESS      = 3;
        public static final int EMAIL_PHOTO_ID     = 4;
        public static final int EMAIL_DISPLAY_NAME = 5;
        /**
        * SPRD:
        * 
        * @{
        */
        public static final int CONTACT_DISPLAY_ACCOUNT_TYPE = 6;
        public static final int CONTACT_DISPLAY_ACCOUNT_NAME = 7;
        /**
        * @}
        */
    }

    private final CharSequence mUnknownNameText;

    public EmailAddressListAdapter(Context context) {
        super(context);

        mUnknownNameText = context.getText(android.R.string.unknownName);
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        final Builder builder;
        if (isSearchMode()) {
            builder = Email.CONTENT_FILTER_URI.buildUpon();
            String query = getQueryString();
            builder.appendPath(TextUtils.isEmpty(query) ? "" : query);
        } else {
            builder = Email.CONTENT_URI.buildUpon();
            /**
            * SPRD:
            * 
            * @{
            */
            configureSelection(loader, directoryId, getFilter());
            /**
            * @}
            */
            if (isSectionHeaderDisplayEnabled()) {
                builder.appendQueryParameter(ContactCounts.ADDRESS_BOOK_INDEX_EXTRAS, "true");
            }
        }
        builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                String.valueOf(directoryId));
        builder.appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true");
        loader.setUri(builder.build());

        if (getContactNameDisplayOrder() == ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY) {
            loader.setProjection(EmailQuery.PROJECTION_PRIMARY);
        } else {
            loader.setProjection(EmailQuery.PROJECTION_ALTERNATIVE);
        }

        if (getSortOrder() == ContactsContract.Preferences.SORT_ORDER_PRIMARY) {
            loader.setSortOrder(Email.SORT_KEY_PRIMARY);
        } else {
            loader.setSortOrder(Email.SORT_KEY_ALTERNATIVE);
        }
    }

    @Override
    public String getContactDisplayName(int position) {
        return ((Cursor) getItem(position)).getString(EmailQuery.EMAIL_DISPLAY_NAME);
    }

    /**
     * Builds a {@link Data#CONTENT_URI} for the current cursor
     * position.
     */
    public Uri getDataUri(int position) {
        long id = ((Cursor) getItem(position)).getLong(EmailQuery.EMAIL_ID);
        return ContentUris.withAppendedId(Data.CONTENT_URI, id);
    }

    @Override
    protected View newView(Context context, int partition, Cursor cursor, int position,
            ViewGroup parent) {
        final ContactListItemView view = new ContactListItemView(context, null);
        view.setUnknownNameText(mUnknownNameText);
        view.setQuickContactEnabled(isQuickContactEnabled());
        return view;
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        ContactListItemView view = (ContactListItemView)itemView;
        bindSectionHeaderAndDivider(view, position);
        bindName(view, cursor);
        bindPhoto(view, cursor);
        bindEmailAddress(view, cursor);
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
    }

    protected void bindEmailAddress(ContactListItemView view, Cursor cursor) {
        CharSequence label = null;
        if (!cursor.isNull(EmailQuery.EMAIL_TYPE)) {
            final int type = cursor.getInt(EmailQuery.EMAIL_TYPE);
            final String customLabel = cursor.getString(EmailQuery.EMAIL_LABEL);

            // TODO cache
            label = Email.getTypeLabel(getContext().getResources(), type, customLabel);
        }
        view.setLabel(label);
        view.showData(cursor, EmailQuery.EMAIL_ADDRESS);
    }

    protected void bindSectionHeaderAndDivider(final ContactListItemView view, int position) {
        final int section = getSectionForPosition(position);
        if (getPositionForSection(section) == position) {
            String title = (String)getSections()[section];
            view.setSectionHeader(title);
        } else {
            view.setDividerVisible(false);
            view.setSectionHeader(null);
        }

        // move the divider for the last item in a section
        if (getPositionForSection(section + 1) - 1 == position) {
            view.setDividerVisible(false);
        } else {
            view.setDividerVisible(true);
        }
    }

    protected void bindName(final ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, EmailQuery.EMAIL_DISPLAY_NAME, getContactNameDisplayOrder());
    }

    protected void bindPhoto(final ContactListItemView view, Cursor cursor) {
        long photoId = 0;
        if (!cursor.isNull(EmailQuery.EMAIL_PHOTO_ID)) {
            photoId = cursor.getLong(EmailQuery.EMAIL_PHOTO_ID);
        }

        /**
        * SPRD:
        * 
        *
        * Original Android code:
        * getPhotoLoader().loadThumbnail(view.getPhotoView(), photoId, false);
        * 
        * @{
        */
        String accountType = cursor.getString(EmailQuery.CONTACT_DISPLAY_ACCOUNT_TYPE);
        String accountName = cursor.getString(EmailQuery.CONTACT_DISPLAY_ACCOUNT_NAME);

        AccountWithDataSet account = null;
        if (accountType != null && accountName != null) {
            account = new AccountWithDataSet(accountName, accountType, null);
        }

        getPhotoLoader().loadThumbnail(view.getPhotoView(), photoId, false,
                getPhotoLoader().getDefaultPhotoProviderForAccount(mContext, account));
        /**
        * @}
        */
    }
//
//    protected void bindSearchSnippet(final ContactListItemView view, Cursor cursor) {
//        view.showSnippet(cursor, SUMMARY_SNIPPET_MIMETYPE_COLUMN_INDEX,
//                SUMMARY_SNIPPET_DATA1_COLUMN_INDEX, SUMMARY_SNIPPET_DATA4_COLUMN_INDEX);
//    }



    /**
    * SPRD:
    * 
    * @{
    */
    protected void bindCheckbox(final ContactListItemView view, int position) {
        view.showCheckbox(isChecked(position));
    }

    private void configureSelection(CursorLoader loader, long directoryId, ContactListFilter filter) {
        if (filter == null || directoryId != Directory.DEFAULT) {
            return;
        }

        final StringBuilder selection = new StringBuilder();
        final List<String> selectionArgs = new ArrayList<String>();

        switch (filter.filterType) {
            case ContactListFilter.FILTER_TYPE_CUSTOM: {
                selection.append(Contacts.IN_VISIBLE_GROUP + "=1");
                selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
                break;
            }
            case ContactListFilter.FILTER_TYPE_ACCOUNT: {
                selection.append("(");

                selection.append(RawContacts.ACCOUNT_TYPE + "=?"
                        + " AND " + RawContacts.ACCOUNT_NAME + "=?");
                selectionArgs.add(filter.accountType);
                selectionArgs.add(filter.accountName);
                if (filter.dataSet != null) {
                    selection.append(" AND " + RawContacts.DATA_SET + "=?");
                    selectionArgs.add(filter.dataSet);
                } else {
                    selection.append(" AND " + RawContacts.DATA_SET + " IS NULL");
                }
                selection.append(")");
                break;
            }
            case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS:
//            case ContactListFilter.FILTER_TYPE_DEFAULT:
//                break; // No selection needed.
            case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY:
                break; // This adapter is always "phone only", so no selection
                       // needed either.
            default:
                break;
        }
        loader.setSelection(selection.toString());
        loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
    }
    /**
    * @}
    */
}
