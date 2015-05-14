/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.editor;

import android.content.Context;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.contacts.ContactsApplication;
import com.android.contacts.R;
import com.android.contacts.editor.Editor.EditorListener;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.dataitem.DataKind;
import com.sprd.contacts.common.util.UniverseUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom view for an entire section of data as segmented by
 * {@link DataKind} around a {@link Data#MIMETYPE}. This view shows a
 * section header and a trigger for adding new {@link Data} rows.
 */
public class KindSectionView extends LinearLayout implements EditorListener {
    private static final String TAG = "KindSectionView";

    private TextView mTitle;
    private ViewGroup mEditors;
    private View mAddFieldFooter;
    private String mTitleString;

    private DataKind mKind;
    private RawContactDelta mState;
    private boolean mReadOnly;

    private ViewIdGenerator mViewIdGenerator;

    private LayoutInflater mInflater;

    private final ArrayList<Runnable> mRunWhenWindowFocused = new ArrayList<Runnable>(1);

	private int mSaveImportFlag;

	private String mCurrentAccountType;

	private String mCurrentAccountName;

    public KindSectionView(Context context) {
        this(context, null);
    }

    public KindSectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mEditors != null) {
            int childCount = mEditors.getChildCount();
            for (int i = 0; i < childCount; i++) {
                mEditors.getChildAt(i).setEnabled(enabled);
            }
        }
        /**
        * SPRD:
        *   for UUI
        *
        * Original Android code:
        * 
        * 
        * @{
        */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
        /**
        * @}
        */
            if (enabled && !mReadOnly) {
    			if (ContactsApplication.getApplication().getString(
    					R.string.show_privatecontacts)
    					.equals(mCurrentAccountName)
    					|| mSaveImportFlag == RawContacts.CONTACTS_BY_PCTOOLS || mSaveImportFlag == RawContacts.CONTACTS_BY_EDITE ) {
    				 mAddFieldFooter.setVisibility(View.GONE);
    			} else {
    				mAddFieldFooter.setVisibility(View.VISIBLE);
    			}
            } else {
                mAddFieldFooter.setVisibility(View.GONE);
            }
        }
    }

    public boolean isReadOnly() {
        return mReadOnly;
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mTitle = (TextView) findViewById(R.id.kind_title);
        mEditors = (ViewGroup) findViewById(R.id.kind_editors);
        /**
        * SPRD:
        *   for UUI Bug 277915 optimization the time of create new contact.
        *
        * Original Android code:
            mAddFieldFooter = findViewById(R.id.add_field_footer);
            mAddFieldFooter.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Setup click listener to add an empty field when the
                    // footer is clicked.
                    mAddFieldFooter.setVisibility(View.GONE);
                    addItem();
                }
            });
        * 
        * 
        * @{
        */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            mAddFieldFooter = findViewById(R.id.add_field_footer);
            mAddFieldFooter.setVisibility(View.GONE);
            mAddFieldFooter.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Setup click listener to add an empty field when the
                    // footer is clicked.
                    mAddFieldFooter.setVisibility(View.GONE);
                    addItem();
                }
            });
        }
        /**
        * @}
        */
    }

    @Override
    public void onDeleteRequested(Editor editor) {
        // If there is only 1 editor in the section, then don't allow the user to delete it.
        // Just clear the fields in the editor.
        /**
         * SPRD: for UUI Original
         * 
         * Android code: 
         * if (getEditorCount() == 1) {
         * editor.clearAllFields(); 
         * } else {
         *  // Otherwise it's okay to delete this {@link Editor} 
         *  editor.deleteEditor(); }
         * 
         * @{
         */
        final boolean animate;
        if (getEditorCount() == 1) {
            editor.clearAllFields();
            animate = true;
        } else {
            // Otherwise it's okay to delete this {@link Editor}
            editor.deleteEditor();

            // This is already animated, don't do anything further here
            animate = false;
        }
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            updateAddFooterVisible(animate);
        } else {
            updateAddMoreEntry();
        }

        /**
        * @}
        */
    }

    @Override
    public void onRequest(int request) {
        // If a field has become empty or non-empty, then check if another row
        // can be added dynamically.
        if (request == FIELD_TURNED_EMPTY || request == FIELD_TURNED_NON_EMPTY) {
            /**
            * SPRD:
            *   for UUI
            *
            * Original Android code:
            * updateAddFooterVisible(true);
            * 
            * @{
            */
            if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
                updateAddFooterVisible(true);
            } else {
                updateAddMoreEntry();
            }
        } else if (request == FIELD_SELECTION_CHANGED) {
            // notify editors to update selection
            updateEditorTypeList();
            /**
            * @}
            */
            
        }
    }

    public void setState(DataKind kind, RawContactDelta state, boolean readOnly, ViewIdGenerator vig) {
        mKind = kind;
        mState = state;
        mReadOnly = readOnly;
        mViewIdGenerator = vig;
        
        /**
         * add by xuhong.tian
         */
        mSaveImportFlag = state.getValues().getAsInteger(Contacts.CONTACT_IMPORT_TYPE, -1);
        mCurrentAccountType=state.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
        mCurrentAccountName=state.getValues().getAsString(RawContacts.ACCOUNT_NAME);
        
        Log.d(TAG, "mSaveImportFlag="+mSaveImportFlag+"mCurrentAccountType="+mCurrentAccountType);

        setId(mViewIdGenerator.getId(state, kind, null, ViewIdGenerator.NO_VIEW_INDEX));

        // TODO: handle resources from remote packages
        mTitleString = (kind.titleRes == -1 || kind.titleRes == 0)
                ? ""
                : getResources().getString(kind.titleRes);
        mTitle.setText(mTitleString);

        rebuildFromState();
        /**
        * SPRD:
        *   for UUI
        *
        * Original Android code:
        * updateAddFooterVisible(false);
        * 
        * @{
        */
        if (UniverseUtils.UNIVERSEUI_SUPPORT){
            if (getEditorCount() != 0) {
                updateAddMoreEntry();
            }
        }else{
            updateAddFooterVisible(false);
        }
        /**
        * @}
        */
        
        updateSectionVisible();
    }

    public String getTitle() {
        return mTitleString;
    }

    public void setTitleVisible(boolean visible) {
        findViewById(R.id.kind_title_layout).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Build editors for all current {@link #mState} rows.
     */
    public void rebuildFromState() {
        // Remove any existing editors
        mEditors.removeAllViews();

        // Check if we are displaying anything here
        boolean hasEntries = mState.hasMimeEntries(mKind.mimeType);

        if (hasEntries) {
            for (ValuesDelta entry : mState.getMimeEntries(mKind.mimeType)) {
                // Skip entries that aren't visible
                if (!entry.isVisible()) continue;
                if (isEmptyNoop(entry)) continue;

                createEditorView(entry);
            }
        }
    }


    /**
     * Creates an EditorView for the given entry. This function must be used while constructing
     * the views corresponding to the the object-model. The resulting EditorView is also added
     * to the end of mEditors
     */
    private View createEditorView(ValuesDelta entry) {
        final View view;
        final int layoutResId = EditorUiUtils.getLayoutResourceId(mKind.mimeType);
        try {
            view = mInflater.inflate(layoutResId, mEditors, false);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot allocate editor with layout resource ID " +
                    layoutResId + " for MIME type " + mKind.mimeType +
                    " with error " + e.toString());
        }

        view.setEnabled(isEnabled());

        if (view instanceof Editor) {
            Editor editor = (Editor) view;
            editor.setDeletable(true);
            editor.setValues(mKind, entry, mState, mReadOnly, mViewIdGenerator);
            editor.setEditorListener(this);
        }
        mEditors.addView(view);
        return view;
    }

    /**
     * Tests whether the given item has no changes (so it exists in the database) but is empty
     */
    private boolean isEmptyNoop(ValuesDelta item) {
        if (!item.isNoop()) return false;
        final int fieldCount = mKind.fieldList.size();
        for (int i = 0; i < fieldCount; i++) {
            final String column = mKind.fieldList.get(i).column;
            final String value = item.getAsString(column);
            if (!TextUtils.isEmpty(value)) return false;
        }
        return true;
    }

    private void updateSectionVisible() {
        /**
        * SPRD:
        *   fix bug106375 unefon 365 Usim,pbr is not existed,read adn
        *
        * Original Android code:
        * setVisibility(getEditorCount() != 0 ? VISIBLE : GONE);
        * 
        * @{
        */
        setVisibility((getEditorCount() != 0 && mKind.typeOverallMax != -2) ? VISIBLE : GONE);
        /**
        * @}
        */
        
    }

    protected void updateAddFooterVisible(boolean animate) {
        if (!mReadOnly && (mKind.typeOverallMax != 1)) {
            // First determine whether there are any existing empty editors.
            updateEmptyEditors();
            // If there are no existing empty editors and it's possible to add
            // another field, then make the "add footer" field visible.
            if (!hasEmptyEditor() && RawContactModifier.canInsert(mState, mKind)) {
            	
            	Log.d(TAG, "mSaveImportFlag=="+mSaveImportFlag+"animate="+animate);
				if (animate) {
					if (ContactsApplication.getApplication()
							.getString(R.string.show_privatecontacts)
							.equals(mCurrentAccountName)
							|| mSaveImportFlag == RawContacts.CONTACTS_BY_PCTOOLS
							|| mSaveImportFlag == RawContacts.CONTACTS_BY_EDITE) {

					} else {
						EditorAnimator.getInstance().showAddFieldFooter(
								mAddFieldFooter);
					}

				} else {
    				if (ContactsApplication.getApplication().getString(
    						R.string.show_privatecontacts)
    						.equals(mCurrentAccountName)
    						|| mSaveImportFlag == RawContacts.CONTACTS_BY_PCTOOLS || mSaveImportFlag == RawContacts.CONTACTS_BY_EDITE ) {

    					Log.d(TAG, "mSaveImportFlag=="+mSaveImportFlag);
    					 mAddFieldFooter.setVisibility(View.GONE);
    				} else {
    					mAddFieldFooter.setVisibility(View.VISIBLE);
    				}
                }
                return;
            }
        }
        if (animate) {
            EditorAnimator.getInstance().hideAddFieldFooter(mAddFieldFooter);
        } else {
            mAddFieldFooter.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the editors being displayed to the user removing extra empty
     * {@link Editor}s, so there is only max 1 empty {@link Editor} view at a time.
     */
    private void updateEmptyEditors() {
        List<View> emptyEditors = getEmptyEditors();

        // If there is more than 1 empty editor, then remove it from the list of editors.
        if (emptyEditors.size() > 1) {
            for (View emptyEditorView : emptyEditors) {
                // If no child {@link View}s are being focused on within
                // this {@link View}, then remove this empty editor.
                if (emptyEditorView.findFocus() == null) {
                    /**
                    * SPRD:
                    *   fix bug164244 delete the content of both the phone number editor of usim contact, then input to the editor again, the second  edit text is no longer displayed
                    *
                    * Original Android code:
                    * 
                    * 
                    * @{
                    */
                    LabeledEditorView editor = (LabeledEditorView)emptyEditorView;
                    ValuesDelta enty = editor.getValues();
                    enty.markDeleted();
                    /**
                    * @}
                    */
                    mEditors.removeView(emptyEditorView);
                }
            }
        }
    }

    /**
     * Returns a list of empty editor views in this section.
     */
    private List<View> getEmptyEditors() {
        List<View> emptyEditorViews = new ArrayList<View>();
        for (int i = 0; i < mEditors.getChildCount(); i++) {
            View view = mEditors.getChildAt(i);
            if (((Editor) view).isEmpty()) {
                emptyEditorViews.add(view);
            }
        }
        return emptyEditorViews;
    }

    /**
     * Returns true if one of the editors has all of its fields empty, or false
     * otherwise.
     */
    private boolean hasEmptyEditor() {
        return getEmptyEditors().size() > 0;
    }

    /**
     * Returns true if all editors are empty.
     */
    public boolean isEmpty() {
        for (int i = 0; i < mEditors.getChildCount(); i++) {
            View view = mEditors.getChildAt(i);
            if (!((Editor) view).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extends superclass implementation to also run tasks
     * enqueued by {@link #runWhenWindowFocused}.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            for (Runnable r: mRunWhenWindowFocused) {
                r.run();
            }
            mRunWhenWindowFocused.clear();
        }
    }

    /**
     * Depending on whether we are in the currently-focused window, either run
     * the argument immediately, or stash it until our window becomes focused.
     */
    private void runWhenWindowFocused(Runnable r) {
        if (hasWindowFocus()) {
            r.run();
        } else {
            mRunWhenWindowFocused.add(r);
        }
    }

    /**
     * Simple wrapper around {@link #runWhenWindowFocused}
     * to ensure that it runs in the UI thread.
     */
    private void postWhenWindowFocused(final Runnable r) {
        post(new Runnable() {
            @Override
            public void run() {
                runWhenWindowFocused(r);
            }
        });
    }

    public void addItem() {
        ValuesDelta values = null;
        // If this is a list, we can freely add. If not, only allow adding the first.
        if (mKind.typeOverallMax == 1) {
            if (getEditorCount() == 1) {
                return;
            }

            // If we already have an item, just make it visible
            ArrayList<ValuesDelta> entries = mState.getMimeEntries(mKind.mimeType);
            if (entries != null && entries.size() > 0) {
                values = entries.get(0);
            }
        }

        // Insert a new child, create its view and set its focus
        if (values == null) {
            values = RawContactModifier.insertChild(mState, mKind);
        }

        final View newField = createEditorView(values);
        if (newField instanceof Editor) {
            /**
            * SPRD:
            *   Bug 266961
            *   New Contact to the machine, add another field, add notes, nickname,
            *   when Internet telephony, the cursor does not jump to the corresponding
            *   edit box to add items.
            * 
            * 
            * @{
            */
            if (mCanRequestFocus) {
            /**
            * @}
            */
                postWhenWindowFocused(new Runnable() {
                    @Override
                    public void run() {
                        newField.requestFocus();
                        ((Editor) newField).editNewlyAddedField();
                    }
                });
            }
        }

        /**
        * SPRD:
        *   for UUI Bug 277915 optimization the time of create new contact.
        *
        * Original Android code:
           // Hide the "add field" footer because there is now a blank field.
            mAddFieldFooter.setVisibility(View.GONE);
        *
        * @{
        */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            // Hide the "add field" footer because there is now a blank field.
            mAddFieldFooter.setVisibility(View.GONE);
        }
        /**
        * @}
        */

        // Ensure we are visible
        updateSectionVisible();
    }

    public int getEditorCount() {
        return mEditors.getChildCount();
    }

    public DataKind getKind() {
        return mKind;
    }
    
    /**
    * SPRD:
    *   for UUI
    *
    * Original Android code:
    * 
    * 
    * @{
    */
    private boolean mCanRequestFocus = true;

    protected void updateAddMoreEntry() {
        if (!mReadOnly && mKind != null && (mKind.typeOverallMax != 1)) {
            // First determine whether there are any existing empty editors.
            updateEmptyEditors();
            // If there are no existing empty editors and it's possible to add
            // another field, then make the "add footer" field visible.
            if (!hasEmptyEditor() && RawContactModifier.canInsert(mState, mKind)) {
                mCanRequestFocus = false;
                addItem();
                return;
            } else {
                mCanRequestFocus = true;
            }
        }
    }

    private void updateEditorTypeList() {
        if (mEditors != null) {
            int childCount = mEditors.getChildCount();
            for (int i = 0; i < childCount; i++) {
                Editor editor = (Editor) mEditors.getChildAt(i);
                editor.onTypeListChanged();
            }
        }
    }

    /**
    * @}
    */

    /*
    * SPRD: bug 261263
    * 
    * @{
    */
    public void setActionDone() {
        int childCount = mEditors.getChildCount();
        Editor editor = (Editor) mEditors.getChildAt(childCount - 1);
        editor.setActionDone();
    }
    /*
    * @}
    */
}
