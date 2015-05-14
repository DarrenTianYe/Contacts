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
package com.sprd.contacts.common.list;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import java.util.Map;
import java.util.HashMap;

/**
 * Action callbacks that can be sent by a phone number picker.
 */
public interface OnPhoneNumberMultiPickerActionListener  {

    /**
     * Returns the selected phone number to the requester.
     */
    void onPickPhoneNumberAction(HashMap<String,String> pairs);
    void onCancel();

}