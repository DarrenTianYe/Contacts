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
 * limitations under the License.
 */

package com.android.contacts.common.util;

import android.net.Uri;

/**
* SPRD:
* 
* @{
*/
import java.io.File;
import android.text.TextUtils;
/**
* @}
*/


/**
 * Utility methods for dealing with URIs.
 */
public class UriUtils {
    /** Static helper, not instantiable. */
    private UriUtils() {}

    /** Checks whether two URI are equal, taking care of the case where either is null. */
    public static boolean areEqual(Uri uri1, Uri uri2) {
        if (uri1 == null && uri2 == null) {
            return true;
        }
        if (uri1 == null || uri2 == null) {
            return false;
        }
        return uri1.equals(uri2);
    }

    /** Parses a string into a URI and returns null if the given string is null. */
    public static Uri parseUriOrNull(String uriString) {
        if (uriString == null) {
            return null;
        }
        return Uri.parse(uriString);
    }

    /** Converts a URI into a string, returns null if the given URI is null. */
    public static String uriToString(Uri uri) {
        return uri == null ? null : uri.toString();
    }

    public static boolean isEncodedContactUri(Uri uri) {
        return uri != null && uri.getLastPathSegment().equals(Constants.LOOKUP_URI_ENCODED);
    }
  
    /**
    * SPRD:
    * 
    * @{
    */
    public static String encodeFilePath(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        String[] fragments = path.split(File.separator);
        StringBuilder sb = new StringBuilder();
        int N = fragments.length;
        for (int i = 0; i < N; ++i) {
            if (i != 0) {
                sb.append(File.separator);
            }
            sb.append(Uri.encode(fragments[i]));
        }
        return sb.toString();
    }
    /**
    * @}
    */
}
