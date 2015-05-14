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

package com.android.contacts.activities;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.editor.ContactEditorFragment.SaveMode;
import com.android.contacts.group.GroupEditorFragment;
import com.android.contacts.util.DialogManager;
import com.android.contacts.util.PhoneCapabilityTester;
import com.sprd.contacts.common.util.UniverseUtils;

public class GroupEditorActivity extends ContactsActivity
        implements DialogManager.DialogShowingViewActivity {

    private static final String TAG = "GroupEditorActivity";

    public static final String ACTION_SAVE_COMPLETED = "saveCompleted";
    public static final String ACTION_ADD_MEMBER_COMPLETED = "addMemberCompleted";
    public static final String ACTION_REMOVE_MEMBER_COMPLETED = "removeMemberCompleted";
    public static final String KEY_SAVE_BUTTON_CLICKED = "saveButtonClicked";

    private GroupEditorFragment mFragment;

    private DialogManager mDialogManager = new DialogManager(this);

    private boolean mBackPressed;
    private boolean mIsSaveButtonClicked;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState !=null) {
            mIsSaveButtonClicked = savedState.getBoolean(KEY_SAVE_BUTTON_CLICKED);
        }
        String action = getIntent().getAction();

        if (ACTION_SAVE_COMPLETED.equals(action)) {
            finish();
            return;
        }

        setContentView(R.layout.group_editor_activity);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Inflate a custom action bar that contains the "done" button for saving changes
            // to the group
            LayoutInflater inflater = (LayoutInflater) getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            /**
             * SPRD: The following code is added by sprdUUI.
             * Original Android code:
             *  View customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar, null);
             *  View saveMenuItem = customActionBarView.findViewById(R.id.save_menu_item);
             *  saveMenuItem.setOnClickListener(new OnClickListener() {
             *
             *  @Override public void onClick(View v) {
             *           mFragment.onDoneClicked();
             *           }
             *    });
             * @{
             */
            View customActionBarView = null;
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar_overlay,
                        null);
                Button saveMenuItem = (Button) customActionBarView
                        .findViewById(R.id.save_menu_item_button);
                saveMenuItem.setPadding(0, 0, 0, 0);
                saveMenuItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mIsSaveButtonClicked){
                            mIsSaveButtonClicked = true;
                            mFragment.onDoneClicked();
                        }
                    }
                });
                Button cancelMenuItem = (Button) customActionBarView
                        .findViewById(R.id.cancel_menu_item_button);
                cancelMenuItem.setPadding(0, 0, 0, 0);
                cancelMenuItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GroupEditorActivity.this.finish();
                    }
                });
            } else {
                customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar, null);
                View saveMenuItem = customActionBarView.findViewById(R.id.save_menu_item);
                saveMenuItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mFragment.onDoneClicked();
                    }
                });
            }
            /**
            * @}
            */

            // Show the custom action bar but hide the home icon and title
            /**
            * SPRD:
            *   The following code is added by sprdUUI.
            *
            * Original Android code:
            *       actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
            *       ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
            *       ActionBar.DISPLAY_SHOW_TITLE);
            *       actionBar.setCustomView(customActionBarView);
            *
            * @{
            */
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                actionBar.setTitle(R.string.menu_newGroup);
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                        | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
                actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL
                                | Gravity.END));

            } else {
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                                ActionBar.DISPLAY_SHOW_TITLE);
                actionBar.setCustomView(customActionBarView);
            }
            /**
            * @}
            */
        }

        mFragment = (GroupEditorFragment) getFragmentManager().findFragmentById(
                R.id.group_editor_fragment);
        mFragment.setListener(mFragmentListener);
        mFragment.setContentResolver(getContentResolver());
        /**
        * SPRD: The following code is added by sprd
        *
        * @{
        */
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            if (action.equals(Intent.ACTION_EDIT))
                actionBar.setTitle(R.string.editGroup);
        }
        /**
        * @}
        */

        // NOTE The fragment will restore its state by itself after orientation changes, so
        // we need to do this only for a new instance.
        if (savedState == null) {
            Uri uri = Intent.ACTION_EDIT.equals(action) ? getIntent().getData() : null;
            mFragment.load(action, uri, getIntent().getExtras());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SAVE_BUTTON_CLICKED, mIsSaveButtonClicked);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogManager.isManagedId(id)) {
            return mDialogManager.onCreateDialog(id, args);
        } else {
            // Nobody knows about the Dialog
            Log.w(TAG, "Unknown dialog requested, id: " + id + ", args: " + args);
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        // If the change could not be saved, then revert to the default "back" button behavior.

        /**
        * SPRD:
        *   Defer the action to make the window properly repaint.
        *
        * Original Android code:
        * if (!mFragment.save()) {
        *
        * @{
        */
        if (!mFragment.save(SaveMode.CLOSE, true)) {
        /**
        * @}
        */
            super.onBackPressed();
        } else {
			mBackPressed = true;
		}
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mFragment == null) {
            return;
        }

        String action = intent.getAction();

        /**
        * SPRD:
        *   Defer the action to make the window properly repaint.
        *
        * Original Android code:
        *         if (ACTION_SAVE_COMPLETED.equals(action)) {
        *           mFragment.onSaveCompleted(true, intent.getData());
         *          }
        *
        * @{
        */
        int errorToast = intent.getIntExtra(ContactSaveService.EXTRA_ERROR_TOAST, 0);
        if (ACTION_SAVE_COMPLETED.equals(action)) {
            mFragment.onSaveCompleted(true, intent.getData(), errorToast);
        }
        /**
        * @}
        */
    }

    private final GroupEditorFragment.Listener mFragmentListener =
            new GroupEditorFragment.Listener() {
        @Override
        public void onGroupNotFound() {
            finish();
        }

        @Override
        public void onReverted() {
            finish();
        }

        @Override
        public void onAccountsNotFound() {
            finish();
        }

        @Override
        public void onSaveFinished(int resultCode, Intent resultIntent) {
            // TODO: Collapse these 2 cases into 1 that will just launch an intent with the VIEW
            // action to see the group URI (when group URIs are supported)
            // For a 2-pane screen, set the activity result, so the original activity (that launched
            // the editor) can display the group detail page
            if (PhoneCapabilityTester.isUsingTwoPanes(GroupEditorActivity.this)) {
                setResult(resultCode, resultIntent);
            } else if (resultIntent != null) {
                // For a 1-pane screen, launch the group detail page
                Intent intent = new Intent(GroupEditorActivity.this, GroupDetailActivity.class);
                intent.setData(resultIntent.getData());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            finish();
        }
    };

    @Override
    public DialogManager getDialogManager() {
        return mDialogManager;
    }

    /**
    * SPRD: The following code is added by sprd.
    *
    * @{
    */
    private BroadcastReceiver groupBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast toast = Toast.makeText(GroupEditorActivity.this,
                    R.string.contacts_group_name_exist, Toast.LENGTH_LONG);
            toast.show();
            if (mBackPressed) {
				finish();
			}
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter groupIntentFilter = new IntentFilter("groupname repeat");
        registerReceiver(groupBroadcastReceiver, groupIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(groupBroadcastReceiver);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.edit_group, menu);
        }
        return true;

    }
    /**
    * @}
    */
}
