
package com.sprd.contacts.detail;

import com.android.contacts.detail.ContactLoaderFragment;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.content.ContentValues;
import java.util.ArrayList;

import com.android.contacts.common.model.AccountTypeManager;
import com.sprd.contacts.common.model.account.PhoneAccountType;
import com.sprd.contacts.common.model.account.SimAccountType;
import com.sprd.contacts.common.model.account.USimAccountType;
import com.android.contacts.detail.ContactDetailDisplayUtils;
import android.content.ContentResolver;
import com.android.contacts.common.model.account.AccountWithDataSet;
import android.database.Cursor;

import com.android.contacts.common.model.ContactLoader;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsApplication;
import com.android.contacts.R;
import com.android.contacts.activities.ContactDetailActivity.FragmentKeyListener;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.activities.JoinContactActivity;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.common.list.ShortcutIntentBuilder;
import com.android.contacts.common.list.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;
import com.android.contacts.common.util.Constants;
import com.android.contacts.common.util.ContactLoaderUtils;
import com.android.contacts.util.PhoneCapabilityTester;
import com.sprd.contacts.common.util.UniverseUtils;
import com.android.internal.util.Objects;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;
import android.content.Entity;
import android.os.SystemProperties;

import android.telephony.TelephonyManager;

public class SprdContactLoaderFragment extends ContactLoaderFragment {

    private static boolean VOICE_SUPPORT = true;

    private boolean mOptionsMenuOptions;
    private boolean mOptionsMenuEditable;
    private boolean mOptionsMenuFilterable;
    private boolean mCanCopy;
    private String displayName;
    private ArrayList<String> mPhones;
    private AccountTypeManager mAccountTypeManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhones = new ArrayList<String>();
        VOICE_SUPPORT = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_voice_capable);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mAccountTypeManager = AccountTypeManager.getInstance(activity);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            inflater.inflate(R.menu.view_contact_new_ui, menu);
        } else {
            inflater.inflate(R.menu.view_contact, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean mMenuEnabled = (!ContactsApplication.sApplication.isBatchOperation())
                && (!ContactSaveService.mIsGroupSaving);
        mOptionsMenuOptions = isContactOptionsChangeEnabled();
        mOptionsMenuEditable = isContactEditable();
        mOptionsMenuShareable = isContactShareable();
        mOptionsMenuFilterable = isContactFilterable();
        mOptionsMenuCanCreateShortcut = isContactCanCreateShortcut();

        if (mContactData != null) {
            mSendToVoicemailState = mContactData.isSendToVoicemail();
            mCustomRingtone = mContactData.getCustomRingtone();
        }

        AccountTypeManager am = AccountTypeManager.getInstance(mContext);
        ArrayList<AccountWithDataSet> allAccounts = (ArrayList) am.getAccounts(true);
        // Hide telephony-related settings (ringtone, send to voicemail)
        // if we don't have a telephone
       // final MenuItem optionsSendToVoicemail = menu.findItem(R.id.menu_send_to_voicemail);
        /**
         * remove the menu about optionsSendToVoicemail by xuhong.tian
         */
/*        if (optionsSendToVoicemail != null && mContactData != null
				&& (mContactData.getDetailUserImportType() == RawContacts.CONTACTS_BY_PCTOOLS || mContactData
				.getDetailUserImportType() == RawContacts.CONTACTS_BY_EDITE)) {
            optionsSendToVoicemail.setChecked(false);
            optionsSendToVoicemail.setVisible(false);
            optionsSendToVoicemail.setEnabled(false);
        }else{
            optionsSendToVoicemail.setChecked(mSendToVoicemailState);
            optionsSendToVoicemail.setVisible(mOptionsMenuOptions);
            optionsSendToVoicemail.setEnabled(mMenuEnabled);
        }*/
        final MenuItem optionsRingtone = menu.findItem(R.id.menu_set_ringtone);
/*        if (optionsSendToVoicemail != null) {
            boolean isRingtoneVisible = true;
            if (mContactData != null && mContactData.getAccount() != null) {
                String accountType = mContactData.getAccount().type;
                if (SimAccountType.ACCOUNT_TYPE.equals(accountType)
                        || USimAccountType.ACCOUNT_TYPE.equals(accountType) || PhoneAccountType.PRIVATE_ACCOUNT_TYPE.equals(accountType)) {
                    isRingtoneVisible = false;
                }
            }
            optionsRingtone.setVisible(mOptionsMenuOptions && isRingtoneVisible);
            optionsRingtone.setEnabled(mMenuEnabled);
        }*/

        // filter-able? (has any phone number)
       // final MenuItem filterMenu = menu.findItem(R.id.menu_add_to_black_list);
        final TelephonyManager telephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
/*        if (telephonyManager.isSupportApplication(TelephonyManager.TYPE_CALL_FIRE_WALL)) {
            filterMenu.setVisible(mOptionsMenuFilterable);
            if (isContactBlocked()) {
                filterMenu.setTitle(R.string.menu_deleteFromBlacklist);
            } else {
                filterMenu.setTitle(R.string.menu_addToBlacklist);
            }
            filterMenu.setEnabled(mMenuEnabled);
        } else {
            filterMenu.setVisible(false);
        }*/

        Log.d(TAG, "mOptionsMenuShareable=="+mOptionsMenuShareable+"mMenuEnabled=="+mMenuEnabled);
        // edit-able?
        final MenuItem editMenu = menu.findItem(R.id.menu_edit);
        editMenu.setVisible(true);
        editMenu.setEnabled(true);
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            editMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            editMenu.setIcon(R.drawable.contact_button_editor_normal_sprd);
        }

        // delete-able?
        final MenuItem deleteMenu = menu.findItem(R.id.menu_delete);
        deleteMenu.setVisible(true);
        deleteMenu.setEnabled(true);
        final MenuItem shareMenu = menu.findItem(R.id.menu_share);
        shareMenu.setVisible(mOptionsMenuShareable);
        shareMenu.setEnabled(mMenuEnabled);
/*        final MenuItem createContactShortcutMenu = menu.findItem(R.id.menu_create_contact_shortcut);
        createContactShortcutMenu.setVisible(mOptionsMenuCanCreateShortcut);
        createContactShortcutMenu.setEnabled(mMenuEnabled);*/

        // copy-able?
        final MenuItem copyMenu = menu.findItem(R.id.menu_copy);

        if (mContactData != null && !mContactData.isUserProfile()
                && allAccounts != null && allAccounts.size() > 1) {
            mCanCopy = true;
        }
        if(mContactData != null && mContactData.getDetailUserImportType() == 0){
        	Log.d(TAG, "mContactData.getDetailUserImportType()===="+mContactData.getDetailUserImportType()+mCanCopy);
        } 
        if (mContactData != null && mContactData.getAccount() != null) {
            String accountType = mContactData.getAccount().type;
            Log.d(TAG, ">>>>>>>>>>accountType==="+accountType);
            if (SimAccountType.ACCOUNT_TYPE.equals(accountType)
                    || USimAccountType.ACCOUNT_TYPE.equals(accountType) || PhoneAccountType.PRIVATE_ACCOUNT_TYPE.equals(accountType)) {
            	mCanCopy = false;
            	 optionsRingtone.setVisible(false);
            }
        }
        copyMenu.setTitle(R.string.copy);
        copyMenu.setVisible(mCanCopy);
        copyMenu.setEnabled(mMenuEnabled);
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            final MenuItem starredMenuItem = menu.findItem(R.id.menu_star);
            AccountWithDataSet accountType = null;
            if (mContactData != null) {
                accountType = mContactData.getAccount();
            }
            starredMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    // Toggle "starred" state
                    // Make sure there is a contact
                    if (mLookupUri != null) {
                        final boolean isStarred = starredMenuItem.isChecked();
                      if (mContactData != null) {
                                // To improve responsiveness, swap out the
                                // picture (and tag) in the UI already
                                ContactDetailDisplayUtils.configureStarredMenuItem(starredMenuItem,
                                        mContactData.isDirectoryEntry(),
                                        mContactData.isUserProfile(),
                                        !isStarred);
                            }
                            // Now perform the real save
                            Intent intent = ContactSaveService.createSetStarredIntent(
                                    mContext, mLookupUri, !isStarred);
                            mContext.startService(intent);
                        }
                    return true;
                }
            });
            // If there is contact data, update the starred state
            if (mContactData != null) {
                ContactDetailDisplayUtils.configureStarredMenuItem(starredMenuItem,
                        mContactData.isDirectoryEntry(), mContactData.isUserProfile(),
                        mContactData.getStarred());
            }
            if (accountType != null
                    && (SimAccountType.ACCOUNT_TYPE.equals(accountType.type)
                    || USimAccountType.ACCOUNT_TYPE.equals(accountType.type))) {
                starredMenuItem.setVisible(false);
                optionsRingtone.setVisible(false);
            }
            starredMenuItem.setEnabled(mMenuEnabled);
        }
    }

    public boolean isContactOptionsChangeEnabled() {
        if (VOICE_SUPPORT)
            return mContactData != null && !mContactData.isDirectoryEntry()
                    && !mContactData.isUserProfile()
                    && PhoneCapabilityTester.isPhone(mContext);
        else
            return false;
    }

	public boolean isContactEditable() {
		if (mContactData == null || mContactData.isDirectoryEntry()) {
			return false;
		}
		AccountWithDataSet account = mContactData.getAccount();
		/**
		 * add by xuhong.tian
		 */
		// if (account != null && !mAccountTypeManager.contains(account, true))
		// {
		// return false;
		// }
		if (account != null) {
			return false;
		}
		return true;
	}

    public boolean isContactFilterable() {
        mPhones.clear();
        if (mContactData == null) {
            return false;
        }
        ArrayList<ContentValues> cvs = mContactData.getAllContentValues();
        for (ContentValues cv : cvs) {
            String mimeType = cv.getAsString(Data.MIMETYPE);
            if (mimeType != null && mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                String phone = cv.getAsString(Phone.NUMBER);
                mPhones.add(phone);
            }
            if (mimeType != null && mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                displayName = cv.getAsString(StructuredName.DISPLAY_NAME);
            }
        }
        return mPhones.size() >= 1 ? true : false;
    }

    public boolean isContactBlocked() {
        if (mContactData == null) {
            return false;
        }
        ArrayList<ContentValues> cvs = mContactData.getAllContentValues();
        for (ContentValues cv : cvs) {
            String mimeType = cv.getAsString(Data.MIMETYPE);
            if (mimeType != null && mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                String phone = cv.getAsString(Phone.NUMBER);
                if (!ContactLoaderUtils.CheckIsBlackNumber(mContext, phone)) {
                    return false;
                }
            }

        }
        return true;
    }

    public boolean isContactCanCreateShortcut() {
        boolean isSimAccountContact = false;
        if (mContactData != null && mContactData.getAccount() != null) {
            if (SimAccountType.ACCOUNT_TYPE.equals(mContactData.getAccount().type)
                    || USimAccountType.ACCOUNT_TYPE.equals(mContactData.getAccount().type)) {
                isSimAccountContact = true;
            }
        }
        return mContactData != null && !mContactData.isUserProfile()
                && !mContactData.isDirectoryEntry()
                && !isSimAccountContact;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_copy: {
                if (mContactData == null)
                    return false;
                if (mListener != null && mContactData != null)
                    mListener.onCopyRequested(mContactData.getLookupKey());
                break;
            }
            case R.id.menu_add_to_black_list: {
                // if (mListener != null) mListener.onFilterRequested(mPhones);
                if (isContactBlocked()) {
                    if (mListener != null)
                        mListener.onNotFilterRequested(mPhones, displayName);
                } else {
                    if (mListener != null)
                        mListener.onFilterRequested(mPhones, displayName);

                }
                return true;
            }
            default:
                super.onOptionsItemSelected(item);
                break;
        }
        return false;
    }

}
