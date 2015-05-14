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

package com.android.contacts.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.preference.PreferenceActivity;
import android.provider.Contacts.Intents.Insert;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.ProviderStatus;
import android.provider.ContactsContract.QuickContact;
import android.provider.ContactsContract.RawContacts;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.activities.ActionBarAdapter.TabState;
import com.android.contacts.detail.ContactDetailFragment;
import com.android.contacts.detail.ContactDetailLayoutController;
import com.android.contacts.detail.ContactDetailUpdatesFragment;
import com.android.contacts.detail.ContactLoaderFragment;
import com.android.contacts.detail.ContactLoaderFragment.ContactLoaderFragmentListener;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.dialog.ClearFrequentsDialog;
import com.sprd.contacts.group.GroupBrowseListFragmentSprd;
import com.android.contacts.group.GroupBrowseListFragment;
import com.android.contacts.group.GroupBrowseListFragment.OnGroupBrowserActionListener;
import com.sprd.contacts.group.GroupDetailFragmentSprd;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.common.interactions.ImportExportDialogFragment;
import com.android.contacts.list.ContactBrowseListFragment;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.list.ContactTileAdapter.DisplayType;
import com.android.contacts.list.ContactTileFrequentFragment;
import com.android.contacts.list.ContactTileListFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.ContactsUnavailableFragment;
import com.android.contacts.list.DefaultContactBrowseListFragment;
import com.android.contacts.common.list.DirectoryListLoader;
import com.android.contacts.list.OnContactBrowserActionListener;
import com.android.contacts.list.OnContactsUnavailableActionListener;
import com.android.contacts.list.ProviderStatusWatcher;
import com.android.contacts.list.ProviderStatusWatcher.ProviderStatusListener;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.preference.ContactsPreferenceActivity;
import com.android.contacts.preference.DisplayOptionsPreferenceFragment;
import com.android.contacts.common.util.AccountFilterUtil;
import com.android.contacts.util.AccountPromptUtils;
import com.android.contacts.common.util.Constants;
import com.android.contacts.util.DialogManager;
import com.android.contacts.util.HelpUtils;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.common.util.UriUtils;
import com.android.contacts.widget.TransitionAnimationView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
* SPRD:
* 
* @{
*/
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.*;
import android.content.res.Resources;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Parcel;
import android.provider.ContactsContract;
import android.provider.ContactsContract.*;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts.Data;
import android.provider.ContactsContract.Intents.UI;
import android.view.WindowManager;

import com.android.contacts.activities.ActionBarAdapter.TabStateNewUI;
import com.sprd.contacts.BatchOperationService;
import com.android.contacts.ContactsApplication;
import com.android.contacts.common.editor.SelectAccountDialogFragment;
import com.sprd.contacts.list.ContactMoreFragment;
import com.android.contacts.common.model.AccountTypeManager;
import com.sprd.contacts.common.model.account.CardDavAccountType;
import com.sprd.contacts.common.model.account.SimAccountType;
import com.sprd.contacts.common.model.account.USimAccountType;
import com.android.contacts.common.model.account.ExchangeAccountType;
import com.sprd.contacts.util.AccountRestrictionUtils;
import com.android.contacts.common.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.common.util.AccountSelectionUtil;
import com.android.contacts.common.vcard.ExportVCardActivity;
import com.android.contacts.common.vcard.VCardCommonArguments;
import com.android.contacts.common.vcard.VCardService;
import com.sprd.contacts.common.util.UniverseUtils;
import com.sprd.contacts.activities.DeleteGroupActivity;
import com.sprd.contacts.detail.SprdContactLoaderFragment;

import java.lang.Iterable;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import neolink.telephony.PrivateIntents;
import neolink.telephony.PrivateMode;

/**
* @}
*/

/**
 * Displays a list to browse contacts. For xlarge screens, this also displays a detail-pane on
 * the right.
 */


/**
* SPRD:
*   Add "ImportExportDialogFragment.Listener","ContactMoreFragment.Listener","SelectAccountDialogFragment.Listener".
*   when merge from 4.1 to 4.3.
* @{
*/
public class PeopleActivity extends ContactsActivity
        implements View.OnCreateContextMenuListener, ActionBarAdapter.Listener,
        DialogManager.DialogShowingViewActivity,ImportExportDialogFragment.Listener,
        ContactListFilterController.ContactListFilterListener, ProviderStatusListener,
        ContactMoreFragment.Listener, SelectAccountDialogFragment.Listener {
    /**
    * @}
    */

    private static final String TAG = "PeopleActivity";

    private static final int TAB_FADE_IN_DURATION = 500;

    private static final String ENABLE_DEBUG_OPTIONS_HIDDEN_CODE = "debug debug!";

    // These values needs to start at 2. See {@link ContactEntryListFragment}.
    private static final int SUBACTIVITY_NEW_CONTACT = 2;
    private static final int SUBACTIVITY_EDIT_CONTACT = 3;
    private static final int SUBACTIVITY_NEW_GROUP = 4;
    private static final int SUBACTIVITY_EDIT_GROUP = 5;
    private static final int SUBACTIVITY_ACCOUNT_FILTER = 6;

    private final DialogManager mDialogManager = new DialogManager(this);

    private ContactsIntentResolver mIntentResolver;
    private ContactsRequest mRequest;

    private ActionBarAdapter mActionBarAdapter;

    private ContactDetailFragment mContactDetailFragment;

    private SprdContactLoaderFragment mContactDetailLoaderFragment;
    private final ContactDetailLoaderFragmentListener mContactDetailLoaderFragmentListener =
            new ContactDetailLoaderFragmentListener();

    private GroupDetailFragmentSprd mGroupDetailFragment;
    private final GroupDetailFragmentListener mGroupDetailFragmentListener =
            new GroupDetailFragmentListener();

    private ContactTileListFragment.Listener mFavoritesFragmentListener =
            new StrequentContactListFragmentListener();

    private ContactListFilterController mContactListFilterController;

    private ContactsUnavailableFragment mContactsUnavailableFragment;
    private ProviderStatusWatcher mProviderStatusWatcher;
    private ProviderStatusWatcher.Status mProviderStatus;

    private boolean mOptionsMenuContactsAvailable;

    /**
     * Showing a list of Contacts. Also used for showing search results in search mode.
     */
    private DefaultContactBrowseListFragment mAllFragment;
    private ContactTileListFragment mFavoritesFragment;
    private ContactTileFrequentFragment mFrequentFragment;
    private GroupBrowseListFragmentSprd mGroupsFragment;

    private View mFavoritesView;
    private View mBrowserView;
    private TransitionAnimationView mPeopleActivityView;
    private TransitionAnimationView mContactDetailsView;
    private TransitionAnimationView mGroupDetailsView;

    /** ViewPager for swipe, used only on the phone (i.e. one-pane mode) */
    private ViewPager mTabPager;
    private TabPagerAdapter mTabPagerAdapter;
    private final TabPagerListener mTabPagerListener = new TabPagerListener();

    private ContactDetailLayoutController mContactDetailLayoutController;

    private boolean mEnableDebugMenuOptions;

    private final Handler mHandler = new Handler();

    /**
     * True if this activity instance is a re-created one.  i.e. set true after orientation change.
     * This is set in {@link #onCreate} for later use in {@link #onStart}.
     */
    private boolean mIsRecreatedInstance;

    /**
     * If {@link #configureFragments(boolean)} is already called.  Used to avoid calling it twice
     * in {@link #onStart}.
     * (This initialization only needs to be done once in onStart() when the Activity was just
     * created from scratch -- i.e. onCreate() was just called)
     */
    private boolean mFragmentInitialized;

    /**
     * Whether or not the current contact filter is valid or not. We need to do a check on
     * start of the app to verify that the user is not in single contact mode. If so, we should
     * dynamically change the filter, unless the incoming intent specifically requested a contact
     * that should be displayed in that mode.
     */
    private boolean mCurrentFilterIsValid;

    /**
     * This is to disable {@link #onOptionsItemSelected} when we trying to stop the activity.
     */
    private boolean mDisableOptionItemSelected;

    /** Sequential ID assigned to each instance; used for logging */
    private final int mInstanceId;
    private static final AtomicInteger sNextInstanceId = new AtomicInteger();

    public PeopleActivity() {
        mInstanceId = sNextInstanceId.getAndIncrement();
        mIntentResolver = new ContactsIntentResolver(this);
        mProviderStatusWatcher = ProviderStatusWatcher.getInstance(this);
    }

    @Override
    public String toString() {
        // Shown on logcat
        return String.format("%s@%d", getClass().getSimpleName(), mInstanceId);
    }

    public boolean areContactsAvailable() {
        /**
        * SPRD:
        * 
        * @{
        */
        return (mProviderStatus != null)
                && (mProviderStatus.status == ProviderStatus.STATUS_NORMAL || mProviderStatus.status == ProviderStatus.STATUS_IMPORTING);
//        return (mProviderStatus != null)
//                && mProviderStatus.status == ProviderStatus.STATUS_NORMAL;
        /**
        * @}
        */
    }

    private boolean areContactWritableAccountsAvailable() {
        return ContactsUtils.areContactWritableAccountsAvailable(this);
    }

    private boolean areGroupWritableAccountsAvailable() {
        return ContactsUtils.areGroupWritableAccountsAvailable(this);
    }

    /**
     * Initialize fragments that are (or may not be) in the layout.
     *
     * For the fragments that are in the layout, we initialize them in
     * {@link #createViewsAndFragments(Bundle)} after inflating the layout.
     *
     * However, there are special fragments which may not be in the layout, so we have to do the
     * initialization here.
     * The target fragments are:
     * - {@link ContactDetailFragment} and {@link ContactDetailUpdatesFragment}:  They may not be
     *   in the layout depending on the configuration.  (i.e. portrait)
     * - {@link ContactsUnavailableFragment}: We always create it at runtime.
     */
    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactDetailFragment) {
            mContactDetailFragment = (ContactDetailFragment) fragment;
        } else if (fragment instanceof ContactsUnavailableFragment) {
            mContactsUnavailableFragment = (ContactsUnavailableFragment)fragment;
            mContactsUnavailableFragment.setOnContactsUnavailableActionListener(
                    new ContactsUnavailableFragmentListener());
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "PeopleActivity.onCreate start");
        }
        super.onCreate(savedState);

        if (!processIntent(false)) {
            finish();
            return;
        }
        /**
        * SPRD:
        * 
        * @{
        */
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        /**
        * @}
        */
        mContactListFilterController = ContactListFilterController.getInstance(this);
        mContactListFilterController.checkFilterValidity(false);
        mContactListFilterController.addListener(this);

        /**
         * SPRD:
         *   Defer the UUI actions to save the time for Activity initialized.
         *   PS: To configure action bar should be placed at front of createViewsAndFragments.
         * @{
         */
        if(UniverseUtils.UNIVERSEUI_SUPPORT){
            int tabHeight = (int) getResources().getDimensionPixelSize(R.dimen.universe_ui_tab_height);
            getActionBar().setAlternativeTabStyle(true);
            getActionBar().setTabHeight(tabHeight);
        }
        /**
        * @}
        */

        mProviderStatusWatcher.addListener(this);
        /**
        * SPRD:
        *       Bug 257722 When a lot of contacts, the system switches to the English
        *       language, contact list updates to display abnormal.
        *
        * @{
        */
        mProviderStatusWatcher.start();
        /**
        * @}
        */

        mIsRecreatedInstance = (savedState != null);
        createViewsAndFragments(savedState);
        getWindow().setBackgroundDrawableResource(R.color.background_primary);
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "PeopleActivity.onCreate finish");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (!processIntent(true)) {
            finish();
            return;
        }
        /**
         * SPRD:
         *   
         * Original Android code:
         * mActionBarAdapter.initialize(null, mRequest);
         * @{
         */
        if(mActionBarAdapter != null){
            mActionBarAdapter.initialize(null, mRequest);
        }
        /**
         * @}
         */


        /**
         * SPRD:
         *   To re-initialize mContactListFilterController if could not created at
         * this activity initialized.
         *
         * @{
         */
        if (mContactListFilterController == null) {
            mContactListFilterController = ContactListFilterController.getInstance(this);
            mContactListFilterController.checkFilterValidity(false);
            mContactListFilterController.addListener(this);
        }
        /**
        * @}
        */
        
        mContactListFilterController.checkFilterValidity(false);
        mCurrentFilterIsValid = true;

        // Re-configure fragments.
        configureFragments(true /* from request */);
        invalidateOptionsMenuIfNeeded();
    }

    /**
     * Resolve the intent and initialize {@link #mRequest}, and launch another activity if redirect
     * is needed.
     *
     * @param forNewIntent set true if it's called from {@link #onNewIntent(Intent)}.
     * @return {@code true} if {@link PeopleActivity} should continue running.  {@code false}
     *         if it shouldn't, in which case the caller should finish() itself and shouldn't do
     *         farther initialization.
     */
    private boolean processIntent(boolean forNewIntent) {
        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, this + " processIntent: forNewIntent=" + forNewIntent
                    + " intent=" + getIntent() + " request=" + mRequest);
        }
        if (!mRequest.isValid()) {
            setResult(RESULT_CANCELED);
            return false;
        }

        Intent redirect = mRequest.getRedirectIntent();
        if (redirect != null) {
            // Need to start a different activity
            startActivity(redirect);
            return false;
        }

        if (mRequest.getActionCode() == ContactsRequest.ACTION_VIEW_CONTACT
                && !PhoneCapabilityTester.isUsingTwoPanes(this)) {
            redirect = new Intent(this, ContactDetailActivity.class);
            redirect.setAction(Intent.ACTION_VIEW);
            redirect.setData(mRequest.getContactUri());
            startActivity(redirect);
            return false;
        }
        return true;
    }

    private void createViewsAndFragments(Bundle savedState) {
        /**
        * SPRD:
        * Original Android code:
        *   setContentView(R.layout.people_activity);
        * @{
        */
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            setContentView(R.layout.people_activity_overlay);
        } else {
            setContentView(R.layout.people_activity);
        }
        /**
        * @}
        */

        final FragmentManager fragmentManager = getFragmentManager();

        // Hide all tabs (the current tab will later be reshown once a tab is selected)
        final FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Prepare the fragments which are used both on 1-pane and on 2-pane.
        final boolean isUsingTwoPanes = PhoneCapabilityTester.isUsingTwoPanes(this);
        if (isUsingTwoPanes) {
            mFavoritesFragment = getFragment(R.id.favorites_fragment);
            mAllFragment = getFragment(R.id.all_fragment);
            mGroupsFragment = getFragment(R.id.groups_fragment);
        } else {
            mTabPager = getView(R.id.tab_pager);
            mTabPagerAdapter = new TabPagerAdapter();
            mTabPager.setAdapter(mTabPagerAdapter);
            mTabPager.setOnPageChangeListener(mTabPagerListener);

            final String FAVORITE_TAG = "tab-pager-favorite";
            final String ALL_TAG = "tab-pager-all";
            final String GROUPS_TAG = "tab-pager-groups";
            /**
            * SPRD:
            * 
            * @{
            */
            final String MORE_TAG = "tab-pager-more";
            /**
            * @}
            */

            // Create the fragments and add as children of the view pager.
            // The pager adapter will only change the visibility; it'll never create/destroy
            // fragments.
            // However, if it's after screen rotation, the fragments have been re-created by
            // the fragment manager, so first see if there're already the target fragments
            // existing.
            mFavoritesFragment = (ContactTileListFragment)
                    fragmentManager.findFragmentByTag(FAVORITE_TAG);
            mAllFragment = (DefaultContactBrowseListFragment)
                    fragmentManager.findFragmentByTag(ALL_TAG);
            mGroupsFragment = (GroupBrowseListFragmentSprd)
                    fragmentManager.findFragmentByTag(GROUPS_TAG);
            /**
             * SPRD:
             * 
             * @{
             */
            mMoreFragment = (ContactMoreFragment)
                    fragmentManager.findFragmentByTag(MORE_TAG);
            /**
             * @}
             */

            if (mFavoritesFragment == null) {
                mFavoritesFragment = new ContactTileListFragment();
                mAllFragment = new DefaultContactBrowseListFragment();
                mGroupsFragment = new GroupBrowseListFragmentSprd();
                /**
                 * SPRD:
                 * 
                 * @{
                 */
                if(UniverseUtils.UNIVERSEUI_SUPPORT){
                    mMoreFragment = new ContactMoreFragment();
                }
                /**
                 * @}
                 */

               
                transaction.add(R.id.tab_pager, mGroupsFragment, GROUPS_TAG);
                transaction.add(R.id.tab_pager, mAllFragment, ALL_TAG);
                transaction.add(R.id.tab_pager, mFavoritesFragment, FAVORITE_TAG);
              
                /**
                 * SPRD:
                 * 
                 * @{
                 */
                if(UniverseUtils.UNIVERSEUI_SUPPORT){
                    transaction.add(R.id.tab_pager, mMoreFragment, MORE_TAG);
                }
                /**
                 * @}
                 */
            }
        }

        mFavoritesFragment.setListener(mFavoritesFragmentListener);

        mAllFragment.setOnContactListActionListener(new ContactBrowserActionListener());

        mGroupsFragment.setListener(new GroupBrowserActionListener());

        // Hide all fragments for now.  We adjust visibility when we get onSelectedTabChanged()
        // from ActionBarAdapter.
        transaction.hide(mFavoritesFragment);
        transaction.hide(mAllFragment);
        transaction.hide(mGroupsFragment);
        /**
         * SPRD:
         * 
         * @{
         */
        if(UniverseUtils.UNIVERSEUI_SUPPORT){
            transaction.hide(mMoreFragment);
        }
        /**
         * @}
         */

        if (isUsingTwoPanes) {
            // Prepare 2-pane only fragments/views...

            // Container views for fragments
            mPeopleActivityView = getView(R.id.people_view);
            mFavoritesView = getView(R.id.favorites_view);
            mContactDetailsView = getView(R.id.contact_details_view);
            mGroupDetailsView = getView(R.id.group_details_view);
            mBrowserView = getView(R.id.browse_view);

            // Only favorites tab with two panes has a separate frequent fragment
            if (PhoneCapabilityTester.isUsingTwoPanesInFavorites(this)) {
                mFrequentFragment = getFragment(R.id.frequent_fragment);
                mFrequentFragment.setListener(mFavoritesFragmentListener);
                mFrequentFragment.setDisplayType(DisplayType.FREQUENT_ONLY);
                mFrequentFragment.enableQuickContact(true);
            }

            mContactDetailLoaderFragment = getFragment(R.id.contact_detail_loader_fragment);
            mContactDetailLoaderFragment.setListener(mContactDetailLoaderFragmentListener);

            mGroupDetailFragment = getFragment(R.id.group_detail_fragment);
            mGroupDetailFragment.setListener(mGroupDetailFragmentListener);
            mGroupDetailFragment.setQuickContact(true);

            if (mContactDetailFragment != null) {
                transaction.hide(mContactDetailFragment);
            }
            transaction.hide(mGroupDetailFragment);

            // Configure contact details
            
            /**
             * SPRD:
             * 
             * @{
             */
            mContactDetailLayoutController = new ContactDetailLayoutController(this, savedState,
                    fragmentManager, mContactDetailsView,
                    findViewById(R.id.contact_detail_container),
                    new ContactDetailFragmentListener());
//            mContactDetailLayoutController = new ContactDetailLayoutController(this, savedState,
//                    getFragmentManager(), mContactDetailsView,
//                    findViewById(R.id.contact_detail_container),
//                    new ContactDetailFragmentListener());
            /**
             * @}
             */
        }
        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();

        // Setting Properties after fragment is created
        if (PhoneCapabilityTester.isUsingTwoPanesInFavorites(this)) {
            mFavoritesFragment.enableQuickContact(true);
            mFavoritesFragment.setDisplayType(DisplayType.STARRED_ONLY);
        } else {
            // For 2-pane in All and Groups but not in Favorites fragment, show the chevron
            // for quick contact popup
            mFavoritesFragment.enableQuickContact(isUsingTwoPanes);
            mFavoritesFragment.setDisplayType(DisplayType.STREQUENT);
        }

        // Configure action bar
        mActionBarAdapter = new ActionBarAdapter(this, this, getActionBar(), isUsingTwoPanes);
        mActionBarAdapter.initialize(savedState, mRequest);

        invalidateOptionsMenuIfNeeded();
    }

    @Override
    protected void onStart() {
        if (!mFragmentInitialized) {
            mFragmentInitialized = true;
            /* Configure fragments if we haven't.
             *
             * Note it's a one-shot initialization, so we want to do this in {@link #onCreate}.
             *
             * However, because this method may indirectly touch views in fragments but fragments
             * created in {@link #configureContentView} using a {@link FragmentTransaction} will NOT
             * have views until {@link Activity#onCreate} finishes (they would if they were inflated
             * from a layout), we need to do it here in {@link #onStart()}.
             *
             * (When {@link Fragment#onCreateView} is called is different in the former case and
             * in the latter case, unfortunately.)
             *
             * Also, we skip most of the work in it if the activity is a re-created one.
             * (so the argument.)
             */
            configureFragments(!mIsRecreatedInstance);
        } else if (PhoneCapabilityTester.isUsingTwoPanes(this) && !mCurrentFilterIsValid) {
            // We only want to do the filter check in onStart for wide screen devices where it
            // is often possible to get into single contact mode. Only do this check if
            // the filter hasn't already been set properly (i.e. onCreate or onActivityResult).

            // Since there is only one {@link ContactListFilterController} across multiple
            // activity instances, make sure the filter controller is in sync withthe current
            // contact list fragment filter.
            // TODO: Clean this up. Perhaps change {@link ContactListFilterController} to not be a
            // singleton?
            mContactListFilterController.setContactListFilter(mAllFragment.getFilter(), true);
            mContactListFilterController.checkFilterValidity(true);
            mCurrentFilterIsValid = true;
        }
        super.onStart();
    }

    @Override
    protected void onPause() {
        mOptionsMenuContactsAvailable = false;
        /**
        * SPRD:
        *       Bug 257722 When a lot of contacts, the system switches to the English
        *       language, contact list updates to display abnormal.
        * Original Android code:
        *       mProviderStatusWatcher.stop();
        *
        * @{
        */
//        mProviderStatusWatcher.stop();
        /**
        * @}
        */
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        
        int mode =ContactsApplication.getApplication().mMode;
        
        Log.d(TAG, "mode=="+mode);    
        
        /**
        * SPRD:
        *       Bug 257722 When a lot of contacts, the system switches to the English
        *       language, contact list updates to display abnormal.
        * Original Android code:
        *       mProviderStatusWatcher.start();
        *
        * @{
        */
//        mProviderStatusWatcher.start();
        /**
        * @}
        */
        updateViewConfiguration(true);

        // Re-register the listener, which may have been cleared when onSaveInstanceState was
        // called.  See also: onSaveInstanceState
        mActionBarAdapter.setListener(this);
        mDisableOptionItemSelected = false;
        if (mTabPager != null) {
            mTabPager.setOnPageChangeListener(mTabPagerListener);
        }
        // Current tab may have changed since the last onSaveInstanceState().  Make sure
        // the actual contents match the tab.
        
        /**
        * SPRD:
        * Original Android code:
        * updateFragmentsVisibility();
        * 
        * @{
        */
        if(UniverseUtils.UNIVERSEUI_SUPPORT){
            updateFragmentsVisibilityNewUI();
        } else {
            updateFragmentsVisibility();
        }
        /**
        * @}
        */
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCurrentFilterIsValid = false;
    }

    @Override
    protected void onDestroy() {
        /**
        * SPRD:
        *       Bug 257722 When a lot of contacts, the system switches to the English
        *       language, contact list updates to display abnormal.
        *
        * @{
        */
        mProviderStatusWatcher.stop();
        /**
        * @}
        */
        mProviderStatusWatcher.removeListener(this);

        // Some of variables will be null if this Activity redirects Intent.
        // See also onCreate() or other methods called during the Activity's initialization.
        if (mActionBarAdapter != null) {
            mActionBarAdapter.setListener(null);
        }
        if (mContactListFilterController != null) {
            mContactListFilterController.removeListener(this);
        }

        super.onDestroy();
    }

	private void configureFragments(boolean fromRequest) {
		if (fromRequest) {
			ContactListFilter filter = null;
			int actionCode = mRequest.getActionCode();
			boolean searchMode = mRequest.isSearchMode();
			final int tabToOpen;

			/**
			 * SPRD: Add the code in if{} module for UniverseUI .
			 * 
			 * Original Android code: The code in else{} module;
			 * 
			 * @{
			 */
			if (UniverseUtils.UNIVERSEUI_SUPPORT) {
				switch (actionCode) {
				case ContactsRequest.ACTION_ALL_CONTACTS:
					filter = ContactListFilter.createFilterWithType(
							ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS , getApplicationContext());
					tabToOpen = TabStateNewUI.ALL;
					break;
				case ContactsRequest.ACTION_CONTACTS_WITH_PHONES:
					filter = ContactListFilter
							.createFilterWithType(ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY);
					tabToOpen = TabStateNewUI.ALL;
					break;

				case ContactsRequest.ACTION_FREQUENT:
				case ContactsRequest.ACTION_STREQUENT:
				case ContactsRequest.ACTION_STARRED:
					tabToOpen = TabStateNewUI.FAVORITES;
					break;
				case ContactsRequest.ACTION_VIEW_CONTACT:
					// We redirect this intent to the detail activity on
					// 1-pane, so we don't get
					// here. It's only for 2-pane.
					tabToOpen = TabStateNewUI.ALL;
					break;
				case ContactsRequest.ACTION_GROUP:
					tabToOpen = TabStateNewUI.GROUPS;
					break;
				default:
					tabToOpen = -1;
					break;
				}
				if (tabToOpen != -1) {
					mActionBarAdapter.setCurrentTabNewUI(tabToOpen);
				}
			} else {
				switch (actionCode) {
				case ContactsRequest.ACTION_ALL_CONTACTS:
					filter = ContactListFilter.createFilterWithType(
							ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS, getApplicationContext());
					tabToOpen = TabState.ALL;
					break;
				case ContactsRequest.ACTION_CONTACTS_WITH_PHONES:
					filter = ContactListFilter
							.createFilterWithType(ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY);
					tabToOpen = TabState.ALL;
					break;

				case ContactsRequest.ACTION_FREQUENT:
				case ContactsRequest.ACTION_STREQUENT:
				case ContactsRequest.ACTION_STARRED:
					tabToOpen = TabState.FAVORITES;
					break;
				case ContactsRequest.ACTION_VIEW_CONTACT:
					// We redirect this intent to the detail activity on
					// 1-pane, so we don't get
					// here. It's only for 2-pane.
					Uri currentlyLoadedContactUri = mContactDetailFragment
							.getUri();
					if (currentlyLoadedContactUri != null
							&& !mRequest.getContactUri().equals(
									currentlyLoadedContactUri)) {
						mContactDetailsView.setMaskVisibility(true);
					}
					tabToOpen = TabState.ALL;
					break;
				case ContactsRequest.ACTION_GROUP:
					tabToOpen = TabState.GROUPS;
					break;
				default:
					tabToOpen = -1;
					break;
				}
				if (tabToOpen != -1) {
					mActionBarAdapter.setCurrentTab(tabToOpen);
				}

				if (filter != null) {
					mContactListFilterController.setContactListFilter(filter,
							false);
					searchMode = false;
				}

				if (mRequest.getContactUri() != null) {
					searchMode = false;
				}
				 mActionBarAdapter.setSearchMode(searchMode);
			}
            configureContactListFragmentForRequest();
			configureContactListFragment();
			configureGroupListFragment();

			invalidateOptionsMenuIfNeeded();
		}
	}

    @Override
    public void onContactListFilterChanged() {
        if (mAllFragment == null || !mAllFragment.isAdded()) {
            return;
        }

        mAllFragment.setFilter(mContactListFilterController.getFilter());

        invalidateOptionsMenuIfNeeded();
    }

    private void setupContactDetailFragment(final Uri contactLookupUri) {
        mContactDetailLoaderFragment.loadUri(contactLookupUri);
        invalidateOptionsMenuIfNeeded();
    }

    private void setupGroupDetailFragment(Uri groupUri) {
        // If we are switching from one group to another, do a cross-fade
        if (mGroupDetailFragment != null && mGroupDetailFragment.getGroupUri() != null &&
                !UriUtils.areEqual(mGroupDetailFragment.getGroupUri(), groupUri)) {
            mGroupDetailsView.startMaskTransition(false, -1);
        }
        
        /**
        * SPRD:
        * Original Android code:
        * mGroupDetailFragment.loadGroup(groupUri);
        * 
        * @{
        */
        if (mGroupDetailFragment != null) {
            mGroupDetailFragment.loadGroup(groupUri);
        }
//        mGroupDetailFragment.loadGroup(groupUri);
        /**
        * @}
        */
        
        invalidateOptionsMenuIfNeeded();
    }

    /**
     * Handler for action bar actions.
     */
    @Override
    public void onAction(int action) {
        switch (action) {
            case ActionBarAdapter.Listener.Action.START_SEARCH_MODE:
                // Tell the fragments that we're in the search mode
                configureFragments(false /* from request */);
                /**
                * SPRD:
                * Original Android code:
                * updateFragmentsVisibility();
                * @{
                */
                if(UniverseUtils.UNIVERSEUI_SUPPORT){
                    updateFragmentsVisibilityNewUI();
                } else {
                    updateFragmentsVisibility();
                }              
//                updateFragmentsVisibility();
                /**
                * @}
                */
                
                invalidateOptionsMenu();
                break;
            case ActionBarAdapter.Listener.Action.STOP_SEARCH_MODE:
                setQueryTextToFragment("");
                /**
                 * SPRD:
                 * Original Android code:
                 * updateFragmentsVisibility();
                 * @{
                 */
                if(UniverseUtils.UNIVERSEUI_SUPPORT){
                    updateFragmentsVisibilityNewUI();
                } else {
                    updateFragmentsVisibility();
                }
//                updateFragmentsVisibility();
                /**
                 * @}
                 */
                invalidateOptionsMenu();
                break;
            case ActionBarAdapter.Listener.Action.CHANGE_SEARCH_QUERY:
                final String queryString = mActionBarAdapter.getQueryString();
                setQueryTextToFragment(queryString);
                updateDebugOptionsVisibility(
                        ENABLE_DEBUG_OPTIONS_HIDDEN_CODE.equals(queryString));
                break;
            default:
                throw new IllegalStateException("Unkonwn ActionBarAdapter action: " + action);
        }
    }

    @Override
    public void onSelectedTabChanged() {
        /**
         * SPRD:
         * Original Android code:
         * updateFragmentsVisibility();
         * @{
         */
        if(UniverseUtils.UNIVERSEUI_SUPPORT){
            updateFragmentsVisibilityNewUI();
        } else {
            updateFragmentsVisibility();
        }
//        updateFragmentsVisibility();
        /**
         * @}
         */
    }

    private void updateDebugOptionsVisibility(boolean visible) {
        if (mEnableDebugMenuOptions != visible) {
            mEnableDebugMenuOptions = visible;
            invalidateOptionsMenu();
        }
    }

    /**
     * Updates the fragment/view visibility according to the current mode, such as
     * {@link ActionBarAdapter#isSearchMode()} and {@link ActionBarAdapter#getCurrentTab()}.
     */
    private void updateFragmentsVisibility() {
        int tab = mActionBarAdapter.getCurrentTab();

        // We use ViewPager on 1-pane.
        if (!PhoneCapabilityTester.isUsingTwoPanes(this)) {
            if (mActionBarAdapter.isSearchMode()) {
                mTabPagerAdapter.setSearchMode(true);
            } else {
                // No smooth scrolling if quitting from the search mode.
                final boolean wasSearchMode = mTabPagerAdapter.isSearchMode();
                mTabPagerAdapter.setSearchMode(false);
                if (mTabPager.getCurrentItem() != tab) {
                    mTabPager.setCurrentItem(tab, !wasSearchMode);
                }
            }
            invalidateOptionsMenu();
            showEmptyStateForTab(tab);
            if (tab == TabState.GROUPS) {
                mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
            }
            return;
        }

        // for the tablet...

        // If in search mode, we use the all list + contact details to show the result.
        if (mActionBarAdapter.isSearchMode()) {
            tab = TabState.ALL;
        }

        switch (tab) {
            case TabState.FAVORITES:
                mFavoritesView.setVisibility(View.VISIBLE);
                mBrowserView.setVisibility(View.GONE);
                mGroupDetailsView.setVisibility(View.GONE);
                mContactDetailsView.setVisibility(View.GONE);
                break;
            case TabState.GROUPS:
                mFavoritesView.setVisibility(View.GONE);
                mBrowserView.setVisibility(View.VISIBLE);
                mGroupDetailsView.setVisibility(View.VISIBLE);
                mContactDetailsView.setVisibility(View.GONE);
                mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
                break;
            case TabState.ALL:
                mFavoritesView.setVisibility(View.GONE);
                mBrowserView.setVisibility(View.VISIBLE);
                mContactDetailsView.setVisibility(View.VISIBLE);
                mGroupDetailsView.setVisibility(View.GONE);
                break;
        }
        mPeopleActivityView.startMaskTransition(false, TAB_FADE_IN_DURATION);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        // Note mContactDetailLoaderFragment is an invisible fragment, but we still have to show/
        // hide it so its options menu will be shown/hidden.
        switch (tab) {
            case TabState.FAVORITES:
                showFragment(ft, mFavoritesFragment);
                showFragment(ft, mFrequentFragment);
                hideFragment(ft, mAllFragment);
                hideFragment(ft, mContactDetailLoaderFragment);
                hideFragment(ft, mContactDetailFragment);
                hideFragment(ft, mGroupsFragment);
                hideFragment(ft, mGroupDetailFragment);
                break;
            case TabState.ALL:
                hideFragment(ft, mFavoritesFragment);
                hideFragment(ft, mFrequentFragment);
                showFragment(ft, mAllFragment);
                showFragment(ft, mContactDetailLoaderFragment);
                showFragment(ft, mContactDetailFragment);
                hideFragment(ft, mGroupsFragment);
                hideFragment(ft, mGroupDetailFragment);
                break;
            case TabState.GROUPS:
                hideFragment(ft, mFavoritesFragment);
                hideFragment(ft, mFrequentFragment);
                hideFragment(ft, mAllFragment);
                hideFragment(ft, mContactDetailLoaderFragment);
                hideFragment(ft, mContactDetailFragment);
                showFragment(ft, mGroupsFragment);
                showFragment(ft, mGroupDetailFragment);
                break;
        }
        if (!ft.isEmpty()) {
            ft.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
            // When switching tabs, we need to invalidate options menu, but executing a
            // fragment transaction does it implicitly.  We don't have to call invalidateOptionsMenu
            // manually.
        }
        showEmptyStateForTab(tab);
    }

    private void showEmptyStateForTab(int tab) {
        if (mContactsUnavailableFragment != null) {
            switch (tab) {
                case TabState.FAVORITES:
                    mContactsUnavailableFragment.setMessageText(
                            R.string.listTotalAllContactsZeroStarred, -1);
                    break;
                case TabState.GROUPS:
                    mContactsUnavailableFragment.setMessageText(R.string.noGroups,
                            areGroupWritableAccountsAvailable() ? -1 : R.string.noAccounts);
                    break;
                case TabState.ALL:
                    mContactsUnavailableFragment.setMessageText(R.string.noContacts, -1);
                    break;
            }
        }
    }

    private class TabPagerListener implements ViewPager.OnPageChangeListener {

        // This package-protected constructor is here because of a possible compiler bug.
        // PeopleActivity$1.class should be generated due to the private outer/inner class access
        // needed here.  But for some reason, PeopleActivity$1.class is missing.
        // Since $1 class is needed as a jvm work around to get access to the inner class,
        // changing the constructor to package-protected or public will solve the problem.
        // To verify whether $1 class is needed, javap PeopleActivity$TabPagerListener and look for
        // references to PeopleActivity$1.
        //
        // When the constructor is private and PeopleActivity$1.class is missing, proguard will
        // correctly catch this and throw warnings and error out the build on user/userdebug builds.
        //
        // All private inner classes below also need this fix.
        TabPagerListener() {}

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Make sure not in the search mode, in which case position != TabState.ordinal().
            if (!mTabPagerAdapter.isSearchMode()) {
                /**
                * SPRD:
                *   Add the code in if{} module for UniverseUI.
                *
                * Original Android code:
                * The code in else{}module;
                * 
                * @{
                */
                if(UniverseUtils.UNIVERSEUI_SUPPORT){
                    mActionBarAdapter.setCurrentTabNewUI(position, false);
                    showEmptyStateForTabNewUI(position);
                    if (position == TabStateNewUI.GROUPS) {
                        mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
                    }       
                } else {
                    mActionBarAdapter.setCurrentTab(position, false);
                    showEmptyStateForTab(position);
                    if (position == TabState.GROUPS) {
                        mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
                    }
                }
                /**
                * @}
                */
                invalidateOptionsMenu();
            }
        }
    }

    /**
     * Adapter for the {@link ViewPager}.  Unlike {@link FragmentPagerAdapter},
     * {@link #instantiateItem} returns existing fragments, and {@link #instantiateItem}/
     * {@link #destroyItem} show/hide fragments instead of attaching/detaching.
     *
     * In search mode, we always show the "all" fragment, and disable the swipe.  We change the
     * number of items to 1 to disable the swipe.
     *
     * TODO figure out a more straight way to disable swipe.
     */
    private class TabPagerAdapter extends PagerAdapter {
        private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;

        private boolean mTabPagerAdapterSearchMode;

        private Fragment mCurrentPrimaryItem;

        public TabPagerAdapter() {
            mFragmentManager = getFragmentManager();
        }

        public boolean isSearchMode() {
            return mTabPagerAdapterSearchMode;
        }

        public void setSearchMode(boolean searchMode) {
            if (searchMode == mTabPagerAdapterSearchMode) {
                return;
            }
            mTabPagerAdapterSearchMode = searchMode;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {

            /**
            * SPRD:
            * Original Android code:
            * return mTabPagerAdapterSearchMode ? 1 : TabState.COUNT;
            * 
            * @{
            */
            int count = 0;
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                count = mTabPagerAdapterSearchMode ? 1 : TabStateNewUI.COUNT;
            } else {
                count = mTabPagerAdapterSearchMode ? 1 : TabState.COUNT;
            }
            return count;
//            return mTabPagerAdapterSearchMode ? 1 : TabState.COUNT;
            /**
            * @}
            */
        }

        /** Gets called when the number of items changes. */
        @Override
        public int getItemPosition(Object object) {
            if (mTabPagerAdapterSearchMode) {
                if (object == mAllFragment) {
                    return 0; // Only 1 page in search mode
                }
            } else {
                /**
                * SPRD:
                *  Modify the code here for UniverseUI.
                *
                * Original Android code:
                * The code remarked;
                * 
                * @{
                */
                int position = 0;
                if (object == mFavoritesFragment) {
                    if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                        position = TabStateNewUI.FAVORITES;
                    } else {
                        position = TabState.FAVORITES;
                    }
                }
                if (object == mAllFragment) {
                    if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                        position = TabStateNewUI.ALL;
                    } else {
                        position = TabState.ALL;
                    }
                }
                if (object == mGroupsFragment) {
                    if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                        position = TabStateNewUI.GROUPS;
                    } else {
                        position = TabState.GROUPS;
                    }
                }
                if (object == mMoreFragment) {
                    if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                        position = TabStateNewUI.MORE;
                    }
                }
                return position;
            
//                if (object == mFavoritesFragment) {
//                    return TabState.FAVORITES;
//                }
//                if (object == mAllFragment) {
//                    return TabState.ALL;
//                }
//                if (object == mGroupsFragment) {
//                    return TabState.GROUPS;
//                }
                /**
                * @}
                */
            }
            return POSITION_NONE;
        }

        @Override
        public void startUpdate(ViewGroup container) {
        }

        private Fragment getFragment(int position) {
            if (mTabPagerAdapterSearchMode) {
                if (position != 0) {
                    // This has only been observed in monkey tests.
                    // Let's log this issue, but not crash
                    Log.w(TAG, "Request fragment at position=" + position + ", eventhough we " +
                            "are in search mode");
                }
                return mAllFragment;
            } else {
                /**
                * SPRD:
                *   Add the code in if{} for UniverseUI.
                *
                * Original Android code:
                * The code in else{};
                * 
                * @{
                */
                if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                    if (position == TabStateNewUI.FAVORITES) {
                        return mFavoritesFragment;
                    } else if (position == TabStateNewUI.ALL) {
                        return mAllFragment;
                    } else if (position == TabStateNewUI.GROUPS) {
                        return mGroupsFragment;
                    } else if (position == TabStateNewUI.MORE) {
                        return mMoreFragment;
                    }

                } else {
                    if (position == TabState.FAVORITES) {
                        return mFavoritesFragment;
                    } else if (position == TabState.ALL) {
                        return mAllFragment;
                    } else if (position == TabState.GROUPS) {
                        return mGroupsFragment;
                    }
                }
                /**
                * @}
                */
            }
            throw new IllegalArgumentException("position: " + position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            Fragment f = getFragment(position);
            mCurTransaction.show(f);

            // Non primary pages are not visible.
            f.setUserVisibleHint(f == mCurrentPrimaryItem);
            return f;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mCurTransaction.hide((Fragment) object);
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            if (mCurTransaction != null) {
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            Fragment fragment = (Fragment) object;
            if (mCurrentPrimaryItem != fragment) {
                if (mCurrentPrimaryItem != null) {
                    mCurrentPrimaryItem.setUserVisibleHint(false);
                }
                if (fragment != null) {
                    fragment.setUserVisibleHint(true);
                }
                mCurrentPrimaryItem = fragment;
            }
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }
    }

    private void setQueryTextToFragment(String query) {
        mAllFragment.setQueryString(query, true);
        mAllFragment.setSearchViewText(query);
        mAllFragment.setVisibleScrollbarEnabled(!mAllFragment.isSearchMode());
    }

    private void configureContactListFragmentForRequest() {
        Uri contactUri = mRequest.getContactUri();
        if (contactUri != null) {
            // For an incoming request, explicitly require a selection if we are on 2-pane UI,
            // (i.e. even if we view the same selected contact, the contact may no longer be
            // in the list, so we must refresh the list).
            if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
                mAllFragment.setSelectionRequired(true);
            }
            mAllFragment.setSelectedContactUri(contactUri);
        }

        Log.d(TAG, "filter=="+mContactListFilterController.getFilter());
        mAllFragment.setFilter(mContactListFilterController.getFilter());
        setQueryTextToFragment(mActionBarAdapter.getQueryString());

        if (mRequest.isDirectorySearchEnabled()) {
            mAllFragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_DEFAULT);
        } else {
            mAllFragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
        }
    }

    private void configureContactListFragment() {
        // Filter may be changed when this Activity is in background.
    	Log.d(TAG, "first filter ==="+mContactListFilterController.getFilter());
        mAllFragment.setFilter(mContactListFilterController.getFilter());

        final boolean useTwoPane = PhoneCapabilityTester.isUsingTwoPanes(this);

        mAllFragment.setVerticalScrollbarPosition(getScrollBarPosition(useTwoPane));
        mAllFragment.setSelectionVisible(useTwoPane);
        mAllFragment.setQuickContactEnabled(!useTwoPane);
    }

    private int getScrollBarPosition(boolean useTwoPane) {
        final boolean isLayoutRtl = isRTL();
        final int position;
        if (useTwoPane) {
            position = isLayoutRtl ? View.SCROLLBAR_POSITION_RIGHT : View.SCROLLBAR_POSITION_LEFT;
        } else {
            position = isLayoutRtl ? View.SCROLLBAR_POSITION_LEFT : View.SCROLLBAR_POSITION_RIGHT;
        }
        return position;
    }

    private boolean isRTL() {
        final Locale locale = Locale.getDefault();
        return TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL;
    }

    private void configureGroupListFragment() {
        final boolean useTwoPane = PhoneCapabilityTester.isUsingTwoPanes(this);
        mGroupsFragment.setVerticalScrollbarPosition(getScrollBarPosition(useTwoPane));
        mGroupsFragment.setSelectionVisible(useTwoPane);
    }

    @Override
    public void onProviderStatusChange() {
        updateViewConfiguration(false);
    }

    private void updateViewConfiguration(boolean forceUpdate) {
        ProviderStatusWatcher.Status providerStatus = mProviderStatusWatcher.getProviderStatus();
        if (!forceUpdate && (mProviderStatus != null)
                && (providerStatus.status == mProviderStatus.status)) return;
        mProviderStatus = providerStatus;

        View contactsUnavailableView = findViewById(R.id.contacts_unavailable_view);
        View mainView = findViewById(R.id.main_view);

        /**
        * SPRD:
        *   Add "mProviderStatus.status == ProviderStatus.STATUS_IMPORTING" when merge from 4.1 to 4.3.
        *
        * Original Android code:
        * if (mProviderStatus.status == ProviderStatus.STATUS_NORMAL);
        * 
        * @{
        */
        if (mProviderStatus.status == ProviderStatus.STATUS_NORMAL
                ||  mProviderStatus.status == ProviderStatus.STATUS_IMPORTING) {
            /**
            * @}
            */
            
            // Ensure that the mTabPager is visible; we may have made it invisible below.
            contactsUnavailableView.setVisibility(View.GONE);
            if (mTabPager != null) {
                mTabPager.setVisibility(View.VISIBLE);
            }

            if (mainView != null) {
                mainView.setVisibility(View.VISIBLE);
            }
            if (mAllFragment != null) {
                mAllFragment.setEnabled(true);
            }
        } else {
            // If there are no accounts on the device and we should show the "no account" prompt
            // (based on {@link SharedPreferences}), then launch the account setup activity so the
            // user can sign-in or create an account.
            //
            // Also check for ability to modify accounts.  In limited user mode, you can't modify
            // accounts so there is no point sending users to account setup activity.
            final UserManager userManager = UserManager.get(this);
            final boolean disallowModifyAccounts = userManager.getUserRestrictions().getBoolean(
                    UserManager.DISALLOW_MODIFY_ACCOUNTS);
            if (!disallowModifyAccounts && !areContactWritableAccountsAvailable() &&
                    AccountPromptUtils.shouldShowAccountPrompt(this)) {
                AccountPromptUtils.launchAccountPrompt(this);
                return;
            }

            // Otherwise, continue setting up the page so that the user can still use the app
            // without an account.
            if (mAllFragment != null) {
                mAllFragment.setEnabled(false);
            }
            if (mContactsUnavailableFragment == null) {
                mContactsUnavailableFragment = new ContactsUnavailableFragment();
                mContactsUnavailableFragment.setOnContactsUnavailableActionListener(
                        new ContactsUnavailableFragmentListener());
                getFragmentManager().beginTransaction()
                        .replace(R.id.contacts_unavailable_container, mContactsUnavailableFragment)
                        .commitAllowingStateLoss();
            }
            mContactsUnavailableFragment.updateStatus(mProviderStatus);

            // Show the contactsUnavailableView, and hide the mTabPager so that we don't
            // see it sliding in underneath the contactsUnavailableView at the edges.
            contactsUnavailableView.setVisibility(View.VISIBLE);
            if (mTabPager != null) {
                mTabPager.setVisibility(View.GONE);
            }

            if (mainView != null) {
                mainView.setVisibility(View.INVISIBLE);
            }

            /**
            * SPRD:
            * Original Android code:
            * showEmptyStateForTab(mActionBarAdapter.getCurrentTab())
            * @{
            */
            if(UniverseUtils.UNIVERSEUI_SUPPORT){
                int tab = mActionBarAdapter.getCurrentTabNewUI();
                showEmptyStateForTabNewUI(tab);
            } else {
                int tab = mActionBarAdapter.getCurrentTab();
                showEmptyStateForTab(tab);
            }
//            showEmptyStateForTab(mActionBarAdapter.getCurrentTab());
            /**
            * @}
            */
        }

        invalidateOptionsMenuIfNeeded();
    }

    private final class ContactBrowserActionListener implements OnContactBrowserActionListener {
        ContactBrowserActionListener() {}

        @Override
        public void onSmsContactAction(Uri contactUri){
            
        }
        @Override
        public void onCallContactAction(Uri contactUri){
            
        }

        @Override
        public void onSelectionChange() {
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                setupContactDetailFragment(mAllFragment.getSelectedContactUri());
            }
        }

        @Override
        public void onViewContactAction(Uri contactLookupUri) {
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                setupContactDetailFragment(contactLookupUri);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, contactLookupUri);
                startActivity(intent);
            }
        }

        @Override
        public void onCreateNewContactAction() {
            Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                intent.putExtras(extras);
            }
            startActivity(intent);
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                intent.putExtras(extras);
            }
            intent.putExtra(
                    ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
            startActivityForResult(intent, SUBACTIVITY_EDIT_CONTACT);
        }

        @Override
        public void onAddToFavoritesAction(Uri contactUri) {
            ContentValues values = new ContentValues(1);
            values.put(Contacts.STARRED, 1);
            getContentResolver().update(contactUri, values, null, null);
        }

        @Override
        public void onRemoveFromFavoritesAction(Uri contactUri) {
            ContentValues values = new ContentValues(1);
            values.put(Contacts.STARRED, 0);
            getContentResolver().update(contactUri, values, null, null);
        }

        @Override
        public void onDeleteContactAction(Uri contactUri) {
            ContactDeletionInteraction.start(PeopleActivity.this, contactUri, false);
        }

        @Override
        public void onFinishAction() {
            onBackPressed();
        }

        @Override
        public void onInvalidSelection() {
			Log.d(TAG, "xxxxxxxxxxxxxx");
            ContactListFilter filter;
            ContactListFilter currentFilter = mAllFragment.getFilter();
            if (currentFilter != null
                    && currentFilter.filterType == ContactListFilter.FILTER_TYPE_SINGLE_CONTACT) {
                filter = ContactListFilter.createFilterWithType(
                        ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS, getApplicationContext());

                mAllFragment.setFilter(filter);
            } else {
                filter = ContactListFilter.createFilterWithType(
                        ContactListFilter.FILTER_TYPE_SINGLE_CONTACT);
                mAllFragment.setFilter(filter, false);
            }
            mContactListFilterController.setContactListFilter(filter, true);
        }
    }

    private class ContactDetailLoaderFragmentListener implements ContactLoaderFragmentListener {
        ContactDetailLoaderFragmentListener() {}

        @Override
        public void onContactNotFound() {
            // Nothing needs to be done here
        }

        @Override
        public void onNotFilterRequested(ArrayList<String> phones,String name){
            
        }
        @Override
        public void onFilterRequested(ArrayList<String> phones,String name){
            
        }
        @Override
        public void onCopyRequested(String lookupKey){
            
        }
        @Override
        public void onDetailsLoaded(final Contact result) {
            if (result == null) {
                // Nothing is loaded. Show empty state.
                mContactDetailLayoutController.showEmptyState();
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
                    mContactDetailLayoutController.setContactData(result);
                }
            });
        }

        @Override
        public void onEditRequested(Uri contactLookupUri) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            intent.putExtra(
                    ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
            startActivityForResult(intent, SUBACTIVITY_EDIT_CONTACT);
        }

        @Override
        public void onDeleteRequested(Uri contactUri) {
            ContactDeletionInteraction.start(PeopleActivity.this, contactUri, false);
        }
    }

    public class ContactDetailFragmentListener implements ContactDetailFragment.Listener {
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
        public void onCreateRawContactRequested(ArrayList<ContentValues> values,
                AccountWithDataSet account) {
            Toast.makeText(PeopleActivity.this, R.string.toast_making_personal_copy,
                    Toast.LENGTH_LONG).show();
            Intent serviceIntent = ContactSaveService.createNewRawContactIntent(
                    PeopleActivity.this, values, account,
                    PeopleActivity.class, Intent.ACTION_VIEW);
            startService(serviceIntent);
        }
    }

    private class ContactsUnavailableFragmentListener
            implements OnContactsUnavailableActionListener {
        ContactsUnavailableFragmentListener() {}

        @Override
        public void onCreateNewContactAction() {
            startActivity(new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI));
        }

        @Override
        public void onAddAccountAction() {
            Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(Settings.EXTRA_AUTHORITIES,
                    new String[] { ContactsContract.AUTHORITY });
            startActivity(intent);
        }

        @Override
        public void onImportContactsFromFileAction() {
            ImportExportDialogFragment.show(getFragmentManager(), areContactsAvailable(),
                    PeopleActivity.class);
        }

        @Override
        public void onFreeInternalStorageAction() {
            startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        }
    }

    private final class StrequentContactListFragmentListener
            implements ContactTileListFragment.Listener {
        StrequentContactListFragmentListener() {}

        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            /**
            * SPRD:
            * 
            * @{
            */
            final Uri uri = contactUri;
            String contactName = null;
            /**
            * @}
            */
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                QuickContact.showQuickContact(PeopleActivity.this, targetRect, contactUri, 0, null);
            } else {
                /**
                * SPRD:
                * Original Android code:
                * The code remarked.
                * @{
                */
                if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return;
                }
                Cursor cursor = getContentResolver().query(uri, new String[] {
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
                }, Contacts.STARRED + "=1", null, null);
                if (cursor == null || cursor.getCount() == 0) {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return;
                }
                // Only one item, no need to use loop
                cursor.moveToFirst();
                contactName = cursor.getString(0);
                AlertDialog dialog = new AlertDialog.Builder(PeopleActivity.this)
                        .setTitle(contactName)
                        .setItems(R.array.items_starred_dialog,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case 0:
                                                Intent intent = new Intent();
                                                intent.setComponent(new ComponentName(
                                                        PeopleActivity.this,
                                                        ContactSaveService.class));
                                                intent.setAction(ContactSaveService.ACTION_SET_STARRED);
                                                intent.putExtra(
                                                        ContactSaveService.EXTRA_CONTACT_URI,
                                                        uri);
                                                intent.putExtra(
                                                        ContactSaveService.EXTRA_STARRED_FLAG,
                                                        false);
                                                startService(intent);
                                                break;
                                            case 1:
                                                startActivity(new Intent(
                                                        Intent.ACTION_VIEW, uri));
                                                break;
                                            case 2:
                                                dialog.dismiss();
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                })
                        .show();
                cursor.close();
                // startActivity(new Intent(Intent.ACTION_VIEW, contactUri));
                /**
                * @}
                */
            }
        }

        @Override
        public void onCallNumberDirectly(String phoneNumber) {
            // No need to call phone number directly from People app.
            Log.w(TAG, "unexpected invocation of onCallNumberDirectly()");
        }
    }

    private final class GroupBrowserActionListener implements OnGroupBrowserActionListener {

        GroupBrowserActionListener() {}

        @Override
        public void onViewGroupAction(Uri groupUri) {
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                setupGroupDetailFragment(groupUri);
            } else {
                Intent intent = new Intent(PeopleActivity.this, GroupDetailActivity.class);
                intent.setData(groupUri);
                startActivity(intent);
            }
        }
    }

    private class GroupDetailFragmentListener implements GroupDetailFragmentSprd.Listener {

        GroupDetailFragmentListener() {}

        @Override
        public void onGroupSizeUpdated(String size) {
            // Nothing needs to be done here because the size will be displayed in the detail
            // fragment
        }

        @Override
        public void onGroupTitleUpdated(String title) {
            // Nothing needs to be done here because the title will be displayed in the detail
            // fragment
        }

        @Override
        public void onAccountTypeUpdated(String accountTypeString, String dataSet) {
            // Nothing needs to be done here because the group source will be displayed in the
            // detail fragment
        }

        @Override
        public void onEditRequested(Uri groupUri) {
            final Intent intent = new Intent(PeopleActivity.this, GroupEditorActivity.class);
            intent.setData(groupUri);
            intent.setAction(Intent.ACTION_EDIT);
            startActivityForResult(intent, SUBACTIVITY_EDIT_GROUP);
        }

        @Override
        public void onContactSelected(Uri contactUri) {
            // Nothing needs to be done here because either quickcontact will be displayed
            // or activity will take care of selection
        }
    }

    public void startActivityAndForwardResult(final Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // Forward extras to the new activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*
        * SPRD:
        *   Add for Bug259910 provider status changed,menu can not display
        * @{
        */
        mOptionsMenuContactsAvailable = areContactsAvailable();
        /*
        * @}
        */
        if (!areContactsAvailable()) {
            // If contacts aren't available, hide all menu items.
            return false;
        }

        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.people_options, menu);

        return true;
    }

    private void invalidateOptionsMenuIfNeeded() {
        if (isOptionsMenuChanged()) {
            invalidateOptionsMenu();
        }
    }

    public boolean isOptionsMenuChanged() {
        if (mOptionsMenuContactsAvailable != areContactsAvailable()) {
            return true;
        }

        if (mAllFragment != null && mAllFragment.isOptionsMenuChanged()) {
            return true;
        }

        if (mContactDetailLoaderFragment != null &&
                mContactDetailLoaderFragment.isOptionsMenuChanged()) {
            return true;
        }

        if (mGroupDetailFragment != null && mGroupDetailFragment.isOptionsMenuChanged()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mOptionsMenuContactsAvailable = areContactsAvailable();
        if (!mOptionsMenuContactsAvailable) {
            return false;
        }

        // Get references to individual menu items in the menu
        final MenuItem addContactMenu = menu.findItem(R.id.menu_add_contact);
        final MenuItem contactsFilterMenu = menu.findItem(R.id.menu_contacts_filter);

      //  MenuItem addGroupMenu = menu.findItem(R.id.menu_add_group);

        final MenuItem clearFrequentsMenu = menu.findItem(R.id.menu_clear_frequents);
        final MenuItem helpMenu = menu.findItem(R.id.menu_help);

        final boolean isSearchMode = mActionBarAdapter.isSearchMode();
        /**
        * SPRD:
        * 
        * @{
        */
        final boolean showMiscOptions = !isSearchMode;
        boolean showMoreMenuItem = true;
        /**
        * @}
        */
        if (isSearchMode) {
            addContactMenu.setVisible(false);
           // addGroupMenu.setVisible(false);
            contactsFilterMenu.setVisible(false);
            clearFrequentsMenu.setVisible(false);
            helpMenu.setVisible(false);
            /**
             * SPRD:
             * 
             * @{
             */
            makeMenuItemVisible(menu, R.id.menu_delete_group, false);
            makeMenuItemVisible(menu, R.id.menu_add_favorite, false);
            makeMenuItemVisible(menu, R.id.menu_batch_delete, false);
            makeMenuItemVisible(menu, R.id.menu_share_by_sms, false);
            makeMenuItemVisible(menu, R.id.menu_import_export, false);
            makeMenuItemVisible(menu, R.id.menu_search, false);
            /**
             * @}
             */
        } else {
            /**
            * SPRD:
            *   Add the code in if{} module for UniverseUI.
            *
            * Original Android code:
            * The code in else{} module;
            * 
            * @{
            */
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                ActionBar actionBar = getActionBar();
                switch (mActionBarAdapter.getCurrentTabNewUI()) {
                    case TabStateNewUI.FAVORITES:
                        addContactMenu.setVisible(false);
                       // addGroupMenu.setVisible(false);
                        contactsFilterMenu.setVisible(false);
                        clearFrequentsMenu.setVisible(hasFrequents());
                        makeMenuItemVisible(menu, R.id.menu_import_export, false);
                        makeMenuItemVisible(menu, R.id.menu_batch_delete, false);
                        makeMenuItemVisible(menu, R.id.menu_delete_group, false);
                        makeMenuItemVisible(menu, R.id.menu_add_favorite, true);
                        makeMenuItemVisible(menu, R.id.menu_search, false);
                        makeMenuItemVisible(menu, R.id.menu_share_by_sms, false);
                        getActionBar().setTitle(R.string.favorite_contacts);
                        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
                        break;
                    case TabStateNewUI.ALL:
                        addContactMenu.setVisible(true);
                       // addGroupMenu.setVisible(false);
                        contactsFilterMenu.setVisible(true);
                        clearFrequentsMenu.setVisible(false);
                        makeMenuItemVisible(menu, R.id.menu_import_export, showMiscOptions);
                        makeMenuItemVisible(menu, R.id.menu_batch_delete, showMiscOptions);
                        makeMenuItemVisible(menu, R.id.menu_delete_group, false);
                        makeMenuItemVisible(menu, R.id.menu_add_favorite, false);
                        makeMenuItemVisible(menu, R.id.menu_search, false);
                        getActionBar().setTitle(R.string.people);
                        getActionBar().setDisplayUseLogoEnabled(true);
                        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
                        break;
                    case TabStateNewUI.GROUPS:
                        // Do not display the "new group" button if no accounts
                        // are available
//                        if (areGroupWritableAccountsAvailable()) {
//                            addGroupMenu.setVisible(true);
//                        } else {
//                            addGroupMenu.setVisible(false);
//                        }
                        addContactMenu.setVisible(false);
                        contactsFilterMenu.setVisible(false);
                        clearFrequentsMenu.setVisible(false);
                        makeMenuItemVisible(menu, R.id.menu_import_export, false);
                        makeMenuItemVisible(menu, R.id.menu_batch_delete, false);
                        makeMenuItemVisible(menu, R.id.menu_add_favorite, false);
                        makeMenuItemVisible(menu, R.id.menu_share_by_sms, false);
                        makeMenuItemVisible(menu, R.id.menu_search, false);
                        if (mGroupsFragment != null &&
                                mGroupsFragment.getAdapter() != null &&
                                mGroupsFragment.getAdapter().getCount() > 0) {
                           // makeMenuItemVisible(menu, R.id.menu_delete_group, true);
                            /**
                             * add by xuhong.tian
                             */
                        	  makeMenuItemVisible(menu, R.id.menu_delete_group, false);
                        } else {
                            makeMenuItemVisible(menu, R.id.menu_delete_group, false);
                        }
                        makeMenuItemVisible(menu, R.id.menu_search, false);
                        getActionBar().setTitle(R.string.contactsGroupsLabel);
                        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
                        break;
                    case TabStateNewUI.MORE:
                        showMoreMenuItem = false;
                        addContactMenu.setVisible(false);
                       // addGroupMenu.setVisible(false);
                        contactsFilterMenu.setVisible(false);
                        clearFrequentsMenu.setVisible(false);
                        makeMenuItemVisible(menu, R.id.menu_import_export, false);
                        makeMenuItemVisible(menu, R.id.menu_batch_delete, false);
                        makeMenuItemVisible(menu, R.id.menu_delete_group, false);
                        makeMenuItemVisible(menu, R.id.menu_search, false);
                        makeMenuItemVisible(menu, R.id.menu_add_favorite, false);
                        makeMenuItemVisible(menu, R.id.menu_share_by_sms, false);
                        getActionBar().setTitle(R.string.tab_more);
                        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);

                }
            } else {
                switch (mActionBarAdapter.getCurrentTab()) {
                    case TabState.FAVORITES:
                        addContactMenu.setVisible(false);
                       // addGroupMenu.setVisible(false);
                        contactsFilterMenu.setVisible(false);
                        clearFrequentsMenu.setVisible(hasFrequents());
                        makeMenuItemVisible(menu, R.id.menu_import_export, false);
                        makeMenuItemVisible(menu, R.id.menu_delete_group, false);
                        makeMenuItemVisible(menu, R.id.menu_batch_delete, false);
                        makeMenuItemVisible(menu, R.id.menu_add_favorite, false);
                        makeMenuItemVisible(menu, R.id.menu_share_by_sms, false);
                        makeMenuItemVisible(menu, R.id.menu_search, false);
                        break;
                    case TabState.ALL:
                        addContactMenu.setVisible(true);
                       // addGroupMenu.setVisible(false);
                        contactsFilterMenu.setVisible(true);
                        clearFrequentsMenu.setVisible(false);
                        makeMenuItemVisible(menu, R.id.menu_import_export, showMiscOptions);
                        makeMenuItemVisible(menu, R.id.menu_delete_group, false);
                        makeMenuItemVisible(menu, R.id.menu_batch_delete, showMiscOptions);
                        makeMenuItemVisible(menu, R.id.menu_add_favorite, false);
                        makeMenuItemVisible(menu, R.id.menu_share_by_sms, false);
                        break;
                    case TabState.GROUPS:
                        // Do not display the "new group" button if no accounts
                        // are available
//                        if (areGroupWritableAccountsAvailable()) {
//                            addGroupMenu.setVisible(true);
//                        } else {
//                            addGroupMenu.setVisible(false);
//                        }
                        addContactMenu.setVisible(false);
                        contactsFilterMenu.setVisible(false);
                        clearFrequentsMenu.setVisible(false);
                        makeMenuItemVisible(menu, R.id.menu_import_export, false);
                        makeMenuItemVisible(menu, R.id.menu_delete_group, false);
                        makeMenuItemVisible(menu, R.id.menu_batch_delete, false);
                        makeMenuItemVisible(menu, R.id.menu_add_favorite, false);
                        makeMenuItemVisible(menu, R.id.menu_share_by_sms, false);
                        makeMenuItemVisible(menu, R.id.menu_search, false);
                        break;
                }
            }
            HelpUtils.prepareHelpMenuItem(this, helpMenu, R.string.help_url_people_main);
            /**
            * @}
            */
        }
        
//        /**
//         * SPRD:
//         * Original Android code:
//         * The code be remarked.
//         * @{
//         */
//        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
//            makeMenuItemVisible(menu, R.id.menu_search, false);
//        } else {
//            makeMenuItemVisible(menu, R.id.menu_search, false);
//        }

        makeMenuItemVisible(menu, R.id.menu_accounts, false);
        //makeMenuItemVisible(menu, R.id.menu_accounts, showMiscOptions && showMoreMenuItem);
        showMoreMenuItem = true;
//        final boolean showMiscOptions = !isSearchMode;
//        makeMenuItemVisible(menu, R.id.menu_search, showMiscOptions);
//        makeMenuItemVisible(menu, R.id.menu_import_export, showMiscOptions);
//        makeMenuItemVisible(menu, R.id.menu_accounts, showMiscOptions);
        /**
        * @}
        */
        makeMenuItemVisible(menu, R.id.menu_settings,
                showMiscOptions && !ContactsPreferenceActivity.isEmpty(this));

        // Debug options need to be visible even in search mode.
        makeMenuItemVisible(menu, R.id.export_database, mEnableDebugMenuOptions);

        return true;
    }

    /**
     * Returns whether there are any frequently contacted people being displayed
     * @return
     */
    private boolean hasFrequents() {
        if (PhoneCapabilityTester.isUsingTwoPanesInFavorites(this)) {
            return mFrequentFragment.hasFrequents();
        } else {
            return mFavoritesFragment.hasFrequents();
        }
    }

    private void makeMenuItemVisible(Menu menu, int itemId, boolean visible) {
        MenuItem item =menu.findItem(itemId);
        if (item != null) {
            item.setVisible(visible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDisableOptionItemSelected) {
            return false;
        }

        switch (item.getItemId()) {
            case android.R.id.home: {
                // The home icon on the action bar is pressed
                if (mActionBarAdapter.isUpShowing()) {
                    // "UP" icon press -- should be treated as "back".
                    onBackPressed();
                }
                return true;
            }
            case R.id.menu_settings: {
                final Intent intent = new Intent(this, ContactsPreferenceActivity.class);
                // as there is only one section right now, make sure it is selected
                // on small screens, this also hides the section selector
                // Due to b/5045558, this code unfortunately only works properly on phones
                boolean settingsAreMultiPane = getResources().getBoolean(
                        com.android.internal.R.bool.preferences_prefer_dual_pane);
                if (!settingsAreMultiPane) {
                    intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                            DisplayOptionsPreferenceFragment.class.getName());
                    intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE,
                            R.string.activity_title_settings);
                }
                startActivity(intent);
                return true;
            }
            case R.id.menu_contacts_filter: {
                AccountFilterUtil.startAccountFilterActivityForResult(
                        this, SUBACTIVITY_ACCOUNT_FILTER,
                        mContactListFilterController.getFilter());
                
                
//        		Intent intent = new Intent(Intent.ACTION_INSERT,
//        				Contacts.CONTENT_URI);
//        		AccountWithDataSet account = new AccountWithDataSet(
//        				"专网联系人",
//        				"neolink.com.android.account.phone", null);
//        		intent.putExtra(Intents.Insert.ACCOUNT, account);
        		//intent.putExtra(Insert.PHONE, "123456");
//        		Bundle extras = new Bundle();
//        		extras.putInt("public_create", 1);
//        		intent.putExtras(extras);
 //       		startActivity(intent);
                return true;
            }
            case R.id.menu_search: {
            	
          //  	importPrivateContacts();
           // 	importGroupContacts();
            	
            	
                onSearchRequested();
                return true;
            }
            case R.id.menu_add_contact: {
                /**
                * SPRD:
                * Original Android code:
                * The code be remarked.
                * @{
                */
                if (ContactsApplication.sApplication.isBatchOperation()
                        || ContactSaveService.mIsGroupSaving) {
                    Toast.makeText(PeopleActivity.this, R.string.toast_batchoperation_is_running,
                            Toast.LENGTH_LONG).show();
                } else {
                    final Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                    // On 2-pane UI, we can let the editor activity finish
                    // itself and return
                    // to this activity to display the new contact.
                    if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
                        intent.putExtra(
                                ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED,
                                true);
                        startActivityForResult(intent, SUBACTIVITY_NEW_CONTACT);
                    } else {
                        // Otherwise, on 1-pane UI, we need the editor to launch
                        // the view contact
                        // intent itself.
                        startActivity(intent);
                    }
                }
                return true;         
//                final Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
//                // On 2-pane UI, we can let the editor activity finish itself and return
//                // to this activity to display the new contact.
//                if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
//                    intent.putExtra(
//                            ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED,
//                            true);
//                    startActivityForResult(intent, SUBACTIVITY_NEW_CONTACT);
//                } else {
//                    // Otherwise, on 1-pane UI, we need the editor to launch the view contact
//                    // intent itself.
//                    startActivity(intent);
//                }
//                return true;
                /**
                * @}
                */
            }
            case R.id.menu_add_group: {
                /**
                 * SPRD:
                 * Original Android code:
                 * The code be remarked.
                 * @{
                 */
                if (ContactsApplication.sApplication.isBatchOperation()
                        || ContactSaveService.mIsGroupSaving) {
                    Toast.makeText(PeopleActivity.this, R.string.toast_batchoperation_is_running,
                            Toast.LENGTH_LONG).show();
                } else {
                    createNewGroup();
                }
//                createNewGroup();
                /**
                 * @}
                 */
                return true;
            }
            case R.id.menu_import_export: {
                /**
                 * SPRD:
                 * Original Android code:
                 * The code be remarked.
                 * @{
                 */
                if (ContactsApplication.sApplication.isBatchOperation()
                        || ContactSaveService.mIsGroupSaving) {
                    Toast.makeText(PeopleActivity.this, R.string.toast_batchoperation_is_running,
                            Toast.LENGTH_LONG).show();
                } else {
                    ImportExportDialogFragment.show(getFragmentManager(), areContactsAvailable(),
                            PeopleActivity.class);
                }
//                ImportExportDialogFragment.show(getFragmentManager(), areContactsAvailable(),
//                        PeopleActivity.class);
                /**
                 * @}
                 */
                return true;
            }
            case R.id.menu_clear_frequents: {
                ClearFrequentsDialog.show(getFragmentManager());
                return true;
            }
            case R.id.menu_accounts: {
                final Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
                intent.putExtra(Settings.EXTRA_AUTHORITIES, new String[] {
                    ContactsContract.AUTHORITY
                });
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
                return true;
            }
            case R.id.export_database: {
                final Intent intent = new Intent("com.android.providers.contacts.DUMP_DATABASE");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
                return true;
            }
            /**
            * SPRD:
            * 
            * @{
            */
            case R.id.menu_add_favorite: {
                if (ContactsApplication.sApplication.isBatchOperation()
                        || ContactSaveService.mIsGroupSaving) {
                    Toast.makeText(PeopleActivity.this, R.string.toast_batchoperation_is_running,
                            Toast.LENGTH_LONG).show();
                } else {
                    final Intent intent = new Intent(UI.MULTI_PICK_ACTION);
                    intent.setData(Contacts.CONTENT_URI);
                    intent.putExtra("setMulitStarred", true);
                    startActivityForResult(intent, SUBACTIVITY_SET_STARRED);
                }
                return true;
            }
            case R.id.menu_batch_delete: {
                if (ContactsApplication.sApplication.isBatchOperation()
                        || ContactSaveService.mIsGroupSaving) {
                    Toast.makeText(PeopleActivity.this, R.string.toast_batchoperation_is_running,
                            Toast.LENGTH_LONG).show();
                } else {
                    final Intent intent = new Intent(UI.MULTI_PICK_ACTION);
                    intent.setData(Contacts.CONTENT_URI);
                    intent.putExtra("mode", SUBACTIVITY_BATCH_DELETE);
                    startActivityForResult(intent, SUBACTIVITY_BATCH_DELETE);
                }
                return true;
            }
            case R.id.menu_delete_group: {
                if (ContactsApplication.sApplication.isBatchOperation()
                        || ContactSaveService.mIsGroupSaving) {
                    Toast.makeText(PeopleActivity.this, R.string.toast_batchoperation_is_running,
                            Toast.LENGTH_LONG).show();
                } else {
                    HashMap<Long, ArrayList<Uri>> hashMap = mGroupsFragment.getGroupPhotoUri();
                    ArrayList<Long> groupId = mGroupsFragment.getGroupIdList();
                    Bundle bundle = new Bundle();
                    for (Long id : groupId) {
                        bundle.putParcelableArrayList(id.toString(), hashMap.get(id));
                    }
                    final Intent intent = new Intent(this, DeleteGroupActivity.class);
                    intent.putExtra(DeleteGroupActivity.GROUP_PHOTO_URI, bundle);
                    startActivity(intent);
                }
                return true;
            }
            case R.id.menu_share_by_sms: {
                if (ContactsApplication.sApplication.isBatchOperation()
                        || ContactSaveService.mIsGroupSaving) {
                    Toast.makeText(PeopleActivity.this, R.string.toast_batchoperation_is_running,
                            Toast.LENGTH_LONG).show();
                } else {
                    final Intent intent = new Intent(UI.MULTI_PICK_ACTION).
                            putExtra(
                                    "cascading",
                                    new Intent(UI.MULTI_PICK_ACTION).setType(
                                            Phone.CONTENT_ITEM_TYPE).
                                            putExtra(
                                                    "cascading",
                                                    new Intent(UI.MULTI_PICK_ACTION)
                                                            .setType(Email.CONTENT_ITEM_TYPE)));
                    startActivityForResult(intent, SUBACTIVITY_SHARE_BY_SMS);
                }
                return true;
            }
            /**
            * @}
            */
        }
        return false;
    }

    private void createNewGroup() {
        final Intent intent = new Intent(this, GroupEditorActivity.class);
        intent.setAction(Intent.ACTION_INSERT);
        startActivityForResult(intent, SUBACTIVITY_NEW_GROUP);
    }

    @Override
    public boolean onSearchRequested() { // Search key pressed.
        /**
        * SPRD:
        * Original Android code:
        * mActionBarAdapter.setSearchMode(true);
        * @{
        */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT){
            mActionBarAdapter.setSearchMode(true);
        }
//        mActionBarAdapter.setSearchMode(true);
        /**
        * @}
        */
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /**
         * SPRD:
         * 
         * @{
         */
        if (data != null && data.getBooleanExtra(ContactSelectionActivity.FILTER_CHANG, false)) {
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                mMoreFragment.getAdapter().notifyDataSetChanged();
            }
        }
        /**
         * @}
         */
        switch (requestCode) {
            case SUBACTIVITY_ACCOUNT_FILTER: {
                AccountFilterUtil.handleAccountFilterResult(
                        mContactListFilterController, resultCode, data);
                /**
                * SPRD:
                * 
                * @{
                */
                if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                    mMoreFragment.getAdapter().notifyDataSetChanged();
                }
                /**
                * @}
                */
                break;
            }

            case SUBACTIVITY_NEW_CONTACT:
            case SUBACTIVITY_EDIT_CONTACT: {
                if (resultCode == RESULT_OK && PhoneCapabilityTester.isUsingTwoPanes(this)) {
                    mRequest.setActionCode(ContactsRequest.ACTION_VIEW_CONTACT);
                    mAllFragment.setSelectionRequired(true);
                    /**
                     * SPRD:
                     * 
                     * @{
                     */
                    mAllFragment.reloadDataAndSetSelectedUri(data.getData());
//                    mAllFragment.setSelectedContactUri(data.getData());
                    /**
                     * @}
                     */
                    // Suppress IME if in search mode
                    if (mActionBarAdapter != null) {
                        mActionBarAdapter.clearFocusOnSearchView();
                    }
                    // No need to change the contact filter
                    mCurrentFilterIsValid = true;
                }
                break;
            }

            case SUBACTIVITY_NEW_GROUP:
            case SUBACTIVITY_EDIT_GROUP: {
                if (resultCode == RESULT_OK && PhoneCapabilityTester.isUsingTwoPanes(this)) {
                    mRequest.setActionCode(ContactsRequest.ACTION_GROUP);
                    mGroupsFragment.setSelectedUri(data.getData());
                }
                break;
            }

            // TODO: Using the new startActivityWithResultFromFragment API this should not be needed
            // anymore
            case ContactEntryListFragment.ACTIVITY_REQUEST_CODE_PICKER:{
                if (resultCode == RESULT_OK) {
                    mAllFragment.onPickerResult(data);
                }
                break;
            }
            /**
            * SPRD:
            * 
            * @{
            */
            case SUBACTIVITY_BATCH_IMPORT: {
                if (resultCode == RESULT_OK) {
                    ArrayList<String> ids = data
                            .getStringArrayListExtra("result_alternative");
                    AccountWithDataSet account = (AccountWithDataSet) (data
                            .getParcelableExtra("dst_account"));
                    if (ids != null && !ids.isEmpty() && account != null) {
                        // new
                        // BatchImportExportTask(PeopleActivity.this,account,lookupKeys.size()).execute(lookupKeys);
                        Intent intent = new Intent(data);
                        intent.setComponent(new ComponentName(PeopleActivity.this,
                                BatchOperationService.class));
                        intent.putExtra(
                                BatchOperationService.KEY_MODE,
                                BatchOperationService.MODE_START_BATCH_IMPORT_EXPORT);
                        startService(intent);
                    }
                }
                break;
            }

            case SUBACTIVITY_SET_STARRED: {
                if (resultCode == RESULT_OK) {
                    ArrayList<String> lookupKeys = data
                            .getStringArrayListExtra("result");
                    if (lookupKeys != null && !lookupKeys.isEmpty()) {
                 /**
                 * SPRD:
                 *
                 * Original Android code:
                 * for (String lookupKey : lookupKeys) {
                 *      Intent intent = new Intent();
                 *       intent.setComponent(new ComponentName(
                 *               PeopleActivity.this, ContactSaveService.class));
                 *       intent.setAction(ContactSaveService.ACTION_SET_STARRED);
                 *       intent.putExtra(ContactSaveService.EXTRA_CONTACT_URI,
                 *               Uri.withAppendedPath(
                 *                           Contacts.CONTENT_LOOKUP_URI, lookupKey));
                 *       intent.putExtra(ContactSaveService.EXTRA_STARRED_FLAG,
                 *                   true);
                 *       startService(intent);
                 *  }
                 * @{
                 */
                        Intent intent = new Intent(data);
                        intent.setComponent(new ComponentName(PeopleActivity.this,
                                BatchOperationService.class));
                        intent.putExtra(BatchOperationService.KEY_MODE,
                                 BatchOperationService.MODE_START_BATCH_STARRED);
                        startService(intent);
                 /**
                 * @}
                 */
                    }
                }
                break;
            }

            case SUBACTIVITY_BATCH_EXPORT_TO_SDCARD: {
                if (resultCode == RESULT_OK) {
                    ArrayList<String> lookupKeys = data
                            .getStringArrayListExtra("result");
                    Intent exportIntent = new Intent(PeopleActivity.this,
                            ExportVCardActivity.class);
                    exportIntent.putStringArrayListExtra("lookup_keys", lookupKeys);
                    exportIntent.putExtra("device_type", mDeviceType);
                    exportIntent.putExtra(VCardCommonArguments.ARG_CALLING_ACTIVITY,
                            "com.android.contacts.activities.PeopleActivity");
                    startActivity(exportIntent);
                }
                break;
            }

            case SUBACTIVITY_SHARE_VISIBLE: {
                if (resultCode == RESULT_OK) {
                    ArrayList<String> lookupKeys = data
                            .getStringArrayListExtra("result");
                    StringBuilder uriListBuilder = new StringBuilder();
                    int index = 0;
                    for (String key : lookupKeys) {
                        if (index != 0)
                            uriListBuilder.append(':');
                        uriListBuilder.append(key);
                        index++;
                    }
                    
                    Uri uri = Uri.withAppendedPath(
                            Contacts.CONTENT_MULTI_VCARD_URI,
                            Uri.encode(uriListBuilder.toString()));
                    final Intent intent = new Intent(Intent.ACTION_SEND);
                    Parcel parcel = Parcel.obtain();
                    intent.setType(Contacts.CONTENT_VCARD_TYPE);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.writeToParcel(parcel, 0);
                    if(Constants.DEBUG)
                        Log.d(TAG, "Shared parcel size is" + parcel.dataSize());
                    if (parcel.dataSize() > MAX_DATA_SIZE) {
                        Toast.makeText(PeopleActivity.this, R.string.transaction_too_large,
                                Toast.LENGTH_LONG).show();
                        parcel.recycle();
                        break;
                    }
                    if (parcel != null) {
                        parcel.recycle();
                    }
                    startActivity(intent);
                }
                break;
            }

            case SUBACTIVITY_BATCH_DELETE: {
                if (resultCode == RESULT_OK) {
                    ArrayList<String> lookupKeys = data
                            .getStringArrayListExtra("result");
                    if (lookupKeys != null && !lookupKeys.isEmpty()) {
                        // new BatchDeleteTask(PeopleActivity.this,
                        // lookupKeys.size()).execute(lookupKeys);
                        Intent intent = new Intent(data);
                        intent.setComponent(new ComponentName(PeopleActivity.this,
                                BatchOperationService.class));
                        intent.putExtra(BatchOperationService.KEY_MODE,
                                BatchOperationService.MODE_START_BATCH_DELETE);
                        startService(intent);
                    }
                }
                break;
            }
            case SUBACTIVITY_SHARE_BY_SMS: {
                if (resultCode == RESULT_OK && data != null) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", "", null));
                    intent.putExtra("share_by_sms", data.getSerializableExtra("result"));
                    startActivity(intent);
                }
                break;
            }
            /**
            * @}
            */
// TODO fix or remove multipicker code
//                else if (resultCode == RESULT_CANCELED && mMode == MODE_PICK_MULTIPLE_PHONES) {
//                    // Finish the activity if the sub activity was canceled as back key is used
//                    // to confirm user selection in MODE_PICK_MULTIPLE_PHONES.
//                    finish();
//                }
//                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO move to the fragment
        switch (keyCode) {
//            case KeyEvent.KEYCODE_CALL: {
//                if (callSelection()) {
//                    return true;
//                }
//                break;
//            }

            case KeyEvent.KEYCODE_DEL: {
                if (deleteSelection()) {
                    return true;
                }
                break;
            }
            default: {
                // Bring up the search UI if the user starts typing
                final int unicodeChar = event.getUnicodeChar();
                if ((unicodeChar != 0)
                        // If COMBINING_ACCENT is set, it's not a unicode character.
                        && ((unicodeChar & KeyCharacterMap.COMBINING_ACCENT) == 0)
                        && !Character.isWhitespace(unicodeChar)) {
                    String query = new String(new int[]{ unicodeChar }, 0, 1);
                    if (!mActionBarAdapter.isSearchMode()) {
                        /**
                        * SPRD:
                        * 
                        * @{
                        */
                        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
                            mActionBarAdapter.setQueryString(query);
                            mActionBarAdapter.setSearchMode(true);
                        }
//                        mActionBarAdapter.setQueryString(query);
//                        mActionBarAdapter.setSearchMode(true);
                        /**
                        * @}
                        */
                        return true;
                    }
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        /**
        * SPRD:
        * 
        * @{
        */
        if (mActionBarAdapter.isSearchMode()) {
            if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
                mActionBarAdapter.setSearchMode(false);
            } else {
                super.onBackPressed();
            }
        } else if (isTaskRoot()) {
            moveTaskToBack(false);
        } else {
            super.onBackPressed();
        }
        // if (mActionBarAdapter.isSearchMode()) {
        // mActionBarAdapter.setSearchMode(false);
        // } else {
        // super.onBackPressed();
        // }
        /**
        * @}
        */
    }

    private boolean deleteSelection() {
        // TODO move to the fragment
//        if (mActionCode == ContactsRequest.ACTION_DEFAULT) {
//            final int position = mListView.getSelectedItemPosition();
//            if (position != ListView.INVALID_POSITION) {
//                Uri contactUri = getContactUri(position);
//                if (contactUri != null) {
//                    doContactDelete(contactUri);
//                    return true;
//                }
//            }
//        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mActionBarAdapter.onSaveInstanceState(outState);
        if (mContactDetailLayoutController != null) {
            mContactDetailLayoutController.onSaveInstanceState(outState);
        }

        // Clear the listener to make sure we don't get callbacks after onSaveInstanceState,
        // in order to avoid doing fragment transactions after it.
        // TODO Figure out a better way to deal with the issue.
        mDisableOptionItemSelected = true;
        mActionBarAdapter.setListener(null);
        if (mTabPager != null) {
            mTabPager.setOnPageChangeListener(null);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // In our own lifecycle, the focus is saved and restore but later taken away by the
        // ViewPager. As a hack, we force focus on the SearchView if we know that we are searching.
        // This fixes the keyboard going away on screen rotation
        if (mActionBarAdapter.isSearchMode()) {
            mActionBarAdapter.setFocusOnSearchView();
        }
    }

    @Override
    public DialogManager getDialogManager() {
        return mDialogManager;
    }

    // Visible for testing
    public ContactBrowseListFragment getListFragment() {
        return mAllFragment;
    }

    // Visible for testing
    public ContactDetailFragment getDetailFragment() {
        return mContactDetailFragment;
    }
    
    /**
    * SPRD:
    * 
    * @{
    */
    private static final int MAX_DATA_SIZE = 150000;
    private static final int SUBACTIVITY_BATCH_DELETE = 7;
    private static final int SUBACTIVITY_BATCH_IMPORT = 8;
    private static final int SUBACTIVITY_BATCH_EXPORT_TO_SDCARD = 9;
    private static final int SUBACTIVITY_SHARE_VISIBLE = 10;
    private static final int SUBACTIVITY_SET_STARRED = 11;
    private static final int SUBACTIVITY_SHARE_BY_SMS = 12;
    
    private int mDeviceType;
    //add for Universe UI
    private ContactMoreFragment mMoreFragment;
    
    public void doCopy() {
        Bundle args = new Bundle();
        SelectAccountDialogFragment.show(getFragmentManager(),
                R.string.copy_to,
                AccountListFilter.ACCOUNTS_CONTACT_WRITABLE,
                args);
    }

    public void doImport(final AccountWithDataSet dstAccount) {
        if (dstAccount != null
                && (SimAccountType.ACCOUNT_TYPE.equals(dstAccount.type) || USimAccountType.ACCOUNT_TYPE
                        .equals(dstAccount.type))) {
            Bundle args = new Bundle();
            args.putParcelable("accounts", dstAccount);
            ConfirmCopyContactDialogFragment dialog =
                    new ConfirmCopyContactDialogFragment();
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), null);
        } else {
            Intent intent = new Intent(UI.MULTI_PICK_ACTION);
            intent.setData(Contacts.CONTENT_URI);
            intent.putExtra("dst_account", dstAccount);
            startActivityForResult(intent, SUBACTIVITY_BATCH_IMPORT);
        }
    }

    public void doPreImport(int resId) {
        if (hasExchangeAccount()) {
            ImportToAccountDialogFragment dialogFragment = ImportToAccountDialogFragment
                    .newInstance(resId);
            dialogFragment.show(getFragmentManager(), null);
        } else {
            AccountSelectionUtil.doImport(this, resId, AccountTypeManager.getInstance(this)
                    .getPhoneAccount());
        }
    }

    public static class ConfirmCopyContactDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    final Intent intent = new Intent(UI.MULTI_PICK_ACTION);
                                    intent.setData(Contacts.CONTENT_URI);
                                    AccountWithDataSet accountData = (AccountWithDataSet) getArguments()
                                            .getParcelable("accounts");
                                    intent.putExtra("dst_account", accountData);
                                    getActivity().startActivityForResult(intent,
                                            SUBACTIVITY_BATCH_IMPORT);
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

    public static class ImportToAccountDialogFragment extends DialogFragment {

        public static ImportToAccountDialogFragment newInstance(int resId) {
            ImportToAccountDialogFragment fragment = new ImportToAccountDialogFragment();
            Bundle args = new Bundle();
            args.putInt("resId", resId);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            final int resId = args.getInt("resId");
            return AccountSelectionUtil.getSelectAccountDialog(getActivity(), resId, null, null,
                    true);
        }
    }

    public void doExport(int deviceType) {
        mDeviceType = deviceType;
        final Intent intent = new Intent(UI.MULTI_PICK_ACTION);
        intent.setData(Contacts.CONTENT_URI);
        startActivityForResult(intent, SUBACTIVITY_BATCH_EXPORT_TO_SDCARD);
    }

    public void doShareVisible() {
        final Intent intent = new Intent(UI.MULTI_PICK_ACTION);
        intent.setData(Contacts.CONTENT_URI);
        startActivityForResult(intent, SUBACTIVITY_SHARE_VISIBLE);
    }

    // add for Universe UI
    private void updateFragmentsVisibilityNewUI() {
        int tab = mActionBarAdapter.getCurrentTabNewUI();
        // We use ViewPager on 1-pane.
        if (!PhoneCapabilityTester.isUsingTwoPanes(this)) {
            if (mActionBarAdapter.isSearchMode()) {
                mTabPagerAdapter.setSearchMode(true);
            } else {
                // No smooth scrolling if quitting from the search mode.
                final boolean wasSearchMode = mTabPagerAdapter.isSearchMode();
                mTabPagerAdapter.setSearchMode(false);
                if (mTabPager.getCurrentItem() != tab) {
                    mTabPager.setCurrentItem(tab, !wasSearchMode);
                }
            }
            invalidateOptionsMenu();
            showEmptyStateForTabNewUI(tab);
            if (tab == TabStateNewUI.GROUPS) {
                mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
            }
            return;
        }

        // for the tablet...

        // If in search mode, we use the all list + contact details to show the
        // result.
        if (mActionBarAdapter.isSearchMode()) {
            tab = TabStateNewUI.ALL;
        }
        switch (tab) {
            case TabStateNewUI.FAVORITES:
                mFavoritesView.setVisibility(View.VISIBLE);
                mBrowserView.setVisibility(View.GONE);
                mGroupDetailsView.setVisibility(View.GONE);
                mContactDetailsView.setVisibility(View.GONE);
                break;
            case TabStateNewUI.GROUPS:
                mFavoritesView.setVisibility(View.GONE);
                mBrowserView.setVisibility(View.GONE);
                mGroupDetailsView.setVisibility(View.VISIBLE);
                mContactDetailsView.setVisibility(View.GONE);
                mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
                break;
            case TabStateNewUI.ALL:
                mFavoritesView.setVisibility(View.GONE);
                mBrowserView.setVisibility(View.VISIBLE);
                mContactDetailsView.setVisibility(View.VISIBLE);
                mGroupDetailsView.setVisibility(View.GONE);
                break;
            case TabStateNewUI.MORE:
                mFavoritesView.setVisibility(View.GONE);
                mBrowserView.setVisibility(View.GONE);
                mContactDetailsView.setVisibility(View.GONE);
                mGroupDetailsView.setVisibility(View.GONE);
        }
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        // Note mContactDetailLoaderFragment is an invisible fragment, but we
        // still have to show/
        // hide it so its options menu will be shown/hidden.
        switch (tab) {
            case TabStateNewUI.FAVORITES:
                showFragment(ft, mFavoritesFragment);
                showFragment(ft, mFrequentFragment);
                hideFragment(ft, mAllFragment);
                hideFragment(ft, mContactDetailLoaderFragment);
                hideFragment(ft, mContactDetailFragment);
                hideFragment(ft, mGroupsFragment);
                hideFragment(ft, mGroupDetailFragment);
                hideFragment(ft, mMoreFragment);
                break;
            case TabStateNewUI.ALL:
                hideFragment(ft, mFavoritesFragment);
                hideFragment(ft, mFrequentFragment);
                showFragment(ft, mAllFragment);
                showFragment(ft, mContactDetailLoaderFragment);
                showFragment(ft, mContactDetailFragment);
                hideFragment(ft, mGroupsFragment);
                hideFragment(ft, mGroupDetailFragment);
                hideFragment(ft, mMoreFragment);
                break;
            case TabStateNewUI.GROUPS:
                hideFragment(ft, mFavoritesFragment);
                hideFragment(ft, mFrequentFragment);
                hideFragment(ft, mAllFragment);
                hideFragment(ft, mContactDetailLoaderFragment);
                hideFragment(ft, mContactDetailFragment);
                showFragment(ft, mGroupsFragment);
                showFragment(ft, mGroupDetailFragment);
                hideFragment(ft, mMoreFragment);
                break;
            case TabStateNewUI.MORE:
                hideFragment(ft, mFavoritesFragment);
                hideFragment(ft, mFrequentFragment);
                hideFragment(ft, mAllFragment);
                hideFragment(ft, mContactDetailLoaderFragment);
                hideFragment(ft, mContactDetailFragment);
                hideFragment(ft, mGroupsFragment);
                hideFragment(ft, mGroupDetailFragment);
                showFragment(ft, mMoreFragment);

        }
        if (!ft.isEmpty()) {
            ft.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
            // When switching tabs, we need to invalidate options menu, but
            // executing a
            // fragment transaction does it implicitly. We don't have to call
            // invalidateOptionsMenu
            // manually.
        }
        showEmptyStateForTabNewUI(tab);

    }

    private void showEmptyStateForTabNewUI(int tab) {
        if (mContactsUnavailableFragment != null) {
            switch (tab) {
                case TabStateNewUI.FAVORITES:
                    mContactsUnavailableFragment.setMessageText(
                            R.string.listTotalAllContactsZeroStarred, -1);
                    break;
                case TabStateNewUI.GROUPS:
                    mContactsUnavailableFragment.setMessageText(R.string.noGroups,
                            areGroupWritableAccountsAvailable() ? -1 : R.string.noAccounts);
                    break;
                case TabStateNewUI.ALL:
                    mContactsUnavailableFragment.setMessageText(R.string.noContacts, -1);
                    break;
            }
        }
    }

    public void onContactMoreSelected(Uri contactUri, Rect targetRect) {

    }

    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        if (account != null) {
            this.doImport(account);
        }
    }

    public void onAccountSelectorCancelled() {
        // dismiss();
    }

    private boolean hasExchangeAccount() {
        boolean hasExchangeAccount = false;
        AccountTypeManager atm = AccountTypeManager.getInstance(this);
        List<AccountWithDataSet> accounts = atm.getAccounts(true);
        for (AccountWithDataSet account : accounts) {
            if (ExchangeAccountType.ACCOUNT_TYPE_AOSP.equals(account.type)
                    || ExchangeAccountType.ACCOUNT_TYPE_GOOGLE.equals(account.type)
                    || CardDavAccountType.ACCOUNT_TYPE.equals(account.type)) {
                hasExchangeAccount = true;
                break;
            }
        }
        return hasExchangeAccount;
    }
    
    public void showNotFoundSdDialog() {
        NotFoundSdDialogFragment dialog =
                new NotFoundSdDialogFragment();
        dialog.show(getFragmentManager(), null);

    }

    public static class NotFoundSdDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.no_sdcard_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.no_sdcard_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).create();
        }
    }

    public boolean getDisableOptionItemSelected() {
        return mDisableOptionItemSelected;
    }
    /**
    * @}
    */
	private void importPrivateContacts() {
		Log.d(TAG, "..........................importPrivateContacts..");
		//ConfigurationManager cfg = new ConfigurationManager();
		//try {
//			cfg.openSession();
//			Log.d(TAG,
//					"importPrivateContacts after openSession.pctoolsFlag.charAt(0)=="
//							+ pctoolsFlag.charAt(0));
//			int number = cfg.getContactNumber();
//			Log.d(TAG, "CONTACT number is--------" + number);
//			int modeType = -1;
//			if (pctoolsFlag.charAt(0) == '1') {
//				modeType = PrivateMode.MODE_PDT_DIGITAL_TRUNKING;
//			} else if (pctoolsFlag.charAt(0) == '0') {
//				modeType = PrivateMode.MODE_MPT1327_ANALOG_TRUNKING;
//			}
//
//			Log.d(TAG, "modeType==" + modeType);
//			if (number > 0) {
//				deleteOldImportContacts(modeType); // delete old contacts
//				// which import by pctool
//				Log.d(TAG, " contacats delete finished !!");
//			}
//			ArrayList<String> pctoolsName = new ArrayList<String>();
//			ArrayList<String> pctoolsNumber = new ArrayList<String>();
//			for (int i = 1; i <= number; i++) {
//
//				ConfigurationManager.Contact contact = cfg.getContact(i);
//				if (contact != null) {
//					pctoolsName.add(contact.contactName);
//					pctoolsNumber.add(contact.mobileNumber);
//					Log.d(TAG, "contact.mobileNumber==" + contact.mobileNumber
//							+ "contact.contactName==" + contact.contactName);
//				}
//			}
//			cfg.closeSession();
			//for (int i = 0; i < pctoolsName.size(); i++) {

				//if (!pctoolsName.isEmpty() && !pctoolsNumber.isEmpty()) {
					ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
					int rawContactInsertIndex = 0;
					int size = ops.size();
					ops.add(ContentProviderOperation
							.newInsert(RawContacts.CONTENT_URI)
							.withValue(RawContacts.ACCOUNT_TYPE,
									"neolink.com.android.account.phone")
							.withValue(
									RawContacts.ACCOUNT_NAME,
									"专网联系人")
							.withValue(RawContacts.RAW_CONTACT_MODE_TYPE,
									2)
							.withValue(RawContacts.RAW_CONTACT_IMPORT_TYPE,
									RawContacts.CONTACTS_BY_PCTOOLS).build());
					ops.add(ContentProviderOperation
							.newInsert(
									android.provider.ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID,
									rawContactInsertIndex)
							.withValue(Data.MIMETYPE,
									StructuredName.CONTENT_ITEM_TYPE)
							.withValue(StructuredName.GIVEN_NAME,
									"test_3").build());
					ops.add(ContentProviderOperation
							.newInsert(
									android.provider.ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID,
									rawContactInsertIndex)
							.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
							.withValue(Phone.NUMBER, "80020900")
							.withValue(Phone.TYPE, Phone.TYPE_MOBILE)
							.withValue(Phone.LABEL, " ").build());

					ops.add(ContentProviderOperation
							.newInsert(
									android.provider.ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID,
									rawContactInsertIndex)
							.withValue(
									ContactsContract.CommonDataKinds.Photo.PHOTO,
									null)
							.withValue(
									ContactsContract.Data.MIMETYPE,
									ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
							.build());
					try {

						getApplicationContext().getContentResolver()
								.applyBatch(ContactsContract.AUTHORITY, ops);
					} catch (RemoteException e) {
						Log.e(TAG,
								String.format("%s: %s", e.toString(),
										e.getMessage()));
					} catch (OperationApplicationException e) {
						Log.e(TAG,
								String.format("%s: %s", e.toString(),
										e.getMessage()));
					}
				//}
			//}
			///resetProperties();
//		} catch (ConfigurationManager.ConfigurationException ex) {
//			Log.e(TAG, "catch exception when import private contacts " + ex);
//			Log.e(TAG, Log.getStackTraceString(ex));
//		}
		Log.d(TAG, "....end .........importPrivateContacts..");

	}
	private int importGroupContacts() {

		try {
			boolean isHaveDefault = true;
				ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
					ContentProviderOperation.Builder builder = ContentProviderOperation
							.newInsert(Groups.CONTENT_URI);
					builder.withValue(Groups.ACCOUNT_TYPE,
							"sprd.com.android.account.phone");
					builder.withValue(
							Groups.ACCOUNT_NAME,
							getApplicationContext().getString(
									R.string.label_phone));
					// builder.withValue(Groups.DATA_SET, " ");
					//if (Integer.valueOf(group_type) == 2) { // dynamic group
															// type
						// builder.withValue(Groups.TITLE,
						// getString(R.string.dynamic_group_name));
					//} else {
						builder.withValue(Groups.TITLE, "group_1");
					//}
					builder.withValue(Groups.GROUP_NUMBER, "80020300");
					builder.withValue(Groups.GROUP_TYPE,
							Integer.valueOf(1));
					builder.withValue(Groups.GROUP_DEFAULT, 0);
					builder.withValue(Groups.GROUP_BY_IMPORT_TYPE,
							Groups.GROUP_IMPORT_TYPE);
					builder.withValue(Groups.GROUP_CURRENT_MODE,
							Settings.Secure.getInt(ContactsApplication
									.getApplication().getContentResolver(),
									Settings.Secure.PRIVATE_PHONE_MODE,
									PrivateMode.MODE_UNKNOWN));
					/*
					 * builder.withValue(Groups.GROUP_LOCATION,
					 * getCurrentLocatin());
					 */

/*					String saveDeaultid = "";
					if (ContactsApplication.getApplication().mMode == PrivateMode.MODE_PDT_DIGITAL_TRUNKING) { // PDT
						String saveDeaultid_pdt = getPreferredDefaultGroupNumber(PREF_FILE_PDT);
						if (saveDeaultid_pdt.equals(num)) {
							builder.withValue(Groups.GROUP_DEFAULT,
									Groups.GROUP_DEFAULT_FLAG);
							isHaveDefault = false;
							updateDefaultGroup(num);
						}

					} else if (ContactsApplication.getApplication().mMode == PrivateMode.MODE_MPT1327_ANALOG_TRUNKING) {// mpt
						String saveDeaultid_mpt = getPreferredDefaultGroupNumber(PREF_FILE_MPT);
						if (saveDeaultid_mpt.equals(num)) {
							builder.withValue(Groups.GROUP_DEFAULT,
									Groups.GROUP_DEFAULT_FLAG);
							isHaveDefault = false;
							updateDefaultGroup(num);
						}
					}*/
					operationList.add(builder.build());
				try {

					getContentResolver().applyBatch(ContactsContract.AUTHORITY,
							operationList);
					Log.d(TAG, "mContentResolver done....");
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.d(TAG, "importGroupContacts RemoteException");
				} catch (OperationApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

					Log.d(TAG,
							"importGroupContacts OperationApplicationException");
				}

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Log.d(TAG, "getprivateManager groupinList11>>");
		}
		// mHandler.sendEmptyMessage(EVENT_DO_FINISH);
		return 0;
	}
}
