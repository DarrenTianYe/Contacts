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

package com.android.contacts.editor;

import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.accounts.Account;
import com.android.contacts.common.model.AccountTypeManager;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import com.sprd.contacts.common.model.account.USimAccountType;
import com.sprd.contacts.common.model.account.SimAccountType;
import com.android.contacts.R;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType.EditField;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.util.PhoneNumberFormatter;
import com.sprd.contacts.common.util.UniverseUtils;

import java.util.ArrayList;


/**
 * Simple editor that handles labels and any {@link EditField} defined for the
 * entry. Uses {@link ValuesDelta} to read any existing {@link RawContact} values,
 * and to correctly write any changes values.
 */
public class TextFieldsEditorView extends LabeledEditorView {
    private static final String TAG = TextFieldsEditorView.class.getSimpleName();

    private EditText[] mFieldEditTexts = null;
    private ViewGroup mFields = null;
    private View mExpansionViewContainer;
    private ImageView mExpansionView;
    private boolean mHideOptional = true;
    private boolean mHasShortAndLongForms;
    private int mMinFieldHeight;
    private int mPreviousViewHeight;

    public TextFieldsEditorView(Context context) {
        super(context);
    }

    public TextFieldsEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextFieldsEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mMinFieldHeight = mContext.getResources().getDimensionPixelSize(
                R.dimen.editor_min_line_item_height);
        /**
        * SPRD:
        *   for UUI
        *
        * Original Android code:
        * 
        * 
        * @{
        */
        if (UniverseUtils.UNIVERSEUI_SUPPORT){
            mMinFieldHeight = mContext.getResources().getDimensionPixelSize(
                    R.dimen.editor_min_line_item_height_overlay);
        }
        /**
        * @}
        */
        mFields = (ViewGroup) findViewById(R.id.editors);
        mExpansionView = (ImageView) findViewById(R.id.expansion_view);
        mExpansionViewContainer = findViewById(R.id.expansion_view_container);
        mExpansionViewContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreviousViewHeight = mFields.getHeight();

                // Save focus
                final View focusedChild = getFocusedChild();
                final int focusedViewId = focusedChild == null ? -1 : focusedChild.getId();

                // Reconfigure GUI
                mHideOptional = !mHideOptional;
                onOptionalFieldVisibilityChange();
                rebuildValues();

                // Restore focus
                View newFocusView = findViewById(focusedViewId);
                if (newFocusView == null || newFocusView.getVisibility() == GONE) {
                    // find first visible child
                    newFocusView = TextFieldsEditorView.this;
                }
                newFocusView.requestFocus();

                EditorAnimator.getInstance().slideAndFadeIn(mFields, mPreviousViewHeight);
            }
        });
    }

    @Override
    public void editNewlyAddedField() {
        // Some editors may have multiple fields (eg: first-name/last-name), but since the user
        // has not selected a particular one, it is reasonable to simply pick the first.
        final View editor = mFields.getChildAt(0);

        // Show the soft-keyboard.
        InputMethodManager imm =
                (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (!imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)) {
                Log.w(TAG, "Failed to show soft input method.");
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (mFieldEditTexts != null) {
            for (int index = 0; index < mFieldEditTexts.length; index++) {
                mFieldEditTexts[index].setEnabled(!isReadOnly() && enabled);
            }
        }
        mExpansionView.setEnabled(!isReadOnly() && enabled);
    }

    /**
     * Creates or removes the type/label button. Doesn't do anything if already correctly configured
     */
    private void setupExpansionView(boolean shouldExist, boolean collapsed) {
        if (shouldExist) {
            mExpansionViewContainer.setVisibility(View.VISIBLE);
            mExpansionView.setImageResource(collapsed
                    ? R.drawable.ic_menu_expander_minimized_holo_light
                    : R.drawable.ic_menu_expander_maximized_holo_light);
        } else {
            mExpansionViewContainer.setVisibility(View.GONE);
        }
    }

    @Override
    protected void requestFocusForFirstEditField() {
        if (mFieldEditTexts != null && mFieldEditTexts.length != 0) {
            EditText firstField = null;
            boolean anyFieldHasFocus = false;
            for (EditText editText : mFieldEditTexts) {
                if (firstField == null && editText.getVisibility() == View.VISIBLE) {
                    firstField = editText;
                }
                if (editText.hasFocus()) {
                    anyFieldHasFocus = true;
                    break;
                }
            }
            if (!anyFieldHasFocus && firstField != null) {
                firstField.requestFocus();
            }
        }
    }

    public void setValue(int field, String value) {
        mFieldEditTexts[field].setText(value);
    }

    @Override
    public void setValues(DataKind kind, ValuesDelta entry, RawContactDelta state, boolean readOnly,
            ViewIdGenerator vig) {
        super.setValues(kind, entry, state, readOnly, vig);
        // Remove edit texts that we currently have
        if (mFieldEditTexts != null) {
            for (EditText fieldEditText : mFieldEditTexts) {
                mFields.removeView(fieldEditText);
            }
        }
        boolean hidePossible = false;
        /**
        * SPRD:
        *   fix bug115812,save contacts time is too long after input much more strings
        *
        * Original Android code:
        * 
        * 
        * @{
        */
        final String mimeType = kind.mimeType;
        final String accountName = state.getValues().getAsString(RawContacts.ACCOUNT_NAME);
        final String accountType = state.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
        /**
        * @}
        */
        int fieldCount = kind.fieldList.size();
        mFieldEditTexts = new EditText[fieldCount];
        for (int index = 0; index < fieldCount; index++) {
            final EditField field = kind.fieldList.get(index);
            final EditText fieldView = new EditText(mContext);
            fieldView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    field.isMultiLine() ? LayoutParams.WRAP_CONTENT : mMinFieldHeight));
            // Set either a minimum line requirement or a minimum height (because {@link TextView}
            // only takes one or the other at a single time).
            if (field.minLines != 0) {
                fieldView.setMinLines(field.minLines);
            } else {
                fieldView.setMinHeight(mMinFieldHeight);
            }
            fieldView.setTextAppearance(getContext(), android.R.style.TextAppearance_Medium);
            /**
            * SPRD:
            *   fix bug174116 New Contact, edit box cursor not vertically centered over the upper
            *
            * Original Android code:
            * fieldView.setGravity(Gravity.TOP);
            * 
            * @{
            */
            fieldView.setGravity(Gravity.CENTER_VERTICAL);
            /**
            * @}
            */
            mFieldEditTexts[index] = fieldView;
            fieldView.setId(vig.getId(state, kind, entry, index));
            if (field.titleRes > 0) {
                fieldView.setHint(field.titleRes);
            }
            int inputType = field.inputType;
            fieldView.setInputType(inputType);
            if (inputType == InputType.TYPE_CLASS_PHONE) {
                PhoneNumberFormatter.setPhoneNumberFormattingTextWatcher(mContext, fieldView);
                fieldView.setTextDirection(View.TEXT_DIRECTION_LTR);
            }

            // Show the "next" button in IME to navigate between text fields
            // TODO: Still need to properly navigate to/from sections without text fields,
            // See Bug: 5713510
            fieldView.setImeOptions(EditorInfo.IME_ACTION_NEXT);

            // Read current value from state
            final String column = field.column;
            final String value = entry.getAsString(column);
            /**
            * SPRD:
            *   fix bug254440 Create a contact name editor to paste 400k character, a "contact no response."
            *
            * @{
            */
            fieldView.setFilters(new  InputFilter[]{ new  InputFilter.LengthFilter(140)});
            /**
            * @}
            */
            fieldView.setText(value);

            // Show the delete button if we have a non-null value
            /**
            * SPRD:
            *   fix bug172528 Editors are of the number of the sim card contacts. Enter the editing interface, press number input box at the back of the delete button. Number is cleared, the delete button is still displayed. Should be changed to not display
            *
            * Original Android code:
            * setDeleteButtonVisible(value != null);
            * 
            * @{
            */
            boolean isEmpty = TextUtils.isEmpty(value);
            setDeleteButtonVisible(!isEmpty);
            setIfTextEmpty(isEmpty);
            /**
            * @}
            */

            // Prepare listener for writing changes
            fieldView.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    // Trigger event for newly changed value
                    /**
                    * SPRD:
                    *   fix bug115812,save contacts time is too long after input much more strings
                    *
                    * Original Android code:
                    * onFieldChanged(column, s.toString());
                    * 
                    * @{
                    */
                    String str= deleteEditable(mimeType, accountName, accountType, s,  fieldView);
                    if(str != null){
                        onFieldChanged(column, str);
                    }
                    /**
                    * @}
                    */
                    
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });

            fieldView.setEnabled(isEnabled() && !readOnly);

            if (field.shortForm) {
                hidePossible = true;
                mHasShortAndLongForms = true;
                fieldView.setVisibility(mHideOptional ? View.VISIBLE : View.GONE);
            } else if (field.longForm) {
                hidePossible = true;
                mHasShortAndLongForms = true;
                fieldView.setVisibility(mHideOptional ? View.GONE : View.VISIBLE);
            } else {
                // Hide field when empty and optional value
                final boolean couldHide = (!ContactsUtils.isGraphic(value) && field.optional);
                final boolean willHide = (mHideOptional && couldHide);
                fieldView.setVisibility(willHide ? View.GONE : View.VISIBLE);
                hidePossible = hidePossible || couldHide;
            }
            /**
            * SPRD:
            *   fix bug180878 make the layout of the ContactEditorActivity to automatically adjust to the screen
            *
            * Original Android code:
            * 
            * 
            * @{
            */
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                int leftPadding = mContext.getResources().getDimensionPixelSize(
                        R.dimen.editor_left_padding);
                int rightPadding = mContext.getResources().getDimensionPixelSize(
                        R.dimen.editor_other_padding);
                fieldView.setPadding(leftPadding, rightPadding, rightPadding, rightPadding);
                fieldView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources()
                        .getDimensionPixelSize(
                                R.dimen.editor_text_size));
                fieldView.setTextColor(this.getResources().getColor(
                        R.color.contact_editor_field_text_color));
            }
            /**
            * @}
            */
            mFields.addView(fieldView);
        }

        // When hiding fields, place expandable
        setupExpansionView(hidePossible, mHideOptional);
        mExpansionView.setEnabled(!readOnly && isEnabled());
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < mFields.getChildCount(); i++) {
            EditText editText = (EditText) mFields.getChildAt(i);
            if (!TextUtils.isEmpty(editText.getText())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the editor is currently configured to show optional fields.
     */
    public boolean areOptionalFieldsVisible() {
        return !mHideOptional;
    }

    public boolean hasShortAndLongForms() {
        return mHasShortAndLongForms;
    }

    /**
     * Populates the bound rectangle with the bounds of the last editor field inside this view.
     */
    public void acquireEditorBounds(Rect bounds) {
        if (mFieldEditTexts != null) {
            for (int i = mFieldEditTexts.length; --i >= 0;) {
                EditText editText = mFieldEditTexts[i];
                if (editText.getVisibility() == View.VISIBLE) {
                    bounds.set(editText.getLeft(), editText.getTop(), editText.getRight(),
                            editText.getBottom());
                    return;
                }
            }
        }
    }

    /**
     * Saves the visibility of the child EditTexts, and mHideOptional.
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.mHideOptional = mHideOptional;

        final int numChildren = mFieldEditTexts == null ? 0 : mFieldEditTexts.length;
        ss.mVisibilities = new int[numChildren];
        for (int i = 0; i < numChildren; i++) {
            ss.mVisibilities[i] = mFieldEditTexts[i].getVisibility();
        }

        return ss;
    }

    /**
     * Restores the visibility of the child EditTexts, and mHideOptional.
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        mHideOptional = ss.mHideOptional;

        /*
        * SPRD:
        *   when new a sim Contacts,then turn the screen ,the mFieldEditTexts is null and will cause a nullPointerException.
        *
        * @orig
        * int numChildren = Math.min(mFieldEditTexts.length, ss.mVisibilities.length);
        * 
        * @{
        */
        int numChildren = Math.min(mFieldEditTexts == null ? 0 : mFieldEditTexts.length, ss.mVisibilities.length);
        /*
        * @}
        */
        for (int i = 0; i < numChildren; i++) {
            mFieldEditTexts[i].setVisibility(ss.mVisibilities[i]);
        }
    }

    private static class SavedState extends BaseSavedState {
        public boolean mHideOptional;
        public int[] mVisibilities;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mVisibilities = new int[in.readInt()];
            in.readIntArray(mVisibilities);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mVisibilities.length);
            out.writeIntArray(mVisibilities);
        }

        @SuppressWarnings({"unused", "hiding" })
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public void clearAllFields() {
        if (mFieldEditTexts != null) {
            for (EditText fieldEditText : mFieldEditTexts) {
                // Update UI (which will trigger a state change through the {@link TextWatcher})
                fieldEditText.setText("");
            }
        }
    }
    /**
    * SPRD:
    * 
    * @{
    */
    private static ArrayList<String> mNoLeftMarginMimeType;

    static {
        mNoLeftMarginMimeType = new ArrayList<String>();
        mNoLeftMarginMimeType.add(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME);
        mNoLeftMarginMimeType.add(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME);
        mNoLeftMarginMimeType.add(Organization.CONTENT_ITEM_TYPE);
    }
    
    public String deleteEditable(String mimeType, String accountName, String accountType, Editable s, EditText fieldView){
        Account account = (accountType != null && accountName != null)? 
                new Account(accountName, accountType): null;
        AccountTypeManager atManager = AccountTypeManager.getInstance(mContext);
        int maxLength = atManager.getAccountTypeFieldsMaxLength(mContext, account, mimeType);
        if (isChinese(s.toString())) {
            maxLength = maxLength - 2;
        }
        if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)&&
                (SimAccountType.ACCOUNT_TYPE.equals(accountType) || 
                        USimAccountType.ACCOUNT_TYPE.equals(accountType))) {
            if (s!=null && s.length()>0 && s.charAt(0) == '+') {
                maxLength = maxLength + 1;
            }
        }
        int maxLen = atManager.getTextFieldsEditorMaxLength(mContext, account,
                s.toString(), maxLength);
        int editStart = fieldView.getSelectionStart();
        int editEnd = fieldView.getSelectionEnd();
        int len = s.toString().length();
        if (maxLen > 0 && len > maxLen) {
            s.delete(maxLen, len);
        }
        if(fieldView.getText() == null){
            return null;
        }
        return fieldView.getText().toString();
    }

    public static final boolean isAllChinese(String strName) {
        if (strName == null) {
            return false;
        }
        char[] ch = strName.toCharArray();
        for (char c : ch) {
            if (!isChinese(c)) {
                if (c == ' ' || c == '.') {
                    continue;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public static final boolean isChinese(String strName) {
        if (strName == null) {
            return false;
        }
        char[] ch = strName.toCharArray();
        for (char c : ch) {
            if (isChinese(c)) {
                return true;
            }
        }
        return false;
    }

    public static final boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }
    /**
    * @}
    */

    /*
    * SPRD: bug 261263
    * 
    * @{
    */
    public void setActionDone(){
        mFieldEditTexts[mFieldEditTexts.length-1].setImeOptions(EditorInfo.IME_ACTION_DONE);
    }
    /*
    * @}
    */
}
