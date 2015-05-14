package com.sprd.contacts.util;

import com.android.contacts.R;

public class SourceUtils {

    public static int getCallsDeleteSourceId(int phoneId) {
        if (phoneId == 1) {
            return R.drawable.ic_list_sim2;
        } else {
            return R.drawable.ic_list_sim1;
        }
    }
}
