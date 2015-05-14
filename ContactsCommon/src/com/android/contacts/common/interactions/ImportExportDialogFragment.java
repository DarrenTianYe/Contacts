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

package com.android.contacts.common.interactions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.common.R;
import com.android.contacts.common.editor.SelectAccountDialogFragment;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.AccountSelectionUtil;
import com.android.contacts.common.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.common.vcard.ExportVCardActivity;
import com.android.contacts.common.vcard.VCardCommonArguments;

import java.util.List;

import com.sprd.contacts.common.model.account.PhoneAccountType;
import com.android.contacts.common.model.account.ExchangeAccountType;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.*;
import android.provider.ContactsContract;
import android.provider.ContactsContract.*;
import android.app.Activity;
import android.provider.ContactsContract.Intents.UI;
import com.android.contacts.common.util.Constants;

import android.os.Environment;
import java.util.ArrayList;
import java.io.File;
/**
 * An dialog invoked to import/export contacts.
 */

/**
 * SPRD:
 * Modify this class when porting from 4.1 to 4.3
 * @{
 */
public class ImportExportDialogFragment extends DialogFragment{
    public static final String TAG = "ImportExportDialogFragment";
    private String mPhoneAccountName = "";

    private static final String ARG_CONTACTS_ARE_AVAILABLE = "CONTACTS_ARE_AVAILABLE";

    private final String[] LOOKUP_PROJECTION = new String[] {
            Contacts.LOOKUP_KEY
    };

    private static final int MODE_IMPORT_FROM_SIM = 0;
    private static final int MODE_EXPORT_TO_SIM = 1;

    public interface Listener {
        void doCopy();

        void doImport(AccountWithDataSet dstAccount);

        void doPreImport(int resId);

        void doExport(int deviceType);

        void doShareVisible();

        void showNotFoundSdDialog();
    }

    public ImportExportDialogFragment() {
    }

    /** Preferred way to show this dialog */
    public static void show(FragmentManager fragmentManager, boolean contactsAreAvailable,
            Class callingActivity) {
        final ImportExportDialogFragment fragment = new ImportExportDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_CONTACTS_ARE_AVAILABLE, contactsAreAvailable);
        args.putString(VCardCommonArguments.ARG_CALLING_ACTIVITY, callingActivity.getName());
        fragment.setArguments(args);
        fragment.show(fragmentManager, ImportExportDialogFragment.TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Wrap our context to inflate list items using the correct theme
        final Resources res = getActivity().getResources();
        final LayoutInflater dialogInflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // final boolean contactsAreAvailable =
        // getArguments().getBoolean(ARG_CONTACTS_ARE_AVAILABLE);

        // Adapter that shows a list of string resources
        final ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(getActivity(),
                R.layout.select_dialog_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final TextView result = (TextView) (convertView != null ? convertView :
                        dialogInflater.inflate(R.layout.select_dialog_item, parent, false));

                final int resId = getItem(position);
                result.setText(resId);
                return result;
            }
        };

        if (AccountTypeManager.getInstance(getActivity()).getAccounts(true).size() > 1) {
            adapter.add(R.string.copy_to);
        }

        if (res.getBoolean(R.bool.config_allow_import_from_sdcard)) {
            if (Constants.STORAGETYPE == 1 || Constants.STORAGETYPE == 2) {
                adapter.add(R.string.import_from_phone);
            }
            adapter.add(R.string.import_from_sdcard);
        }
        if (res.getBoolean(R.bool.config_allow_export_to_sdcard)) {
            if (Constants.STORAGETYPE == 1
                    || Constants.STORAGETYPE == 2) {
                adapter.add(R.string.export_to_phone);
            }
            adapter.add(R.string.export_to_sdcard);
        }
        if (res.getBoolean(R.bool.config_allow_share_visible_contacts)) {
            adapter.add(R.string.share_visible_contacts);
            // if (contactsAreAvailable) {
            // adapter.add(R.string.share_visible_contacts);
            // }
        }

        final DialogInterface.OnClickListener clickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean dismissDialog = false;
                        final int resId = adapter.getItem(which);
                        switch (resId) {
                            case R.string.copy_to:
                                dismissDialog = handleCopyRequest();
                                break;

                            case R.string.import_from_sdcard: {
                                boolean sdReady = false;
                                sdReady = Environment.getExternalStoragePathState().equals(
                                        Environment.MEDIA_MOUNTED);
                                if (!sdReady) {
                                    dismissDialog = handleNotFoundSd();
                                } else {
                                    dismissDialog = handleImportRequest(R.string.import_from_sdcard);
                                }
                                break;
                            }
                            case R.string.export_to_sdcard: {
                                boolean sdReady = false;
                                sdReady = Environment.getExternalStoragePathState().equals(
                                        Environment.MEDIA_MOUNTED);
                                if (!sdReady) {
                                    dismissDialog = handleNotFoundSd();
                                } else {
                                    dismissDialog = handleExportToSd(R.string.export_to_sdcard);
                                }
                                break;
                            }
                            case R.string.import_from_phone: {
                                File targetDirectory;
                                targetDirectory = Environment.getInternalStoragePath();
                                if (!(targetDirectory.exists() &&
                                        targetDirectory.isDirectory() &&
                                        targetDirectory.canRead()) &&
                                        !targetDirectory.mkdirs()) {
                                    dismissDialog = handleNotFoundSd();
                                } else {
                                    dismissDialog = handleImportRequest(R.string.import_from_phone);
                                }
                                break;
                            }
                            case R.string.export_to_phone: {
                                File targetDirectory;
                                targetDirectory = Environment.getInternalStoragePath();
                                if (!(targetDirectory.exists() &&
                                        targetDirectory.isDirectory() &&
                                        targetDirectory.canRead()) &&
                                        !targetDirectory.mkdirs()) {
                                    dismissDialog = handleNotFoundSd();
                                } else {
                                    dismissDialog = handleExportToSd(R.string.export_to_phone);
                                }
                                break;
                            }
                            case R.string.share_visible_contacts: {
                                dismissDialog = handleShareVisible();
                                break;
                            }
                            default: {
                                dismissDialog = true;
                                Log.e(TAG, "Unexpected resource: "
                                        + getActivity().getResources().getResourceEntryName(resId));
                            }
                        }
                        if (dismissDialog) {
                            dialog.dismiss();
                        }
                    }
                };
        return new AlertDialog.Builder(getActivity())
                // .setTitle(contactsAreAvailable
                // ? R.string.dialog_import_export
                // : R.string.dialog_import)

                .setTitle(R.string.dialog_import_export)
                .setSingleChoiceItems(adapter, -1, clickListener)
                .create();
    }

    private boolean handleImportRequest(int resId) {
        if (getActivity() != null) {
            ((Listener) getActivity()).doPreImport(resId);
            return true;
        } else {
            return false;
        }
    }

    private boolean handleCopyRequest() {
        if (getActivity() != null) {
            ((Listener) getActivity()).doCopy();
            return true;
        } else {
            return false;
        }
    }

    private boolean handleExportToSd(int deviceType) {
        if (getActivity() != null) {
            ((Listener) getActivity()).doExport(deviceType);
            return true;
        } else {
            return false;
        }
    }

    private boolean handleShareVisible() {
        if (getActivity() != null) {
            ((Listener) getActivity()).doShareVisible();
            return true;
        } else {
            return false;
        }
    }

    private boolean handleNotFoundSd() {
        if (getActivity() != null) {
            ((Listener) getActivity()).showNotFoundSdDialog();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        AccountTypeManager atm = AccountTypeManager.getInstance(activity);
        AccountWithDataSet account = atm.getPhoneAccount();
        // mPhoneAccountName = account.name;
    }

    private void notFoundSdcard() {
        getDialog().dismiss();
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.no_sdcard_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.no_sdcard_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }
}

/**
 * @}
 */
