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

import android.app.ActionBar;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.editor.ContactEditorFragment.SaveMode;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.util.DialogManager;

import java.util.ArrayList;

import android.view.Gravity;
import android.os.IBinder;
import android.view.inputmethod.InputMethodManager;
import com.sprd.contacts.common.util.UniverseUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

public class ContactEditorActivity extends ContactsActivity
        implements DialogManager.DialogShowingViewActivity {
    private static final String TAG = "ContactEditorActivity";

    public static final String ACTION_JOIN_COMPLETED = "joinCompleted";
    public static final String ACTION_SAVE_COMPLETED = "saveCompleted";

    /**
     * Boolean intent key that specifies that this activity should finish itself
     * (instead of launching a new view intent) after the editor changes have been
     * saved.
     */
    public static final String INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED =
            "finishActivityOnSaveCompleted";

    private ContactEditorFragment mFragment;
    private boolean mFinishActivityOnSaveCompleted;

    private DialogManager mDialogManager = new DialogManager(this);

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        // Determine whether or not this activity should be finished after the user is done
        // editing the contact or if this activity should launch another activity to view the
        // contact's details.
        mFinishActivityOnSaveCompleted = intent.getBooleanExtra(
                INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, false);

        // The only situation where action could be ACTION_JOIN_COMPLETED is if the
        // user joined the contact with another and closed the activity before
        // the save operation was completed.  The activity should remain closed then.
        if (ACTION_JOIN_COMPLETED.equals(action)) {
            finish();
            return;
        }

        if (ACTION_SAVE_COMPLETED.equals(action)) {
            finish();
            return;
        }
        /*
        * SPRD:
        *   Bug258352
        *       optimize edit contact layout.
        *
        * @orig
        *      setContentView(R.layout.contact_editor_activity);
        *
        * @{
        */
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            setContentView(R.layout.contact_editor_activity_overlay);
        } else {
            setContentView(R.layout.contact_editor_activity);
        }
        /*
        * @}
        */
        ActionBar actionBar = getActionBar();
        
        /**
         * SPRD:
         * Modify here when porting from 4.1 to 4.3
         * 
         * @{
         */
        if (actionBar != null) {
            // Inflate a custom action bar that contains the "done" button for
            // saving changes
            // to the contact
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = null;
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                customActionBarView = inflater.inflate(
                        R.layout.editor_custom_action_bar_overlay, null);
                Button saveMenuItem = (Button) customActionBarView
                        .findViewById(R.id.save_menu_item_button);
                saveMenuItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mIsSaveButtonClicked || mFragment.mIsSaveFailure == true) {
                            mIsSaveButtonClicked = true;
                            mFragment.mIsSaveFailure = false;
                            mFragment.doSaveAction();
                        }
                    }
                });
                Button cancelMenuItem = (Button) customActionBarView
                        .findViewById(R.id.cancel_menu_item_button);
                cancelMenuItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mFragment.revert();
                    }
                });
            } else {
                customActionBarView = inflater.inflate(
                        R.layout.editor_custom_action_bar, null);
                View saveMenuItem = customActionBarView
                        .findViewById(R.id.save_menu_item);
                saveMenuItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mIsSaveButtonClicked || mFragment.mIsSaveFailure == true) {
                            mIsSaveButtonClicked = true;
                            mFragment.mIsSaveFailure = false;
                            mFragment.doSaveAction();
                        }
                    }
                });
            }
            // Show the custom action bar but hide the home icon and title
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                if (ACTION_EDIT.equals(action)) {
                    actionBar.setTitle(R.string.edit_contact);
                } else {
                    actionBar.setTitle(R.string.menu_newContact);
                }

                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                        | ActionBar.DISPLAY_SHOW_TITLE
                        | ActionBar.DISPLAY_HOME_AS_UP);
                actionBar.setCustomView(customActionBarView,
                        new ActionBar.LayoutParams(
                                ActionBar.LayoutParams.WRAP_CONTENT,
                                ActionBar.LayoutParams.WRAP_CONTENT,
                                Gravity.CENTER_VERTICAL | Gravity.END));

            } else {
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM
                                | ActionBar.DISPLAY_SHOW_HOME
                                | ActionBar.DISPLAY_SHOW_TITLE);
                actionBar.setCustomView(customActionBarView);
            }
        }
        
        /**
         * @}
         */

        mFragment = (ContactEditorFragment) getFragmentManager().findFragmentById(
                R.id.contact_editor_fragment);
        mFragment.setListener(mFragmentListener);
        
        /**
         * SPRD:
         * Modify here when porting from 4.1 to 4.3
         * 
         * @{
         */
        if (savedState == null) {
            Uri uri = Intent.ACTION_EDIT.equals(action) ? getIntent().getData() : null;
            mFragment.load(action, uri, getIntent().getExtras());
        }
        /**
         * @}
         */
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mFragment == null) {
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            mFragment.setIntentExtras(intent.getExtras());
            /**
             * SPRD:
             * bug#117298 add Usim group name length and capacity exception process.
             * 
             * @{
             */
            int errorToastForGroupCreate = 
                    intent.getIntExtra(ContactSaveService.EXTRA_ERROR_TOAST, 0);
            if (errorToastForGroupCreate != 0) {
                Toast.makeText(this,errorToastForGroupCreate, Toast.LENGTH_SHORT)
                .show();
            }
            /**
             * @}
             */
        } else if (ACTION_SAVE_COMPLETED.equals(action)) {
            /**
             * SPRD:
             * Modify here when porting from 4.1 to 4.3.
             * stable sim importing,add phonebook exception process
             * 
             * @{
             */
            mFragment.onSaveCompleted(true,
                    intent.getIntExtra(ContactEditorFragment.SAVE_MODE_EXTRA_KEY, SaveMode.CLOSE),
                    intent.getBooleanExtra(ContactSaveService.EXTRA_SAVE_SUCCEEDED, false),
                    intent.getData(),intent.getIntExtra(ContactSaveService.EXTRA_ERROR_TOAST, 0));
            /**
             * @}
             */
        } else if (ACTION_JOIN_COMPLETED.equals(action)) {
            mFragment.onJoinCompleted(intent.getData());
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogManager.isManagedId(id)) return mDialogManager.onCreateDialog(id, args);

        // Nobody knows about the Dialog
        Log.w(TAG, "Unknown dialog requested, id: " + id + ", args: " + args);
        return null;
    }

    @Override
    public void onBackPressed() {
        /**
         * SPRD:
         * Modify here when porting from 4.1 to 4.3.
         * Original Android code:
         * mFragment.save(SaveMode.CLOSE);
         * @{
         */
        mFragment.revert();
        /**
         * @}
         */
    }

    private final ContactEditorFragment.Listener mFragmentListener =
            new ContactEditorFragment.Listener() {
        @Override
        public void onReverted() {
            finish();
        }

        @Override
        public void onSaveFinished(Intent resultIntent) {
            /**
             * SPRD:
             * Modify here when porting from 4.1 to 4.3.
             * @{
             */
            setResult(resultIntent == null ? RESULT_CANCELED : RESULT_OK, resultIntent);
            hideSoftKeyboard();
            finish();
            /**
             * @}
             */
        }

        @Override
        public void onContactSplit(Uri newLookupUri) {
            finish();
        }

        @Override
        public void onContactNotFound() {
            finish();
        }

        @Override
        public void onEditOtherContactRequested(
                Uri contactLookupUri, ArrayList<ContentValues> values) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            intent.putExtra(ContactEditorFragment.INTENT_EXTRA_ADD_TO_DEFAULT_DIRECTORY, "");

            // Pass on all the data that has been entered so far
            if (values != null && values.size() != 0) {
                intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, values);
            }

            startActivity(intent);
            finish();
        }

        @Override
        public void onCustomCreateContactActivityRequested(AccountWithDataSet account,
                Bundle intentExtras) {
            final AccountTypeManager accountTypes =
                    AccountTypeManager.getInstance(ContactEditorActivity.this);
            final AccountType accountType = accountTypes.getAccountType(
                    account.type, account.dataSet);

            Intent intent = new Intent();
            intent.setClassName(accountType.syncAdapterPackageName,
                    accountType.getCreateContactActivityClassName());
            intent.setAction(Intent.ACTION_INSERT);
            intent.setType(Contacts.CONTENT_ITEM_TYPE);
            if (intentExtras != null) {
                intent.putExtras(intentExtras);
            }
            intent.putExtra(RawContacts.ACCOUNT_NAME, account.name);
            intent.putExtra(RawContacts.ACCOUNT_TYPE, account.type);
            intent.putExtra(RawContacts.DATA_SET, account.dataSet);
            intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            startActivity(intent);
            finish();
        }

        @Override
        public void onCustomEditContactActivityRequested(AccountWithDataSet account,
                Uri rawContactUri, Bundle intentExtras, boolean redirect) {
            final AccountTypeManager accountTypes =
                    AccountTypeManager.getInstance(ContactEditorActivity.this);
            final AccountType accountType = accountTypes.getAccountType(
                    account.type, account.dataSet);

            Intent intent = new Intent();
            intent.setClassName(accountType.syncAdapterPackageName,
                    accountType.getEditContactActivityClassName());
            intent.setAction(Intent.ACTION_EDIT);
            intent.setData(rawContactUri);
            if (intentExtras != null) {
                intent.putExtras(intentExtras);
            }

            if (redirect) {
                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                startActivity(intent);
                finish();
            } else {
                startActivity(intent);
            }
        }
    };

    @Override
    public DialogManager getDialogManager() {
        return mDialogManager;
    }
    
    /**
     * SPRD:
     * 
     * @{
     */
    public static final String ACTION_EDIT = "android.intent.action.EDIT";
    public boolean mIsSaveButtonClicked;
    
    @Override
    protected void onResume(){
        mIsSaveButtonClicked =false;
        super.onResume();
    }
    
    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager)
                ContactEditorActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager == null) {
            return;
        }

        View currentFocus = ContactEditorActivity.this.getCurrentFocus();
        if (currentFocus == null) {
            return;
        }

        IBinder token = currentFocus.getWindowToken();
        if (token == null) {
            return;
        }
        inputMethodManager.hideSoftInputFromWindow(token, 0);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                    onBackPressed();
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * @}
     */
}
