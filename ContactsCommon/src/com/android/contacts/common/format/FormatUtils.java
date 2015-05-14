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
package com.android.contacts.common.format;

import android.database.CharArrayBuffer;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import com.google.common.annotations.VisibleForTesting;

import java.util.Arrays;

/**
 * Assorted utility methods related to text formatting in Contacts.
 */
public class FormatUtils {

    /**
     * Finds the earliest point in buffer1 at which the first part of buffer2 matches.  For example,
     * overlapPoint("abcd", "cdef") == 2.
     */
    public static int overlapPoint(CharArrayBuffer buffer1, CharArrayBuffer buffer2) {
        if (buffer1 == null || buffer2 == null) {
            return -1;
        }
        return overlapPoint(Arrays.copyOfRange(buffer1.data, 0, buffer1.sizeCopied),
                Arrays.copyOfRange(buffer2.data, 0, buffer2.sizeCopied));
    }

    /**
     * Finds the earliest point in string1 at which the first part of string2 matches.  For example,
     * overlapPoint("abcd", "cdef") == 2.
     */
    @VisibleForTesting
    public static int overlapPoint(String string1, String string2) {
        if (string1 == null || string2 == null) {
            return -1;
        }
        return overlapPoint(string1.toCharArray(), string2.toCharArray());
    }

    /**
     * Finds the earliest point in array1 at which the first part of array2 matches.  For example,
     * overlapPoint("abcd", "cdef") == 2.
     */
    public static int overlapPoint(char[] array1, char[] array2) {
        if (array1 == null || array2 == null) {
            return -1;
        }
        int count1 = array1.length;
        int count2 = array2.length;

        // Ignore matching tails of the two arrays.
        while (count1 > 0 && count2 > 0 && array1[count1 - 1] == array2[count2 - 1]) {
            count1--;
            count2--;
        }

        int size = count2;
        for (int i = 0; i < count1; i++) {
            if (i + size > count1) {
                size = count1 - i;
            }
            int j;
            for (j = 0; j < size; j++) {
                if (array1[i+j] != array2[j]) {
                    break;
                }
            }
            if (j == size) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Applies the given style to a range of the input CharSequence.
     * @param style The style to apply (see the style constants in {@link Typeface}).
     * @param input The CharSequence to style.
     * @param start Starting index of the range to style (will be clamped to be a minimum of 0).
     * @param end Ending index of the range to style (will be clamped to a maximum of the input
     *     length).
     * @param flags Bitmask for configuring behavior of the span.  See {@link android.text.Spanned}.
     * @return The styled CharSequence.
     */
    public static CharSequence applyStyleToSpan(int style, CharSequence input, int start, int end,
            int flags) {
        // Enforce bounds of the char sequence.
        start = Math.max(0, start);
        end = Math.min(input.length(), end);
        SpannableString text = new SpannableString(input);
        text.setSpan(new StyleSpan(style), start, end, flags);
        return text;
    }

    @VisibleForTesting
    public static void copyToCharArrayBuffer(String text, CharArrayBuffer buffer) {
        if (text != null) {
            char[] data = buffer.data;
            if (data == null || data.length < text.length()) {
                buffer.data = text.toCharArray();
            } else {
                text.getChars(0, text.length(), data, 0);
            }
            buffer.sizeCopied = text.length();
        } else {
            buffer.sizeCopied = 0;
        }
    }

    /** Returns a String that represents the content of the given {@link CharArrayBuffer}. */
    @VisibleForTesting
    public static String charArrayBufferToString(CharArrayBuffer buffer) {
        return new String(buffer.data, 0, buffer.sizeCopied);
    }

    /**
    * SPRD:
    * The follow methods,include 'indexOfWordPrefix'、'indexOfSpellSearch'、'getFirstChar'
    * and 'removeBlkAndDot',designed to implement the highlight show feature of contacts' 
    * name when searching contacts.
    * 
    * Original Android code:
    * indexOfWordPrefix(CharSequence text, char[] prefix)
    * @{
    */
    
    
    /**
     * Finds the index of the first word that starts with the given prefix.
     * <p>
     * If not found, returns -1.
     * 
     * @param text the text in which to search for the prefix
     * @param prefix the text to find, in upper case letters
     */
    public static int indexOfWordPrefix(CharSequence text, String prefix) {
        return indexOfWordPrefix(text, prefix, null);
    }

    public static int indexOfWordPrefix(CharSequence text, String prefix, int[] length) {
        if (prefix == null || text == null) {
            return -1;
        }

        boolean isChinese = isChinese(text.toString())
                && isChinese(String.valueOf(prefix));
        int textLength = text.length();
        int prefixLength = prefix.length();

        if (prefixLength == 0 || textLength < prefixLength) {
            return -1;
        }

        int i = 0;
        while (i < textLength) {
            // Skip non-word characters
            while (i < textLength && !Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }

            if (i + prefixLength > textLength) {
                return -1;
            }

            // Compare the prefixes
            int j;
            int blkAndDotinPre = 0;
            int blkAndDotinTex = 0;
            for (j = 0; j + blkAndDotinPre < prefixLength;) {
                if (i + j + blkAndDotinTex >= textLength) {
                    break;
                }
                if (prefix.charAt(j + blkAndDotinPre) == ' ' || prefix.charAt(j + blkAndDotinPre) == '.') {
                    blkAndDotinPre++;
                    continue;
                } else if (text.charAt(i + j + blkAndDotinTex) == ' '
                        || text.charAt(i + j + blkAndDotinTex) == '.') {
                    blkAndDotinTex++;
                    continue;
                } else if (Character.toUpperCase(text.charAt(i + j + blkAndDotinTex)) != prefix.charAt(j
                        + blkAndDotinPre)) {
                    break;
                }
                j++;
            }
            if (j == prefixLength - blkAndDotinPre) {
                if (length != null) {
                    length[0] = j + blkAndDotinTex;
                }
                return i;
            }

            // Skip this word
            if (isChinese) {
                i++;
            } else {
                while (i < textLength && Character.isLetterOrDigit(text.charAt(i))) {
                    i++;
                }
            }
        }

        return -1;
    }

    /**
     * Finds the index of the first word that starts with the given prefix. To
     * deal with Chinese contact name when spelling search
     * <p>
     * If not found, returns -1.
     * 
     * @param displayText the display name of the contact without spelling
     * @param text contact name with spelling coming from the sort_key column in
     *            table raw_contacts
     * @param prefix prefix the text to find,the abbreviation or full spelling
     *            of contact name
     * @param length a return value of the string length which is matched
     */
    // To deal with Chinese contact name when spelling search
    public static int indexOfSpellSearch(CharSequence displayText, CharSequence text,
            char[] prefix, int[] length) {
        if (prefix == null || text == null) {
            return -1;
        }
        int textLength = text.length();
        prefix = removeBlkAndDot(prefix);
        int prefixLength = prefix.length;
        char[] firstCharStr = getFirstChar(displayText, text, textLength);

        if (prefixLength == 0 || textLength < prefixLength) {
            return -1;
        }
        int i = 0;
        int chCharLocation = 0;
        int strNum = 0;
        while (i < textLength) {
            // Skip non-word characters
            while (i < textLength && !Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
            // the number of string which have been skipped
            strNum++;
            if (i + prefixLength > textLength) {
                return -1;
            }
            // skip Chinese
            if (isChinese(text.charAt(i))) {
                chCharLocation++;
                i++;
                continue;
            }
            int j;
            int k = 0;
            int innerChNum = 0;
            int innerStrNum = 1;
            for (j = 0; j < prefixLength;) {
                if (i + j + k + innerChNum >= textLength)
                    break;
                if (Character.toUpperCase(text.charAt(i + j + k + innerChNum)) != prefix[j]) {
                    if (!Character.isLetterOrDigit(text.charAt(i + j + k + innerChNum))) {
                        k++;
                        if (Character.isLetterOrDigit(text.charAt(i + j + k + innerChNum))) {
                            innerStrNum++;
                        }
                        continue;
                    } else if (isChinese(text.charAt(i + j + k + innerChNum))) {
                        innerChNum++;
                        continue;
                    } else {
                        if (j == 1) {
                            while (j < prefixLength && firstCharStr[j + chCharLocation] != '0') {
                                if (firstCharStr[j + chCharLocation] != prefix[j]) {
                                    break;
                                }
                                j++;
                            }
                            if (j == prefixLength) {
                                length[0] = j;
                                return chCharLocation;
                            } else {
                                j = 1;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
                j++;
            }
            if (j == prefixLength) {
                length[0] = innerStrNum - innerChNum;
                return strNum - chCharLocation - 1;
            }
            // Skip this spell
            while (i < textLength && Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
        }
        return -1;
    }

    // get the first character of every spelling in this text
    // this method is designed for spell search,so do not use it by yourself
    private static char[] getFirstChar(CharSequence diaplayText, CharSequence text, int length) {
        char[] firstCharStr = new char[length];
        if (!isAllChinese(diaplayText.toString())) {
            Arrays.fill(firstCharStr, '0');
            return firstCharStr;
        }
        int i;
        int j = 1;
        firstCharStr[0] = text.charAt(0);
        for (i = 1; i < length - 1; i++) {
            if (text.charAt(i) == ' ' && isChinese(text.charAt(i - 1))) {
                firstCharStr[j] = text.charAt(i + 1);
                j++;
                i += 3;
            }
        }
        // the sign of end for firstCharStr
        Arrays.fill(firstCharStr, j, length, '0');
        return firstCharStr;
    }

    private static char[] removeBlkAndDot(char[] prefix) {
        if (prefix == null) {
            return null;
        }
        int length = 0;
        for (char c : prefix) {
            if (c != ' ' && c != '.') {
                length++;
            }
        }
        char[] result = new char[length];
        int i = 0;
        for (char c : prefix) {
            if (c != ' ' && c != '.') {
                result[i] = c;
                i++;
            }
        }
        return result;
    }

    public static final boolean isChinese(String strName) {
        if (strName == null) {
            return false;
        }
        char[] ch = strName.toCharArray();
        for (char c : ch) {
            if (isChinese(c)) {
                return true;
            }
        }
        return false;
    }

    public static final boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }
    public static final boolean isAllChinese(String strName) {
        if (strName == null) {
            return false;
        }
        char[] ch = strName.toCharArray();
        for (char c : ch) {
            if (!isChinese(c)) {
                if (c == ' ' || c == '.') {
                    continue;
                } else {
                    return false;
                }
            }
        }
        return true;
    }
    /**
    * @}
    */
}
