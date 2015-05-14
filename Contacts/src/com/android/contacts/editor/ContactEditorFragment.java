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
 * limitations under the License
 */

package com.android.contacts.editor;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Entity;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import com.android.contacts.common.model.RawContactModifier;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.text.TextUtils;

import com.android.contacts.common.util.Constants;
import com.android.contacts.ContactSaveService;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.activities.ContactEditorAccountsChangedActivity;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.activities.JoinContactActivity;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.account.GoogleAccountType;
import com.android.contacts.common.util.AccountsListAdapter;
import com.android.contacts.common.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.detail.PhotoSelectionHandler;
import com.android.contacts.editor.AggregationSuggestionEngine.Suggestion;
import com.android.contacts.editor.Editor.EditorListener;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.ContactLoader;
import com.android.contacts.common.model.RawContact;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.util.ContactPhotoUtils;
import com.android.contacts.util.HelpUtils;
import com.android.contacts.util.UiClosables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.Long;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import com.android.contacts.common.util.NameConverter;
import com.sprd.contacts.common.util.UniverseUtils;
import com.sprd.contacts.util.AccountRestrictionUtils;
import com.sprd.contacts.common.model.account.SimAccountType;
import com.sprd.contacts.common.model.account.USimAccountType;
import com.sprd.contacts.common.model.account.PhoneAccountType;

public class ContactEditorFragment extends Fragment implements
        SplitContactConfirmationDialogFragment.Listener,
        AggregationSuggestionEngine.Listener, AggregationSuggestionView.Listener,
        RawContactReadOnlyEditorView.Listener {

    private static final String TAG = ContactEditorFragment.class.getSimpleName();

    private static final int LOADER_DATA = 1;
    private static final int LOADER_GROUPS = 2;

    private static final String KEY_URI = "uri";
    private static final String KEY_ACTION = "action";
    private static final String KEY_EDIT_STATE = "state";
    private static final String KEY_RAW_CONTACT_ID_REQUESTING_PHOTO = "photorequester";
    private static final String KEY_VIEW_ID_GENERATOR = "viewidgenerator";
    private static final String KEY_CURRENT_PHOTO_URI = "currentphotouri";
    private static final String KEY_CONTACT_ID_FOR_JOIN = "contactidforjoin";
    private static final String KEY_CONTACT_WRITABLE_FOR_JOIN = "contactwritableforjoin";
    private static final String KEY_SHOW_JOIN_SUGGESTIONS = "showJoinSuggestions";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_STATUS = "status";
    private static final String KEY_NEW_LOCAL_PROFILE = "newLocalProfile";
    private static final String KEY_IS_USER_PROFILE = "isUserProfile";
    private static final String KEY_UPDATED_PHOTOS = "updatedPhotos";
    private static final String KEY_IS_EDIT = "isEdit";
    private static final String KEY_HAS_NEW_CONTACT = "hasNewContact";
    private static final String KEY_NEW_CONTACT_READY = "newContactDataReady";
    private static final String KEY_EXISTING_CONTACT_READY = "existingContactDataReady";
    private static final String KEY_RAW_CONTACTS = "rawContacts";

    public static final String SAVE_MODE_EXTRA_KEY = "saveMode";


    /**
     * An intent extra that forces the editor to add the edited contact
     * to the default group (e.g. "My Contacts").
     */
    public static final String INTENT_EXTRA_ADD_TO_DEFAULT_DIRECTORY = "addToDefaultDirectory";

    public static final String INTENT_EXTRA_NEW_LOCAL_PROFILE = "newLocalProfile";

    /**
     * Modes that specify what the AsyncTask has to perform after saving
     */
    public interface SaveMode {
        /**
         * Close the editor after saving
         */
        public static final int CLOSE = 0;

        /**
         * Reload the data so that the user can continue editing
         */
        public static final int RELOAD = 1;

        /**
         * Split the contact after saving
         */
        public static final int SPLIT = 2;

        /**
         * Join another contact after saving
         */
        public static final int JOIN = 3;

        /**
         * Navigate to Contacts Home activity after saving.
         */
        public static final int HOME = 4;
    }

    private interface Status {
        /**
         * The loader is fetching data
         */
        public static final int LOADING = 0;

        /**
         * Not currently busy. We are waiting for the user to enter data
         */
        public static final int EDITING = 1;

        /**
         * The data is currently being saved. This is used to prevent more
         * auto-saves (they shouldn't overlap)
         */
        public static final int SAVING = 2;

        /**
         * Prevents any more saves. This is used if in the following cases:
         * - After Save/Close
         * - After Revert
         * - After the user has accepted an edit suggestion
         */
        public static final int CLOSING = 3;

        /**
         * Prevents saving while running a child activity.
         */
        public static final int SUB_ACTIVITY = 4;
    }

    private static final int REQUEST_CODE_JOIN = 0;
    private static final int REQUEST_CODE_ACCOUNTS_CHANGED = 1;

    /**
     * The raw contact for which we started "take photo" or "choose photo from gallery" most
     * recently.  Used to restore {@link #mCurrentPhotoHandler} after orientation change.
     */
    private long mRawContactIdRequestingPhoto;
    /**
     * The {@link PhotoHandler} for the photo editor for the {@link #mRawContactIdRequestingPhoto}
     * raw contact.
     *
     * A {@link PhotoHandler} is created for each photo editor in {@link #bindPhotoHandler}, but
     * the only "active" one should get the activity result.  This member represents the active
     * one.
     */
    private PhotoHandler mCurrentPhotoHandler;

    private final EntityDeltaComparator mComparator = new EntityDeltaComparator();

    private Cursor mGroupMetaData;

    private Uri mCurrentPhotoUri;
    private Bundle mUpdatedPhotos = new Bundle();

    private Context mContext;
    private String mAction;
    private Uri mLookupUri;
    private Bundle mIntentExtras;
    private Listener mListener;

    private long mContactIdForJoin;
    private boolean mContactWritableForJoin;

    private ContactEditorUtils mEditorUtils;

    private LinearLayout mContent;
    private RawContactDeltaList mState;

    private ViewIdGenerator mViewIdGenerator;

    private long mLoaderStartTime;

    private int mStatus;

    // Whether to show the new contact blank form and if it's corresponding delta is ready.
    private boolean mHasNewContact = false;
    private boolean mNewContactDataReady = false;

    // Whether it's an edit of existing contact and if it's corresponding delta is ready.
    private boolean mIsEdit = false;
    private boolean mExistingContactDataReady = false;

    // This is used to pre-populate the editor with a display name when a user edits a read-only
    // contact.
    private String mDefaultDisplayName;

    // Used to temporarily store existing contact data during a rebind call (i.e. account switch)
    /*
    * SPRD:
    *
    * @orig
    * private ImmutableList<RawContact> mRawContacts;
    *
    * @{
    */
    private static ImmutableList<RawContact> mRawContacts;
    /*
    * @}
    */

    private AggregationSuggestionEngine mAggregationSuggestionEngine;
    private long mAggregationSuggestionsRawContactId;
    private View mAggregationSuggestionView;

    private ListPopupWindow mAggregationSuggestionPopup;

    private static final class AggregationSuggestionAdapter extends BaseAdapter {
        private final Activity mActivity;
        private final boolean mSetNewContact;
        private final AggregationSuggestionView.Listener mListener;
        private final List<Suggestion> mSuggestions;

        public AggregationSuggestionAdapter(Activity activity, boolean setNewContact,
                AggregationSuggestionView.Listener listener, List<Suggestion> suggestions) {
            mActivity = activity;
            mSetNewContact = setNewContact;
            mListener = listener;
            mSuggestions = suggestions;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Suggestion suggestion = (Suggestion) getItem(position);
            LayoutInflater inflater = mActivity.getLayoutInflater();
            AggregationSuggestionView suggestionView =
                    (AggregationSuggestionView) inflater.inflate(
                            R.layout.aggregation_suggestions_item, null);
            suggestionView.setNewContact(mSetNewContact);
            suggestionView.setListener(mListener);
            suggestionView.bindSuggestion(suggestion);
            return suggestionView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return mSuggestions.get(position);
        }

        @Override
        public int getCount() {
            return mSuggestions.size();
        }
    }

    private OnItemClickListener mAggregationSuggestionItemClickListener =
            new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final AggregationSuggestionView suggestionView = (AggregationSuggestionView) view;
            suggestionView.handleItemClickEvent();
            UiClosables.closeQuietly(mAggregationSuggestionPopup);
            mAggregationSuggestionPopup = null;
        }
    };

    private boolean mAutoAddToDefaultGroup;

    private boolean mEnabled = true;
    private boolean mRequestFocus;
    private boolean mNewLocalProfile = false;
    private boolean mIsUserProfile = false;

	private int isPublicFlag;

	private RawContactDelta entity;

    public ContactEditorFragment() {
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;
            if (mContent != null) {
                int count = mContent.getChildCount();
                for (int i = 0; i < count; i++) {
                    mContent.getChildAt(i).setEnabled(enabled);
                }
            }
            setAggregationSuggestionViewEnabled(enabled);
            final Activity activity = getActivity();
            if (activity != null) activity.invalidateOptionsMenu();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mEditorUtils = ContactEditorUtils.getInstance(mContext);
    }

    @Override
    public void onStop() {
        super.onStop();

        UiClosables.closeQuietly(mAggregationSuggestionPopup);

        // If anything was left unsaved, save it now but keep the editor open.
        if (!getActivity().isChangingConfigurations() && mStatus == Status.EDITING) {
            save(SaveMode.RELOAD);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAggregationSuggestionEngine != null) {
            mAggregationSuggestionEngine.quit();
        }
        /**
        * SPRD:
        *   fix bug163111 [WindowLeak]com.android.contacts.activities.ContactEditorActivity
        *
        * Original Android code:
        * 
        * 
        * @{
        */
        if (mPhotoHandler != null) {
            mPhotoHandler.destroy();
            mPhotoHandler = null;
        }
        /**
        * @}
        */
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        final View view = inflater.inflate(R.layout.contact_editor_fragment, container, false);

        mContent = (LinearLayout) view.findViewById(R.id.editors);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        validateAction(mAction);

        if (mState.isEmpty()) {
            // The delta list may not have finished loading before orientation change happens.
            // In this case, there will be a saved state but deltas will be missing.  Reload from
            // database.
            if (Intent.ACTION_EDIT.equals(mAction)) {
                // Either...
                // 1) orientation change but load never finished.
                // or
                // 2) not an orientation change.  data needs to be loaded for first time.
                getLoaderManager().initLoader(LOADER_DATA, null, mDataLoaderListener);
            }
        } else {
            // Orientation change, we already have mState, it was loaded by onCreate
            /**
             * SPRD:
             *   fix Bug 260683
             * ori code
             *          bindEditors();
             * @{
             */
            if (!mReplaceDialogShowing) {
                bindEditors();
            }
             /**
             * @}
             */
        }

        // Handle initial actions only when existing state missing
        if (savedInstanceState == null) {
            if (Intent.ACTION_EDIT.equals(mAction)) {
                mIsEdit = true;
            } else if (Intent.ACTION_INSERT.equals(mAction)) {
                mHasNewContact = true;
                final Account account = mIntentExtras == null ? null :
                        (Account) mIntentExtras.getParcelable(Intents.Insert.ACCOUNT);
                final String dataSet = mIntentExtras == null ? null :
                        mIntentExtras.getString(Intents.Insert.DATA_SET);
                
				 if(mIntentExtras != null){
					 /**
					  * add by xuhong.tian on 11.13 that when (mIntentExtras!=null) from dialpad insert contacts 
					  */
            		 isPublicFlag = mIntentExtras.getInt("public_create", -1);
            		 
            		 if(mIntentExtras.getBoolean("phone_only", false)){
            			 isPublicFlag = 0;
            		 }
            	  }

				 Log.d(TAG, "isPublicFlag=="+isPublicFlag+"account=="+account+"mIntentExtras=="+mIntentExtras);
                if (account != null) {
                    // Account specified in Intent
                    createContact(new AccountWithDataSet(account.name, account.type, dataSet));
                } else {
                    // No Account specified. Let the user choose
                    // Load Accounts async so that we can present them
                    selectAccountAndCreateContact(isPublicFlag);
                }
            }
        }
    }

    /**
     * Checks if the requested action is valid.
     *
     * @param action The action to test.
     * @throws IllegalArgumentException when the action is invalid.
     */
    private void validateAction(String action) {
        if (Intent.ACTION_EDIT.equals(action) || Intent.ACTION_INSERT.equals(action) ||
                ContactEditorActivity.ACTION_SAVE_COMPLETED.equals(action)) {
            return;
        }
        throw new IllegalArgumentException("Unknown Action String " + mAction +
                ". Only support " + Intent.ACTION_EDIT + " or " + Intent.ACTION_INSERT + " or " +
                ContactEditorActivity.ACTION_SAVE_COMPLETED);
    }

    @Override
    public void onStart() {
        /**
        * SPRD:
        *   fix bug168982 please remove the group settings when editting the user profile
        *
        * Original Android code:
        * 
        * @{
        */
        if (!isEditingUserProfile()) {
        /**
        * @}
        */
            getLoaderManager().initLoader(LOADER_GROUPS, null, mGroupLoaderListener);
        }
        super.onStart();
    }

    public void load(String action, Uri lookupUri, Bundle intentExtras) {
        mAction = action;
        mLookupUri = lookupUri;
        mIntentExtras = intentExtras;

        /**
        * SPRD:
        * 
        * @{
        */
        if(mIntentExtras != null && mIntentExtras.containsKey(ContactsContract.Intents.Insert.PHONE)){
            String website = null;
            if(mIntentExtras.getString(ContactsContract.Intents.Insert.PHONE) != null){
            website = mIntentExtras.getString(ContactsContract.Intents.Insert.PHONE);
            }
            if(!TextUtils.isEmpty(website) && website.contains("//")){
            mIntentExtras.remove(ContactsContract.Intents.Insert.PHONE);
            mIntentExtras.putString("website", website);
            }
        }
        /**
        * @}
        */

        mAutoAddToDefaultGroup = mIntentExtras != null
                && mIntentExtras.containsKey(INTENT_EXTRA_ADD_TO_DEFAULT_DIRECTORY);
        mNewLocalProfile = mIntentExtras != null
                && mIntentExtras.getBoolean(INTENT_EXTRA_NEW_LOCAL_PROFILE);

        /**
        * SPRD:
        *   fix bug168982 please remove the group settings when editting the user profile
        *
        * Original Android code:
        * 
        * @{
        */
        if (mLookupUri != null) {
            List<String> segments = mLookupUri.getPathSegments();
            mIsUserProfile = segments != null ? segments.contains(PROFILE_SEGMENT) : false;
        }
        /**
        * @}
        */
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    @Override
    public void onCreate(Bundle savedState) {
        if (savedState != null) {
            // Restore mUri before calling super.onCreate so that onInitializeLoaders
            // would already have a uri and an action to work with
            mLookupUri = savedState.getParcelable(KEY_URI);
            mAction = savedState.getString(KEY_ACTION);
        }

        super.onCreate(savedState);

        if (savedState == null) {
            // If savedState is non-null, onRestoreInstanceState() will restore the generator.
            mViewIdGenerator = new ViewIdGenerator();
        } else {
            // Read state from savedState. No loading involved here
            mState = savedState.<RawContactDeltaList> getParcelable(KEY_EDIT_STATE);
            mRawContactIdRequestingPhoto = savedState.getLong(
                    KEY_RAW_CONTACT_ID_REQUESTING_PHOTO);
            mViewIdGenerator = savedState.getParcelable(KEY_VIEW_ID_GENERATOR);
            mCurrentPhotoUri = savedState.getParcelable(KEY_CURRENT_PHOTO_URI);
            mContactIdForJoin = savedState.getLong(KEY_CONTACT_ID_FOR_JOIN);
            mContactWritableForJoin = savedState.getBoolean(KEY_CONTACT_WRITABLE_FOR_JOIN);
            mAggregationSuggestionsRawContactId = savedState.getLong(KEY_SHOW_JOIN_SUGGESTIONS);
            mEnabled = savedState.getBoolean(KEY_ENABLED);
            mStatus = savedState.getInt(KEY_STATUS);
            mNewLocalProfile = savedState.getBoolean(KEY_NEW_LOCAL_PROFILE);
            mIsUserProfile = savedState.getBoolean(KEY_IS_USER_PROFILE);
            mUpdatedPhotos = savedState.getParcelable(KEY_UPDATED_PHOTOS);
            mIsEdit = savedState.getBoolean(KEY_IS_EDIT);
            mHasNewContact = savedState.getBoolean(KEY_HAS_NEW_CONTACT);
            mNewContactDataReady = savedState.getBoolean(KEY_NEW_CONTACT_READY);
            mExistingContactDataReady = savedState.getBoolean(KEY_EXISTING_CONTACT_READY);
            mRawContacts = ImmutableList.copyOf(savedState.<RawContact>getParcelableArrayList(
                    KEY_RAW_CONTACTS));
            /**
            * SPRD:
            *   fix bug174143 Access to the information screen, click Picture to add a number to an existing contact
            *
            * Original Android code:
            * 
            * 
            * @{
            */
            mIntentExtras = savedState.getBundle(KEY_INTENT_EXTRAS);
            mPhoneticNameAdded = savedState.getBoolean(KEY_PHONETIC_NAME_ADDED, false);
            /**
            * @}
            */

            /**
             * SPRD:
             *   fix Bug 260683
             * @{
             */
            mReplaceDialogShowing = savedState.getBoolean(KEY_DIALOG_SHOWING);
             /**
             * @}
             */
        }

        // mState can still be null because it may not have have finished loading before
        // onSaveInstanceState was called.
        if (mState == null) {
            mState = new RawContactDeltaList();
        }
    }

    public void setData(Contact contact) {

        // If we have already loaded data, we do not want to change it here to not confuse the user
        if (!mState.isEmpty()) {
            Log.v(TAG, "Ignoring background change. This will have to be rebased later");
            return;
        }

        // See if this edit operation needs to be redirected to a custom editor
        mRawContacts = contact.getRawContacts();
        if (mRawContacts.size() == 1) {
            RawContact rawContact = mRawContacts.get(0);
            String type = rawContact.getAccountTypeString();
            String dataSet = rawContact.getDataSet();
            AccountType accountType = rawContact.getAccountType(mContext);
            if (accountType.getEditContactActivityClassName() != null &&
                    !accountType.areContactsWritable()) {
                if (mListener != null) {
                    String name = rawContact.getAccountName();
                    long rawContactId = rawContact.getId();
                    mListener.onCustomEditContactActivityRequested(
                            new AccountWithDataSet(name, type, dataSet),
                            ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                            mIntentExtras, true);
                }
                return;
            }
        }

        String displayName = null;
        // Check for writable raw contacts.  If there are none, then we need to create one so user
        // can edit.  For the user profile case, there is already an editable contact.
        if (!contact.isUserProfile() && !contact.isWritableContact(mContext)) {
            mHasNewContact = true;

            // This is potentially an asynchronous call and will add deltas to list.
            selectAccountAndCreateContact(-1);
            displayName = contact.getDisplayName();
        }

        /**
        * SPRD:
        *   fix bug174143 Access to the information screen, click Picture to add a number to an existing contact
        *
        * Original Android code:
        * // This also adds deltas to list
        * // If displayName is null at this point it is simply ignored later on by the editor.
        *    bindEditorsForExistingContact(displayName, contact.isUserProfile(),
        *            mRawContacts);
        * 
        * @{
        */
        int replaceDialogId = showReplaceDialog(contact);
        // For user profile, change the contacts query URI
        mIsUserProfile = contact.isUserProfile();
        if (replaceDialogId == 0) {
            // This also adds deltas to list
            // If displayName is null at this point it is simply ignored later on by the editor.
            bindEditorsForExistingContact(displayName, contact.isUserProfile(),
                    mRawContacts);
        } else if ((replaceDialogId == ID_CONFIRM_REPLACE_DIALOG ||
                replaceDialogId == ID_CONFIRM_REPLACE_DIALOG_LIST) && !mReplaceDialogShowing) {
            Message message = mHandler.obtainMessage();
            message.what = replaceDialogId;
            mHandler.sendMessage(message);
        }
        /**
        * @}
        */
    }

    @Override
    public void onExternalEditorRequest(AccountWithDataSet account, Uri uri) {
        mListener.onCustomEditContactActivityRequested(account, uri, null, false);
    }

    private void bindEditorsForExistingContact(String displayName, boolean isUserProfile,
            ImmutableList<RawContact> rawContacts) {
        setEnabled(true);
        /**
         * SPRD:
         *   fix Bug 260683
         * @{
         */
        mReplaceDialogShowing = false;
         /**
         * @}
         */
        mDefaultDisplayName = displayName;

        mState.addAll(rawContacts.iterator());
        setIntentExtras(mIntentExtras);
        mIntentExtras = null;

        // For user profile, change the contacts query URI
        mIsUserProfile = isUserProfile;
        boolean localProfileExists = false;

        if (mIsUserProfile) {
            for (RawContactDelta state : mState) {
                // For profile contacts, we need a different query URI
                state.setProfileQueryUri();
                // Try to find a local profile contact
                /**
                * SPRD:
                *   fix bug158942 edit my profile info, there are two item of my local info and my phone info
                *
                * Original Android code:
                * if (state.getValues().getAsString(RawContacts.ACCOUNT_TYPE) == null) {
                    localProfileExists = true;
                * }
                * 
                * @{
                */
                if (PhoneAccountType.ACCOUNT_TYPE.equals(state.getValues().getAsString(RawContacts.ACCOUNT_TYPE))) {
                    localProfileExists = true;
                }
                /**
                * @}
                */
            }
            // Editor should always present a local profile for editing
            if (!localProfileExists) {
                final RawContact rawContact = new RawContact();
                /**
                * SPRD:
                *   fix bug158942 edit my profile info, there are two item of my local info and my phone info
                *
                * Original Android code:
                * rawContact.setAccountToLocal();
                * 
                * @{
                */
                rawContact.getValues().put(RawContacts.ACCOUNT_TYPE, AccountTypeManager.getInstance(mContext).getPhoneAccount().type);
                rawContact.getValues().put(RawContacts.ACCOUNT_NAME, AccountTypeManager.getInstance(mContext).getPhoneAccount().name);
                rawContact.getValues().putNull(RawContacts.DATA_SET);
                /**
                * @}
                */

                RawContactDelta insert = new RawContactDelta(ValuesDelta.fromAfter(
                        rawContact.getValues()));
                insert.setProfileQueryUri();
                mState.add(insert);
            }
        }
        mRequestFocus = true;
        mExistingContactDataReady = true;
        bindEditors();
    }

    /**
     * Merges extras from the intent.
     */
    public void setIntentExtras(Bundle extras) {
        if (extras == null || extras.size() == 0) {
            return;
        }

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        for (RawContactDelta state : mState) {
            final AccountType type = state.getAccountType(accountTypes);
            /**
             * SPRD: 
             *   fix bug126251 add phone number to sim contact,can not replace old number,and porting bug98944 
             * Original Android code: 
             *   if(type.areContactsWritable()) {
             *    // Apply extras to the first writable raw contact only
             *    RawContactModifier.parseExtras(mContext, type, state, extras);
             * 
             * @{
             */
            final String accountType = state.getAccountType();
            final String accountName = state.getAccountName();
            if (accountName == null && type.areContactsWritable()) {
                RawContactModifier.parseExtras(mContext, type, state, extras);
            } else {
                AccountWithDataSet account = new AccountWithDataSet(accountName, accountType, null);
                String mimeType = null;
                String newData = null;
                if (type.areContactsWritable()) {
                    final ContentValues values = new ContentValues();
                    RawContactDelta insert = new RawContactDelta(ValuesDelta.fromAfter(values));
                    RawContactModifier.parseExtras(mContext, type, insert, extras);
                    ArrayList<ContentValues> contentValues = insert.getContentValues();
                    for (ContentValues insertValues : contentValues) {
                        String tmpMimeType = insertValues.getAsString(Data.MIMETYPE);
                        if (!CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE.equals(tmpMimeType)) {
                            mimeType = tmpMimeType;
                            newData = insertValues.getAsString(Data.DATA1);
                        } else {
                            mimeType = null;
                            newData = null;
                        }
                        if (mimeType != null && newData != null) {
                            final ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
                            if (entries != null
                                    && !GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                                ArrayList<String> oldDataList = new ArrayList<String>();
                                for (ValuesDelta entry : entries) {
                                    String oldData = entry.getAsString(Data.DATA1);
                                    oldDataList.add(oldData);
                                }
                                int typeOverallMax = -1;
                                if (!SimAccountType.ACCOUNT_TYPE.equals(type.accountType)
                                        && !USimAccountType.ACCOUNT_TYPE.equals(type.accountType)) {
                                    typeOverallMax = AccountTypeManager
                                            .getTypeOverallMaxForAccount(type, mimeType);
                                } else {
                                    int max = AccountRestrictionUtils.get(mContext)
                                            .getTypeOverallMax(account, mimeType);
                                    if (max != 0) {
                                        typeOverallMax = max;
                                    }
                                }
                                if (oldDataList.size() == typeOverallMax) {
                                    if (typeOverallMax == 1) {
                                        for (ValuesDelta entry : entries) {
                                            entry.put(Data.DATA1, newData);
                                        }
                                    } else {
                                        Long replaceDataId = extras.getLong("replaceDataId", -1);
                                        if (replaceDataId != -1) {
                                            for (ValuesDelta entry : entries) {
                                                if (replaceDataId.equals(entry.getAsLong(Data._ID))) {
                                                    entry.put(Data.DATA1, newData);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    RawContactModifier.parseExtras(mContext, type, state, extras);
                                }
                            } else {
                                RawContactModifier.parseExtras(mContext, type, state, extras);
                            }
                        }
                    }
                    /**
                     * @}
                     */
                    break;
                }
            }
        }
    }

    private void selectAccountAndCreateContact(int mode) {
        // If this is a local profile, then skip the logic about showing the accounts changed
        // activity and create a phone-local contact.
        if (mNewLocalProfile) {
            /**
            * SPRD:
            *   fix bug152056 Replace null account and assign a phone account for profile contact.
            *
            * Original Android code:
            *   createContact(null);
            * 
            * @{
            */
            createContact(AccountTypeManager.getInstance(mContext).getPhoneAccount());
            /**
            * @}
            */
            return;
        }
        /**
        * SPRD:
        *   fix bug98944 support multiple accounts filter and account mime-type capability detection for ContactEditorActivity
        *
        * Original Android code:
        * // If there is no default account or the accounts have changed such that we need to
        * // prompt the user again, then launch the account prompt.
        * if (mEditorUtils.shouldShowAccountChangedNotification()) {
        *     Intent intent = new Intent(mContext, ContactEditorAccountsChangedActivity.class);
        *     mStatus = Status.SUB_ACTIVITY;
        *     startActivityForResult(intent, REQUEST_CODE_ACCOUNTS_CHANGED);
        * } else {
        *    // Otherwise, there should be a default account. Then either create a local contact
        *    // (if default account is null) or create a contact with the specified account.
        *    AccountWithDataSet defaultAccount = mEditorUtils.getDefaultAccount();
        *    if (defaultAccount == null) {
        *        createContact(null);
        *    } else {
        *        createContact(defaultAccount);
        *    }
        * }
        * 
        * @{
        */
        Intent intent = new Intent(mContext, ContactEditorAccountsChangedActivity.class);
    	intent.putExtra("public_create", mode);
        Log.d(TAG, "selectAccountAndCreateContact : mIntent = " + mIntentExtras);
        if (mIntentExtras != null && mIntentExtras.containsKey(Constants.INTENT_KEY_ACCOUNTS)) {
            intent.putParcelableArrayListExtra(Constants.INTENT_KEY_ACCOUNTS,
                    mIntentExtras.getParcelableArrayList(Constants.INTENT_KEY_ACCOUNTS));
            Log.d(TAG,
                    "selectAccountAndCreateContact : " + " account = "
                            + mIntentExtras.getParcelableArrayList(Constants.INTENT_KEY_ACCOUNTS));
        }
        mStatus = Status.SUB_ACTIVITY;
        startActivityForResult(intent, REQUEST_CODE_ACCOUNTS_CHANGED);
        /**
        * @}
        */
    }

    /**
     * Create a contact by automatically selecting the first account. If there's no available
     * account, a device-local contact should be created.
     */
    private void createContact() {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(mContext).getAccounts(true);
        // No Accounts available. Create a phone-local contact.
        if (accounts.isEmpty()) {
            createContact(null);
            return;
        }

        // We have an account switcher in "create-account" screen, so don't need to ask a user to
        // select an account here.
        createContact(accounts.get(0));
    }

    /**
     * Shows account creation screen associated with a given account.
     *
     * @param account may be null to signal a device-local contact should be created.
     */
    private void createContact(AccountWithDataSet account) {
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        final AccountType accountType =
                accountTypes.getAccountType(account != null ? account.type : null,
                        account != null ? account.dataSet : null);

        if (accountType.getCreateContactActivityClassName() != null) {
            if (mListener != null) {
                mListener.onCustomCreateContactActivityRequested(account, mIntentExtras);
            }
        } else {
            bindEditorsForNewContact(account, accountType);
        }
    }

    /**
     * Removes a current editor ({@link #mState}) and rebinds new editor for a new account.
     * Some of old data are reused with new restriction enforced by the new account.
     *
     * @param oldState Old data being edited.
     * @param oldAccount Old account associated with oldState.
     * @param newAccount New account to be used.
     */
    private void rebindEditorsForNewContact(
            RawContactDelta oldState, AccountWithDataSet oldAccount,
            AccountWithDataSet newAccount) {
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        AccountType oldAccountType = accountTypes.getAccountType(
                oldAccount.type, oldAccount.dataSet);
        AccountType newAccountType = accountTypes.getAccountType(
                newAccount.type, newAccount.dataSet);

        if (newAccountType.getCreateContactActivityClassName() != null) {
            Log.w(TAG, "external activity called in rebind situation");
            if (mListener != null) {
                mListener.onCustomCreateContactActivityRequested(newAccount, mIntentExtras);
            }
        } else {
            mExistingContactDataReady = false;
            mNewContactDataReady = false;
            mState = new RawContactDeltaList();
            bindEditorsForNewContact(newAccount, newAccountType, oldState, oldAccountType);
            /**
             * SPRD:
             *   fix Bug 260683
             * Ori code
             *      if (mIsEdit) {
             *          bindEditorsForExistingContact(mDefaultDisplayName, mIsUserProfile, mRawContacts);
             *      }
             * @{
             */
            if (mIsEdit && !mReplaceDialogShowing) {
                bindEditorsForExistingContact(mDefaultDisplayName, mIsUserProfile, mRawContacts);
            }
             /**
             * @}
             */
        }
    }

    private void bindEditorsForNewContact(AccountWithDataSet account,
            final AccountType accountType) {
        bindEditorsForNewContact(account, accountType, null, null);
    }

    private void bindEditorsForNewContact(AccountWithDataSet newAccount,
            final AccountType newAccountType, RawContactDelta oldState,
            AccountType oldAccountType) {
        mStatus = Status.EDITING;

        final RawContact rawContact = new RawContact();
        if (newAccount != null) {
            rawContact.setAccount(newAccount);
        } else {
            rawContact.setAccountToLocal();
        }

        final ValuesDelta valuesDelta = ValuesDelta.fromAfter(rawContact.getValues());
        final RawContactDelta insert = new RawContactDelta(valuesDelta);
        if (oldState == null) {
            // Parse any values from incoming intent
            RawContactModifier.parseExtras(mContext, newAccountType, insert, mIntentExtras);
        } else {
            RawContactModifier.migrateStateForNewContact(mContext, oldState, insert,
                    oldAccountType, newAccountType);
        }

        // Ensure we have some default fields (if the account type does not support a field,
        // ensureKind will not add it, so it is safe to add e.g. Event)
        RawContactModifier.ensureKindExists(insert, newAccountType, Phone.CONTENT_ITEM_TYPE);
        RawContactModifier.ensureKindExists(insert, newAccountType, Email.CONTENT_ITEM_TYPE);
        RawContactModifier.ensureKindExists(insert, newAccountType, Organization.CONTENT_ITEM_TYPE);
        RawContactModifier.ensureKindExists(insert, newAccountType, Event.CONTENT_ITEM_TYPE);
        /**
        * SPRD:
        *   Bug 265008 New Contact interface to remove a few unrelated elements
        *   can not have a scroll bar appears.
        *
        * Original Android code:
                RawContactModifier.ensureKindExists(insert, newAccountType,
                        StructuredPostal.CONTENT_ITEM_TYPE);
        *
        * @{
        */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            RawContactModifier.ensureKindExists(insert, newAccountType,
                    StructuredPostal.CONTENT_ITEM_TYPE);
        }
        /**
        * @}
        */

        // Set the correct URI for saving the contact as a profile
        if (mNewLocalProfile) {
            insert.setProfileQueryUri();
        }

        mState.add(insert);

        mRequestFocus = true;

        mNewContactDataReady = true;
        bindEditors();
    }

    /*
    * SPRD:
    *  fix bug 270858 when set the photo,the StructName is show abnormal.
    *  @orig
    *  private void bindEditors(){
    * @{
    */
    private void bindEditors(){
        bindEditors(false);
    }
    private void bindEditors(boolean isUpdatePhoto) {
        /*
         * @}
         */
        
        // bindEditors() can only bind views if there is data in mState, so immediately return
        // if mState is null
        if (mState.isEmpty()) {
            return;
        }

        // Check if delta list is ready.  Delta list is populated from existing data and when
        // editing an read-only contact, it's also populated with newly created data for the
        // blank form.  When the data is not ready, skip. This method will be called multiple times.
        if ((mIsEdit && !mExistingContactDataReady) || (mHasNewContact && !mNewContactDataReady)) {
            return;
        }

        // Sort the editors
        Collections.sort(mState, mComparator);

        // Remove any existing editors and rebuild any visible
        mContent.removeAllViews();

        final LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        int numRawContacts = mState.size();
        for (int i = 0; i < numRawContacts; i++) {
            // TODO ensure proper ordering of entities in the list
            final RawContactDelta rawContactDelta = mState.get(i);
            if (!rawContactDelta.isVisible()) continue;
            entity = mState.get(i);
            final String accountType = rawContactDelta.getAccountType();
            final String accountName = rawContactDelta.getAccountName();
            final Account account = (accountType != null && accountName != null)? 
                    new Account(accountName, accountType): null;  
            final AccountType type = rawContactDelta.getAccountType(accountTypes);
            final long rawContactId = rawContactDelta.getRawContactId();

            final BaseRawContactEditorView editor;
            if (!type.areContactsWritable()) {
                editor = (BaseRawContactEditorView) inflater.inflate(
                        R.layout.raw_contact_readonly_editor_view, mContent, false);
                ((RawContactReadOnlyEditorView) editor).setListener(this);
            } else {
                /**
                * SPRD:
                *   for UUI
                *
                * Original Android code:
                * editor = (RawContactEditorView) inflater.inflate(R.layout.raw_contact_editor_view,
                *       mContent, false);
                * 
                * @{
                */
                if (UniverseUtils.UNIVERSEUI_SUPPORT){
                    editor = (RawContactEditorView) inflater.inflate(R.layout.raw_contact_editor_view_overlay,
                            mContent, false);
                }else{
                    editor = (RawContactEditorView) inflater.inflate(R.layout.raw_contact_editor_view,
                            mContent, false);
                }
                ((RawContactEditorView) editor).setIfPhoneticNameAdded(mPhoneticNameAdded);
            }
            mRawContactEditor = editor;
            // verify bindEditors EntityDelta
            String strValue = null;
            int maxLen = -1, maxLength = -1;
            for (String mimetype : rawContactDelta.getMimeTypes()) {
                maxLen = accountTypes.getAccountTypeFieldsMaxLength(mContext,
                        account, mimetype);             
                if (mimetype == null || maxLen <= 0) {
                    continue;
                }
                for (ValuesDelta child : rawContactDelta.getMimeEntries(mimetype)) {
                    if (child == null) {
                        continue;
                    }
                    for (String cKey : child.keySet()) {
                        strValue = child.getAsString(cKey);
                        maxLength = accountTypes.getTextFieldsEditorMaxLength(mContext, account, strValue, maxLen);
                        if (cKey == null || strValue == null ||maxLength <= 0) {
                            continue;
                        }
                        if (!cKey.equals("_id") && !strValue.equals(mimetype)
                                && (strValue.length() > maxLength)) {
                            if (Phone.CONTENT_ITEM_TYPE.equals(mimetype) &&
                                    (SimAccountType.ACCOUNT_TYPE.equals(accountType) ||
                                    USimAccountType.ACCOUNT_TYPE.equals(accountType))) {
                                if (strValue.length() > 0 && strValue.charAt(0) == '+') {
                                    maxLength = maxLength + 1;
                                }
                            }
                            child.put(cKey, strValue.substring(0, maxLength));
                        }
                    }
                }
            }
            
        if (!UniverseUtils.UNIVERSEUI_SUPPORT){
                /**
                * @}
                */
            if (mHasNewContact && !mNewLocalProfile) {
                final List<AccountWithDataSet> accounts =
                        AccountTypeManager.getInstance(mContext).getAccounts(true);
                if (accounts.size() > 1) {
                    addAccountSwitcher(mState.get(0), editor);
                } else {
                    disableAccountSwitcher(editor);
                }
            } else {
                disableAccountSwitcher(editor);
            }
        }
		if (Intent.ACTION_INSERT.equals(mAction) && numRawContacts == 1) {
			final List<AccountWithDataSet> accounts = AccountTypeManager
					.getInstance(mContext).getAccounts(true);
			if (isPublicFlag == 1 && accounts.size() > 0
					&& !mNewLocalProfile) {
				disableAccountSwitcher(editor);
			} else {
				if (accounts.size() > 0 && !mNewLocalProfile) {
					addAccountSwitcher(mState.get(0), editor);
				} else {
					disableAccountSwitcher(editor);
				}
			}
		} else {
            disableAccountSwitcher(editor);
        }
        
            editor.setEnabled(mEnabled);

            mContent.addView(editor);

            /*
             * SPRD:
             *  fix bug 270858 when set the photo,the StructName is show abnormal.
             *  @orig
             *  editor.setState(rawContactDelta, type, mViewIdGenerator, isEditingUserProfile());
             * @{
             */
            if (isUpdatePhoto) {
                editor.setStateforPhoto(rawContactDelta, type, mViewIdGenerator, isEditingUserProfile(),
                        isUpdatePhoto);
            } else {
                editor.setState(rawContactDelta, type, mViewIdGenerator, isEditingUserProfile());
            }
            /*
             * @}
             */

            // Set up the photo handler.
            bindPhotoHandler(editor, type, mState);

            // If a new photo was chosen but not yet saved, we need to
            // update the thumbnail to reflect this.
            Bitmap bitmap = updatedBitmapForRawContact(rawContactId);
            if (bitmap != null) editor.setPhotoBitmap(bitmap);

            if (editor instanceof RawContactEditorView) {
                final Activity activity = getActivity();
                final RawContactEditorView rawContactEditor = (RawContactEditorView) editor;
                EditorListener listener = new EditorListener() {

                    @Override
                    public void onRequest(int request) {
                        if (activity.isFinishing()) { // Make sure activity is still running.
                            return;
                        }
                        if (request == EditorListener.FIELD_CHANGED && !isEditingUserProfile()) {
                            acquireAggregationSuggestions(activity, rawContactEditor);
                        }
                    }

                    @Override
                    public void onDeleteRequested(Editor removedEditor) {
                    }
                };

                final StructuredNameEditorView nameEditor = rawContactEditor.getNameEditor();
                if (mRequestFocus) {
                    nameEditor.requestFocus();
                    mRequestFocus = false;
                }
                nameEditor.setEditorListener(listener);
                if (!TextUtils.isEmpty(mDefaultDisplayName)) {
                    nameEditor.setDisplayName(mDefaultDisplayName);
                }

                final TextFieldsEditorView phoneticNameEditor =
                        rawContactEditor.getPhoneticNameEditor();
                phoneticNameEditor.setEditorListener(listener);
                rawContactEditor.setAutoAddToDefaultGroup(mAutoAddToDefaultGroup);

                if (rawContactId == mAggregationSuggestionsRawContactId) {
                    acquireAggregationSuggestions(activity, rawContactEditor);
                }
            }
        }
        mRequestFocus = false;
        /**
        * SPRD:
        *   fix bug168982 please remove the group settings when editting the user profile
        *
        * Original Android code:
        * 
        * 
        * @{
        */
        if (!isEditingUserProfile()) {
        /**
        * @}
        */
            bindGroupMetaData();
        }

        // Show editor now that we've loaded state
        mContent.setVisibility(View.VISIBLE);

        // Refresh Action Bar as the visibility of the join command
        // Activity can be null if we have been detached from the Activity
        final Activity activity = getActivity();
        if (activity != null) activity.invalidateOptionsMenu();
    }

    /**
     * If we've stashed a temporary file containing a contact's new photo,
     * decode it and return the bitmap.
     * @param rawContactId identifies the raw-contact whose Bitmap we'll try to return.
     * @return Bitmap of photo for specified raw-contact, or null
    */
    private Bitmap updatedBitmapForRawContact(long rawContactId) {
        String path = mUpdatedPhotos.getString(String.valueOf(rawContactId));
        return BitmapFactory.decodeFile(path);
    }

    private void bindPhotoHandler(BaseRawContactEditorView editor, AccountType type,
            RawContactDeltaList state) {
        final int mode;
        if (type.areContactsWritable()) {
            if (editor.hasSetPhoto()) {
                if (hasMoreThanOnePhoto()) {
                    mode = PhotoActionPopup.Modes.PHOTO_ALLOW_PRIMARY;
                } else {
                    mode = PhotoActionPopup.Modes.PHOTO_DISALLOW_PRIMARY;
                }
            } else {
                mode = PhotoActionPopup.Modes.NO_PHOTO;
            }
        } else {
            if (editor.hasSetPhoto() && hasMoreThanOnePhoto()) {
                mode = PhotoActionPopup.Modes.READ_ONLY_ALLOW_PRIMARY;
            } else {
                // Read-only and either no photo or the only photo ==> no options
                editor.getPhotoEditor().setEditorListener(null);
                return;
            }
        }
        /**
        * SPRD:
        *   fix bug163111 [WindowLeak]com.android.contacts.activities.ContactEditorActivity
        *
        * Original Android code:
        * final PhotoHandler photoHandler = new PhotoHandler(mContext, editor, mode, state);
        * editor.getPhotoEditor().setEditorListener(
        *       (PhotoHandler.PhotoEditorListener) photoHandler.getListener());
        * // Note a newly created raw contact gets some random negative ID, so any value is valid
        * // here. (i.e. don't check against -1 or anything.)
        * if (mRawContactIdRequestingPhoto == editor.getRawContactId()) {
        *     mCurrentPhotoHandler = photoHandler;
        * }
        * 
        * @{
        */
        mPhotoHandler = new PhotoHandler(mContext, editor, mode, state);
        editor.getPhotoEditor().setEditorListener(
                (PhotoHandler.PhotoEditorListener) mPhotoHandler.getListener());

        // Note a newly created raw contact gets some random negative ID, so any value is valid
        // here. (i.e. don't check against -1 or anything.)
        if (mRawContactIdRequestingPhoto == editor.getRawContactId()) {
            mCurrentPhotoHandler = mPhotoHandler;
        }
        /**
        * @}
        */
    }

    private void bindGroupMetaData() {
        if (mGroupMetaData == null) {
            return;
        }

        int editorCount = mContent.getChildCount();
        for (int i = 0; i < editorCount; i++) {
            BaseRawContactEditorView editor = (BaseRawContactEditorView) mContent.getChildAt(i);
            editor.setGroupMetaData(mGroupMetaData);
        }
    }

    private void saveDefaultAccountIfNecessary() {
        // Verify that this is a newly created contact, that the contact is composed of only
        // 1 raw contact, and that the contact is not a user profile.
        if (!Intent.ACTION_INSERT.equals(mAction) && mState.size() == 1 &&
                !isEditingUserProfile()) {
            return;
        }

        // Find the associated account for this contact (retrieve it here because there are
        // multiple paths to creating a contact and this ensures we always have the correct
        // account).
        final RawContactDelta rawContactDelta = mState.get(0);
        String name = rawContactDelta.getAccountName();
        String type = rawContactDelta.getAccountType();
        String dataSet = rawContactDelta.getDataSet();

        AccountWithDataSet account = (name == null || type == null) ? null :
                new AccountWithDataSet(name, type, dataSet);
        mEditorUtils.saveDefaultAndAllAccounts(account);
    }

    private void addAccountSwitcher(
            final RawContactDelta currentState, BaseRawContactEditorView editor) {
        final AccountWithDataSet currentAccount = new AccountWithDataSet(
                currentState.getAccountName(),
                currentState.getAccountType(),
                currentState.getDataSet());
        final View accountView = editor.findViewById(R.id.account);
        final View anchorView = editor.findViewById(R.id.account_container);
        accountView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ListPopupWindow popup = new ListPopupWindow(mContext, null);
                final AccountsListAdapter adapter =
                        new AccountsListAdapter(mContext,
                        AccountListFilter.ACCOUNTS_CONTACT_WRITABLE, currentAccount);
                popup.setWidth(anchorView.getWidth());
                popup.setAnchorView(anchorView);
                popup.setAdapter(adapter);
                popup.setModal(true);
                popup.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
                popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
                        UiClosables.closeQuietly(popup);
                        AccountWithDataSet newAccount = adapter.getItem(position);
                        if (!newAccount.equals(currentAccount)) {
                            rebindEditorsForNewContact(currentState, currentAccount, newAccount);
                        }
                    }
                });
                popup.show();
            }
        });
    }

    private void disableAccountSwitcher(BaseRawContactEditorView editor) {
        // Remove the pressed state from the account header because the user cannot switch accounts
        // on an existing contact
        final View accountView = editor.findViewById(R.id.account);
        accountView.setBackground(null);
        accountView.setEnabled(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.edit_contact, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // This supports the keyboard shortcut to save changes to a contact but shouldn't be visible
        // because the custom action bar contains the "save" button now (not the overflow menu).
        // TODO: Find a better way to handle shortcuts, i.e. onKeyDown()?
        final MenuItem doneMenu = menu.findItem(R.id.menu_done);
        final MenuItem splitMenu = menu.findItem(R.id.menu_split);
        final MenuItem joinMenu = menu.findItem(R.id.menu_join);
        final MenuItem helpMenu = menu.findItem(R.id.menu_help);
        final MenuItem discardMenu = menu.findItem(R.id.menu_discard);

        // Set visibility of menus
        doneMenu.setVisible(false);
        discardMenu.setVisible(!UniverseUtils.UNIVERSEUI_SUPPORT);
        /**
        * SPRD:
        *   sim contact should not be joined or splited
        *
        * Original Android code:
        * // Split only if more than one raw profile and not a user profile
        * splitMenu.setVisible(mState.size() > 1 && !isEditingUserProfile());
        *
        * // Cannot join a user profile
        * joinMenu.setVisible(!isEditingUserProfile());
        * 
        * @{
        */
        // Split only if more than one raw profile and not a user profile
        splitMenu.setVisible(mState != null && mState.size() > 1 && !isEditingUserProfile());

        // Cannot join a user profile
        joinMenu.setVisible(!isEditingUserProfile() && !isSimType());

		if(entity != null){
	        int saveImportFlag =entity.getValues().getAsInteger(Contacts.CONTACT_IMPORT_TYPE, -1);
			
			String accoutNameFromValues = entity.getValues()
					.getAsString(RawContacts.ACCOUNT_NAME);
			if (RawContacts.CONTACTS_BY_EDITE == saveImportFlag
					|| RawContacts.CONTACTS_BY_PCTOOLS == saveImportFlag
					|| mContext.getString(R.string.show_privatecontacts)
							.equals(accoutNameFromValues)) {

		        menu.findItem(R.id.menu_join).setVisible(false);
		        menu.findItem(R.id.menu_split).setVisible(false);
			}
		}
        
        /**
        * @}
        */
        /**
        * SPRD:
        *   for UUI
        *
        * Original Android code:
        * // Discard menu is only available if at least one raw contact is editable
        * discardMenu.setVisible(mState != null &&
        *         mState.getFirstWritableRawContact(mContext) != null);
        *
        * // help menu depending on whether this is inserting or editing
        * if (Intent.ACTION_INSERT.equals(mAction)) {
        *     // inserting
        *     HelpUtils.prepareHelpMenuItem(mContext, helpMenu, R.string.help_url_people_add);
        * } else if (Intent.ACTION_EDIT.equals(mAction)) {
        *     // editing
        *     HelpUtils.prepareHelpMenuItem(mContext, helpMenu, R.string.help_url_people_edit);
        * } else {
        *     // something else, so don't show the help menu
        *     helpMenu.setVisible(false);
        * }
        * 
        * @{
        */

        /**
        * @}
        */

        int size = menu.size();
        for (int i = 0; i < size; i++) {
            menu.getItem(i).setEnabled(mEnabled);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_done:
                return save(SaveMode.CLOSE);
            case R.id.menu_discard:
                return revert();
            case R.id.menu_split:
                return doSplitContactAction();
            case R.id.menu_join:
                return doJoinContactAction();
        }
        return false;
    }

    private boolean doSplitContactAction() {
        if (!hasValidState()) return false;

        final SplitContactConfirmationDialogFragment dialog =
                new SplitContactConfirmationDialogFragment();
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), SplitContactConfirmationDialogFragment.TAG);
        return true;
    }

    private boolean doJoinContactAction() {
        if (!hasValidState()) {
            return false;
        }

        // If we just started creating a new contact and haven't added any data, it's too
        // early to do a join
        if (mState.size() == 1 && mState.get(0).isContactInsert() && !hasPendingChanges()) {
            Toast.makeText(mContext, R.string.toast_join_with_empty_contact,
                            Toast.LENGTH_LONG).show();
            return true;
        }

        return save(SaveMode.JOIN);
    }

    /**
     * Check if our internal {@link #mState} is valid, usually checked before
     * performing user actions.
     */
    private boolean hasValidState() {
        return mState.size() > 0;
    }

    /**
     * Return true if there are any edits to the current contact which need to
     * be saved.
     */
    private boolean hasPendingChanges() {
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        return RawContactModifier.hasChanges(mState, accountTypes);
    }

    /**
     * Saves or creates the contact based on the mode, and if successful
     * finishes the activity.
     */
    public boolean save(int saveMode) {
        if (!hasValidState() || mStatus != Status.EDITING) {
            //SPRD:258904
            mIsSaveFailure = true;
            //SPRD:258904
            return false;
        }

        // If we are about to close the editor - there is no need to refresh the data
        if (saveMode == SaveMode.CLOSE || saveMode == SaveMode.SPLIT) {
            getLoaderManager().destroyLoader(LOADER_DATA);
        }

        mStatus = Status.SAVING;

        if (!hasPendingChanges()) {
            if (mLookupUri == null && saveMode == SaveMode.RELOAD) {
                // We don't have anything to save and there isn't even an existing contact yet.
                // Nothing to do, simply go back to editing mode
                mStatus = Status.EDITING;
                return true;
            }
            onSaveCompleted(false, saveMode, mLookupUri != null, mLookupUri);
            /**
            * SPRD:
            *   for UUI
            *
            * Original Android code:
            *             return true;
            *         }
            *
            * setEnabled(false);
            * 
            * @{
            */
            mIsSaveFailure = true;
            return true;
        }
        // check whether the saved data complies with account restriction
        String mimeType = AccountRestrictionUtils.get(mContext).violateFieldLengthRestriction(
                mState);

        if (mimeType != null) {
            int resId = AccountRestrictionUtils.get(mContext).mimeToRes(mimeType);
            String text = getString(R.string.field_too_long, getString(resId));
            mIsSaveFailure = true;
            Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
            mStatus = Status.EDITING;
            return true;
        }
        boolean isEmailAddress=AccountRestrictionUtils.get(mContext).violateEmailFormatRestriction(mState);
        if(!isEmailAddress){
            int toastId=R.string.email_format_fail;
            Toast.makeText(mContext, toastId, Toast.LENGTH_LONG).show();
            mStatus=Status.EDITING;
            mIsSaveFailure = true;
            return true;
        }
            /**
            * @}
            */

        // Store account as default account, only if this is a new contact
        saveDefaultAccountIfNecessary();
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
            Log.startPerfTracking(Constants.PERFORMANCE_TAG + ": Start new a contacts ");
        }
         /**
         * @}
         */
        // Save contact
        Intent intent = ContactSaveService.createSaveContactIntent(mContext, mState,
                SAVE_MODE_EXTRA_KEY, saveMode, isEditingUserProfile(),
                ((Activity)mContext).getClass(), ContactEditorActivity.ACTION_SAVE_COMPLETED,
                mUpdatedPhotos);
        mContext.startService(intent);

        // Don't try to save the same photos twice.
        mUpdatedPhotos = new Bundle();

        return true;
    }

    public static class CancelEditDialogFragment extends DialogFragment {

        public static void show(ContactEditorFragment fragment) {
            CancelEditDialogFragment dialog = new CancelEditDialogFragment();
            dialog.setTargetFragment(fragment, 0);
            dialog.show(fragment.getFragmentManager(), "cancelEditor");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.cancel_confirmation_dialog_message)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int whichButton) {
                                ((ContactEditorFragment)getTargetFragment()).doRevertAction();
                            }
                        }
                    )
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            return dialog;
        }
    }
    /**
    * SPRD:
    *   revert editing onbackpressed
    *
    * Original Android code:
    * private boolean revert() {
    * 
    * @{
    */
    public boolean revert() {
    /**
    * @}
    */
        if (mState.isEmpty() || !hasPendingChanges()) {
            doRevertAction();
        } else {
            /*
             * SPRD:
             *      Bug 289310
             *
             * Original Android code:
             * CancelEditDialogFragment.show(this);
             *
             * @{
             */
            if (!getActivity().isFinishing()) {
                CancelEditDialogFragment.show(this);
            }
             /*
             * @}
             */
        }
        return true;
    }

    private void doRevertAction() {
        // When this Fragment is closed we don't want it to auto-save
        mStatus = Status.CLOSING;
        if (mListener != null) mListener.onReverted();
    }

    public void doSaveAction() {
        /**
        * SPRD:
        *   fix bug78117 Can't save new contacts after user unloock the screen
        *
        * Original Android code:
        * 
        * 
        * @{
        */
        mStatus = Status.EDITING;
        /**
        * @}
        */
        save(SaveMode.CLOSE);
    }

    public void onJoinCompleted(Uri uri) {
        onSaveCompleted(false, SaveMode.RELOAD, uri != null, uri);
    }

    /**
    * SPRD:
    *   for UUI
    *
    * Original Android code:
    * public void onSaveCompleted(boolean hadChanges, int saveMode, boolean saveSucceeded,
    *       Uri contactLookupUri) {
    * 
    * @{
    */
    public void onSaveCompleted(boolean hadChanges, int saveMode, boolean saveSucceeded,
            Uri contactLookupUri) {
        onSaveCompleted(hadChanges, saveMode, saveSucceeded, contactLookupUri, 0);
    }
    public void onSaveCompleted(boolean hadChanges, int saveMode, boolean saveSucceeded,
            Uri contactLookupUri, int errorToast) {
        Log.i(TAG, "onSaveCompleted(" + saveMode + ", " + contactLookupUri);
    /**
    * @}
    */
        if (hadChanges) {
            if (saveSucceeded) {
                if (saveMode != SaveMode.JOIN) {
                    /**
                    * SPRD:
                    *   stable sim importing,add phonebook exception process
                    *
                    * Original Android code:
                    *           Toast.makeText(mContext, R.string.contactSavedToast, Toast.LENGTH_SHORT).show();
                    *     }
                    * } else {
                    *     Toast.makeText(mContext, R.string.contactSavedErrorToast, Toast.LENGTH_LONG).show();
                    * }
                    * 
                    * @{
                    */
                        if (contactLookupUri != null) {
                            Toast.makeText(mContext, R.string.contactSavedToast, Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            Toast.makeText(mContext, R.string.contactDeletedToast, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                } else {
                    mIsSaveFailure = true;
                   // mStatus = Status.EDITING;
                    if (errorToast != 0) {
                        Toast.makeText(mContext, errorToast, Toast.LENGTH_LONG).show();
                        if (errorToast == R.string.sim_is_full) {
                            mListener.onSaveFinished(null);
                            return;
                        }
                    }else {
                        Toast.makeText(mContext, R.string.contactSavedErrorToast, Toast.LENGTH_LONG).show();
                    }
                }
                    /**
                    * @}
                    */
        }
        switch (saveMode) {
            case SaveMode.CLOSE:
                /**
                * SPRD:
                *   fix bug179096 Lock screen on ContactEditorFragment, after unlocking back to ContactDetailActivity
                *
                * Original Android code:
                * 
                * 
                * @{
                */
                if(!hadChanges){
                    revert();
                }
                /**
                * @}
                */
            case SaveMode.HOME:
                final Intent resultIntent;
                /**
                * SPRD:
                *   fix a bug cause by buildDiff, which will transform an 'update' action to a 'delete' action when updating the contact and set all data to null
                *
                * Original Android code:
                * if (saveSucceeded && contactLookupUri != null) {
                * 
                * @{
                */
                if (saveSucceeded) {
                    if (contactLookupUri != null) {
                        /**
                         * @}
                         */
                        final String requestAuthority =
                                mLookupUri == null ? null : mLookupUri.getAuthority();

                        final String legacyAuthority = "contacts";

                        resultIntent = new Intent();
                        resultIntent.setAction(Intent.ACTION_VIEW);
                        if (legacyAuthority.equals(requestAuthority)) {
                            // Build legacy Uri when requested by caller
                            final long contactId = ContentUris.parseId(Contacts.lookupContact(
                                    mContext.getContentResolver(), contactLookupUri));
                            final Uri legacyContentUri = Uri.parse("content://contacts/people");
                            final Uri legacyUri = ContentUris.withAppendedId(
                                    legacyContentUri, contactId);
                            resultIntent.setData(legacyUri);
                        } else {
                            // Otherwise pass back a lookup-style Uri
                            resultIntent.setData(contactLookupUri);
                        }

                /**
                * SPRD:
                *   for UUI
                *
                * Original Android code:
                *       } else {
                *     resultIntent = null;
                * }
                * // It is already saved, so prevent that it is saved again
                * mStatus = Status.CLOSING;
                * if (mListener != null) mListener.onSaveFinished(resultIntent);
                * 
                * @{
                */
                    } else {
                        resultIntent = null;
                    }
                    // It is already saved, so prevent that it is saved again
                    mStatus = Status.CLOSING;
                    if (mListener != null)
                        mListener.onSaveFinished(resultIntent);
                } else {
                    mStatus = Status.EDITING;
                    resultIntent = null;
                }
                /**
                * @}
                */
                break;

            case SaveMode.RELOAD:
                /**
                * SPRD:
                *   fix bug180129 there is no any input on the label of custom, lock and unlock the screen, it cant save the label
                *
                * Original Android code:
                * 
                * 
                * @{
                */
                if (mRawContactEditor != null && mRawContactEditor instanceof RawContactEditorView) {
                    RawContactEditorView editor = (RawContactEditorView) mRawContactEditor;
                    LabeledEditorView editorView = editor.getNameEditor();
                    if (editorView != null) {
                        editorView.dismissCustomDialog();
                    }
                }
                /**
                * @}
                */
            case SaveMode.JOIN:
                if (saveSucceeded && contactLookupUri != null) {
                    // If it was a JOIN, we are now ready to bring up the join activity.
                    if (saveMode == SaveMode.JOIN && hasValidState()) {
                        showJoinAggregateActivity(contactLookupUri);
                    }

                    // If this was in INSERT, we are changing into an EDIT now.
                    // If it already was an EDIT, we are changing to the new Uri now
                    mState = new RawContactDeltaList();
                    load(Intent.ACTION_EDIT, contactLookupUri, null);
                    mStatus = Status.LOADING;
                    getLoaderManager().restartLoader(LOADER_DATA, null, mDataLoaderListener);
                }
                break;

            case SaveMode.SPLIT:
                mStatus = Status.CLOSING;
                if (mListener != null) {
                    mListener.onContactSplit(contactLookupUri);
                } else {
                    Log.d(TAG, "No listener registered, can not call onSplitFinished");
                }
                break;
        }
    }

    /**
     * Shows a list of aggregates that can be joined into the currently viewed aggregate.
     *
     * @param contactLookupUri the fresh URI for the currently edited contact (after saving it)
     */
    private void showJoinAggregateActivity(Uri contactLookupUri) {
        if (contactLookupUri == null || !isAdded()) {
            return;
        }

        mContactIdForJoin = ContentUris.parseId(contactLookupUri);
        mContactWritableForJoin = isContactWritable();
        final Intent intent = new Intent(JoinContactActivity.JOIN_CONTACT);
        intent.putExtra(JoinContactActivity.EXTRA_TARGET_CONTACT_ID, mContactIdForJoin);
        startActivityForResult(intent, REQUEST_CODE_JOIN);
    }

    /**
     * Performs aggregation with the contact selected by the user from suggestions or A-Z list.
     */
    private void joinAggregate(final long contactId) {
        Intent intent = ContactSaveService.createJoinContactsIntent(mContext, mContactIdForJoin,
                contactId, mContactWritableForJoin,
                ContactEditorActivity.class, ContactEditorActivity.ACTION_JOIN_COMPLETED);
        mContext.startService(intent);
    }

    /**
     * Returns true if there is at least one writable raw contact in the current contact.
     */
    private boolean isContactWritable() {
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        int size = mState.size();
        for (int i = 0; i < size; i++) {
            RawContactDelta entity = mState.get(i);
            final AccountType type = entity.getAccountType(accountTypes);
            if (type.areContactsWritable()) {
                return true;
            }
        }
        return false;
    }

    private boolean isEditingUserProfile() {
        return mNewLocalProfile || mIsUserProfile;
    }

    public static interface Listener {
        /**
         * Contact was not found, so somehow close this fragment. This is raised after a contact
         * is removed via Menu/Delete (unless it was a new contact)
         */
        void onContactNotFound();

        /**
         * Contact was split, so we can close now.
         * @param newLookupUri The lookup uri of the new contact that should be shown to the user.
         * The editor tries best to chose the most natural contact here.
         */
        void onContactSplit(Uri newLookupUri);

        /**
         * User has tapped Revert, close the fragment now.
         */
        void onReverted();

        /**
         * Contact was saved and the Fragment can now be closed safely.
         */
        void onSaveFinished(Intent resultIntent);

        /**
         * User switched to editing a different contact (a suggestion from the
         * aggregation engine).
         */
        void onEditOtherContactRequested(
                Uri contactLookupUri, ArrayList<ContentValues> contentValues);

        /**
         * Contact is being created for an external account that provides its own
         * new contact activity.
         */
        void onCustomCreateContactActivityRequested(AccountWithDataSet account,
                Bundle intentExtras);

        /**
         * The edited raw contact belongs to an external account that provides
         * its own edit activity.
         *
         * @param redirect indicates that the current editor should be closed
         *            before the custom editor is shown.
         */
        void onCustomEditContactActivityRequested(AccountWithDataSet account, Uri rawContactUri,
                Bundle intentExtras, boolean redirect);
    }

    private class EntityDeltaComparator implements Comparator<RawContactDelta> {
        /**
         * Compare EntityDeltas for sorting the stack of editors.
         */
        @Override
        public int compare(RawContactDelta one, RawContactDelta two) {
            // Check direct equality
            if (one.equals(two)) {
                return 0;
            }

            final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
            String accountType1 = one.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
            String dataSet1 = one.getValues().getAsString(RawContacts.DATA_SET);
            final AccountType type1 = accountTypes.getAccountType(accountType1, dataSet1);
            String accountType2 = two.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
            String dataSet2 = two.getValues().getAsString(RawContacts.DATA_SET);
            final AccountType type2 = accountTypes.getAccountType(accountType2, dataSet2);

            // Check read-only
            if (!type1.areContactsWritable() && type2.areContactsWritable()) {
                return 1;
            } else if (type1.areContactsWritable() && !type2.areContactsWritable()) {
                return -1;
            }

            // Check account type
            boolean skipAccountTypeCheck = false;
            boolean isGoogleAccount1 = type1 instanceof GoogleAccountType;
            boolean isGoogleAccount2 = type2 instanceof GoogleAccountType;
            if (isGoogleAccount1 && !isGoogleAccount2) {
                return -1;
            } else if (!isGoogleAccount1 && isGoogleAccount2) {
                return 1;
            } else if (isGoogleAccount1 && isGoogleAccount2){
                skipAccountTypeCheck = true;
            }

            int value;
            if (!skipAccountTypeCheck) {
                if (type1.accountType == null) {
                    return 1;
                }
                value = type1.accountType.compareTo(type2.accountType);
                if (value != 0) {
                    return value;
                } else {
                    // Fall back to data set.
                    if (type1.dataSet != null) {
                        value = type1.dataSet.compareTo(type2.dataSet);
                        if (value != 0) {
                            return value;
                        }
                    } else if (type2.dataSet != null) {
                        return 1;
                    }
                }
            }

            // Check account name
            String oneAccount = one.getAccountName();
            if (oneAccount == null) oneAccount = "";
            String twoAccount = two.getAccountName();
            if (twoAccount == null) twoAccount = "";
            value = oneAccount.compareTo(twoAccount);
            if (value != 0) {
                return value;
            }

            // Both are in the same account, fall back to contact ID
            Long oneId = one.getRawContactId();
            Long twoId = two.getRawContactId();
            if (oneId == null) {
                return -1;
            } else if (twoId == null) {
                return 1;
            }

            return (int)(oneId - twoId);
        }
    }

    /**
     * Returns the contact ID for the currently edited contact or 0 if the contact is new.
     */
    protected long getContactId() {
        for (RawContactDelta rawContact : mState) {
            Long contactId = rawContact.getValues().getAsLong(RawContacts.CONTACT_ID);
            if (contactId != null) {
                return contactId;
            }
        }
        return 0;
    }

    /**
     * Triggers an asynchronous search for aggregation suggestions.
     */
    private void acquireAggregationSuggestions(Context context,
            RawContactEditorView rawContactEditor) {
        long rawContactId = rawContactEditor.getRawContactId();
        if (mAggregationSuggestionsRawContactId != rawContactId
                && mAggregationSuggestionView != null) {
            mAggregationSuggestionView.setVisibility(View.GONE);
            mAggregationSuggestionView = null;
            mAggregationSuggestionEngine.reset();
        }

        mAggregationSuggestionsRawContactId = rawContactId;

        if (mAggregationSuggestionEngine == null) {
            mAggregationSuggestionEngine = new AggregationSuggestionEngine(context);
            mAggregationSuggestionEngine.setListener(this);
            mAggregationSuggestionEngine.start();
        }

        mAggregationSuggestionEngine.setContactId(getContactId());

        LabeledEditorView nameEditor = rawContactEditor.getNameEditor();
        mAggregationSuggestionEngine.onNameChange(nameEditor.getValues());
    }

    @Override
    public void onAggregationSuggestionChange() {
        Activity activity = getActivity();
        if ((activity != null && activity.isFinishing())
                || !isVisible() ||  mState.isEmpty() || mStatus != Status.EDITING) {
            return;
        }

        UiClosables.closeQuietly(mAggregationSuggestionPopup);

        if (mAggregationSuggestionEngine.getSuggestedContactCount() == 0) {
            return;
        }

        final RawContactEditorView rawContactView =
                (RawContactEditorView)getRawContactEditorView(mAggregationSuggestionsRawContactId);
        if (rawContactView == null) {
            return; // Raw contact deleted?
        }
        final View anchorView = rawContactView.findViewById(R.id.anchor_view);
        mAggregationSuggestionPopup = new ListPopupWindow(mContext, null);
        mAggregationSuggestionPopup.setAnchorView(anchorView);
        mAggregationSuggestionPopup.setWidth(anchorView.getWidth());
        mAggregationSuggestionPopup.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
        mAggregationSuggestionPopup.setAdapter(
                new AggregationSuggestionAdapter(getActivity(),
                        mState.size() == 1 && mState.get(0).isContactInsert(),
                        this, mAggregationSuggestionEngine.getSuggestions()));
        mAggregationSuggestionPopup.setOnItemClickListener(mAggregationSuggestionItemClickListener);
        mAggregationSuggestionPopup.show();
    }

    @Override
    public void onJoinAction(long contactId, List<Long> rawContactIdList) {
        long rawContactIds[] = new long[rawContactIdList.size()];
        for (int i = 0; i < rawContactIds.length; i++) {
            rawContactIds[i] = rawContactIdList.get(i);
        }
        JoinSuggestedContactDialogFragment dialog =
                new JoinSuggestedContactDialogFragment();
        Bundle args = new Bundle();
        args.putLongArray("rawContactIds", rawContactIds);
        dialog.setArguments(args);
        dialog.setTargetFragment(this, 0);
        try {
            dialog.show(getFragmentManager(), "join");
        } catch (Exception ex) {
            // No problem - the activity is no longer available to display the dialog
        }
    }

    public static class JoinSuggestedContactDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.aggregation_suggestion_join_dialog_message)
                    .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ContactEditorFragment targetFragment =
                                        (ContactEditorFragment) getTargetFragment();
                                long rawContactIds[] =
                                        getArguments().getLongArray("rawContactIds");
                                targetFragment.doJoinSuggestedContact(rawContactIds);
                            }
                        }
                    )
                    .setNegativeButton(android.R.string.no, null)
                    .create();
        }
    }

    /**
     * Joins the suggested contact (specified by the id's of constituent raw
     * contacts), save all changes, and stay in the editor.
     */
    protected void doJoinSuggestedContact(long[] rawContactIds) {
        if (!hasValidState() || mStatus != Status.EDITING) {
            return;
        }

        mState.setJoinWithRawContacts(rawContactIds);
        save(SaveMode.RELOAD);
    }

    @Override
    public void onEditAction(Uri contactLookupUri) {
        SuggestionEditConfirmationDialogFragment dialog =
                new SuggestionEditConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable("contactUri", contactLookupUri);
        dialog.setArguments(args);
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), "edit");
    }

    public static class SuggestionEditConfirmationDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.aggregation_suggestion_edit_dialog_message)
                    .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ContactEditorFragment targetFragment =
                                        (ContactEditorFragment) getTargetFragment();
                                Uri contactUri =
                                        getArguments().getParcelable("contactUri");
                                targetFragment.doEditSuggestedContact(contactUri);
                            }
                        }
                    )
                    .setNegativeButton(android.R.string.no, null)
                    .create();
        }
    }

    /**
     * Abandons the currently edited contact and switches to editing the suggested
     * one, transferring all the data there
     */
    protected void doEditSuggestedContact(Uri contactUri) {
        if (mListener != null) {
            // make sure we don't save this contact when closing down
            mStatus = Status.CLOSING;
            mListener.onEditOtherContactRequested(
                    contactUri, mState.get(0).getContentValues());
        }
    }

    public void setAggregationSuggestionViewEnabled(boolean enabled) {
        if (mAggregationSuggestionView == null) {
            return;
        }

        LinearLayout itemList = (LinearLayout) mAggregationSuggestionView.findViewById(
                R.id.aggregation_suggestions);
        int count = itemList.getChildCount();
        for (int i = 0; i < count; i++) {
            itemList.getChildAt(i).setEnabled(enabled);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(KEY_URI, mLookupUri);
        outState.putString(KEY_ACTION, mAction);

        if (hasValidState()) {
            // Store entities with modifications
            outState.putParcelable(KEY_EDIT_STATE, mState);
        }
        outState.putLong(KEY_RAW_CONTACT_ID_REQUESTING_PHOTO, mRawContactIdRequestingPhoto);
        outState.putParcelable(KEY_VIEW_ID_GENERATOR, mViewIdGenerator);
        outState.putParcelable(KEY_CURRENT_PHOTO_URI, mCurrentPhotoUri);
        outState.putLong(KEY_CONTACT_ID_FOR_JOIN, mContactIdForJoin);
        outState.putBoolean(KEY_CONTACT_WRITABLE_FOR_JOIN, mContactWritableForJoin);
        outState.putLong(KEY_SHOW_JOIN_SUGGESTIONS, mAggregationSuggestionsRawContactId);
        outState.putBoolean(KEY_ENABLED, mEnabled);
        outState.putBoolean(KEY_NEW_LOCAL_PROFILE, mNewLocalProfile);
        outState.putBoolean(KEY_IS_USER_PROFILE, mIsUserProfile);
        outState.putInt(KEY_STATUS, mStatus);
        outState.putParcelable(KEY_UPDATED_PHOTOS, mUpdatedPhotos);
        outState.putBoolean(KEY_HAS_NEW_CONTACT, mHasNewContact);
        outState.putBoolean(KEY_IS_EDIT, mIsEdit);
        outState.putBoolean(KEY_NEW_CONTACT_READY, mNewContactDataReady);
        outState.putBoolean(KEY_EXISTING_CONTACT_READY, mExistingContactDataReady);
        outState.putParcelableArrayList(KEY_RAW_CONTACTS,
                mRawContacts == null ?
                Lists.<RawContact> newArrayList() :  Lists.newArrayList(mRawContacts));
        /**
        * SPRD:
        *   fix bug174143 Access to the information screen, click Picture to add a number to an existing contact
        *
        * Original Android code:
        * 
        * 
        * @{
        */
        outState.putBundle(KEY_INTENT_EXTRAS, mIntentExtras);
        if (mExistedDataIdList != null && mExistedDataList != null) {
            outState.putStringArrayList(KEY_DATA_LIST, mExistedDataList);
            outState.putStringArrayList(KEY_DATA_ID_LIST, mExistedDataIdList);
        }
        
        /**
        * @}
        */

        /**
        * SPRD:
        *   fix Bug 260683
        * @{
        */
        outState.putBoolean(KEY_DIALOG_SHOWING, mReplaceDialogShowing);
        /**
        * @}
        */

        /**
        * SPRD:
        *   fix bug181040 Contact Editor interface, vertical screen add phonetic name and then switch screens, phonetic name disappears
        *
        * Original Android code:
        * 
        * 
        * @{
        */
        if (mRawContactEditor instanceof RawContactEditorView) {
            outState.putBoolean(KEY_PHONETIC_NAME_ADDED,
                    ((RawContactEditorView) mRawContactEditor)
                            .getIfPhoneticNameAdded());
        }
        /**
        * @}
        */

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mStatus == Status.SUB_ACTIVITY) {
            mStatus = Status.EDITING;
        }

        // See if the photo selection handler handles this result.
        if (mCurrentPhotoHandler != null && mCurrentPhotoHandler.handlePhotoActivityResult(
                requestCode, resultCode, data)) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_JOIN: {
                // Ignore failed requests
                if (resultCode != Activity.RESULT_OK) return;
                if (data != null) {
                    final long contactId = ContentUris.parseId(data.getData());
                    joinAggregate(contactId);
                }
                break;
            }
            case REQUEST_CODE_ACCOUNTS_CHANGED: {
                // Bail if the account selector was not successful.
                if (resultCode != Activity.RESULT_OK) {
                    mListener.onReverted();
                    return;
                }
                // If there's an account specified, use it.
                if (data != null) {
                    AccountWithDataSet account = data.getParcelableExtra(Intents.Insert.ACCOUNT);
                    if (account != null) {
                        createContact(account);
                        return;
                    }
                }
                // If there isn't an account specified, then this is likely a phone-local
                // contact, so we should continue setting up the editor by automatically selecting
                // the most appropriate account.
                createContact();
                break;
            }
        }
    }

    /**
     * Sets the photo stored in mPhoto and writes it to the RawContact with the given id
     */
    private void setPhoto(long rawContact, Bitmap photo, Uri photoUri) {
        BaseRawContactEditorView requestingEditor = getRawContactEditorView(rawContact);

        if (photo == null || photo.getHeight() < 0 || photo.getWidth() < 0) {
            // This is unexpected.
            Log.w(TAG, "Invalid bitmap passed to setPhoto()");
        }

        if (requestingEditor != null) {
            requestingEditor.setPhotoBitmap(photo);
        } else {
            Log.w(TAG, "The contact that requested the photo is no longer present.");
        }

        mUpdatedPhotos.putParcelable(String.valueOf(rawContact), photoUri);
    }

    /**
     * Finds raw contact editor view for the given rawContactId.
     */
    public BaseRawContactEditorView getRawContactEditorView(long rawContactId) {
        for (int i = 0; i < mContent.getChildCount(); i++) {
            final View childView = mContent.getChildAt(i);
            if (childView instanceof BaseRawContactEditorView) {
                final BaseRawContactEditorView editor = (BaseRawContactEditorView) childView;
                if (editor.getRawContactId() == rawContactId) {
                    return editor;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if there is currently more than one photo on screen.
     */
    private boolean hasMoreThanOnePhoto() {
        int countWithPicture = 0;
        final int numEntities = mState.size();
        for (int i = 0; i < numEntities; i++) {
            final RawContactDelta entity = mState.get(i);
            if (entity.isVisible()) {
                final ValuesDelta primary = entity.getPrimaryEntry(Photo.CONTENT_ITEM_TYPE);
                if (primary != null && primary.getPhoto() != null) {
                    countWithPicture++;
                } else {
                    final long rawContactId = entity.getRawContactId();
                    final Uri uri = mUpdatedPhotos.getParcelable(String.valueOf(rawContactId));
                    if (uri != null) {
                        try {
                            mContext.getContentResolver().openInputStream(uri);
                            countWithPicture++;
                        } catch (FileNotFoundException e) {
                        }
                    }
                }

                if (countWithPicture > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The listener for the data loader
     */
    private final LoaderManager.LoaderCallbacks<Contact> mDataLoaderListener =
            new LoaderCallbacks<Contact>() {
        @Override
        public Loader<Contact> onCreateLoader(int id, Bundle args) {
            mLoaderStartTime = SystemClock.elapsedRealtime();
            return new ContactLoader(mContext, mLookupUri, true);
        }

        @Override
        public void onLoadFinished(Loader<Contact> loader, Contact data) {
            final long loaderCurrentTime = SystemClock.elapsedRealtime();
            Log.v(TAG, "Time needed for loading: " + (loaderCurrentTime-mLoaderStartTime));
            if (!data.isLoaded()) {
                // Item has been deleted
                Log.i(TAG, "No contact found. Closing activity");
                if (mListener != null) mListener.onContactNotFound();
                return;
            }

            mStatus = Status.EDITING;
            mLookupUri = data.getLookupUri();
            final long setDataStartTime = SystemClock.elapsedRealtime();
            setData(data);
            final long setDataEndTime = SystemClock.elapsedRealtime();

            Log.v(TAG, "Time needed for setting UI: " + (setDataEndTime-setDataStartTime));
            Log.d(TAG, "tian.......isPrivate="+data.getDetailUserImportType()+"mode=="+data.getDeatailUserMode());
        }

        @Override
        public void onLoaderReset(Loader<Contact> loader) {
        }
    };

    /**
     * The listener for the group meta data loader for all groups.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return new GroupMetaDataLoader(mContext, Groups.CONTENT_URI);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mGroupMetaData = data;
            bindGroupMetaData();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    @Override
    public void onSplitContactConfirmed() {
        if (mState.isEmpty()) {
            // This may happen when this Fragment is recreated by the system during users
            // confirming the split action (and thus this method is called just before onCreate()),
            // for example.
            Log.e(TAG, "mState became null during the user's confirming split action. " +
                    "Cannot perform the save action.");
            return;
        }

        mState.markRawContactsForSplitting();
        save(SaveMode.SPLIT);
    }

    /**
     * Custom photo handler for the editor.  The inner listener that this creates also has a
     * reference to the editor and acts as an {@link EditorListener}, and uses that editor to hold
     * state information in several of the listener methods.
     */
    private final class PhotoHandler extends PhotoSelectionHandler {

        final long mRawContactId;
        private final BaseRawContactEditorView mEditor;
        private final PhotoActionListener mPhotoEditorListener;

        public PhotoHandler(Context context, BaseRawContactEditorView editor, int photoMode,
                RawContactDeltaList state) {
            super(context, editor.getPhotoEditor(), photoMode, false, state);
            mEditor = editor;
            mRawContactId = editor.getRawContactId();
            mPhotoEditorListener = new PhotoEditorListener();
        }

        @Override
        public PhotoActionListener getListener() {
            return mPhotoEditorListener;
        }

        @Override
        public void startPhotoActivity(Intent intent, int requestCode, Uri photoUri) {
            mRawContactIdRequestingPhoto = mEditor.getRawContactId();
            mCurrentPhotoHandler = this;
            mStatus = Status.SUB_ACTIVITY;
            mCurrentPhotoUri = photoUri;
            ContactEditorFragment.this.startActivityForResult(intent, requestCode);
        }

        private final class PhotoEditorListener extends PhotoSelectionHandler.PhotoActionListener
                implements EditorListener {

            @Override
            public void onRequest(int request) {
                if (!hasValidState()) return;

                if (request == EditorListener.REQUEST_PICK_PHOTO) {
                    onClick(mEditor.getPhotoEditor());
                }
            }

            @Override
            public void onDeleteRequested(Editor removedEditor) {
                // The picture cannot be deleted, it can only be removed, which is handled by
                // onRemovePictureChosen()
            }

            /**
             * User has chosen to set the selected photo as the (super) primary photo
             */
            @Override
            public void onUseAsPrimaryChosen() {
                // Set the IsSuperPrimary for each editor
                int count = mContent.getChildCount();
                for (int i = 0; i < count; i++) {
                    final View childView = mContent.getChildAt(i);
                    if (childView instanceof BaseRawContactEditorView) {
                        final BaseRawContactEditorView editor =
                                (BaseRawContactEditorView) childView;
                        final PhotoEditorView photoEditor = editor.getPhotoEditor();
                        photoEditor.setSuperPrimary(editor == mEditor);
                    }
                }
                /*
                 * SPRD:
                 *  fix bug 270858 when set the photo,the StructName is show abnormal.
                 *  @orig
                 *  bindEditors();
                 * @{
                 */
                bindEditors(true);
                /*
                 * @}
                 */
            }

            /**
             * User has chosen to remove a picture
             */
            @Override
            public void onRemovePictureChosen() {
                mEditor.setPhotoBitmap(null);

                // Prevent bitmap from being restored if rotate the device.
                // (only if we first chose a new photo before removing it)
                mUpdatedPhotos.remove(String.valueOf(mRawContactId));
                /*
                 * SPRD:
                 *  fix bug 270858 when set the photo,the StructName is show abnormal.
                 *  @orig
                 *  bindEditors();
                 * @{
                 */
                bindEditors(true);
                /*
                 * @}
                 */
            }

            @Override
            public void onPhotoSelected(Uri uri) throws FileNotFoundException {
                final Bitmap bitmap = ContactPhotoUtils.getBitmapFromUri(mContext, uri);
                setPhoto(mRawContactId, bitmap, uri);
                mCurrentPhotoHandler = null;
                /*
                 * SPRD:
                 *  fix bug 270858 when set the photo,the StructName is show abnormal.
                 *  @orig
                 *  bindEditors();
                 * @{
                 */
                bindEditors(true);
                /*
                 * @}
                 */
            }

            @Override
            public Uri getCurrentPhotoUri() {
                return mCurrentPhotoUri;
            }

            @Override
            public void onPhotoSelectionDismissed() {
                // Nothing to do.
            }
        }
    }
    /**
    * SPRD:
    * 
    * @{
    */
    private static final int ID_CONFIRM_REPLACE_DIALOG = 1;
    private static final int ID_CONFIRM_REPLACE_DIALOG_LIST = 2;

    private static final String KEY_INTENT_EXTRAS = "intentExtras";
    private static final String KEY_DATA_LIST = "dataList";
    private static final String KEY_DATA_ID_LIST = "dataIdList";
    private static final String KEY_PHONETIC_NAME_ADDED = "phoneticNameAdded";
    private static final String KEY_DIALOG_SHOWING = "hasDialog";

    private static final String PROFILE_SEGMENT = "profile";

    private boolean mReplaceDialogShowing = false;
    private PhotoHandler mPhotoHandler;

    private Set<String> mInsertedMimeTypes;
    private ArrayList<String> mExistedDataList;
    private ArrayList<String> mExistedDataIdList;
    private Long mSelectDataId;

    private BaseRawContactEditorView mRawContactEditor;
    private boolean mPhoneticNameAdded;
    public boolean mIsSaveFailure = false;
    
    @Override
    public void onPause() {
        super.onPause();
        if (mRawContactEditor != null
                && mRawContactEditor instanceof RawContactEditorView) {
            RawContactEditorView editor = (RawContactEditorView) mRawContactEditor;
            PopupMenu popupMenu = editor.getPopupMenu();
            if (popupMenu != null) {
                popupMenu.dismiss();
            }
        }
        if (mPhotoHandler != null) {
			mPhotoHandler.destroy();
			mPhotoHandler = null;
		}
    }

    private boolean isSimType() {
        boolean SimAccount = false;
        if (mState != null && mState.size()>0) {
            String accountType = mState.get(0).getValues().getAsString(RawContacts.ACCOUNT_TYPE);
            if (accountType != null
                    && (accountType.equals(SimAccountType.ACCOUNT_TYPE) || accountType
                            .equals(USimAccountType.ACCOUNT_TYPE))) {
                SimAccount = true;
            }
        }
        return SimAccount;
    }

    private int showReplaceDialog(Contact contact) {
        if (contact == null) {
            return 0;
        }
        RawContactDeltaList entityDeltaList = contact.createRawContactDeltaList();
        mExistedDataIdList = new ArrayList<String>();
        mExistedDataList = new ArrayList<String>();

        AccountWithDataSet account = contact.getAccount();
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(mContext);
        AccountType accountType = accountTypeManager.getAccountTypeForAccount(account);
        final ContentValues contentValues = new ContentValues();
        RawContactDelta insert = new RawContactDelta(ValuesDelta.fromAfter(contentValues));
        RawContactModifier.parseExtras(mContext, accountType, insert, mIntentExtras);
        mInsertedMimeTypes = insert.getMimeTypes();
        mInsertedMimeTypes.remove(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);

        if (entityDeltaList == null) {
            return 0;
        }
        for (RawContactDelta entityDelta : entityDeltaList) {

            final RawContactDelta entity = entityDelta;
            final ValuesDelta values = entity.getValues();
            if (values == null || !values.isVisible()) {
                continue;
            }

            String strValue = null;
            for (String mimetype : entity.getMimeTypes()) {
                int maxLen = -1;
                if (mInsertedMimeTypes.contains(mimetype)) {
                    if (!SimAccountType.ACCOUNT_TYPE.equals(accountType.accountType)
                            && !USimAccountType.ACCOUNT_TYPE.equals(accountType.accountType)) {
                        maxLen = AccountTypeManager.getTypeOverallMaxForAccount(accountType,
                                mimetype);
                    } else {
                        int max = AccountRestrictionUtils.get(mContext)
                                .getTypeOverallMax(account, mimetype);
                        if (max != 0) {
                            maxLen = max;
                        }
                    }
                    if (mimetype == null || maxLen == -2) {
                        continue;
                    }
                    for (ValuesDelta child : entity.getMimeEntries(mimetype)) {
                        if (child.containsKey("_id") && child.containsKey("data1")) {
                            mExistedDataList.add(child.getAsString("data1"));
                            mExistedDataIdList.add(child.getAsString("_id"));
                        } else {
                            continue;
                        }
                    }
                    if (mExistedDataList.size() > 0 && mExistedDataList.size() == maxLen) {
                        if (maxLen == 1) {
                            return ID_CONFIRM_REPLACE_DIALOG;
                        } else if (maxLen > 1) {
                            return ID_CONFIRM_REPLACE_DIALOG_LIST;
                        }
                    } else {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    public static class ConfirmReplaceDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.non_phone_add_to_contacts)
                    .setMessage(R.string.confirmCoverPhoneNumber)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ContactEditorFragment targetFragment =
                                            (ContactEditorFragment) getTargetFragment();
                                    targetFragment.bindEditorsForExistingContact(null, false, mRawContacts);
                                }
                            }
                    )
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (getActivity() != null) {
                                        getActivity().finish();
                                    }
                                }
                            })
                    .create();
            return dialog;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }

    public static class ConfirmReplaceListDialogFragment extends DialogFragment {
        static Long mSelectDataId = null;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ArrayList<String> dataList = getArguments().getStringArrayList(KEY_DATA_LIST);
            ArrayList<String> dataIdList = getArguments().getStringArrayList(KEY_DATA_ID_LIST);
            String[] dataStrings = (String[]) dataList
                    .toArray(new String[dataList.size()]);
            final String[] dataIds = (String[]) dataIdList.toArray(new String[dataIdList.size()]);

            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.replaceSelected)
                    .setSingleChoiceItems(dataStrings, 0,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which >= 0) {
                                        mSelectDataId = Long.valueOf(dataIds[which]);
                                    }
                                }
                            })
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ContactEditorFragment targetFragment =
                                            (ContactEditorFragment) getTargetFragment();
                                    if (mSelectDataId == null) {
                                        mSelectDataId = Long.valueOf(dataIds[0]);
                                    }
                                    targetFragment.mIntentExtras.putLong("replaceDataId",
                                            mSelectDataId);
                                    targetFragment.bindEditorsForExistingContact(null, false, mRawContacts);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (getActivity() != null) {
                                        getActivity().finish();
                                    }
                                }
                            }).create();
            return dialog;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }

    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ID_CONFIRM_REPLACE_DIALOG:
                    ConfirmReplaceDialogFragment dialog =
                            new ConfirmReplaceDialogFragment();
                    mReplaceDialogShowing = true;
                    dialog.setTargetFragment(ContactEditorFragment.this, 0);
                    dialog.show(getFragmentManager(), null);
                    break;
                case ID_CONFIRM_REPLACE_DIALOG_LIST:
                    ConfirmReplaceListDialogFragment listDialog =
                            new ConfirmReplaceListDialogFragment();
                    mReplaceDialogShowing = true;
                    Bundle args = new Bundle();
                    args.putStringArrayList(KEY_DATA_LIST, mExistedDataList);
                    args.putStringArrayList(KEY_DATA_ID_LIST, mExistedDataIdList);
                    listDialog.setArguments(args);
                    listDialog.setTargetFragment(ContactEditorFragment.this, 0);
                    listDialog.show(getFragmentManager(), null);
                    break;
            }
        }
    };
    /**
    * @}
    */
}
