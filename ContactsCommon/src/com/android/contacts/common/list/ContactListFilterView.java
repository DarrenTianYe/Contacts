/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.contacts.common.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.sprd.contacts.common.model.account.PhoneAccountType;
import com.sprd.contacts.common.util.UniverseUtils;

/**
 * Contact list filter parameters.
 */
public class ContactListFilterView extends LinearLayout {

    private static final String TAG = ContactListFilterView.class.getSimpleName();

    private ImageView mIcon;
    private TextView mAccountType;
    private TextView mAccountUserName;
    private RadioButton mRadioButton;
    private ContactListFilter mFilter;
    private boolean mSingleAccount;

    public ContactListFilterView(Context context) {
        super(context);
    }

    public ContactListFilterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setContactListFilter(ContactListFilter filter) {
        mFilter = filter;
    }

    public ContactListFilter getContactListFilter() {
        return mFilter;
    }

    public void setSingleAccount(boolean flag) {
        this.mSingleAccount = flag;
    }

    @Override
    public void setActivated(boolean activated) {
        super.setActivated(activated);
        if (mRadioButton != null) {
            mRadioButton.setChecked(activated);
        } else {
            // We're guarding against null-pointer exceptions,
            // but otherwise this code is not expected to work
            // properly if the button hasn't been initialized.
            Log.wtf(TAG, "radio-button cannot be activated because it is null");
        }
    }

    public void bindView(AccountTypeManager accountTypes, int DefaultfilterType) {
    	
    	Log.d(TAG, "mFilter.filterType=="+mFilter.filterType+"mAccountType=="+mAccountType+"mFilter=="+mFilter+"filterType=="+DefaultfilterType);
        if (mAccountType == null) {
            mIcon = (ImageView) findViewById(R.id.icon);
            mAccountType = (TextView) findViewById(R.id.accountType);
            mAccountUserName = (TextView) findViewById(R.id.accountUserName);
            mRadioButton = (RadioButton) findViewById(R.id.radioButton);
            
/*            if(DefaultfilterType ==mFilter.filterType)
                mRadioButton.setChecked(true);*/
            /**
             * add by xuhong.tian on 11.26 that set default filter .
             */
        }

        if (mFilter == null) {
            mAccountType.setText(R.string.contactsList);
            return;
        }

        Log.d(TAG, "mFilter.filterType=="+mFilter.filterType);
        mAccountUserName.setVisibility(View.GONE);
        switch (mFilter.filterType) {
            case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS: {
                bindView(R.drawable.ic_contact_picture_180_holo_light, R.string.list_filter_all_accounts);
                break;
            }
            case ContactListFilter.FILTER_TYPE_STARRED: {
                bindView(R.drawable.ic_menu_star_holo_light, R.string.list_filter_all_starred);
                break;
            }
            case ContactListFilter.FILTER_TYPE_CUSTOM: {
                bindView(R.drawable.ic_menu_settings_holo_light, R.string.list_filter_customize);
                break;
            }
            case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY: {
                bindView(0, R.string.list_filter_phones);
                break;
            }
            case ContactListFilter.FILTER_TYPE_SINGLE_CONTACT: {
                bindView(0, R.string.list_filter_single);
                break;
            }
            case ContactListFilter.FILTER_TYPE_ACCOUNT: {
                mAccountUserName.setVisibility(View.VISIBLE);
                mIcon.setVisibility(View.VISIBLE);
                if (mFilter.icon != null) {
                    mIcon.setImageDrawable(mFilter.icon);
                } else {
                    mIcon.setImageResource(R.drawable.unknown_source);
                }
                final AccountType accountType =
                        accountTypes.getAccountType(mFilter.accountType, mFilter.dataSet);
                /**
                * SPRD:
                *
                *
                * Original Android code:
                * mAccountUserName.setText(mFilter.accountName);
                *
                * @{
                */
                if (mFilter.accountType.equals(PhoneAccountType.ACCOUNT_TYPE)) {
                   // mAccountUserName.setText(R.string.show_publiccontacts);
                    mAccountType.setText(R.string.show_publiccontacts);
                } else {
                   // mAccountUserName.setText(mFilter.accountName);
                    mAccountType.setText(accountType.getDisplayLabel(getContext()));
                }
                /**
                * @}
                */
               
                break;
            }
            case ContactListFilter.FILTER_TYPE_PRIVATECONTACTS: {
            	/**
            	 * add by xuhong.tian
            	 */
                bindView(R.drawable.private_contacts_icon, R.string.show_privatecontacts);
                break;
            }
        }
    }

    private void bindView(int iconResource, int textResource) {
        if (iconResource != 0) {
            mIcon.setVisibility(View.VISIBLE);
            mIcon.setImageResource(iconResource);
        } else {
            mIcon.setVisibility(View.GONE);
        }

        mAccountType.setText(textResource);
    }
}
