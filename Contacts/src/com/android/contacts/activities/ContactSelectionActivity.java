/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.contacts.activities;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.Intents;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import com.android.contacts.common.util.AccountFilterUtil;
import android.provider.ContactsContract.Data;
import android.content.ContentValues;
import com.android.contacts.common.widget.ContextMenuAdapter;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.list.ContactListFilter;
import android.graphics.Color;
import android.app.Dialog;
import android.content.DialogInterface;
import android.app.DialogFragment;
import android.app.AlertDialog;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.group.GroupEditorFragment;
import com.android.contacts.common.util.Constants;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.common.list.DirectoryListLoader;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.list.ContactListFilterController.ContactListFilterListener;
import com.android.contacts.list.EmailAddressPickerFragment;
import com.android.contacts.list.LegacyPhoneNumberPickerFragment;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.list.OnEmailAddressPickerActionListener;
import com.android.contacts.common.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.OnPostalAddressPickerActionListener;
import com.android.contacts.common.list.PhoneNumberPickerFragment;
import com.android.contacts.list.PostalAddressPickerFragment;
import com.google.common.collect.Sets;
import com.sprd.contacts.common.util.UniverseUtils;
import com.sprd.contacts.list.OnAllInOneDataPickerActionListener;
import com.sprd.contacts.list.AllInOneDataPickerFragment;
import com.sprd.contacts.list.OnAllInOneDataMultiPickerActionListener;
import com.sprd.contacts.list.OnEmailAddressMultiPickerActionListener;
import com.sprd.contacts.common.list.OnPhoneNumberMultiPickerActionListener;
import com.sprd.contacts.common.model.account.PhoneAccountType;
import com.sprd.contacts.list.OnContactMultiPickerActionListener;
import com.sprd.contacts.group.GroupDetailFragmentSprd;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Parcel;
import android.widget.Toast;
/**
 * Displays a list of contacts (or phone numbers or postal addresses) for the
 * purposes of selecting one.
 */
public class ContactSelectionActivity extends ContactsActivity
        implements View.OnCreateContextMenuListener, OnQueryTextListener, OnClickListener,
                OnCloseListener, OnFocusChangeListener, ContactListFilterController.ContactListFilterListener{
    private static final String TAG = "ContactSelectionActivity";

    private static final int SUBACTIVITY_ADD_TO_EXISTING_CONTACT = 0;

    private static final String KEY_ACTION_CODE = "actionCode";
    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

    // Delay to allow the UI to settle before making search view visible
    private static final int FOCUS_DELAY = 200;

    // add by sprd
    private static final int MAX_DATA_SIZE= 500000;

    private ContactsIntentResolver mIntentResolver;
    protected ContactEntryListFragment<?> mListFragment;

    private int mActionCode = -1;

    private ContactsRequest mRequest;
    private SearchView mSearchView;
    /**
     * Can be null. If null, the "Create New Contact" button should be on the menu.
     */
    private View mCreateNewContactButton;
    private BroadcastReceiver mSelecStatusReceiver;
    public ContactSelectionActivity() {
        mIntentResolver = new ContactsIntentResolver(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {

        if (fragment instanceof ContactEntryListFragment<?>) {
            mListFragment = (ContactEntryListFragment<?>) fragment;
            setupActionListener();
        }
    }
    
    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (savedState != null) {
            mActionCode = savedState.getInt(KEY_ACTION_CODE);
            /**
            * SPRD:
            * 
            * @{
            */
            mPermanentAccountFilter = (ContactListFilter)savedState.getParcelable(KEY_FILTER);
            setupActionListener();
            mIsFilterChanged = savedState.getBoolean(KEY_FILTER_CHANG);
            /**
            * @}
            */
        }

        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        if (!mRequest.isValid()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        /**
        * SPRD:
        * 
        * @{
        */
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        mContactListFilterController = ContactListFilterController.getInstance(this);
        mContactListFilterController.checkFilterValidity(false);
        mContactListFilterController.addListener(this);
        mFilter=mContactListFilterController.getFilter();
        
        Log.d(TAG, "mFilterddddddddddd=="+mFilter.accountType);
        /**
        * @}
        */

        Intent redirect = mRequest.getRedirectIntent();
        if (redirect != null) {
            // Need to start a different activity
            startActivity(redirect);
            finish();
            return;
        }

        configureActivityTitle();

        setContentView(R.layout.contact_picker);

        /**
        * SPRD:
        * 
        * @{
        */
        mAccountManager = AccountTypeManager
                .getInstance(ContactSelectionActivity.this);
        ArrayList<AccountWithDataSet> allAccounts = (ArrayList) mAccountManager
                .getAccounts(false);
        mAccountNum = allAccounts.size();
        /**
        * @}
        */

        if (mActionCode != mRequest.getActionCode()) {
            mActionCode = mRequest.getActionCode();
            configureListFragment();
        }

        prepareSearchViewAndActionBar();

        Log.d(TAG, "shouldShowCreateNewContactButton()=="+shouldShowCreateNewContactButton()+"mActionCode"+mActionCode+"mRequest.isSearchMode()=="+mRequest.isSearchMode());
        
        mCreateNewContactButton = findViewById(R.id.new_contact);
        if (mCreateNewContactButton != null) {
            if (shouldShowCreateNewContactButton()) {
                mCreateNewContactButton.setVisibility(View.VISIBLE);
                mCreateNewContactButton.setOnClickListener(this);
                Log.d(TAG, "shouldShowCr");
            } else {
                mCreateNewContactButton.setVisibility(View.GONE);
            }
        }
    }

    private boolean shouldShowCreateNewContactButton() {
        return (mActionCode == ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT
                || (mActionCode == ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT
                        && !mRequest.isSearchMode()));
    }

    private void prepareSearchViewAndActionBar() {
        // Postal address pickers (and legacy pickers) don't support search, so just show
        // "HomeAsUp" button and title.
        if (mRequest.getActionCode() == ContactsRequest.ACTION_PICK_POSTAL ||
                mRequest.isLegacyCompatibilityMode()) {
            findViewById(R.id.search_view).setVisibility(View.GONE);
            final ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                /**
                * SPRD:
                * 
                *
                * Original Android code:
                * actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
                * 
                * @{
                */
                if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                    View customActionBarView = null;
                    LayoutInflater inflater = (LayoutInflater) getSystemService
                            (Context.LAYOUT_INFLATER_SERVICE);
                    customActionBarView = inflater.inflate(
                            R.layout.editor_custom_action_bar_overlay, null);
                    mDoneMenuItem = (Button) customActionBarView
                            .findViewById(R.id.save_menu_item_button);
                    mDoneMenuItem
                            .setVisibility(mListFragment.isMultiPickerSupported() ? View.VISIBLE
                                    : View.GONE);
                    mDoneMenuDisableColor = mDoneMenuItem.getCurrentTextColor();
                    setDoneMenu(mListFragment.getSelecStatus());
                    mDoneMenuItem.setText(R.string.menu_done);
                    mDoneMenuItem.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListFragment.onMultiPickerSelected();
                        }
                    });
                    mDoneMenuItem
                            .setVisibility(mListFragment.isMultiPickerSupported() ? View.VISIBLE
                                    : View.GONE);
                    Button cancelMenuItem = (Button) customActionBarView
                            .findViewById(R.id.cancel_menu_item_button);
                    cancelMenuItem.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onBackPressed();
                        }
                    });
                    actionBar.setTitle(R.string.contactPickerActivityTitle);
                    actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                            | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
                    actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL
                                    | Gravity.END));

                } else {
                    actionBar.setDisplayShowHomeEnabled(true);
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    actionBar.setDisplayShowTitleEnabled(true);
                }
                /**
                * @}
                */
            }
            return;
        }

        // If ActionBar is available, show SearchView on it. If not, show SearchView inside the
        // Activity's layout.
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            /**
            * SPRD:
            * 
            * @{
            */
            if (UniverseUtils.UNIVERSEUI_SUPPORT){
                findViewById(R.id.search_view).setVisibility(View.GONE);
                View customActionBarView=null;
                LayoutInflater inflater = (LayoutInflater) getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar_overlay, null);
                mDoneMenuItem = (Button)customActionBarView.findViewById(R.id.save_menu_item_button);
                mDoneMenuItem.setVisibility(mListFragment.isMultiPickerSupported()? View.VISIBLE:View.GONE);
                mDoneMenuDisableColor = mDoneMenuItem.getCurrentTextColor();
                setDoneMenu(false);
                mDoneMenuItem.setText(R.string.menu_done);
                mDoneMenuItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(getIntent().getExtras() != null &&
                                getIntent().getExtras().getInt("mode") == SUBACTIVITY_BATCH_DELETE){
                            ConfirmBatchDeleteDialogFragment cDialog = new ConfirmBatchDeleteDialogFragment();
                            cDialog.setTargetFragment(mListFragment, 0);
                            cDialog.show(getFragmentManager(), null);
                        }else{
                            mListFragment.onMultiPickerSelected();
                        }
                    }
                });
                mDoneMenuItem.setVisibility(mListFragment.isMultiPickerSupported()? View.VISIBLE:View.GONE);
                View cancelMenuItem = customActionBarView.findViewById(R.id.cancel_menu_item_button);
                cancelMenuItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });
                actionBar.setTitle(R.string.contactPickerActivityTitle);
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
                actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.END));
            }
            /**
            * @}
            */
                else{
                    final View searchViewOnLayout = findViewById(R.id.search_view);
                    if (searchViewOnLayout != null) {
                        searchViewOnLayout.setVisibility(View.GONE);
                    }

                    final View searchViewContainer = LayoutInflater.from(actionBar.getThemedContext())
                            .inflate(R.layout.custom_action_bar, null);
                    mSearchView = (SearchView) searchViewContainer.findViewById(R.id.search_view);

                    // In order to make the SearchView look like "shown via search menu", we need to
                    // manually setup its state. See also DialtactsActivity.java and ActionBarAdapter.java.
                    mSearchView.setIconifiedByDefault(true);
                    mSearchView.setQueryHint(getString(R.string.hint_findContacts));
                    mSearchView.setIconified(false);

                    mSearchView.setOnQueryTextListener(this);
                    mSearchView.setOnCloseListener(this);
                    mSearchView.setOnQueryTextFocusChangeListener(this);

                    actionBar.setCustomView(searchViewContainer,
                            new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    actionBar.setDisplayShowCustomEnabled(true);
                    actionBar.setDisplayShowHomeEnabled(true);
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }
        } else {
            mSearchView = (SearchView) findViewById(R.id.search_view);
            mSearchView.setQueryHint(getString(R.string.hint_findContacts));
            mSearchView.setOnQueryTextListener(this);

            // This is a hack to prevent the search view from grabbing focus
            // at this point.  If search view were visible, it would always grabs focus
            // because it is the first focusable widget in the window.
            mSearchView.setVisibility(View.INVISIBLE);
            mSearchView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSearchView.setVisibility(View.VISIBLE);
                }
            }, FOCUS_DELAY);
        }

        // Clear focus and suppress keyboard show-up.
        /**
        * SPRD:
        * 
        *
        * Original Android code:
        * mSearchView.clearFocus();
        * 
        * @{
        */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            mSearchView.clearFocus();
        }
        /**
        * @}
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If we want "Create New Contact" button but there's no such a button in the layout,
        // try showing a menu for it.
        if (shouldShowCreateNewContactButton() && mCreateNewContactButton == null) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.contact_picker_options, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Go back to previous screen, intending "cancel"
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.create_new_contact: {
                startCreateNewContactActivity();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTION_CODE, mActionCode);
        /**
        * SPRD:
        * 
        * @{
        */
        outState.putParcelable(KEY_FILTER, mPermanentAccountFilter);
        if (mIsFilterChanged) {
            outState.putBoolean(KEY_FILTER_CHANG, mIsFilterChanged);
        }
        /**
        * @}
        */
    }

    private void configureActivityTitle() {
        if (!TextUtils.isEmpty(mRequest.getActivityTitle())) {
            setTitle(mRequest.getActivityTitle());
            return;
        }

        int actionCode = mRequest.getActionCode();
        switch (actionCode) {
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            /**
            * SPRD:
            * 
            * @{
            */
            case ContactsRequest.ACTION_MULTI_PICK_CONTACT:
            /**
            * @}
            */
            case ContactsRequest.ACTION_PICK_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                setTitle(R.string.shortcutActivityTitle);
                break;
            }

            /**
            * SPRD:
            * 
            * @{
            */
            case ContactsRequest.ACTION_MULTI_PICK_PHONE:
            /**
            * @}
            */
            case ContactsRequest.ACTION_PICK_PHONE: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            /**
            * SPRD:
            * 
            * @{
            */
            case ContactsRequest.ACTION_MULTI_PICK_EMAIL:
            /**
            * @}
            */
            case ContactsRequest.ACTION_PICK_EMAIL: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                setTitle(R.string.callShortcutActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                setTitle(R.string.messageShortcutActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_POSTAL: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }
            case ContactsRequest.ACTION_VIEW_CONTACT_SELECT_FAST_CALL: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }
        }
    }

    /**
     * Creates the fragment based on the current request.
     */
    public void configureListFragment() {
        /**
        * SPRD:
        * 
        * @{
        */
        AccountWithDataSet account = getIntent().getParcelableExtra(Intents.Insert.ACCOUNT);
        if (account != null) {
            mPermanentAccountFilter = ContactListFilter.createAccountFilter(account.type,
                    account.name, null, null);
        }
        /**
        * @}
        */
        Log.d(TAG, "mActionCode=="+mActionCode);
        
        switch (mActionCode) {
            /**
            * SPRD:
            * 
            * @{
            */
            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: // fall through
            /**
            * @}
            */
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                /**
                * SPRD:
                * 
                *
                * Original Android code:
                * fragment.setEditMode(true);
                * 
                * @{
                */
                if (mActionCode == ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT) {
                    fragment.setEditMode(false); 
                }else {
                    fragment.setEditMode(true);
                }
                /**
                * @}
                */
                fragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
                mListFragment = fragment;
                /**
                * SPRD:
                * 
                * @{
                */
                if (mPermanentAccountFilter!=null) {
                    fragment.setPermanentFilter(mPermanentAccountFilter);
                }
                // check for mime-type capability
                ContactListFilter filter=getAccountFilterForMimeType(getIntent().getExtras());
                if (filter != null) {
                    if (mIsAllaccount) {
                        mPermanentAccountFilter = null;
                        fragment.setFilter(mFilter);
                    } else {
                        mPermanentAccountFilter = filter;
                        fragment.setPermanentFilter(mPermanentAccountFilter);
                    }
                }
                /**
                 * add by xuhong.tian
                 */
    			//fragment.setSearchFilter(2); // when user insert and edite contacts,search view can show sim contacts and private contacats ,so add flag to tell them. 
    			fragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
    			mFilter = ContactListFilter
    					.createAccountFilter("sprd.com.android.account.phone", getString(R.string.label_phone), null, null);
    			fragment.setPermanentFilter(mFilter);
    			fragment.setFilter(mFilter);
    			mListFragment = fragment;
                
                /**
                * @}
                */
                break;
            }

            case ContactsRequest.ACTION_PICK_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setIncludeProfile(mRequest.shouldIncludeProfile());
                mListFragment = fragment;
                /**
                * SPRD:
                * 
                * @{
                */
                if (getIntent().getBooleanExtra("no_sim", false)) {
                    AccountTypeManager am = AccountTypeManager
                            .getInstance(ContactSelectionActivity.this);
                    ArrayList<AccountWithDataSet> allAccounts = (ArrayList) am
                            .getAccounts(false);
                    ArrayList<AccountWithDataSet> accounts = (ArrayList) allAccounts
                            .clone();
                    Iterator<AccountWithDataSet> iter = accounts.iterator();
                    while (iter.hasNext()) {
                        AccountWithDataSet accountWithDataSet = iter.next();
                        if (accountWithDataSet.type.equals("sprd.com.android.account.sim") || accountWithDataSet.type.equals("sprd.com.android.account.usim")) {
                            iter.remove();
                        }
                    }
                    mPermanentAccountFilter = ContactListFilter
                            .createAccountsFilter(accounts);
                    fragment.setPermanentFilter(mPermanentAccountFilter);
                }else{
                    // check for mime-type capability
                    ContactListFilter filter=getAccountFilterForMimeType(getIntent().getExtras());
                    if (filter != null) {
                        if (mIsAllaccount) {
                            mPermanentAccountFilter = null;
                            fragment.setFilter(mFilter);
                        } else {
                            mPermanentAccountFilter = filter;
                            fragment.setPermanentFilter(mPermanentAccountFilter);
                        }
                    }
                }
                /**
                * @}
                */
                break;
            }
            
            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                /**
                * SPRD:
                * 
                *
                * Original Android code:
                *  ContactPickerFragment fragment = new ContactPickerFragment();
                mListFragment = fragment;
                * 
                * @{
                */
                ContactPickerFragment fragment = new ContactPickerFragment(this);
                fragment.setShortcutRequested(true);
                AccountTypeManager am = AccountTypeManager
                        .getInstance(ContactSelectionActivity.this);
                ArrayList<AccountWithDataSet> allAccounts = (ArrayList) am
                        .getAccounts(false);
                ArrayList<AccountWithDataSet> accounts = (ArrayList) allAccounts
                        .clone();
                Iterator<AccountWithDataSet> iter = accounts.iterator();
                while (iter.hasNext()) {
                    AccountWithDataSet accountWithDataSet = iter.next();
                    if (accountWithDataSet.type.equals("sprd.com.android.account.sim") || accountWithDataSet.type.equals("sprd.com.android.account.usim")) {
                        iter.remove();
                    }
                }
                mPermanentAccountFilter = ContactListFilter
                        .createAccountsFilter(accounts);
                fragment.setPermanentFilter(mPermanentAccountFilter);
                mListFragment = fragment;
                /**
                * @}
                */
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONE: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setFilter(ContactListFilter
						.createAccountFilter(
								PhoneAccountType.ACCOUNT_TYPE,
								getString(R.string.label_phone), null,
								null));
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAIL: {
                mListFragment = new EmailAddressPickerFragment();
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setShortcutAction(Intent.ACTION_CALL);

                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setShortcutAction(Intent.ACTION_SENDTO);

                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_POSTAL: {
                PostalAddressPickerFragment fragment = new PostalAddressPickerFragment();
                mListFragment = fragment;
                break;
            }

            /**
            * SPRD:
            * 
            * @{
            */
            case ContactsRequest.ACTION_MULTI_PICK_CONTACT: {   
                ContactPickerFragment fragment = new ContactPickerFragment(this);
                fragment.setMultiPickerSupported(true);
                fragment.setCreateContactEnabled(false);
                fragment.setEditMode(false);
                
    			Log.d(TAG, "mRequest=" + mRequest+"mActionCodeWhichSms=="+mRequest.getSmsFlag());
                Log.d(TAG, "getIntent()=="+getIntent().getAction()+"data="+getIntent());
                
                if (getIntent().hasExtra("src_account")) {
                    account = (AccountWithDataSet) (getIntent().getParcelableExtra("src_account"));
                    mPermanentAccountFilter = ContactListFilter.createAccountFilter(account.type,
                            account.name, null, null);
                    fragment.setAddGroupMemSelection(getIntent().getStringExtra(GroupEditorFragment.CONTACTID_IN_GROUP));
                    fragment.setPermanentFilter(mPermanentAccountFilter);
                    
                    
                    
                } else if (getIntent().hasExtra("dst_account")) {
                    AccountWithDataSet dstAcount = (AccountWithDataSet) getIntent()
                            .getExtra("dst_account");
                    AccountTypeManager am = AccountTypeManager
                            .getInstance(ContactSelectionActivity.this);
                    ArrayList<AccountWithDataSet> allAccounts = (ArrayList) am
                            .getAccounts(false);
                    ArrayList<AccountWithDataSet> accounts = (ArrayList) allAccounts
                            .clone();
                    Iterator<AccountWithDataSet> iter = accounts.iterator();
                    while (iter.hasNext()) {
                        AccountWithDataSet accountWithDataSet = iter.next();
                        if (accountWithDataSet.name.equals(dstAcount.name)) {
                            iter.remove();
                        }
                    }
                    mPermanentAccountFilter = ContactListFilter
                            .createAccountsFilter(accounts);
                    fragment.setPermanentFilter(mPermanentAccountFilter);
                } else if (getIntent().hasExtra("setMulitStarred")) {
                    AccountTypeManager am = AccountTypeManager
                            .getInstance(ContactSelectionActivity.this);
                    ArrayList<AccountWithDataSet> allAccounts = (ArrayList) am
                            .getAccounts(false);
                    ArrayList<AccountWithDataSet> accounts = (ArrayList) allAccounts
                            .clone();
                    Iterator<AccountWithDataSet> iter = accounts.iterator();
                    while (iter.hasNext()) {
                        AccountWithDataSet accountWithDataSet = iter.next();
                        if (accountWithDataSet.type
                                .equals("sprd.com.android.account.usim")
                                || accountWithDataSet.type
                                .equals("sprd.com.android.account.sim")) {
                            iter.remove();
                        }
                    }
                    mPermanentAccountFilter = ContactListFilter
                            .createAccountsFilter(accounts);
                    fragment.setStarMemFlag();
                    fragment.setPermanentFilter(mPermanentAccountFilter);
                } else if (getIntent().hasExtra("move_group_member")) {
                    Long groupId = (Long) getIntent().getExtra(GroupDetailFragmentSprd.SRC_GROUP_ID);
                    mGroupFilter = ContactListFilter.createGroupFilter(groupId);
                    fragment.setFilter(mGroupFilter);
                } else if (getIntent().hasExtra("delete_group_member")) {
                    Long groupId = (Long) getIntent().getExtra("delete_group_member");
                    mGroupFilter = ContactListFilter.createGroupFilter(groupId);
                    fragment.setFilter(mGroupFilter);    
                } else if (getIntent().hasExtra(ContactPickerFragment.MMS_MULTI_VCARD)) {
                	Log.d(TAG, "deletye...........aaaaaaaaaa....");
                    fragment.setFilter(mFilter);
                } else {  // add for patch delete 
                	Log.d(TAG, "deletye..............."+mFilter.filterType+mFilter.accountType);
                	

            		SharedPreferences mPrefs = PreferenceManager
            				.getDefaultSharedPreferences(this);
            		ContactListFilter privatefilterType=ContactListFilter.getDefaultPreferenceFilter(mPrefs); 
                	 fragment.setFilter(privatefilterType);
                	Log.d(TAG, "deletye..............."+privatefilterType);         	
                	if(privatefilterType.filterType == ContactListFilter.FILTER_TYPE_SINGLE_CONTACT)
                    fragment.setFilter(mFilter);
                }
                mListFragment = fragment;
                break;
            }

		case ContactsRequest.ACTION_MULTI_PICK_ALL_IN_ONE_DATA: { // add for
																	// private
																	// sms
																	// filters
			AllInOneDataPickerFragment fragment = new AllInOneDataPickerFragment();
			if (getIntent().hasExtra("select_group_member")) {
				long groupId = (long) getIntent().getLongExtra(
						"select_group_member", -1);
				fragment.setFilter(ContactListFilter.createGroupFilter(groupId));
			} else {
				fragment.setFilter(ContactListFilter
						.createAccountFilterForPrivatePeople(
								PhoneAccountType.PRIVATE_ACCOUNT_TYPE,
								getString(R.string.show_privatecontacts), null,
								null));
			}
			fragment.setMultiPickerSupported(true);
			fragment.setCascadingData(mRequest.getCascadingData());
			mListFragment = fragment;
			break;
		}

            case ContactsRequest.ACTION_MULTI_PICK_PHONE: {
                PhoneNumberPickerFragment fragment = new PhoneNumberPickerFragment();
                fragment.setMultiPickerSupported(true);
                fragment.setFilter(mFilter);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_MULTI_PICK_EMAIL: {
                EmailAddressPickerFragment fragment = new EmailAddressPickerFragment();
                fragment.setMultiPickerSupported(true);
                fragment.setFilter(mFilter);
                mListFragment = fragment;
                break;
            }
            case ContactsRequest.ACTION_VIEW_CONTACT_SELECT_FAST_CALL: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setFilter(ContactListFilter
						.createAccountFilterForPrivatePeople(
								PhoneAccountType.PRIVATE_ACCOUNT_TYPE,
								getString(R.string.show_privatecontacts), null,
								null));
                mListFragment = fragment;
                break;
            }
            /**
            * @}
            */

            default:
                /*
                * SPRD:
                *   Bug 296790
                *   CRASH: com.android.contacts (java.lang.IllegalStateException: Invalid action code: 10).
                *
                * @orig
                * throw new IllegalStateException("Invalid action code: " + mActionCode);
                *
                * @{
                */
                if (!isFinishing()) {
                    Log.e(TAG, "Invalid action code: " + mActionCode);
                    finish();
                    return;
                }
                /*
                * @}
                */
        }

        // Setting compatibility is no longer needed for PhoneNumberPickerFragment since that logic
        // has been separated into LegacyPhoneNumberPickerFragment.  But we still need to set
        // compatibility for other fragments.
        mListFragment.setLegacyCompatibilityMode(mRequest.isLegacyCompatibilityMode());
        mListFragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);

        getFragmentManager().beginTransaction()
                .replace(R.id.list_container, mListFragment)
                .commitAllowingStateLoss();
    }

    private PhoneNumberPickerFragment getPhoneNumberPickerFragment(ContactsRequest request) {
        if (mRequest.isLegacyCompatibilityMode()) {
            return new LegacyPhoneNumberPickerFragment();
        } else {
            return new PhoneNumberPickerFragment();
        }
    }

    public void setupActionListener() {
        /**
        * SPRD:
        * 
        *
        * 
        * if (mListFragment instanceof ContactPickerFragment) {
            ((ContactPickerFragment) mListFragment).setOnContactPickerActionListener(
                    new ContactPickerActionListener());
        } else if (mListFragment instanceof PhoneNumberPickerFragment) {
            ((PhoneNumberPickerFragment) mListFragment).setOnPhoneNumberPickerActionListener(
                    new PhoneNumberPickerActionListener());
        } else if (mListFragment instanceof PostalAddressPickerFragment) {
            ((PostalAddressPickerFragment) mListFragment).setOnPostalAddressPickerActionListener(
                    new PostalAddressPickerActionListener());
        } else if (mListFragment instanceof EmailAddressPickerFragment) {
            ((EmailAddressPickerFragment) mListFragment).setOnEmailAddressPickerActionListener(
                    new EmailAddressPickerActionListener());
        } else {
            throw new IllegalStateException("Unsupported list fragment type: " + mListFragment);
        }
        * 
        * @{
        */
        if (mListFragment instanceof ContactPickerFragment) {
            if (mListFragment.isMultiPickerSupported()) {
                ((ContactPickerFragment) mListFragment).setOnContactMultiPickerActionListener(
                        new ContactMultiPickerActionListener());
            } else {
                ((ContactPickerFragment) mListFragment).setOnContactPickerActionListener(
                        new ContactPickerActionListener());     
            }
        } else if (mListFragment instanceof PhoneNumberPickerFragment) {
            if (mListFragment.isMultiPickerSupported()) {
                ((PhoneNumberPickerFragment) mListFragment).setOnPhoneNumberMultiPickerActionListener(
                        new PhoneNumberMultiPickerActionListener());
            } else {
                ((PhoneNumberPickerFragment) mListFragment).setOnPhoneNumberPickerActionListener(
                        new PhoneNumberPickerActionListener());
            }
        } else if (mListFragment instanceof PostalAddressPickerFragment) {
            ((PostalAddressPickerFragment) mListFragment).setOnPostalAddressPickerActionListener(
                    new PostalAddressPickerActionListener());
        } else if (mListFragment instanceof EmailAddressPickerFragment) {
            if (mListFragment.isMultiPickerSupported()) {
                ((EmailAddressPickerFragment) mListFragment).setOnEmailAddressMultiPickerActionListener(
                        new EmailAddressMultiPickerActionListener());
            } else {
                ((EmailAddressPickerFragment) mListFragment).setOnEmailAddressPickerActionListener(
                        new EmailAddressPickerActionListener());
            }
        } else if (mListFragment instanceof AllInOneDataPickerFragment) {
            if (mListFragment.isMultiPickerSupported()) {
                ((AllInOneDataPickerFragment) mListFragment).setOnAllInOneDataMultiPickerActionListener(
                        new AllInOneDataMultiPickerActionListener());
            } else {
                ((AllInOneDataPickerFragment) mListFragment).setOnAllInOneDataPickerActionListener(
                        new AllInOneDataPickerActionListener());
            }
        } else {
            throw new IllegalStateException("Unsupported list fragment type: " + mListFragment);
        }
        /**
        * @}
        */
    }

    private final class ContactPickerActionListener implements OnContactPickerActionListener {
        @Override
        public void onCreateNewContactAction() {
            startCreateNewContactActivity();
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
            /*
            * SPRD:
            *
            * @orig
            *  Bundle extras = getIntent().getExtras();
            *   if (launchAddToContactDialog(extras)) {
            *    // Show a confirmation dialog to add the value(s) to the existing contact.
            *    Intent intent = new Intent(ContactSelectionActivity.this,
            *            ConfirmAddDetailActivity.class);
            *    intent.setData(contactLookupUri);
            *    if (extras != null) {
            *        // First remove name key if present because the dialog does not support name
            *        // editing. This is fine because the user wants to add information to an
            *        // existing contact, who should already have a name and we wouldn't want to
            *        // override the name.
            *        extras.remove(Insert.NAME);
            *        intent.putExtras(extras);
            *    }

            *    // Wait for the activity result because we want to keep the picker open (in case the
            *    // user cancels adding the info to a contact and wants to pick someone else).
            *    startActivityForResult(intent, SUBACTIVITY_ADD_TO_EXISTING_CONTACT);
            *    } else {
            *    // Otherwise launch the full contact editor.
            *    startActivityAndForwardResult(new Intent(Intent.ACTION_EDIT, contactLookupUri));
            *    }
            *
            * @{
            */
            startActivityAndForwardResult(new Intent(Intent.ACTION_EDIT, contactLookupUri));
            /*
            * @}
            */
        }

        @Override
        public void onPickContactAction(Uri contactUri) {
            returnPickerResult(contactUri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }

        /**
         * Returns true if is a single email or single phone number provided in the {@link Intent}
         * extras bundle so that a pop-up confirmation dialog can be used to add the data to
         * a contact. Otherwise return false if there are other intent extras that require launching
         * the full contact editor. Ignore extras with the key {@link Insert.NAME} because names
         * are a special case and we typically don't want to replace the name of an existing
         * contact.
         */
        private boolean launchAddToContactDialog(Bundle extras) {
            if (extras == null) {
                return false;
            }

            // Copy extras because the set may be modified in the next step
            Set<String> intentExtraKeys = Sets.newHashSet();
            intentExtraKeys.addAll(extras.keySet());

            // Ignore name key because this is an existing contact.
            if (intentExtraKeys.contains(Insert.NAME)) {
                intentExtraKeys.remove(Insert.NAME);
            }

            int numIntentExtraKeys = intentExtraKeys.size();
            if (numIntentExtraKeys == 2) {
                boolean hasPhone = intentExtraKeys.contains(Insert.PHONE) &&
                        intentExtraKeys.contains(Insert.PHONE_TYPE);
                boolean hasEmail = intentExtraKeys.contains(Insert.EMAIL) &&
                        intentExtraKeys.contains(Insert.EMAIL_TYPE);
                return hasPhone || hasEmail;
            } else if (numIntentExtraKeys == 1) {
                return intentExtraKeys.contains(Insert.PHONE) ||
                        intentExtraKeys.contains(Insert.EMAIL);
            }
            // Having 0 or more than 2 intent extra keys means that we should launch
            // the full contact editor to properly handle the intent extras.
            return false;
        }
    }

    private final class PhoneNumberPickerActionListener implements
            OnPhoneNumberPickerActionListener {
        @Override
        public void onPickPhoneNumberAction(Uri dataUri,String number,String mode) {
            returnPickerResult(dataUri);
        }

        @Override
        public void onCallNumberDirectly(String phoneNumber) {
            Log.w(TAG, "Unsupported call.");
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }

        public void onHomeInActionBarSelected() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    private final class PostalAddressPickerActionListener implements
            OnPostalAddressPickerActionListener {
        @Override
        public void onPickPostalAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    private final class EmailAddressPickerActionListener implements
            OnEmailAddressPickerActionListener {
        @Override
        public void onPickEmailAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    public void startActivityAndForwardResult(final Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // Forward extras to the new activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
            /**
            * SPRD:
            * 
            * @{
            */
            Log.d(TAG, "tian.........account");
            ArrayList<AccountWithDataSet> tmp = getAccountsForMimeType(extras);
            intent.putParcelableArrayListExtra(Constants.INTENT_KEY_ACCOUNTS, tmp);
            /**
            * @}
            */
        }
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mListFragment.setQueryString(newText, true);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        /**
        * SPRD:
        * 
        *
        * Original Android code:
        * return false;
        * 
        * @{
        */
        if (mListFragment.isSearchMode() && mSearchView != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
            mSearchView.clearFocus();
            return true;
        } else {
            return false;
        }
        /**
        * @}
        */
    }

    @Override
    public boolean onClose() {
        if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }
        return true;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        switch (view.getId()) {
            case R.id.search_view: {
                if (hasFocus) {
                    showInputMethod(mSearchView.findFocus());
                }
            }
        }
    }

    public void returnPickerResult(Uri data) {
        Intent intent = new Intent();
        intent.setData(data);
        returnPickerResult(intent);
    }

    public void returnPickerResult(Intent intent) {
        /**
        * SPRD:
        * 
        * @{
        */
        if (mIsFilterChanged) {
            intent.putExtra(FILTER_CHANG, mIsFilterChanged);
        }
        /**
        * @}
        */
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.new_contact: {
                startCreateNewContactActivity();
                break;
            }
        }
    }

    private void startCreateNewContactActivity() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        intent.putExtra(ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
        startActivityAndForwardResult(intent);
    }

    private void showInputMethod(View view) {
        final InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (!imm.showSoftInput(view, 0)) {
                Log.w(TAG, "Failed to show soft input method.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SUBACTIVITY_ADD_TO_EXISTING_CONTACT) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    startActivity(data);
                }
                finish();
            }
        }
        /**
        * SPRD:
        * 
        * @{
        */
        else if (requestCode == SUBACTIVITY_ACCOUNT_FILTER){
            AccountFilterUtil.handleAccountFilterResult(
                    mContactListFilterController, resultCode, data);
            mIsFilterChanged = true;
        }
        /**
        * @}
        */
    }





    /**
    * SPRD:
    * 
    * @{
    */
    public static final String MOVE_GROUP_COMPLETE = "move_group_member_completed";
    public static final String FILTER_CHANG = "filter_changed";

    private static final String KEY_FILTER = "mFilter";
    private static final String KEY_FILTER_CHANG = "mIsFilterChanged";

    private static final int SUBACTIVITY_ACCOUNT_FILTER = 6;
    private static final int SUBACTIVITY_BATCH_DELETE = 7;

    private static final String[] ACCOUNT_PROJECTION = new String[] {
            Contacts.DISPLAY_ACCOUNT_TYPE,
            Contacts.DISPLAY_ACCOUNT_NAME
    };
    private static final String[] DATA_PROJECTION = new String[] {
            Data._ID,
            Data.DATA1
    };

    private ContactListFilterController mContactListFilterController;
    private ContactListFilter mPermanentAccountFilter = null;
    private ContactListFilter mFilter = null;
    private ContactListFilter mGroupFilter = null;
    private Button mDoneMenuItem;
    private int mDoneMenuDisableColor = Color.WHITE;
    private Long mSelectDataId;
    private boolean mIsAllaccount = false;
    private int mAccountNum = 0;
    private AccountTypeManager mAccountManager;
    private boolean mIsFilterChanged = false;

    @Override
    protected void onResume(){
        super.onResume();
        IntentFilter filter = new IntentFilter("com.android.contacts.common.action.SSU");
        mSelecStatusReceiver = new SSUReceiver();
        registerReceiver(mSelecStatusReceiver, filter);
        setDoneMenu(mListFragment.getSelecStatus());

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mSelecStatusReceiver);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        // If we want "Create New Contact" button but there's no such a button
        // in the layout,
        // try showing a menu for it.
        if (!(shouldShowCreateNewContactButton() && mCreateNewContactButton == null)) {
            final MenuItem newContactMenu = menu.findItem(R.id.create_new_contact);
            if (newContactMenu != null) {
                newContactMenu.setVisible(false);
            }
        }
        return true;
    }

    private final class EmailAddressMultiPickerActionListener implements
            OnEmailAddressMultiPickerActionListener {
        public void onPickEmailAddressAction(HashMap<String, String> pairs) {
            returnPickerResult(pairs);
        }

        public void onCancel() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    private final class PhoneNumberMultiPickerActionListener implements
            OnPhoneNumberMultiPickerActionListener {
        public void onPickPhoneNumberAction(HashMap<String, String> pairs) {
            returnPickerResult(pairs);
        }

        public void onCancel() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    private final class AllInOneDataMultiPickerActionListener implements
            OnAllInOneDataMultiPickerActionListener {
        public void onPickAllInOneDataAction(HashMap<String, String> pairs) {
            returnPickerResult(pairs);
        }

        public void onCancel() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    private final class ContactMultiPickerActionListener implements
            OnContactMultiPickerActionListener {
        public void onPickContactAction(ArrayList<String> lookupKeys, ArrayList<String> ids) {
            returnPickerResult(lookupKeys, ids);
        }

        public void onCancel() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    private final class AllInOneDataPickerActionListener implements
            OnAllInOneDataPickerActionListener {
        @Override
        public void onPickAllInOneDataAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }

        public void onHomeInActionBarSelected() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuAdapter menuAdapter = mListFragment.getContextMenuAdapter();
        if (menuAdapter != null) {
            return menuAdapter.onContextItemSelected(item);
        }

        return super.onContextItemSelected(item);
    }

    public void returnPickerResult() {
        Intent intent = new Intent();
        if (mIsFilterChanged) {
            intent.putExtra(FILTER_CHANG, mIsFilterChanged);
        }
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    public void returnPickerResult(ArrayList<String> data, ArrayList<String> data2) {
        Intent intent = getIntent();
        intent.putStringArrayListExtra("result", data);
        intent.putStringArrayListExtra("result_alternative", data2);
        /**
         * SPRD: add iLog Original Android code:
         * 
         * @{
         */
        intent.putExtra("filter", mContactListFilterController.getFilter().accountName);
        /**
         * @}
         */
        Parcel parcel = Parcel.obtain();
        intent.writeToParcel(parcel, 0);
        if (Constants.DEBUG) {
            Log.d(TAG, "returnPickerResult parcel size is" + parcel.dataSize());
        }
        if (parcel.dataSize() > MAX_DATA_SIZE) {
            Toast.makeText(ContactSelectionActivity.this, R.string.transaction_too_large,
                    Toast.LENGTH_LONG).show();
            parcel.recycle();
            return;
        }
        if (parcel != null) {
            parcel.recycle();
        }

        returnPickerResult(intent);
    }

    public void returnPickerResult(HashMap<String, String> data) {
        Intent intent = new Intent();
        if (data.isEmpty()) {
            returnPickerResult();
        } else {
            intent.putExtra("result", data);
            returnPickerResult(intent);
        }
    }

    @Override
    public void onContactListFilterChanged() {
        ContactListFilter filter = mContactListFilterController.getFilter();
        // if (mFilter.equals(filter)) {
        // return;
        // }
        ArrayList<AccountWithDataSet> allAccounts = (ArrayList) mAccountManager
                .getAccounts(false);
        if (mPermanentAccountFilter != null && mAccountNum != allAccounts.size()) {
            // if the account information is changed,reconfigure list fragment
            mAccountNum = allAccounts.size();
            configureListFragment();
        } else {
        	Log.d(TAG, "filter=="+filter.accountType);
            mFilter = filter;
            mListFragment.setFilter(mFilter);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        if (mIsFilterChanged) {
            intent.putExtra(FILTER_CHANG, mIsFilterChanged);
        }
        setResult(RESULT_CANCELED, intent);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (mContactListFilterController != null) {
            mContactListFilterController.removeListener(this);
        }
        super.onDestroy();
    }

    private ArrayList<AccountWithDataSet> getAccountsForMimeType(Bundle extras) {
        ArrayList<AccountWithDataSet> ret = new ArrayList<AccountWithDataSet>();
        List<AccountWithDataSet> accounts = mAccountManager.getAccounts(true);
        Log.i(TAG, "getAccountsForMimeType:" + (extras == null ? " null" : extras.toString())
                + " accounts = " + accounts);
        final int mAllaccountNum = accounts.size();
        for (AccountWithDataSet account : accounts) {
            final ContentValues values = new ContentValues();
            RawContactDelta insert = new RawContactDelta(ValuesDelta.fromAfter(values));
            RawContactModifier.parseExtras(ContactSelectionActivity.this,
                    mAccountManager.getAccountTypeForAccount(account), insert, extras);
            Set<String> supportedMimeTypes = insert.getMimeTypes();
            Log.i(TAG, "supportedMimeTypes:" + supportedMimeTypes);
            supportedMimeTypes.remove(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
            if (!supportedMimeTypes.isEmpty()) {
                ret.add(account);
            }
        }
        if (ret.size() == mAllaccountNum) {
            mIsAllaccount = true;
        }
        Log.i(TAG, "getAccountsForMimeType: the result accounts obtained after treatment is "
                + ret);
        return ret;
    }

    private ContactListFilter getAccountFilterForMimeType(Bundle extras) {
        ArrayList<AccountWithDataSet> tmp = getAccountsForMimeType(extras);
        if (tmp.isEmpty()) {
            return null;
        }
        return ContactListFilter.createAccountsFilter(tmp);
    }

    public ContactListFilter getPermanentFilter() {
        return mPermanentAccountFilter;
    }

    public void setDoneMenu(boolean enabled) {
        if (enabled) {
           // mDoneMenuItem.setEnabled(true);
           // mDoneMenuItem.setTextColor(mDoneMenuDisableColor);
        } else {
           // mDoneMenuItem.setEnabled(false);
           // mDoneMenuItem.setTextColor(getResources().getColor(R.color.action_bar_button_disable_text_color));
        }
    }

    public class SSUReceiver extends BroadcastReceiver{

        public void onReceive(final Context context, final Intent intent) {
            if(mListFragment instanceof ContactEntryListFragment<?>){
                boolean enabled =mListFragment.getSelecStatus();
                setDoneMenu(enabled);
            }
         
        }
    }
    public static class ConfirmBatchDeleteDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity())
                    .setTitle(R.string.batch_delete_confim_title)
                    .setMessage(R.string.batch_delete_confim_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    ((ContactEntryListFragment) getTargetFragment())
                                            .onMultiPickerSelected();
                                }
                            });
            return builder.create();
        }
    }

    /**
    * @}
    */
}
