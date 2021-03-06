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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;

import android.content.Intent;
import android.content.Loader;
import android.content.IContentService;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Directory;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;

import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.common.widget.CompositeCursorAdapter.Partition;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.R;
import com.android.contacts.common.widget.ContextMenuAdapter;
import com.android.contacts.common.preference.ContactsPreferences;
import com.android.contacts.common.util.Constants;
import com.sprd.contacts.common.util.UniverseUtils;
import com.sprd.contacts.common.widget.BladeView;
import java.util.Locale;
import java.util.ArrayList;
/*SPRD @{*/
import com.sprd.contacts.common.ContactListEmptyView;
/*SPRD @}*/

/**
 * Common base class for various contact-related list fragments.
 */
public abstract class ContactEntryListFragment<T extends ContactEntryListAdapter>
        extends Fragment
        implements OnItemClickListener, OnScrollListener, OnFocusChangeListener, OnTouchListener, OnClickListener,
                LoaderCallbacks<Cursor>, TextWatcher {
    private static final String TAG = "ContactEntryListFragment";

    // TODO: Make this protected. This should not be used from the PeopleActivity but
    // instead use the new startActivityWithResultFromFragment API
    public static final int ACTIVITY_REQUEST_CODE_PICKER = 1;

    private static final String KEY_LIST_STATE = "liststate";
    private static final String KEY_SECTION_HEADER_DISPLAY_ENABLED = "sectionHeaderDisplayEnabled";
    private static final String KEY_PHOTO_LOADER_ENABLED = "photoLoaderEnabled";
    private static final String KEY_QUICK_CONTACT_ENABLED = "quickContactEnabled";
    private static final String KEY_INCLUDE_PROFILE = "includeProfile";
    private static final String KEY_SEARCH_MODE = "searchMode";
    private static final String KEY_VISIBLE_SCROLLBAR_ENABLED = "visibleScrollbarEnabled";
    private static final String KEY_SCROLLBAR_POSITION = "scrollbarPosition";
    private static final String KEY_QUERY_STRING = "queryString";
    private static final String KEY_DIRECTORY_SEARCH_MODE = "directorySearchMode";
    private static final String KEY_SELECTION_VISIBLE = "selectionVisible";
    private static final String KEY_REQUEST = "request";
    private static final String KEY_DARK_THEME = "darkTheme";
    private static final String KEY_LEGACY_COMPATIBILITY = "legacyCompatibility";
    private static final String KEY_DIRECTORY_RESULT_LIMIT = "directoryResultLimit";
    private static final String KEY_CHECKED_LIMIT_COUNT = "checked_limit_count";
    private static final String DIRECTORY_ID_ARG_KEY = "directoryId";

    private static final int DIRECTORY_LOADER_ID = -1;

    private static final int DIRECTORY_SEARCH_DELAY_MILLIS = 300;
    private static final int DIRECTORY_SEARCH_MESSAGE = 1;

    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

    private boolean mSectionHeaderDisplayEnabled;
    private boolean mPhotoLoaderEnabled;
    private boolean mQuickContactEnabled = true;
    private boolean mIncludeProfile;
    private boolean mSearchMode;
    private boolean mVisibleScrollbarEnabled;
    private int mVerticalScrollbarPosition = getDefaultVerticalScrollbarPosition();
    private String mQueryString;
    private int mDirectorySearchMode = DirectoryListLoader.SEARCH_MODE_NONE;
    private boolean mSelectionVisible;
    private boolean mLegacyCompatibility;

    private boolean mEnabled = true;

    private T mAdapter;
    private View mView;
    private boolean mSelecStatus;
    /**
    * SPRD:
    * 
    *
    * Original Android code:
    * private ListView mListView;
    * private Context mContext;
    * 
    * @{
    */
    protected ListView mListView;
    protected Context mContext;
    /**
    * @}
    */
    /**
     * SPRD:
     * 
     * @{
     */
     public static final int CHECKED_ITEMS_MAX = 3500;

     private static final String KEY_CHECKED_ITEM_IDS = "checkedItemIds";
     private static final String KEY_CHECKED_ITEM_OFFSET = "checkedItemoffset";
     private static final String KEY_MULTI_PICKER_SUPPORTED = "multiPickerSupported";
     private static final String KEY_FILTER = "mFilter";
     private static final String KEY_SEARCH_VISIBLE = "search_visible";
     private static final String KEY_FIRST_DIVIDER_VISIBLE = "first_divider_visible";
     private static final String KEY_SELECT_TEXT_VISIBLE = "select_text_visible";
     private static final String KEY_SELECT_DONE_STATUS = "select_done_status";

     private static final int SUBACTIVITY_BATCH_DELETE = 7;
     private boolean mMultiPickerSupported=false;
     private View mFirstDivider;
     private long[] mCheckedItems;
     private ContactListFilter mFilter;
     private boolean mSearchVisible = true;
     private boolean mFirstDividerVisible = false;
     private boolean mSelectTextVisible = true;

     private int mSelectionOffset = 0;
     private ContextMenuAdapter mContextMenuAdapter;

     private LinearLayout mFooter;
     private Button mOkBtn;
     private Button mRevertBtn;
     private BladeView mBladeView;
     private CheckBox mSelectAll;
     private TextView mSelectAllTxt;
     private TextView mSelectText;
     private RelativeLayout mSelectContacts;
     protected RelativeLayout mSearchViewContainer;
     private EditText mSearchView;
     private ImageView mClearAll;
     /**
      * @}
      */
    /**
     * Used for keeping track of the scroll state of the list.
     */
    private Parcelable mListState;

    private int mDisplayOrder;
    private int mSortOrder;
    private int mDirectoryResultLimit = DEFAULT_DIRECTORY_RESULT_LIMIT;
    private int mCheckedLimitCount = 5000;

    private ContactPhotoManager mPhotoManager;
    private ContactsPreferences mContactsPrefs;
/*SPRD @{*/
    private ContactListEmptyView mEmptyView;
/*SPRD @}*/

    private boolean mForceLoad;

    private boolean mDarkTheme;

    protected boolean mUserProfileExists;

    private static final int STATUS_NOT_LOADED = 0;
    private static final int STATUS_LOADING = 1;
    private static final int STATUS_LOADED = 2;

    private int mDirectoryListStatus = STATUS_NOT_LOADED;

    /**
     * Indicates whether we are doing the initial complete load of data (false) or
     * a refresh caused by a change notification (true)
     */
    private boolean mLoadPriorityDirectoriesOnly;

    private LoaderManager mLoaderManager;

    private Handler mDelayedDirectorySearchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DIRECTORY_SEARCH_MESSAGE) {
                loadDirectoryPartition(msg.arg1, (DirectoryPartition) msg.obj);
            }
        }
    };
    private int defaultVerticalScrollbarPosition;

    protected abstract View inflateView(LayoutInflater inflater, ViewGroup container);
    protected abstract T createListAdapter();

    /**
     * @param position Please note that the position is already adjusted for
     *            header views, so "0" means the first list item below header
     *            views.
     */
    protected abstract void onItemClick(int position, long id);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setContext(activity);
        setLoaderManager(super.getLoaderManager());
    }

    /**
     * Sets a context for the fragment in the unit test environment.
     */
    public void setContext(Context context) {
        mContext = context;
        configurePhotoLoader();
    }

    public Context getContext() {
        return mContext;
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;
            if (mAdapter != null) {
                if (mEnabled) {
                    reloadData();
                } else {
                    mAdapter.clearPartitions();
                }
            }
        }
    }

    /**
     * Overrides a loader manager for use in unit tests.
     */
    public void setLoaderManager(LoaderManager loaderManager) {
        mLoaderManager = loaderManager;
    }

    @Override
    public LoaderManager getLoaderManager() {
        return mLoaderManager;
    }

    public T getAdapter() {
        return mAdapter;
    }

    @Override
    public View getView() {
        return mView;
    }

    public ListView getListView() {
        return mListView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SECTION_HEADER_DISPLAY_ENABLED, mSectionHeaderDisplayEnabled);
        outState.putBoolean(KEY_PHOTO_LOADER_ENABLED, mPhotoLoaderEnabled);
        outState.putBoolean(KEY_QUICK_CONTACT_ENABLED, mQuickContactEnabled);
        outState.putBoolean(KEY_INCLUDE_PROFILE, mIncludeProfile);
        outState.putBoolean(KEY_SEARCH_MODE, mSearchMode);
        outState.putBoolean(KEY_VISIBLE_SCROLLBAR_ENABLED, mVisibleScrollbarEnabled);
        outState.putInt(KEY_SCROLLBAR_POSITION, mVerticalScrollbarPosition);
        outState.putInt(KEY_DIRECTORY_SEARCH_MODE, mDirectorySearchMode);
        outState.putBoolean(KEY_SELECTION_VISIBLE, mSelectionVisible);
        outState.putBoolean(KEY_LEGACY_COMPATIBILITY, mLegacyCompatibility);
        outState.putString(KEY_QUERY_STRING, mQueryString);
        outState.putInt(KEY_DIRECTORY_RESULT_LIMIT, mDirectoryResultLimit);
        outState.putBoolean(KEY_DARK_THEME, mDarkTheme);

        /**
        * SPRD:
        * 
        * @{
        */
        outState.putBoolean(KEY_MULTI_PICKER_SUPPORTED, mMultiPickerSupported);
        outState.putBoolean(KEY_SEARCH_VISIBLE, mSearchVisible);
        outState.putBoolean(KEY_FIRST_DIVIDER_VISIBLE, mFirstDividerVisible);
        outState.putBoolean(KEY_SELECT_TEXT_VISIBLE, mSelectTextVisible);
        outState.putInt(KEY_CHECKED_LIMIT_COUNT,mCheckedLimitCount);
        outState.putBoolean(KEY_SELECT_DONE_STATUS, mSelecStatus);
        if (mAdapter != null) {
            outState.putParcelable(KEY_FILTER, mAdapter.getFilter());
            if (isMultiPickerSupported()) {
                outState.putLongArray(KEY_CHECKED_ITEM_IDS, mAdapter.getAllCheckedItemIds());
                outState.putInt(KEY_CHECKED_ITEM_OFFSET, mSelectionOffset);
            }
        }
        /**
        * @}
        */
        if (mListView != null) {
            outState.putParcelable(KEY_LIST_STATE, mListView.onSaveInstanceState());
        }
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mAdapter = createListAdapter();
        mContactsPrefs = new ContactsPreferences(mContext);
        restoreSavedState(savedState);
    }

    public void restoreSavedState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        mSectionHeaderDisplayEnabled = savedState.getBoolean(KEY_SECTION_HEADER_DISPLAY_ENABLED);
        mPhotoLoaderEnabled = savedState.getBoolean(KEY_PHOTO_LOADER_ENABLED);
        mQuickContactEnabled = savedState.getBoolean(KEY_QUICK_CONTACT_ENABLED);
        mIncludeProfile = savedState.getBoolean(KEY_INCLUDE_PROFILE);
        mSearchMode = savedState.getBoolean(KEY_SEARCH_MODE);
        mVisibleScrollbarEnabled = savedState.getBoolean(KEY_VISIBLE_SCROLLBAR_ENABLED);
        mVerticalScrollbarPosition = savedState.getInt(KEY_SCROLLBAR_POSITION);
        mDirectorySearchMode = savedState.getInt(KEY_DIRECTORY_SEARCH_MODE);
        mSelectionVisible = savedState.getBoolean(KEY_SELECTION_VISIBLE);
        mLegacyCompatibility = savedState.getBoolean(KEY_LEGACY_COMPATIBILITY);
        mQueryString = savedState.getString(KEY_QUERY_STRING);
        mDirectoryResultLimit = savedState.getInt(KEY_DIRECTORY_RESULT_LIMIT);
        mDarkTheme = savedState.getBoolean(KEY_DARK_THEME);
        /**
        * SPRD:
        * 
        * @{
        */
        mMultiPickerSupported = savedState.getBoolean(KEY_MULTI_PICKER_SUPPORTED);
        mFilter = (ContactListFilter) savedState.getParcelable(KEY_FILTER);
        mSearchVisible = savedState.getBoolean(KEY_SEARCH_VISIBLE);
        mFirstDividerVisible = savedState.getBoolean(KEY_FIRST_DIVIDER_VISIBLE);
        mSelectTextVisible = savedState.getBoolean(KEY_SELECT_TEXT_VISIBLE);
        if (isMultiPickerSupported() && savedState.getLongArray(KEY_CHECKED_ITEM_IDS) != null) {
            mCheckedItems = savedState.getLongArray(KEY_CHECKED_ITEM_IDS);
            mSelectionOffset = savedState.getInt(KEY_CHECKED_ITEM_OFFSET);
        }
        mSelecStatus = savedState.getBoolean(KEY_SELECT_DONE_STATUS);
        /**
        * @}
        */

        // Retrieve list state. This will be applied in onLoadFinished
        mListState = savedState.getParcelable(KEY_LIST_STATE);
        mCheckedLimitCount = savedState.getInt(KEY_CHECKED_LIMIT_COUNT);
    }

    @Override
    public void onStart() {
        super.onStart();

        mContactsPrefs.registerChangeListener(mPreferencesChangeListener);

        mForceLoad = loadPreferences();

        mDirectoryListStatus = STATUS_NOT_LOADED;
        mLoadPriorityDirectoriesOnly = true;

        startLoading();
        /**
        * SPRD:
        * bug 263320
        * @{
        */
        if(mQueryString == null && !mSearchMode && mSearchView != null){
            mSearchView.setText("");
        }
        /**
        * @}
        */

    }

    protected void startLoading() {
        if (mAdapter == null) {
            // The method was called before the fragment was started
            return;
        }

        configureAdapter();
        int partitionCount = mAdapter.getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            Partition partition = mAdapter.getPartition(i);
            if (partition instanceof DirectoryPartition) {
                DirectoryPartition directoryPartition = (DirectoryPartition)partition;
                if (directoryPartition.getStatus() == DirectoryPartition.STATUS_NOT_LOADED) {
                    if (directoryPartition.isPriorityDirectory() || !mLoadPriorityDirectoriesOnly) {
                        startLoadingDirectoryPartition(i);
                    }
                }
            } else {
                getLoaderManager().initLoader(i, null, this);
            }
        }

        // Next time this method is called, we should start loading non-priority directories
        mLoadPriorityDirectoriesOnly = false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == DIRECTORY_LOADER_ID) {
            DirectoryListLoader loader = new DirectoryListLoader(mContext);
            loader.setDirectorySearchMode(mAdapter.getDirectorySearchMode());
            loader.setLocalInvisibleDirectoryEnabled(
                    ContactEntryListAdapter.LOCAL_INVISIBLE_DIRECTORY_ENABLED);
            return loader;
        } else {
            CursorLoader loader = createCursorLoader(mContext);
            long directoryId = args != null && args.containsKey(DIRECTORY_ID_ARG_KEY)
                    ? args.getLong(DIRECTORY_ID_ARG_KEY)
                    : Directory.DEFAULT;
            mAdapter.configureLoader(loader, directoryId);
            return loader;
        }
    }

    public CursorLoader createCursorLoader(Context context) {
        return new CursorLoader(context, null, null, null, null, null);
    }

    private void startLoadingDirectoryPartition(int partitionIndex) {
        DirectoryPartition partition = (DirectoryPartition)mAdapter.getPartition(partitionIndex);
        partition.setStatus(DirectoryPartition.STATUS_LOADING);
        long directoryId = partition.getDirectoryId();
        if (mForceLoad) {
            if (directoryId == Directory.DEFAULT) {
                loadDirectoryPartition(partitionIndex, partition);
            } else {
                loadDirectoryPartitionDelayed(partitionIndex, partition);
            }
        } else {
            Bundle args = new Bundle();
            args.putLong(DIRECTORY_ID_ARG_KEY, directoryId);
            getLoaderManager().initLoader(partitionIndex, args, this);
        }
    }

    /**
     * Queues up a delayed request to search the specified directory. Since
     * directory search will likely introduce a lot of network traffic, we want
     * to wait for a pause in the user's typing before sending a directory request.
     */
    private void loadDirectoryPartitionDelayed(int partitionIndex, DirectoryPartition partition) {
        mDelayedDirectorySearchHandler.removeMessages(DIRECTORY_SEARCH_MESSAGE, partition);
        Message msg = mDelayedDirectorySearchHandler.obtainMessage(
                DIRECTORY_SEARCH_MESSAGE, partitionIndex, 0, partition);
        mDelayedDirectorySearchHandler.sendMessageDelayed(msg, DIRECTORY_SEARCH_DELAY_MILLIS);
    }

    /**
     * Loads the directory partition.
     */
    protected void loadDirectoryPartition(int partitionIndex, DirectoryPartition partition) {
        Bundle args = new Bundle();
        args.putLong(DIRECTORY_ID_ARG_KEY, partition.getDirectoryId());
        getLoaderManager().restartLoader(partitionIndex, args, this);
    }

    /**
     * Cancels all queued directory loading requests.
     */
    private void removePendingDirectorySearchRequests() {
        mDelayedDirectorySearchHandler.removeMessages(DIRECTORY_SEARCH_MESSAGE);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!mEnabled) {
            return;
        }

        int loaderId = loader.getId();
        if (loaderId == DIRECTORY_LOADER_ID) {
            mDirectoryListStatus = STATUS_LOADED;
            mAdapter.changeDirectories(data);
            startLoading();
        } else {
            onPartitionLoaded(loaderId, data);
            if (isSearchMode()) {
                int directorySearchMode = getDirectorySearchMode();
                if (directorySearchMode != DirectoryListLoader.SEARCH_MODE_NONE) {
                    if (mDirectoryListStatus == STATUS_NOT_LOADED) {
                        mDirectoryListStatus = STATUS_LOADING;
                        getLoaderManager().initLoader(DIRECTORY_LOADER_ID, null, this);
                    } else {
                        startLoading();
                    }
                }
            } else {
                mDirectoryListStatus = STATUS_NOT_LOADED;
                getLoaderManager().destroyLoader(DIRECTORY_LOADER_ID);
            }
        }
        /**
        * SPRD:
        *       Bug 277267 After adding the contact interface, check some of the
        *       contacts, turn screen, full box is checked on the contacts and the
        *       Done button is grayed.
        * @{
        */
        if (isMultiPickerSupported()) {
            refreshRevertButton();
        }
        /**
        * @}
        */
    }

    public void onLoaderReset(Loader<Cursor> loader) {
    }

    protected void onPartitionLoaded(int partitionIndex, Cursor data) {
        if (partitionIndex >= mAdapter.getPartitionCount()) {
            // When we get unsolicited data, ignore it.  This could happen
            // when we are switching from search mode to the default mode.
            return;
        }

        mAdapter.changeCursor(partitionIndex, data);
        setProfileHeader();
        showCount(partitionIndex, data);

        if (!isLoading()) {
            completeRestoreInstanceState();
        }
    }

    public boolean isLoading() {
        if (mAdapter != null && mAdapter.isLoading()) {
            return true;
        }

        if (isLoadingDirectoryList()) {
            return true;
        }

        return false;
    }

    public boolean isLoadingDirectoryList() {
        return isSearchMode() && getDirectorySearchMode() != DirectoryListLoader.SEARCH_MODE_NONE
                && (mDirectoryListStatus == STATUS_NOT_LOADED
                        || mDirectoryListStatus == STATUS_LOADING);
    }

    @Override
    public void onStop() {
        super.onStop();
        mContactsPrefs.unregisterChangeListener();
        mAdapter.clearPartitions();
        /**
        * SPRD:
        * 
        * @{
        */
        if (mSearchView != null && !mSearchMode) {
            mSearchView.clearFocus();
            hideSoftKeyboard();
        }
        /**
        * @}
        */
    }

    protected void reloadData() {
        removePendingDirectorySearchRequests();
        mAdapter.onDataReload();
        mLoadPriorityDirectoriesOnly = true;
        mForceLoad = true;
        startLoading();
    }

    /**
     * Shows the count of entries included in the list. The default
     * implementation does nothing.
     */
    protected void showCount(int partitionIndex, Cursor data) {
        /**
        * SPRD:
        * 
        * @{
        */
        if (data == null || data.getCount() == 0) {
            prepareEmptyView();
            if (mSelectContacts != null) {
                mSelectContacts.setVisibility(View.GONE);
            }
            if (mSearchViewContainer != null && TextUtils.isEmpty(mQueryString)) {
                mSearchViewContainer.setVisibility(View.GONE);
            }
        } else {
            if (mSelectContacts != null) {
                mSelectContacts.setVisibility(View.VISIBLE);
            }
            if (mSearchViewContainer != null && mSearchVisible) {
                mSearchViewContainer.setVisibility(View.VISIBLE);
            }
            prepareListView();
        }
        /**
        * @}
        */
    }

    /**
     * Shows a view at the top of the list with a pseudo local profile prompting the user to add
     * a local profile. Default implementation does nothing.
     */
    protected void setProfileHeader() {
        mUserProfileExists = false;
    }

    /**
     * Provides logic that dismisses this fragment. The default implementation
     * does nothing.
     */
    protected void finish() {
    }

    public void setSectionHeaderDisplayEnabled(boolean flag) {
        if (mSectionHeaderDisplayEnabled != flag) {
            mSectionHeaderDisplayEnabled = flag;
            if (mAdapter != null) {
                mAdapter.setSectionHeaderDisplayEnabled(flag);
            }
            /**
            * SPRD:
            * 
            * @{
            */
            configureBladeView();
            /**
            * @}
            */
            if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
                configureVerticalScrollbar();
            }
        }
    }

    public boolean isSectionHeaderDisplayEnabled() {
        return mSectionHeaderDisplayEnabled;
    }

    public void setVisibleScrollbarEnabled(boolean flag) {
        if (mVisibleScrollbarEnabled != flag) {
            mVisibleScrollbarEnabled = flag;
            if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
                configureVerticalScrollbar();
            }
        }
    }

    public boolean isVisibleScrollbarEnabled() {
        return mVisibleScrollbarEnabled;
    }

    public void setVerticalScrollbarPosition(int position) {
        if (mVerticalScrollbarPosition != position) {
            mVerticalScrollbarPosition = position;
            if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
                configureVerticalScrollbar();
            }
        }
    }

    private void configureVerticalScrollbar() {
        boolean hasScrollbar = isVisibleScrollbarEnabled() && isSectionHeaderDisplayEnabled();

        if (mListView != null) {
			if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
				mListView.setFastScrollEnabled(hasScrollbar);
				mListView.setFastScrollAlwaysVisible(hasScrollbar);
			} else {
				mListView.setFastScrollEnabled(false);
				mListView.setFastScrollAlwaysVisible(false);
			}
            mListView.setVerticalScrollbarPosition(mVerticalScrollbarPosition);
            mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
            int leftPadding = 0;
            int rightPadding = 0;
            if (mVerticalScrollbarPosition == View.SCROLLBAR_POSITION_LEFT) {
                leftPadding = mContext.getResources().getDimensionPixelOffset(
                        R.dimen.list_visible_scrollbar_padding);
            } else {
                /**
                * SPRD:
                * 
                *
                * Original Android code:
                * rightPadding = mContext.getResources().getDimensionPixelOffset(
                        R.dimen.list_visible_scrollbar_padding);
                * 
                * @{
                */
                if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
                    rightPadding = mContext.getResources().getDimensionPixelOffset(
                            R.dimen.list_visible_scrollbar_padding);
                }
                /**
                * @}
                */
            }
            mListView.setPadding(leftPadding, mListView.getPaddingTop(),
                    rightPadding, mListView.getPaddingBottom());
        }
    }

    public void setPhotoLoaderEnabled(boolean flag) {
        mPhotoLoaderEnabled = flag;
        configurePhotoLoader();
    }

    public boolean isPhotoLoaderEnabled() {
        return mPhotoLoaderEnabled;
    }

    /**
     * Returns true if the list is supposed to visually highlight the selected item.
     */
    public boolean isSelectionVisible() {
        return mSelectionVisible;
    }

    public void setSelectionVisible(boolean flag) {
        this.mSelectionVisible = flag;
    }

    public void setQuickContactEnabled(boolean flag) {
        this.mQuickContactEnabled = flag;
    }

    public void setIncludeProfile(boolean flag) {
        mIncludeProfile = flag;
        if(mAdapter != null) {
            mAdapter.setIncludeProfile(flag);
        }
    }

    /**
     * Enter/exit search mode.  By design, a fragment enters search mode only when it has a
     * non-empty query text, so the mode must be tightly related to the current query.
     * For this reason this method must only be called by {@link #setQueryString}.
     *
     * Also note this method doesn't call {@link #reloadData()}; {@link #setQueryString} does it.
     */
    protected void setSearchMode(boolean flag) {
        if (mSearchMode != flag) {
            mSearchMode = flag;
            setSectionHeaderDisplayEnabled(!mSearchMode);

            if (!flag) {
                mDirectoryListStatus = STATUS_NOT_LOADED;
                getLoaderManager().destroyLoader(DIRECTORY_LOADER_ID);
            }

            if (mAdapter != null) {
                mAdapter.setPinnedPartitionHeadersEnabled(flag);
                mAdapter.setSearchMode(flag);

                mAdapter.clearPartitions();
                if (!flag) {
                    // If we are switching from search to regular display, remove all directory
                    // partitions after default one, assuming they are remote directories which
                    // should be cleaned up on exiting the search mode.
                    mAdapter.removeDirectoriesAfterDefault();
                }
                mAdapter.configureDefaultPartition(false, flag);
            }

			if (mListView != null) {
				if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
					mListView.setFastScrollEnabled(!flag);
				} else {
					mListView.setFastScrollEnabled(false);
				}
			}
        }
    }

    public final boolean isSearchMode() {
        return mSearchMode;
    }

    public final String getQueryString() {
        return mQueryString;
    }

    public void setQueryString(String queryString, boolean delaySelection) {
        // Normalize the empty query.
        if (TextUtils.isEmpty(queryString)) queryString = null;

        if (!TextUtils.equals(mQueryString, queryString)) {
            mQueryString = queryString;
            setSearchMode(!TextUtils.isEmpty(mQueryString));
            if (mAdapter != null) {
                mAdapter.setQueryString(queryString);
                reloadData();
            }
        }
    }

    public int getDirectoryLoaderId() {
        return DIRECTORY_LOADER_ID;
    }

    public int getDirectorySearchMode() {
        return mDirectorySearchMode;
    }

    public void setDirectorySearchMode(int mode) {
        mDirectorySearchMode = mode;
    }

    public boolean isLegacyCompatibilityMode() {
        return mLegacyCompatibility;
    }

    public void setLegacyCompatibilityMode(boolean flag) {
        mLegacyCompatibility = flag;
    }

    protected int getContactNameDisplayOrder() {
        return mDisplayOrder;
    }

    protected void setContactNameDisplayOrder(int displayOrder) {
        mDisplayOrder = displayOrder;
        if (mAdapter != null) {
            mAdapter.setContactNameDisplayOrder(displayOrder);
        }
    }

    public int getSortOrder() {
        return mSortOrder;
    }

    public void setSortOrder(int sortOrder) {
        mSortOrder = sortOrder;
        if (mAdapter != null) {
            mAdapter.setSortOrder(sortOrder);
        }
    }

    public void setDirectoryResultLimit(int limit) {
        mDirectoryResultLimit = limit;
    }

    protected boolean loadPreferences() {
        boolean changed = false;
        if (getContactNameDisplayOrder() != mContactsPrefs.getDisplayOrder()) {
            setContactNameDisplayOrder(mContactsPrefs.getDisplayOrder());
            changed = true;
        }

        if (getSortOrder() != mContactsPrefs.getSortOrder()) {
            setSortOrder(mContactsPrefs.getSortOrder());
            changed = true;
        }

        return changed;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        onCreateView(inflater, container);

        mAdapter = createListAdapter();
        mAdapter.setCheckedLimitCount(mCheckedLimitCount);

        boolean searchMode = isSearchMode();
        mAdapter.setSearchMode(searchMode);
        mAdapter.configureDefaultPartition(false, searchMode);
        mAdapter.setPhotoLoader(mPhotoManager);
        /**
        * SPRD:
        * 
        * @{
        */
        mAdapter.setMultiPickerSupported(mMultiPickerSupported);
        /**
        * @}
        */
        mListView.setAdapter(mAdapter);

        if (!isSearchMode()) {
            mListView.setFocusableInTouchMode(true);
            mListView.requestFocus();
        }

        return mView;
    }

    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        mView = inflateView(inflater, container);

        mListView = (ListView)mView.findViewById(android.R.id.list);
        /**
        * SPRD:
        * 
        * @{
        */
        mBladeView = (BladeView)getView().findViewById(R.id.blade);
        /**
        * @}
        */
        if (mListView == null) {
            throw new RuntimeException(
                    "Your content must have a ListView whose id attribute is " +
                    "'android.R.id.list'");
        }

        View emptyView = mView.findViewById(android.R.id.empty);
        if (emptyView != null) {
      //      mListView.setEmptyView(emptyView);
/*SPRD @{*/
            if (emptyView instanceof ContactListEmptyView) {
                mEmptyView = (ContactListEmptyView)emptyView;
            }
/*SPRD @}*/
        }

        mListView.setOnItemClickListener(this);
        mListView.setOnFocusChangeListener(this);
        mListView.setOnTouchListener(this);
		if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
			mListView.setFastScrollEnabled(!isSearchMode());
		} else {
			mListView.setFastScrollEnabled(false);
		}

        // Tell list view to not show dividers. We'll do it ourself so that we can *not* show
        // them when an A-Z headers is visible.
        mListView.setDividerHeight(0);

        // We manually save/restore the listview state
        mListView.setSaveEnabled(false);

        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            configureVerticalScrollbar();
        }
        configurePhotoLoader();
        /**
        * SPRD:
        * 
        * @{
        */
        if (mContextMenuAdapter != null) {
            mListView.setOnCreateContextMenuListener(mContextMenuAdapter);
        }

        configureBladeView();
        judgeCheckedLimitCount();
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            mSearchViewContainer = (RelativeLayout) getView().findViewById(
                    R.id.search_view_container);
            if (mSearchViewContainer != null) {
                if (!mSearchVisible) {
                    mSearchViewContainer.setVisibility(View.GONE);
                } else {
                    mSearchView = (EditText) getView().findViewById(
                            R.id.search_view);
                    mSearchView.addTextChangedListener(this);
                    mSearchView.clearFocus();
                    mClearAll = (ImageView) getView().findViewById(
                            R.id.clear_all_img);
                    mClearAll.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mSearchView.setText("");
                            mClearAll.setVisibility(View.GONE);
                        }
                    });
                }
            }
            if (mFirstDividerVisible) {
                mFirstDivider = getView().findViewById(R.id.divider);
                mFirstDivider.setVisibility(View.VISIBLE);
            }
        }

        if (isMultiPickerSupported()) {
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                ViewStub selectStub = (ViewStub) getView().findViewById(
                        R.id.select_contact_stub);
                if (selectStub != null) {
                    mSelectContacts = (RelativeLayout) selectStub.inflate();

                    mSelectAll = (CheckBox) mSelectContacts
                            .findViewById(R.id.select_contact_cb);
                    mSelectAllTxt = (TextView) mSelectContacts
                            .findViewById(R.id.select_all_contact);
                    mSelectAll.setOnClickListener(this);
                }
            } else {
                // show footer
                ViewStub stub = (ViewStub) getView().findViewById(R.id.footer_stub);
                if (stub != null) {
                    mFooter = (LinearLayout) stub.inflate();
                    mFooter.setVisibility(View.VISIBLE);
                    mOkBtn = (Button) mFooter.findViewById(R.id.done);
                    mRevertBtn = (Button) mFooter.findViewById(R.id.revert);
                    mRevertBtn.setText(R.string.menu_select_all);
                    mOkBtn.setEnabled(false);
                    mOkBtn.setOnClickListener(this);
                    mRevertBtn.setOnClickListener(this);
                }
            }
        } 
        /**
        * @}
        */
    }

    protected void configurePhotoLoader() {
        if (isPhotoLoaderEnabled() && mContext != null) {
            if (mPhotoManager == null) {
                mPhotoManager = ContactPhotoManager.getInstance(mContext);
            }
            if (mListView != null) {
                mListView.setOnScrollListener(this);
            }
            if (mAdapter != null) {
                mAdapter.setPhotoLoader(mPhotoManager);
            }
        }
    }

    protected void configureAdapter() {
        if (mAdapter == null) {
            return;
        }

        mAdapter.setQuickContactEnabled(mQuickContactEnabled);
        mAdapter.setIncludeProfile(mIncludeProfile);
        mAdapter.setQueryString(mQueryString);
        mAdapter.setDirectorySearchMode(mDirectorySearchMode);
        mAdapter.setPinnedPartitionHeadersEnabled(mSearchMode);
        mAdapter.setContactNameDisplayOrder(mDisplayOrder);
        mAdapter.setSortOrder(mSortOrder);
        mAdapter.setSectionHeaderDisplayEnabled(mSectionHeaderDisplayEnabled);
        mAdapter.setSelectionVisible(mSelectionVisible);
        mAdapter.setDirectoryResultLimit(mDirectoryResultLimit);
        mAdapter.setDarkTheme(mDarkTheme);
        /**
        * SPRD:
        * 
        * @{
        */
        mAdapter.setMultiPickerSupported(mMultiPickerSupported);
        if (mFilter != null) {
            mAdapter.setFilter(mFilter);
            mFilter = null;
        }
        if (isMultiPickerSupported()) {
            mAdapter.setAllCheckedItemIds(mCheckedItems);
            mCheckedItems = null;
            refreshRevertButton();
        }
        /**
        * @}
        */
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
            mPhotoManager.pause();
        } else if (isPhotoLoaderEnabled()) {
            mPhotoManager.resume();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        hideSoftKeyboard();

        int adjPosition = position - mListView.getHeaderViewsCount();
        if (adjPosition >= 0) {
            /**
            * SPRD:
            * 
            * @{
            */
            if (isMultiPickerSupported()) {
                boolean isChecked = getAdapter().isChecked(adjPosition);
                if (!isChecked
                        && getAdapter().getCurrentCheckedItems().size() >= mCheckedLimitCount) {
                    if (mCheckedLimitCount == CHECKED_ITEMS_MAX) {
                        Toast.makeText(mContext, mContext.getString(
                            R.string.contacts_selection_too_more, mCheckedLimitCount),
                            Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        Toast.makeText(mContext, mContext.getString(
                                R.string.contacts_selection_for_mms_limit, mCheckedLimitCount),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                    return;
                }
                getAdapter().setChecked(adjPosition, !isChecked);
                refreshRevertButton();
                getAdapter().notifyDataSetChanged();
                return;
            }
            /**
            * @}
            */
            onItemClick(adjPosition, id);
        }
    }

    private void hideSoftKeyboard() {
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager)
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mListView.getWindowToken(), 0);
    }

    /**
     * Dismisses the soft keyboard when the list takes focus.
     */
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == mListView && hasFocus) {
            hideSoftKeyboard();
        }
    }

    /**
     * Dismisses the soft keyboard when the list is touched.
     */
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (view == mListView) {
            hideSoftKeyboard();
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        /**
        * SPRD:
        * 
        * @{
        */
        if (mSearchView != null) {
            mSearchView.clearFocus();
        }
        /**
        * @}
        */
        removePendingDirectorySearchRequests();
    }

    /**
     * Restore the list state after the adapter is populated.
     */
    protected void completeRestoreInstanceState() {
        if (mListState != null) {
            mListView.onRestoreInstanceState(mListState);
            mListState = null;
        }
    }

    public void setDarkTheme(boolean value) {
        mDarkTheme = value;
        if (mAdapter != null) mAdapter.setDarkTheme(value);
    }

    /**
     * Processes a result returned by the contact picker.
     */
    public void onPickerResult(Intent data) {
        throw new UnsupportedOperationException("Picker result handler is not implemented.");
    }

    private ContactsPreferences.ChangeListener mPreferencesChangeListener =
            new ContactsPreferences.ChangeListener() {
        @Override
        public void onChange() {
            loadPreferences();
            reloadData();
        }
    };

    private int getDefaultVerticalScrollbarPosition() {
        final Locale locale = Locale.getDefault();
        final int layoutDirection = TextUtils.getLayoutDirectionFromLocale(locale);
        switch (layoutDirection) {
            case View.LAYOUT_DIRECTION_RTL:
                return View.SCROLLBAR_POSITION_LEFT;
            case View.LAYOUT_DIRECTION_LTR:
            default:
                return View.SCROLLBAR_POSITION_RIGHT;
        }
    }



    /**
    * SPRD:
    * 
    * @{
    */
    /**
     * Configures the empty view. It is called when we are about to populate
     * the list with an empty cursor.
     */
    protected void prepareEmptyView() {
        if (mView == null) {
            Log.e("monkey", "Exception may caused, because mView is null, dumpStack");
            Thread.dumpStack();
        } else if (mView.findViewById(R.id.list_container) == null) {
            Log.e("monkey", "Exception may caused, because mView.findViewById is null, dumpStack");
            Thread.dumpStack();
        } else {
            mView.findViewById(R.id.list_container).setVisibility(View.GONE);
        }
        mListView.setVisibility(View.GONE);
        if (mFooter != null) {
            mFooter.setVisibility(View.GONE);
        }
    }

    protected void prepareListView() {
        if (mView == null) {
            Log.e("monkey", "Exception may caused, because mView is null, dumpStack");
            Thread.dumpStack();
        } else if (mView.findViewById(R.id.list_container) == null) {
            Log.e("monkey", "Exception may caused, because mView.findViewById is null, dumpStack");
            Thread.dumpStack();
        } else {
            mView.findViewById(R.id.list_container).setVisibility(View.VISIBLE);
        }
        mListView.setVisibility(View.VISIBLE);
        if (mFooter != null) {
            mFooter.setVisibility(View.VISIBLE);
            refreshRevertButton();
        }
        if (getEmptyView() != null) {
            getEmptyView().setVisibility(View.GONE);
        }
    }

    public void setMultiPickerSupported(boolean suppported) {
        mMultiPickerSupported = suppported;
    }

    public boolean isMultiPickerSupported() {
        return mMultiPickerSupported;
    }

    private void configureBladeView() {
        boolean hasBladeView = isSectionHeaderDisplayEnabled();
        if (mListView != null) {
            BladeView bladeView = (BladeView) getView().findViewById(R.id.blade);
            if (bladeView == null) {
                return;
            }
            if (!hasBladeView || !UniverseUtils.UNIVERSEUI_SUPPORT) {
                bladeView.setVisibility(View.GONE);
                return;
            }
            final String[] alphabet = bladeView.getFullAlphabet(bladeView.isTw(mContext));
            bladeView.setVisibility(View.VISIBLE);
            bladeView.setOnItemClickListener(new BladeView.OnItemClickListener() {
                public void onItemClick(int item) {
                    ContactsSectionIndexer indexer = (ContactsSectionIndexer) getAdapter()
                            .getIndexer();
                    if (indexer == null) {
                        return;
                    }

                    String currentSection = alphabet[item];
                    int currentSectionItem = -1;
                    Object[] sections = indexer.getSections();
                    for (int i = 0; i < sections.length; ++i) {
                        String s = (String) sections[i];
                        if (s.equals(currentSection)) {
                            currentSectionItem = i;
                        }
                    }
                    if (currentSectionItem == -1) {
                        return;
                    }
                    int position = indexer.getPositionForSection(currentSectionItem);
                    if (position == -1) {
                        return;
                    }
                    int adjPosition = position + mListView.getHeaderViewsCount();
                    if (Constants.DEBUG)
                        Log.d(TAG, "onItemClick:" + item + " " + currentSectionItem + " "
                                + adjPosition + " " + mListView.getHeaderViewsCount());
                    mListView.setSelectionFromTop(adjPosition, 0);
                }
            });
        }
    }

    public void setContextMenuAdapter(ContextMenuAdapter adapter) {
        mContextMenuAdapter = adapter;
        if (mListView != null) {
            mListView.setOnCreateContextMenuListener(adapter);
        }
    }

    public ContextMenuAdapter getContextMenuAdapter() {
        return mContextMenuAdapter;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSearchView != null) {
            if (mSearchMode) {
                mSearchView.requestFocus();
            } else {
                mSearchView.clearFocus();
                hideSoftKeyboard();
            }
        }
    }

    /**
     * Dismisses the search UI along with the keyboard if the filter text is
     * empty.
     */
    public void onClose() {
        hideSoftKeyboard();
        finish();
    }

    protected void setEmptyText(int resourceId) {
        if (getEmptyView() == null) {
            return;
        }
        getEmptyView().setVisibility(View.VISIBLE);

        TextView empty = (TextView) getEmptyView().findViewById(R.id.emptyText);
        if (UniverseUtils.UNIVERSEUI_SUPPORT && !TextUtils.isEmpty(mQueryString)) {
            empty.setText(mContext.getText(R.string.no_match_contact));
        } else {
            empty.setText(mContext.getText(resourceId));
        }
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            //empty.setPadding(0, 80, 0, 0);
            empty.setGravity(Gravity.CENTER);
        }

        empty.setVisibility(View.VISIBLE);
    }

    // TODO redesign into an async task or loader
    protected boolean isSyncActive() {
        Account[] accounts = AccountManager.get(mContext).getAccounts();
        if (accounts != null && accounts.length > 0) {
            IContentService contentService = ContentResolver.getContentService();
            for (Account account : accounts) {
                try {
                    if (contentService.isSyncActive(account, ContactsContract.AUTHORITY)) {
                        return true;
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Could not get the sync status");
                }
            }
        }
        return false;
    }

    public void setFilter(ContactListFilter filter) {

    }

    protected boolean hasIccCard() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.hasIccCard();
    }

    @Override
    public void onClick(View v) {
        ArrayList<String> ret = new ArrayList<String>();
        ContactEntryListAdapter adapter = getAdapter();
        if (v == mOkBtn) {
            Intent intent = getActivity().getIntent();
            int mode = 0;
            if (intent != null) {
                mode = intent.getIntExtra("mode", 0);
            }
            if (mode == SUBACTIVITY_BATCH_DELETE) {
                new AlertDialog.Builder(getActivity())
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        onMultiPickerSelected();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_delete_contact)
                        .show();
            } else {
                onMultiPickerSelected();
            }
        } else if (v == mRevertBtn) {
            toggleRevertButton();
        } else if (v == mSelectAll) {
            toggleRevertButton();
        }
    }

    public void onMultiPickerSelected() {
    }

    private void toggleRevertButton() {
        ContactEntryListAdapter adapter = getAdapter();
        int currentPosition = mListView.getLastVisiblePosition();
        if (adapter.isAllChecked()) {
            adapter.checkAll(false, mSelectionOffset);
            mSelectionOffset = 0;
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                mSelectAllTxt.setText(R.string.select_all_contacts);
                mSelectAll.setChecked(false);
                refreshDone(false);
            } else {
                mRevertBtn.setText(R.string.menu_select_all);
                mOkBtn.setEnabled(false);
            }
        } else {
            if (currentPosition + 1 <= mCheckedLimitCount) {
                adapter.checkAll(true, 0);
            } else {
                mSelectionOffset = currentPosition + 1 - mCheckedLimitCount;
                adapter.checkAll(true, mSelectionOffset);
            }

            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                mSelectAllTxt.setText(R.string.cancel_select_all_contacts);
                mSelectAll.setChecked(true);
                refreshDone(true);
            } else {
                mRevertBtn.setText(R.string.menu_select_none);
                mOkBtn.setEnabled(true);
            }
            if (adapter.getCount() > mCheckedLimitCount) {
                if (mCheckedLimitCount == CHECKED_ITEMS_MAX) {
                    Toast.makeText(mContext, mContext.getString(
                            R.string.contacts_selection_too_more, mCheckedLimitCount),
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(mContext, mContext.getString(
                            R.string.contacts_selection_for_mms_limit, mCheckedLimitCount),
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void refreshRevertButton() {
        ContactEntryListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        if (adapter.isAllChecked()) {
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                mSelectAllTxt.setText(R.string.cancel_select_all_contacts);
                mSelectAll.setChecked(true);
                refreshDone(true);
            } else {
                mRevertBtn.setText(R.string.menu_select_none);
                mOkBtn.setEnabled(true);
            }

        } else {
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                mSelectAllTxt.setText(R.string.select_all_contacts);
                mSelectAll.setChecked(false);
                if (adapter.hasCheckedItems()) {
                    refreshDone(true);
                } else {
                    refreshDone(false);
                }
            } else {
                mRevertBtn.setText(R.string.menu_select_all);
                if (adapter.hasCheckedItems()) {
                    mOkBtn.setEnabled(true);
                } else {
                    mOkBtn.setEnabled(false);
                }

            }
        }
    }

    protected int getListFilterId() {
        return R.string.list_filter_all_accounts;
    }

    @Override
    public void afterTextChanged(Editable s) {
        // TODO Auto-generated method stub
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTextChanged(CharSequence queryString, int start, int before, int count) {
        // TODO Auto-generated method stub
        if (!TextUtils.isEmpty(queryString.toString().trim())) {
            mClearAll.setVisibility(View.VISIBLE);
        } else {
            mClearAll.setVisibility(View.GONE);
        }
        setQueryString(queryString.toString().trim(), true);
    }

    private void refreshDone(boolean enabled) {
    	mSelecStatus = enabled;
    	Intent intent = new Intent();
    	intent.setAction("com.android.contacts.common.action.SSU");
    	getActivity().sendBroadcast(intent);
    }

    public boolean getSelecStatus() {
        return mSelecStatus;
    }
    
    public void setSearchVisible(boolean visible) {
        mSearchVisible = visible;
    }

    public void setFirstDividerVisible(boolean visible) {
        mFirstDividerVisible = visible;
    }

    public void setSelectTextVisible(boolean visible) {
        mSelectTextVisible = visible;
    }

    public void clearToggleRevert() {
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            mSelectAllTxt.setText(R.string.select_all_contacts);
            mSelectAll.setChecked(false);
        }
    }

    public void clearAllCheckItems() {
        mCheckedItems = null;
        mAdapter.clearCheckedItems();
        mAdapter.notifyDataSetChanged();
    }

    public View getEmptyView() {
       // return mListView.getEmptyView();
       return mEmptyView;
    }

    public void judgeCheckedLimitCount() {
        Intent intent = getActivity().getIntent();
        if(intent != null){
            mCheckedLimitCount = intent.getIntExtra(KEY_CHECKED_LIMIT_COUNT,CHECKED_ITEMS_MAX);
        }
    }
    
    //bug  264659
    public void setSearchViewText(String queryString){
        if(!TextUtils.isEmpty(queryString) && mSearchView != null){
            mSearchView.setText(queryString);
            mSearchView.setSelection(queryString.length());
        }
    }
    /**
    * @}
    */
}
