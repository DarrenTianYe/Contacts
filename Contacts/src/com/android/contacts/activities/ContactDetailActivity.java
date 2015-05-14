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

package com.android.contacts.activities;

/* XXX: Please trim these imports, make them in
 * order to improve readability of our codes. */
import com.sprd.contacts.BatchOperationService;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.detail.ContactDetailDisplayUtils;
import com.android.contacts.detail.ContactDetailFragment;
import com.android.contacts.detail.ContactDetailLayoutController;
import com.sprd.contacts.detail.SprdContactLoaderFragment;
import com.android.contacts.detail.ContactLoaderFragment.ContactLoaderFragmentListener;
import com.android.contacts.common.editor.SelectAccountDialogFragment;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.R;
import com.android.contacts.common.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.common.util.ContactLoaderUtils;
import com.android.contacts.util.PhoneCapabilityTester;
import com.sprd.contacts.common.util.UniverseUtils;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.detail.ContactDetailDisplayUtils;
import com.android.contacts.detail.ContactDetailFragment;
import com.android.contacts.detail.ContactDetailLayoutController;
import com.android.contacts.detail.ContactLoaderFragment;
import com.android.contacts.detail.ContactLoaderFragment.ContactLoaderFragmentListener;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.util.PhoneCapabilityTester;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import com.sprd.contacts.common.model.account.SimAccountType;
import com.sprd.contacts.common.model.account.USimAccountType;

/**
* SPRD:
*   fix Bug #149916 Landscape, the contact list of id contact number of grey box shows half.
*
* Original Android code:
* public class ContactDetailActivity extends ContactsActivity
* 
* @{
*/
public class ContactDetailActivity extends ContactsActivity implements
SelectAccountDialogFragment.Listener {
/**
* @}
*/
    private static final String TAG = "ContactDetailActivity";

    /** Shows a toogle button for hiding/showing updates. Don't submit with true */
    private static final boolean DEBUG_TRANSITIONS = false;

    private Contact mContactData;
    private Uri mLookupUri;

    private ContactDetailLayoutController mContactDetailLayoutController;
    private SprdContactLoaderFragment mLoaderFragment;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        /**
        * SPRD:
        *    for add to blackList.
        *
        * Original Android code:
        * 
        * 
        * @{
        */
        mContext = ContactDetailActivity.this;
        /**
        * @}
        */
        if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
            // This activity must not be shown. We have to select the contact in the
            // PeopleActivity instead ==> Create a forward intent and finish
            final Intent originalIntent = getIntent();
            Intent intent = new Intent();
            intent.setAction(originalIntent.getAction());
            intent.setDataAndType(originalIntent.getData(), originalIntent.getType());

            // If we are launched from the outside, we should create a new task, because the user
            // can freely navigate the app (this is different from phones, where only the UP button
            // kicks the user into the full app)
            if (shouldUpRecreateTask(intent)) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            } else {
                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                        Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }

            intent.setClass(this, PeopleActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.contact_detail_activity);

        mContactDetailLayoutController = new ContactDetailLayoutController(this, savedState,
                getFragmentManager(), null, findViewById(R.id.contact_detail_container),
                mContactDetailFragmentListener);

        // We want the UP affordance but no app icon.
        // Setting HOME_AS_UP, SHOW_TITLE and clearing SHOW_HOME does the trick.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            /**
             * SPRD: for UUI
             * Original Android code:
             * actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
             * ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_HOME_AS_UP |
             * ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME);
             * actionBar.setTitle("");
             * 
             * @{
             */
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                actionBar.setTitle("");
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                        | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);

            } else {
                actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP
                        | ActionBar.DISPLAY_SHOW_TITLE,
                        ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE
                                | ActionBar.DISPLAY_SHOW_HOME);
                actionBar.setTitle("");
            }
            /**
             * @}
             */

        }

        Log.i(TAG, getIntent().getData().toString());
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
         if (fragment instanceof SprdContactLoaderFragment) {
            mLoaderFragment = (SprdContactLoaderFragment) fragment;
            mLoaderFragment.setListener(mLoaderFragmentListener);
            mLoaderFragment.loadUri(getIntent().getData());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        /**
        * SPRD:
        *   for UUI
        *
        * Original Android code:
        * MenuInflater inflater = getMenuInflater();
        * inflater.inflate(R.menu.star, menu);
        * 
        * @{
        */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.star, menu);
        }
        /**
        * @}
        */
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /**
         * SPRD: for UUI Original Android code: 
         * final MenuItem starredMenuItem =menu.findItem(R.id.menu_star);
         * 
         * @{
         */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            final MenuItem starredMenuItem = menu.findItem(R.id.menu_star);
            AccountWithDataSet accountType = null;
            if (mContactData != null) {
                accountType = mContactData.getAccount();
            }
         /**
         * @}
         */
        starredMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Toggle "starred" state
                // Make sure there is a contact
                if (mLookupUri != null) {
                    // Read the current starred value from the UI instead of using the last
                    // loaded state. This allows rapid tapping without writing the same
                    // value several times
                    final boolean isStarred = starredMenuItem.isChecked();

                    // To improve responsiveness, swap out the picture (and tag) in the UI already
                    ContactDetailDisplayUtils.configureStarredMenuItem(starredMenuItem,
                            mContactData.isDirectoryEntry(), mContactData.isUserProfile(),
                            !isStarred);

                    // Now perform the real save
                    Intent intent = ContactSaveService.createSetStarredIntent(
                            ContactDetailActivity.this, mLookupUri, !isStarred);
                    ContactDetailActivity.this.startService(intent);
                }
                return true;
            }
        });
        // If there is contact data, update the starred state
        if (mContactData != null) {
            ContactDetailDisplayUtils.configureStarredMenuItem(starredMenuItem,
                    mContactData.isDirectoryEntry(), mContactData.isUserProfile(),
                    mContactData.getStarred());
        }
            /**
             * SPRD: for UUI Original Android code:
             * 
             * @{
             */
            if (accountType != null
                    && (SimAccountType.ACCOUNT_TYPE.equals(accountType.type) || USimAccountType.ACCOUNT_TYPE
                            .equals(accountType.type))) {
                starredMenuItem.setVisible(false);
            }
            /**
             * add by xuhong.tian
             */
           
    		if (mContactData != null && mContactData.getAccount() != null
    				&& (mContactData.getDetailUserImportType() == RawContacts.CONTACTS_BY_EDITE
    						|| mContactData.getDetailUserImportType() == RawContacts.CONTACTS_BY_PCTOOLS
    						|| "sprd.com.android.account.usim"
    								.equals(accountType.type) || "sprd.com.android.account.sim"
    							.equals(accountType.type))) {
    			starredMenuItem.setVisible(false);
    			 Log.d(TAG, "mContactData.getDetailUserImportType()="+mContactData.getDetailUserImportType()+mContactData.getDetailUserImportType()+"accountType.type="+accountType.type);
    	            
    		}
        }
            /**
            * @}
            */
       
        
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // First check if the {@link ContactLoaderFragment} can handle the key
        if (mLoaderFragment != null && mLoaderFragment.handleKeyDown(keyCode)) return true;

        // Otherwise find the correct fragment to handle the event
        FragmentKeyListener mCurrentFragment = mContactDetailLayoutController.getCurrentPage();
        if (mCurrentFragment != null && mCurrentFragment.handleKeyDown(keyCode)) return true;

        // In the last case, give the key event to the superclass.
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mContactDetailLayoutController != null) {
            mContactDetailLayoutController.onSaveInstanceState(outState);
        }
    }

    private final ContactLoaderFragmentListener mLoaderFragmentListener =
            new ContactLoaderFragmentListener() {
        @Override
        public void onContactNotFound() {
            finish();
        }

        @Override
        public void onDetailsLoaded(final Contact result) {
            if (result == null) {
                return;
            }
            // Since {@link FragmentTransaction}s cannot be done in the onLoadFinished() of the
            // {@link LoaderCallbacks}, then post this {@link Runnable} to the {@link Handler}
            // on the main thread to execute later.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // If the activity is destroyed (or will be destroyed soon), don't update the UI
                    if (isFinishing()) {
                        return;
                    }
                    mContactData = result;
                    mLookupUri = result.getLookupUri();
                    invalidateOptionsMenu();
                    setupTitle();
                    mContactDetailLayoutController.setContactData(mContactData);
                }
            });
        }

        @Override
        public void onEditRequested(Uri contactLookupUri) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            intent.putExtra(
                    ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
            // Don't finish the detail activity after launching the editor because when the
            // editor is done, we will still want to show the updated contact details using
            // this activity.
            startActivity(intent);
        }

        @Override
        public void onDeleteRequested(Uri contactUri) {
            ContactDeletionInteraction.start(ContactDetailActivity.this, contactUri, true);
        }

                 /* @{
                 */
                @Override
                public void onCopyRequested(final String lookupKey) {
                    Bundle args = new Bundle();
                    if (mContactData != null) {
                        args.putParcelable("account", mContactData.getAccount());
                    }
                    SelectAccountDialogFragment.show(getFragmentManager(),
                            R.string.copy_to,
                            AccountListFilter.ACCOUNTS_CONTACT_WRITABLE,
                            args);
                }

                @Override
                public void onNotFilterRequested(ArrayList<String> phones, String name) {
                    // remove all phones to blacklist
                    for (int i = 0; i < phones.size(); i++) {
                        String phone = phones.get(i);
                        if (ContactLoaderUtils.CheckIsBlackNumber(mContext, phone)) {
                            if (!ContactLoaderUtils.deleteFromBlockList(mContext, phone)) {
                                Toast.makeText(ContactDetailActivity.this,
                                        R.string.failed_removeFromBlacklist,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }
                    Toast.makeText(ContactDetailActivity.this,
                            R.string.success_removeFromBlacklist,
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFilterRequested(final ArrayList<String> phones, String name) {
                    try {
                        for (int i = 0; i < phones.size(); i++) {
                            String phone = phones.get(i);
                            if (!ContactLoaderUtils.CheckIsBlackNumber(mContext, phone)) {

                                if (!ContactLoaderUtils.putToBlockList(mContext, phone, BLOCK_ALL,
                                        name)) {
                                    Toast.makeText(ContactDetailActivity.this,
                                            R.string.failed_addToBlacklist,
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        }
                        Toast.makeText(ContactDetailActivity.this, R.string.success_addToBlacklist,
                                Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(ContactDetailActivity.this, R.string.failed_addToBlacklist,
                                Toast.LENGTH_SHORT).show();
                    }
                }
                /**
                 * @}
                 */
    };

    /**
     * Setup the activity title and subtitle with contact name and company.
     */
    private void setupTitle() {
        CharSequence displayName = ContactDetailDisplayUtils.getDisplayName(this, mContactData);
        String company =  ContactDetailDisplayUtils.getCompany(this, mContactData);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(displayName);
        actionBar.setSubtitle(company);

        final StringBuilder talkback = new StringBuilder();
        if (!TextUtils.isEmpty(displayName)) {
            talkback.append(displayName);
        }
        if (!TextUtils.isEmpty(company)) {
            if (talkback.length() != 0) {
                talkback.append(", ");
            }
            talkback.append(company);
        }

        if (talkback.length() != 0) {
            AccessibilityManager accessibilityManager =
                    (AccessibilityManager) this.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (accessibilityManager.isEnabled()) {
                View decorView = getWindow().getDecorView();
                decorView.setContentDescription(talkback);
                decorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            }
        }
    }

    private final ContactDetailFragment.Listener mContactDetailFragmentListener =
            new ContactDetailFragment.Listener() {
        @Override
        public void onItemClicked(Intent intent) {
            if (intent == null) {
                return;
            }
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity found for intent: " + intent);
            }
        }

        @Override
        public void onCreateRawContactRequested(
                ArrayList<ContentValues> values, AccountWithDataSet account) {
            Toast.makeText(ContactDetailActivity.this, R.string.toast_making_personal_copy,
                    Toast.LENGTH_LONG).show();
            Intent serviceIntent = ContactSaveService.createNewRawContactIntent(
                    ContactDetailActivity.this, values, account,
                    ContactDetailActivity.class, Intent.ACTION_VIEW);
            startService(serviceIntent);

        }
    };

    /**
     * This interface should be implemented by {@link Fragment}s within this
     * activity so that the activity can determine whether the currently
     * displayed view is handling the key event or not.
     */
    public interface FragmentKeyListener {
        /**
         * Returns true if the key down event will be handled by the implementing class, or false
         * otherwise.
         */
        public boolean handleKeyDown(int keyCode);
    }

    /**
     * SPRD:
     * 
     * @{
     */
    private static final int BLOCK_ALL = 7;   //block mms,phone,vt
    private Context mContext;
    @Override
    public void onAccountChosen(AccountWithDataSet dstAccount, Bundle extraArgs) {
        if (mContactData == null) {
            return;
        }

        confirmImport(dstAccount, new String[] {
                Long.toString(mContactData.getContactId())
        });
    }

    @Override
    public void onAccountSelectorCancelled() {

    }

    private void confirmImport(final AccountWithDataSet dstAccount, final String[] ids) {
        if (dstAccount != null
                && (SimAccountType.ACCOUNT_TYPE.equals(dstAccount.type) || USimAccountType.ACCOUNT_TYPE
                        .equals(dstAccount.type))) {
            Bundle args = new Bundle();
            args.putParcelable("accounts", dstAccount);
            args.putStringArray("result_alternative", ids);
            ConfirmCopyDetailContactDialogFragment dialog =
                    new ConfirmCopyDetailContactDialogFragment();
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), null);
        } else {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(ContactDetailActivity.this,
                    BatchOperationService.class));
            intent.putExtra(BatchOperationService.KEY_MODE,
                    BatchOperationService.MODE_START_BATCH_IMPORT_EXPORT);
            intent.putExtra("dst_account", dstAccount);
            intent.putStringArrayListExtra("result_alternative",
                    new ArrayList<String>(Arrays.asList(ids)));
            startService(intent);
        }

    }

    public static class ConfirmCopyDetailContactDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName(getActivity(),
                                    BatchOperationService.class));
                            intent.putExtra(BatchOperationService.KEY_MODE,
                                    BatchOperationService.MODE_START_BATCH_IMPORT_EXPORT);
                            AccountWithDataSet accountData = (AccountWithDataSet) getArguments()
                                    .getParcelable("accounts");
                            String[] ids = (String[]) getArguments().getStringArray(
                                    "result_alternative");
                            intent.putExtra("dst_account", accountData);
                            intent.putStringArrayListExtra("result_alternative",
                                    new ArrayList<String>(Arrays.asList(ids)));
                            getActivity().startService(intent);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.alert_maybe_lost_info)
                    .create();
        }
    }

//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home: {
//                if (UniverseUtils.UNIVERSEUI_SUPPORT) {
//                    onBackPressed();
//                    finish();
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
    /**
     * @}
     */
}
