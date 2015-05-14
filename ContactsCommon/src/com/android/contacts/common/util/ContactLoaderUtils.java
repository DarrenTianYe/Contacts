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

package com.android.contacts.common.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import com.android.contacts.common.util.Constants;
/**
 * Utility methods for the {@link ContactLoader}.
 */
public final class ContactLoaderUtils {

    /** Static helper, not instantiable. */
    private ContactLoaderUtils() {}

    /**
     * Transforms the given Uri and returns a Lookup-Uri that represents the contact.
     * For legacy contacts, a raw-contact lookup is performed. An {@link IllegalArgumentException}
     * can be thrown if the URI is null or the authority is not recognized.
     *
     * Do not call from the UI thread.
     */
    @SuppressWarnings("deprecation")
    public static Uri ensureIsContactUri(final ContentResolver resolver, final Uri uri)
            throws IllegalArgumentException {
        if (uri == null) throw new IllegalArgumentException("uri must not be null");

        final String authority = uri.getAuthority();

        // Current Style Uri?
        if (ContactsContract.AUTHORITY.equals(authority)) {
            final String type = resolver.getType(uri);
            // Contact-Uri? Good, return it
            if (ContactsContract.Contacts.CONTENT_ITEM_TYPE.equals(type)) {
                return uri;
            }

            // RawContact-Uri? Transform it to ContactUri
            if (RawContacts.CONTENT_ITEM_TYPE.equals(type)) {
                final long rawContactId = ContentUris.parseId(uri);
                return RawContacts.getContactLookupUri(resolver,
                        ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId));
            }

            // Anything else? We don't know what this is
            throw new IllegalArgumentException("uri format is unknown");
        }

        // Legacy Style? Convert to RawContact
        final String OBSOLETE_AUTHORITY = Contacts.AUTHORITY;
        if (OBSOLETE_AUTHORITY.equals(authority)) {
            // Legacy Format. Convert to RawContact-Uri and then lookup the contact
            final long rawContactId = ContentUris.parseId(uri);
            return RawContacts.getContactLookupUri(resolver,
                    ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId));
        }

        throw new IllegalArgumentException("uri authority is unknown");
    }
    /**
    * SPRD:
    * 
    * @{
    */
    private static final String TAG = "ContactLoaderUtils";
    
    public static boolean CheckIsBlackNumber(Context context, String str) {
        ContentResolver cr = context.getContentResolver();
        String mumber_value;
        int block_type;
        String[] columns = new String[] {
                BlackColumns.BlackMumber.MUMBER_VALUE,
                BlackColumns.BlackMumber.BLOCK_TYPE
        };

        Cursor cursor = cr.query(BlackColumns.BlackMumber.CONTENT_URI, columns, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    mumber_value = cursor.getString(cursor.getColumnIndex(
                            BlackColumns.BlackMumber.MUMBER_VALUE));
                    block_type = cursor.getInt(cursor.getColumnIndex(
                            BlackColumns.BlackMumber.BLOCK_TYPE));
                    // jinwei 2011-11-18 refer to recent call log
                    if (PhoneNumberUtils.compare(str.trim(), mumber_value.trim())) {

                        return true;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            // process exception
            Log.e(TAG, "CheckIsBlackNumber:exceotion");
        } finally {
            if (cursor != null)
                cursor.close();
            else
                Log.i(TAG, "cursor == null");
        }
        return false;
    }

    public static boolean putToBlockList(Context context, String phoneNumber, int Blocktype,
            String name) {
        ContentResolver cr = context.getContentResolver();
        String normalizeNumber = PhoneNumberUtils.normalizeNumber(phoneNumber);
        ContentValues values = new ContentValues();
        if (values != null) {
            try {
                values.put(BlackColumns.BlackMumber.MUMBER_VALUE, phoneNumber);
                values.put(BlackColumns.BlackMumber.BLOCK_TYPE, Blocktype);
                values.put(BlackColumns.BlackMumber.NAME, name);
                values.put(BlackColumns.BlackMumber.MIN_MATCH,
                        PhoneNumberUtils.toCallerIDMinMatch(normalizeNumber));
                if (Constants.DEBUG)
                    Log.d(TAG, "putToBlockList:values=" + values);
            } catch (Exception e) {
                Log.e(TAG, "putToBlockList:exception");
            }
        }
        Uri result = null;
        try {
            result = cr.insert(BlackColumns.BlackMumber.CONTENT_URI, values);
        } catch (Exception e) {
            Log.e(TAG, "putToBlockList: provider == null");
        }
        return result != null ? true : false;
    }

    public static boolean deleteFromBlockList(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        String[] columns = new String[] {
                BlackColumns.BlackMumber._ID, BlackColumns.BlackMumber.MUMBER_VALUE,
        };
        String mumber_value;
        int result = -1;
        Cursor cursor = cr.query(BlackColumns.BlackMumber.CONTENT_URI, columns, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    mumber_value = cursor.getString(cursor.getColumnIndex(
                            BlackColumns.BlackMumber.MUMBER_VALUE));

                    if (PhoneNumberUtils.compare(phoneNumber.trim(), mumber_value.trim())) {

                        result = cr.delete(BlackColumns.BlackMumber.CONTENT_URI,
                                BlackColumns.BlackMumber.MUMBER_VALUE + "='" + mumber_value + "'",
                                null);
                        break;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            // process exception
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            } else {
                Log.i(TAG, "cursor == null");
            }
        }
        if (result < 0) {
            return false;
        }
        return true;
    }

    public static class BlackColumns {
        public static final String AUTHORITY = "com.sprd.providers.block";

        public static final class BlackMumber implements BaseColumns {
            public static final Uri CONTENT_URI = Uri
                    .parse("content://com.sprd.providers.block/black_mumbers");

            public static final String MUMBER_VALUE = "mumber_value";
            public static final String BLOCK_TYPE = "block_type";
            public static final String NOTES = "notes";
            public static final String NAME = "name";
            public static final String MIN_MATCH = "min_match";
        }

        public static final class BlockRecorder implements BaseColumns {
            public static final Uri CONTENT_URI = Uri
                    .parse("content://com.sprd.providers.block/block_recorded");

            public static final String MUMBER_VALUE = "mumber_value";
            public static final String BLOCK_DATE = "block_date";
            public static final String NAME = "name";
        }
    }
    /**
    * @}
    */
}
