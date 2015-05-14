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

package com.android.contacts.common.util;

/**
* SPRD:
* 
* @{
*/
import android.os.Environment;
import android.os.Debug;
import android.os.SystemProperties;
/**
* @}
*/
public class Constants {

    /**
     * Log tag for performance measurement.
     * To enable: adb shell setprop log.tag.ContactsPerf VERBOSE
     */
    public static final String PERFORMANCE_TAG = "ContactsPerf";

    // Used for lookup URI that contains an encoded JSON string.
    public static final String LOOKUP_URI_ENCODED = "encoded";
     /**
    * SPRD:
    * 
    * @{
    */
    public static final String SCHEME_TEL = "tel";
    public static final String SCHEME_VTEL = "vtel";
    public static final String SCHEME_SMSTO = "smsto";
    public static final String SCHEME_MAILTO = "mailto";
    public static final String SCHEME_IMTO = "imto";
    public static final String SCHEME_SIP = "sip";
    public static final String ACTION_ADD_BLACKLIST = "com.sprd.firewall.ui.BlackCallsListAddActivity.action";

    public static final int STORAGETYPE = Environment.getStorageType();
    public static final boolean DEBUG = Debug.isDebug();

    public static boolean IsEa() {
        return !"1".equals(SystemProperties.get("ro.device.support.nand"));
    }

    public static final String INTENT_KEY_ACCOUNTS = "accounts";
    public static final String INTENT_EXTRA_NEW_LOCAL_PROFILE = "newLocalProfile";
    public static final boolean IS_DEBUG = SystemProperties.getBoolean("persist.sys.output.log",
            false);
    /**
    * @}
    */
}
