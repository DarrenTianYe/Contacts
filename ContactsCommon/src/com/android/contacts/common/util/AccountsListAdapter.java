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

package com.android.contacts.common.util;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;

import java.util.ArrayList;
import java.util.List;
/**
* SPRD:
* 
* @{
*/
import android.sim.Sim;
import android.sim.SimManager;
import com.sprd.contacts.common.model.account.PhoneAccountType;
import com.sprd.contacts.common.model.account.SimAccountType;
import com.sprd.contacts.common.model.account.USimAccountType;
import com.sprd.contacts.common.util.UniverseUtils;
/**
* @}
*/

import neolink.telephony.PrivateManager;
import neolink.telephony.PrivateMode;

/**
 * List-Adapter for Account selection
 */
public final class AccountsListAdapter extends BaseAdapter {
    private static final String TAG = "AccountsListAdapter";
	private final LayoutInflater mInflater;
    private final List<AccountWithDataSet> mAccounts;
    private final AccountTypeManager mAccountTypes;
    private final Context mContext;
    
	/**
	 * add by xuhong.tian
	 */
	public int mMode=PrivateMode.MODE_UNKNOWN;
	private  PrivateManager mManager;

    /**
     * Filters that affect the list of accounts that is displayed by this adapter.
     */
    public enum AccountListFilter {
        ALL_ACCOUNTS,                   // All read-only and writable accounts
        ACCOUNTS_CONTACT_WRITABLE,      // Only where the account type is contact writable
        ACCOUNTS_GROUP_WRITABLE         // Only accounts where the account type is group writable
    }

    public AccountsListAdapter(Context context, AccountListFilter accountListFilter) {
        this(context, accountListFilter, null);
    }
    
    public AccountsListAdapter(Context context, AccountListFilter accountListFilter, int mode) {
        this(context, accountListFilter, null, mode);
    }

    /**
     * @param currentAccount the Account currently selected by the user, which should come
     * first in the list. Can be null.
     */
	public AccountsListAdapter(Context context,
			AccountListFilter accountListFilter,
			AccountWithDataSet currentAccount) {
		mContext = context;
		mAccountTypes = AccountTypeManager.getInstance(context);
		mAccounts = getAccounts(accountListFilter);
		mManager = new PrivateManager(context);
		mMode = mManager.getMode();

		/**
		 * add by xuhong.tian add the account about private contacts.
		 */
		if (mMode == PrivateMode.MODE_PDT_DIGITAL_TRUNKING
				|| mMode == PrivateMode.MODE_MPT1327_ANALOG_TRUNKING) {

			AccountWithDataSet accountWithDataSet = new AccountWithDataSet(
					mContext.getString(R.string.show_privatecontacts),
					"neolink.com.android.account.phone", null);
			mAccounts.add(accountWithDataSet);
		}
		if (currentAccount != null && !mAccounts.isEmpty()
				&& !mAccounts.get(0).equals(currentAccount)
				&& mAccounts.remove(currentAccount)) {
			mAccounts.add(0, currentAccount);
		}

		/**
		 * SPRD:
		 * 
		 * @{
		 */
		if (currentAccount != null) {
			mAccounts.remove(currentAccount);
		}

		if (UniverseUtils.UNIVERSEUI_SUPPORT) {
			initSim(context);
		}
		/**
		 * @}
		 */

		mInflater = LayoutInflater.from(context);
	}

    /**
     * @param currentAccount the Account currently selected by the user, which should come
     * first in the list. Can be null. add by xuhong.tian called by not show private account 
     */
    public AccountsListAdapter(Context context, AccountListFilter accountListFilter,
            AccountWithDataSet currentAccount, int mode) {
        mContext = context;
        mAccountTypes = AccountTypeManager.getInstance(context);
        mAccounts = getAccounts(accountListFilter);
        
        Log.d(TAG, "ZZZZZZZZ");
		/**
		 * add by xuhong.tian add the account about private contacts.
		 */
//		if (mMode == PrivateMode.MODE_PDT_DIGITAL_TRUNKING
//				|| mMode == PrivateMode.MODE_MPT1327_ANALOG_TRUNKING) {
//
//			AccountWithDataSet accountWithDataSet = new AccountWithDataSet(
//					mContext.getString(R.string.show_privatecontacts),
//					"neolink.com.android.account.phone", null);
//			mAccounts.add(accountWithDataSet);
//		}
		if (currentAccount != null && !mAccounts.isEmpty()
				&& !mAccounts.get(0).equals(currentAccount)
				&& mAccounts.remove(currentAccount)) {
			mAccounts.add(0, currentAccount);
		}

        /**
        * SPRD:
        * 
        * @{
        */
        if (currentAccount != null) {
            mAccounts.remove(currentAccount);
        }

        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            initSim(context);
        }
        /**
        * @}
        */
        
        mInflater = LayoutInflater.from(context);
    }
    
    private List<AccountWithDataSet> getAccounts(AccountListFilter accountListFilter) {
        if (accountListFilter == AccountListFilter.ACCOUNTS_GROUP_WRITABLE) {
            return new ArrayList<AccountWithDataSet>(mAccountTypes.getGroupWritableAccounts());
        }
        return new ArrayList<AccountWithDataSet>(mAccountTypes.getAccounts(
                accountListFilter == AccountListFilter.ACCOUNTS_CONTACT_WRITABLE));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        final View resultView = convertView != null ? convertView
//                : mInflater.inflate(R.layout.account_selector_list_item, parent, false);
    	
    	Log.d("tian", "getView");
    	
    	final View resultView = mInflater.inflate(R.layout.account_selector_list_item, parent, false);

        final TextView text1 = (TextView) resultView.findViewById(android.R.id.text1);
        final TextView text2 = (TextView) resultView.findViewById(android.R.id.text2);
        final ImageView icon = (ImageView) resultView.findViewById(android.R.id.icon);

        final AccountWithDataSet account = mAccounts.get(position);
        final AccountType accountType = mAccountTypes.getAccountType(account.type, account.dataSet);

        /**
         * remove the original resource. by xuhong.tian
         */
        //text1.setText(accountType.getDisplayLabel(mContext));
        if (PhoneAccountType.ACCOUNT_TYPE.equals(account.type)) {
        	//text1.setText(mContext.getString(R.string.label_phone));
        	text1.setText(mContext.getString(R.string.show_publiccontacts));
        } else if(PhoneAccountType.PRIVATE_ACCOUNT_TYPE.equals(account.type)){
        	//text1.setText(account.name);
			text1.setText(mContext.getString(R.string.show_privatecontacts)); // show_publiccontacts
		
        }else{
        	text1.setText(accountType.getDisplayLabel(mContext));
        }
        

        // For email addresses, we don't want to truncate at end, which might cut off the domain
        // name.
        
        /**
        * SPRD:
        * Modify these sentences for UniverseUI.
        * Original code:
        * The code be remarked.
        * @{
        */
        Log.d(TAG, "UniverseUtils.UNIVERSEUI_SUPPORT="+UniverseUtils.UNIVERSEUI_SUPPORT+"accountType.getDisplayLabel(mContext)=="+accountType.getDisplayLabel(mContext)+"account.name="+account.name+"account.type="+account.type);
        
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {  // generally it is false
            if (SimAccountType.ACCOUNT_TYPE.equals(account.type)
                    || USimAccountType.ACCOUNT_TYPE.equals(account.type)) {
                String simSlotName;
                if (mSims == null) {
                    text2.setText(account.name);
                    icon.setImageDrawable(accountType.getDisplayIcon(mContext));
                } else {
                    for (Sim sim : mSims) {
                        simSlotName = mAccountTypes.getSimSlotName(sim.getPhoneId());
                        if (simSlotName == null) {
                            break;
                        } else {
                            if (mAccountTypes.isExistInSimSlotName(simSlotName)) {
                                if (simSlotName.equals(account.name)) {
                                    text2.setText(sim.getName());
                                    break;
                                }
                            } else {
                                text2.setText(account.name);
                            }
                        }
                    }
                    icon.setImageDrawable(mAccountTypes.getAccountIcon(account));
                }
            } else if (PhoneAccountType.ACCOUNT_TYPE.equals(account.type)) {
                text2.setText(mContext.getString(R.string.label_phone));
                icon.setImageDrawable(accountType.getDisplayIcon(mContext));
            } else {
                text2.setText(account.name);
                icon.setImageDrawable(accountType.getDisplayIcon(mContext));
            }
        } else {
            if (PhoneAccountType.ACCOUNT_TYPE.equals(account.type)) {
               // text2.setText(mContext.getString(R.string.label_phone));
                text2.setText(mContext.getString(R.string.show_publiccontacts));
            } else {
                text2.setText(account.name);
            }
            icon.setImageDrawable(accountType.getDisplayIcon(mContext));
        }
        text2.setEllipsize(TruncateAt.MIDDLE);
        
//        text2.setText(account.name);
//        text2.setEllipsize(TruncateAt.MIDDLE);
//
//        icon.setImageDrawable(accountType.getDisplayIcon(mContext));
        /**
        * @}
        */

        return resultView;
    }

    @Override
    public int getCount() {
        return mAccounts.size();
    }

    @Override
    public AccountWithDataSet getItem(int position) {
        return mAccounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    
    /**
    * SPRD:
    * 
    * @{
    */
    private Sim mSims[] = null;
    private SimManager mSimManager;
    
    private void initSim(Context context) {
        mSimManager = SimManager.get(context);
        if (mSimManager == null) {
            return;
        }
        mSims = mSimManager.getSims();
    }
    
    public AccountsListAdapter(Context context, List<AccountWithDataSet> accounts) {
        mContext = context;
        mAccountTypes = AccountTypeManager.getInstance(context);
        mAccounts = accounts;

        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            initSim(context);
        }
        /**
         * add by xuhong.tian add the account about private contacts.
         */
		//if(mMode ==PrivateMode.MODE_PDT_DIGITAL_TRUNKING || mMode== PrivateMode.MODE_MPT1327_ANALOG_TRUNKING){ 
        
        Log.d("tian","AccountsListAdapter");
//        AccountWithDataSet accountWithDataSet = new AccountWithDataSet(
//        		"专网联系人", "neolink.com.android.account.phone", null);
//        mAccounts.add(accountWithDataSet);  
//	}
        mInflater = LayoutInflater.from(context);
    }
    /**
     * @}
     */

}

