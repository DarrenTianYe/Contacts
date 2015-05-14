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
package com.android.contacts.group;

import android.content.Intent;

/**
 * Meta-data for a contact group.  We load all groups associated with the contact's
 * constituent accounts.
 */
public final class GroupListItem {
    private final String mAccountName;
    private final String mAccountType;
    private final String mDataSet;
    private final long mGroupId;
    private final String mTitle;
    private final boolean mIsFirstGroupInAccount;
    private final int mMemberCount;
    
    private final int mGroupNumber;
    final Intent mCallIntent;
    private final int mGroupImport;
    private final int mGrouptype;

    public GroupListItem(String accountName, String accountType, String dataSet, long groupId,
            String title, boolean isFirstGroupInAccount, int memberCount,int GroupNumber,Intent CallIntent,int groupImport,int grouptype) {
        mAccountName = accountName;
        mAccountType = accountType;
        mDataSet = dataSet;
        mGroupId = groupId;
        mTitle = title;
        mIsFirstGroupInAccount = isFirstGroupInAccount;
        mMemberCount = memberCount;
        mGroupNumber = GroupNumber;
        mCallIntent = CallIntent;
        mGroupImport =groupImport;
        mGrouptype = grouptype;
    }

    public String getAccountName() {
        return mAccountName;
    }

    public String getAccountType() {
        return mAccountType;
    }

    public String getDataSet() {
        return mDataSet;
    }

    public long getGroupId() {
        return mGroupId;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getMemberCount() {
        return mMemberCount;
    }

    public boolean hasMemberCount() {
        return mMemberCount != -1;
    }

    public boolean isFirstGroupInAccount() {
        return mIsFirstGroupInAccount;
    }
    
    public int getGroupNumber() {
        return mGroupNumber;
    }
    
    public Intent getGroupCallIntent() {
        return mCallIntent;
    }
    
	public int getImportType() {

		return mGroupImport;
	}
	
	public int getGroupType() {

		return mGrouptype;
	}
}