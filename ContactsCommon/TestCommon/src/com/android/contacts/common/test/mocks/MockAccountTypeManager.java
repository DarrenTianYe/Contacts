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
package com.android.contacts.common.test.mocks;

import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountTypeWithDataSet;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import android.graphics.drawable.Drawable;
/**
 * A mock {@link AccountTypeManager} class.
 */
public class MockAccountTypeManager extends AccountTypeManager {

    public AccountType[] mTypes;
    public AccountWithDataSet[] mAccounts;

    public MockAccountTypeManager(AccountType[] types, AccountWithDataSet[] accounts) {
        this.mTypes = types;
        this.mAccounts = accounts;
    }

    @Override
    public AccountType getAccountType(AccountTypeWithDataSet accountTypeWithDataSet) {
        for (AccountType type : mTypes) {
            if (Objects.equal(accountTypeWithDataSet.accountType, type.accountType)
                    && Objects.equal(accountTypeWithDataSet.dataSet, type.dataSet)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public List<AccountWithDataSet> getAccounts(boolean writableOnly) {
        return Arrays.asList(mAccounts);
    }

    @Override
    public List<AccountWithDataSet> getGroupWritableAccounts() {
        return Arrays.asList(mAccounts);
    }

    @Override
    public Map<AccountTypeWithDataSet, AccountType> getUsableInvitableAccountTypes() {
        return Maps.newHashMap(); // Always returns empty
    }

    @Override
    public List<AccountType> getAccountTypes(boolean writableOnly) {
        final List<AccountType> ret = Lists.newArrayList();
        synchronized (this) {
            for (AccountType type : mTypes) {
                if (!writableOnly || type.areContactsWritable()) {
                    ret.add(type);
                }
            }
        }
        return ret;
    }
    // SPRD:add for Universe UI
    public String getSimSlotName(int phoneId) {
        return null;
    }
    public List<AccountWithDataSet> getSimAccounts(){
        return null;
    }
    public AccountWithDataSet getPhoneAccount(){
        return null;
    }
    public boolean isPhoneAccount(AccountWithDataSet account){
        return false;
    }
    public boolean isSimAccount(AccountWithDataSet account){
        return false;
    }

    public Drawable getAccountIcon(AccountWithDataSet account){
        return null;
    }
}
