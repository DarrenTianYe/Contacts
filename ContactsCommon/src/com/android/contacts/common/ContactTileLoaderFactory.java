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
 * limitations under the License
 */
package com.android.contacts.common;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;

/**
 * Used to create {@link CursorLoader}s to load different groups of
 * {@link com.android.contacts.list.ContactTileView}.
 */
public final class ContactTileLoaderFactory {

    public final static int CONTACT_ID = 0;
    public final static int DISPLAY_NAME = 1;
    public final static int STARRED = 2;
    public final static int PHOTO_URI = 3;
    public final static int LOOKUP_KEY = 4;
    public final static int CONTACT_PRESENCE = 5;
    public final static int CONTACT_STATUS = 6;

    // Only used for StrequentPhoneOnlyLoader
    public final static int PHONE_NUMBER = 5;
    public final static int PHONE_NUMBER_TYPE = 6;
    public final static int PHONE_NUMBER_LABEL = 7;
    public final static int IS_DEFAULT_NUMBER = 8;
    public final static int PINNED = 9;
    // The _ID field returned for strequent items actually contains data._id instead of
    // contacts._id because the query is performed on the data table. In order to obtain the
    // contact id for strequent items, we thus have to use Phone.contact_id instead.
    public final static int CONTACT_ID_FOR_DATA = 10;

    private static final String[] COLUMNS = new String[] {
        Contacts._ID, // ..........................................0
        Contacts.DISPLAY_NAME, // .................................1
        Contacts.STARRED, // ......................................2
        Contacts.PHOTO_URI, // ....................................3
        Contacts.LOOKUP_KEY, // ...................................4
        Contacts.CONTACT_PRESENCE, // .............................5
        Contacts.CONTACT_STATUS, // ...............................6
        /*
        * SPRD: Bug 304379
        *
        * @{
        */
        RawContacts.ACCOUNT_TYPE,
        /*
        * @}
        */
    };

    /**
     * Projection used for the {@link Contacts#CONTENT_STREQUENT_URI}
     * query when {@link ContactsContract#STREQUENT_PHONE_ONLY} flag
     * is set to true. The main difference is the lack of presence
     * and status data and the addition of phone number and label.
     */
    private static final String[] COLUMNS_PHONE_ONLY = new String[] {
        Contacts._ID, // ..........................................0
        Contacts.DISPLAY_NAME, // .................................1
        Contacts.STARRED, // ......................................2
        Contacts.PHOTO_URI, // ....................................3
        Contacts.LOOKUP_KEY, // ...................................4
        Phone.NUMBER, // ..........................................5
        Phone.TYPE, // ............................................6
        Phone.LABEL, // ...........................................7
        Phone.IS_SUPER_PRIMARY, //.................................8
        Contacts.PINNED, // .......................................9
        Phone.CONTACT_ID, //........................................10
        /*
        * SPRD: Bug 304379
        *
        * @{
        */
        RawContacts.ACCOUNT_TYPE,
        /*
        * @}
        */
    };

    private static final String STARRED_ORDER = Contacts.DISPLAY_NAME+" COLLATE NOCASE ASC";

    public static CursorLoader createStrequentLoader(Context context) {
        /*
        * SPRD:
        *   Bug 304379
        *   After saving the SIM card contacts to make calls, restart the phone
        *   after the common contact interface SIM card contacts Call records disappear
        *
        * @orig
        *  return new CursorLoader(context, Contacts.CONTENT_STREQUENT_URI, COLUMNS, null, null,
                STARRED_ORDER);
        *
        * @{
        */
        return getStrequentLoader(context);
        /*
        * @}
        */
    }

    public static CursorLoader createStrequentPhoneOnlyLoader(Context context) {
        Uri uri = Contacts.CONTENT_STREQUENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.STREQUENT_PHONE_ONLY, "true").build();
        /*
        * SPRD:
        *   Bug 304379
        *   After saving the SIM card contacts to make calls, restart the phone
        *   after the common contact interface SIM card contacts Call records disappear
        *
        * @orig
        *  return new CursorLoader(context, uri, COLUMNS_PHONE_ONLY, null, null, null);
        *
        * @{
        */
        return getStrequentPhoneOnlyLoader(context, uri);
        /*
        * @}
        */
    }

    public static CursorLoader createStarredLoader(Context context) {
        return new CursorLoader(context, Contacts.CONTENT_URI, COLUMNS, Contacts.STARRED + "=?",
                new String[]{"1"},  STARRED_ORDER);
    }

    public static CursorLoader createFrequentLoader(Context context) {
        return new CursorLoader(context, Contacts.CONTENT_FREQUENT_URI, COLUMNS,
                 Contacts.STARRED + "=?", new String[]{"0"}, null);
    }
    /*
    * SPRD:
    *       Bug 304379
    * @{
    */
    private static final String SIM_ACCOUNT = "sprd.com.android.account.sim";
    private static final String USIM_ACCOUNT = "sprd.com.android.account.usim";

    public final static int RAW_ACCOUNT_TYPE = 11;

    private static CursorLoader getStrequentLoader(Context context) {
        return new CursorLoader(context, Contacts.CONTENT_STREQUENT_URI, COLUMNS,
                RawContacts.ACCOUNT_TYPE + " != \"" + SIM_ACCOUNT + "\" AND "
                        + RawContacts.ACCOUNT_TYPE + " != \"" + USIM_ACCOUNT + "\"", null,
                STARRED_ORDER);
    }

    private static CursorLoader getStrequentPhoneOnlyLoader(Context context, Uri uri) {
        return new CursorLoader(context, uri, COLUMNS_PHONE_ONLY, RawContacts.ACCOUNT_TYPE
                + " != \"" + SIM_ACCOUNT + "\" AND "
                + RawContacts.ACCOUNT_TYPE + " != \"" + USIM_ACCOUNT + "\"", null, null);
    }
    /*
    * @}
    */
}
