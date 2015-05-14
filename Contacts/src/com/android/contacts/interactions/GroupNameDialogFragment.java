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
package com.android.contacts.interactions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.android.contacts.R;

/**
 * A common superclass for creating and renaming groups.
 */
public abstract class GroupNameDialogFragment extends DialogFragment {
    protected abstract int getTitleResourceId();
    protected abstract void initializeGroupLabelEditText(EditText editText);
    protected abstract void onCompleted(String groupLabel);
    /**
    * SPRD:
    *     The following code is added by sprd.
    * @{
    */
    protected abstract int getMaxTextLength(String mString);
    /**
    * @}
    */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater layoutInflater = LayoutInflater.from(builder.getContext());
        final View view = layoutInflater.inflate(R.layout.group_name_dialog, null);
        final EditText editText = (EditText) view.findViewById(R.id.group_label);
        initializeGroupLabelEditText(editText);

        builder.setTitle(getTitleResourceId());
        builder.setView(view);
        editText.requestFocus();
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int whichButton) {
                        onCompleted(editText.getText().toString().trim());
                    }
                }
            );

        /**
        * SPRD:
        *   The following code is added by sprd.
        *
        * Original Android code:
        *   builder.setNegativeButton(android.R.string.cancel, null);
        * 
        * @{
        */
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InputMethodManager im = (InputMethodManager) getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                im.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
        /**
        * @}
        */
        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                updateOkButtonState(dialog, editText);
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateOkButtonState(dialog, editText);
                /**
                * SPRD:
                *       The following code is added by sprd.
                * @{
                */
                int maxLen = getMaxTextLength(s.toString());
                if (editText == null || s.toString() == null || maxLen <= 0) {
                    return;
                }
                int len = s.toString().length();
                if (maxLen > 0 && len > maxLen) {
                    s.delete(maxLen, len);
                }
                /**
                * @}
                */
            }
        });
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return dialog;
    }

    /* package */ void updateOkButtonState(AlertDialog dialog, EditText editText) {
        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        okButton.setEnabled(!TextUtils.isEmpty(editText.getText().toString().trim()));
    }
}
