/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.sprd.contacts.common.widget;

import java.util.Locale;

import com.sprd.contacts.common.util.UniverseUtils;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.TextView;
import android.view.Gravity;
import android.widget.PopupWindow;
import android.view.MotionEvent;
import android.graphics.Paint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews.RemoteView;
import com.android.contacts.common.R;

public class BladeView extends View {
    private int mMinCharHeight = 1;
    private int mCharHeight = 5;
    private OnItemClickListener mOnItemClickListener;
    private PopupWindow mPopupWindow;
    private TextView mPopupText;
    private int mTextColor;
    private boolean mTouched = false;
    private boolean mIsTW = false;
    private int mCurItem = 0;
    private float mHeight = 0;
    private static int BLADE_BACKGROUND_WIDTH = 25;

    private int[] mTextColorSet;
    private final String[] mAlphabet = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
    };

    private final String[] mTraditional = {
             "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
             "17", "18", "19", "20",
             "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
    };

    private final String[] mTraditionalFull = {
            "1劃", "2劃", "3劃", "4劃", "5劃", "6劃", "7劃", "8劃", "9劃", "10劃", "11劃", "12劃", "13劃", "14劃", "15劃", "16劃",
            "17劃", "18劃", "19劃", "20劃",
            "21劃", "22劃", "23劃", "24劃", "25劃", "26劃", "27劃", "28劃", "29劃", "30劃", "31劃", "32劃",
           "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
           "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
   };
    private String TAG = "BladeView";
    private Context mContext;

    public BladeView(Context context) {
        this(context, null);
        mContext = context;
        BLADE_BACKGROUND_WIDTH = mContext.getResources().getDimensionPixelSize(
                R.dimen.contact_bladeview_width);
        mIsTW = isTw(context);
        mTextColorSet = new int[(mIsTW ? mTraditional.length : mAlphabet.length)];
    }

    public BladeView(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.textViewStyle);
        mContext = context;
        BLADE_BACKGROUND_WIDTH = mContext.getResources().getDimensionPixelSize(
                R.dimen.contact_bladeview_width);
        mIsTW = isTw(context);
        mTextColorSet = new int[(mIsTW ? mTraditional.length : mAlphabet.length)];
    }

    public BladeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        final Resources.Theme theme = getContext().getTheme();
        ColorStateList textColor = null;
        BLADE_BACKGROUND_WIDTH = mContext.getResources().getDimensionPixelSize(
                R.dimen.contact_bladeview_width);
        TypedArray a = theme.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.TextViewAppearance, defStyle, 0);
        TypedArray appearance = null;
        int ap = a.getResourceId(
                com.android.internal.R.styleable.TextViewAppearance_textAppearance, -1);
        a.recycle();
        if (ap != -1) {
            appearance = theme.obtainStyledAttributes(
                    ap, com.android.internal.R.styleable.TextAppearance);
        }
        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);
                if (attr == com.android.internal.R.styleable.TextAppearance_textColor) {
                    textColor = appearance.getColorStateList(attr);
                }
            }
        }
        if (textColor != null) {
            mTextColor = textColor.getDefaultColor();
        } else {
            mTextColor = Color.GRAY;
        }
        mIsTW = isTw(context);
        mTextColorSet = new int[(mIsTW ? mTraditional.length : mAlphabet.length)];
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        mHeight = (float)heightSize;
        int charNumber = getAlphabet(mIsTW).length;
        int charHeigtht = heightSize / charNumber;
        if (charHeigtht < mMinCharHeight) {
            heightSize = mMinCharHeight * charNumber;
            mCharHeight = mMinCharHeight;
        } else {
            mCharHeight = charHeigtht;
        }
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            setMeasuredDimension(BLADE_BACKGROUND_WIDTH, heightSize);
        }
        else {
            int widthSize = mCharHeight + getPaddingRight() + getPaddingLeft();
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int textSize = 0;
        int charNumber = getAlphabet(mIsTW).length;
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(mCharHeight - 6);
        textSize = mCharHeight - 6;
        paint.setColor(this.getResources().getColor(R.color.contact_bladeview_index_text_color));
        for (int i = 0; i < charNumber; ++i) {
            String currentChar = getAlphabet(mIsTW)[i];
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                if (mTextColorSet[i] == Color.TRANSPARENT) {
                    if (mTouched) {
                        paint.setColor(this.getResources().getColor(
                                R.color.contact_bladeview_index_pressed_text_color));
                    }
                    else {
                        paint.setColor(this.getResources().getColor(
                                R.color.contact_bladeview_index_text_color));
                    }
                }
                float y = (i + 1) * mCharHeight;
                if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    if (mIsTW) {
                        if (i % 8 != 0) {
                            currentChar = " ";
                        }
                        if (i == 0) {
                            currentChar = " ";
                        }
                        paint.setTextSize((mCharHeight) * 5);
                        textSize = (mCharHeight) * 5;
                        y = i * (mHeight / (float)mTraditional.length );
                        canvas.drawText(currentChar,
                                (BLADE_BACKGROUND_WIDTH - textSize) / 2
                                , y, paint);
                    } else {
                        if (i % 3 != 0) {
                            currentChar = " ";
                        }
                        paint.setTextSize((mCharHeight) * 2);
                        textSize = (mCharHeight) * 2;
                        y = (i + 2) * mCharHeight;
                        canvas.drawText(currentChar,
                                BLADE_BACKGROUND_WIDTH / 4
                                , y, paint);
                    }
                } else if (mIsTW) {
                    if (i == 0 || i % 3 != 0) {
                        currentChar = " ";
                    }
                    paint.setTextSize((mCharHeight) * 2);
                    textSize = (mCharHeight) * 2;
                    y = i * (mHeight / (float)mTraditional.length );
                    canvas.drawText(currentChar,
                            (BLADE_BACKGROUND_WIDTH - textSize) / 2, y, paint);
                } else {
                    paint.setTextSize(mCharHeight);
                    textSize = mCharHeight;
                    y = (i + 1) *(mHeight / (float)mAlphabet.length);
                    canvas.drawText(currentChar,
                            BLADE_BACKGROUND_WIDTH / 4
                            , y, paint);
                }
            } else {
                canvas.drawText(currentChar, getPaddingLeft(), (i + 1)
                        * mCharHeight, paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        final int charNumber = getAlphabet(mIsTW).length;
        if (action == MotionEvent.ACTION_DOWN
                || action == MotionEvent.ACTION_MOVE) {
            getParent().requestDisallowInterceptTouchEvent(true);
            int item = (int) (event.getY() / mCharHeight);
            if (item < 0 || item >= charNumber) {
                return true;
            }
            showPopup(item);
            performItemClicked(item);
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                mTouched = true;
                mCurItem = item;
                this.setBackgroundResource(R.drawable.call_bg_addnew_sprd);
            }
        } else {
            dismissPopup();
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                this.setBackgroundColor(Color.TRANSPARENT);
                mTouched = false;
            }
        }
        return true;
        // return super.onTouchEvent(event);
    }

    private void showPopup(int item) {
        if (mPopupWindow == null) {
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                Log.e("ministorm", "width = " + this.getResources().getDimensionPixelSize(
                        R.dimen.contact_bladeview_popup_width));
                mPopupWindow = new PopupWindow(this.getResources().getDimensionPixelSize(
                        R.dimen.contact_bladeview_popup_width), this.getResources()
                        .getDimensionPixelSize(R.dimen.contact_bladeview_popup_width));
            } else {
                mPopupWindow = new PopupWindow(180, 180);
            }
            mPopupText = new TextView(getContext());
            mPopupText.setTextSize(this.getResources().getDimension(
                    R.dimen.contact_bladeview_popup_text_size));
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                mPopupText.setBackgroundResource(R.drawable.ic_contacts_index_backgroud_sprd);
                mPopupText.setTextColor(this.getResources().getColor(
                        R.color.contact_bladeview_popup_text_color));
            } else {
                mPopupText.setBackgroundColor(Color.GRAY);
                mPopupText.setTextColor(Color.CYAN);
            }
            mPopupText.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            mPopupWindow.setContentView(mPopupText);
        }

        String text = getAlphabet(mIsTW)[item];
        mPopupText.setText(text);
        if (mPopupWindow.isShowing()) {
            mPopupWindow.update();
        } else {
            mPopupWindow.showAtLocation(getRootView(), Gravity.CENTER_HORIZONTAL
                    | Gravity.CENTER_VERTICAL, 0, 0);
        }
    }

    private void dismissPopup() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    private void performItemClicked(int item) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(item);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int item);
    }

    public void configCharacterColorToDefault() {

        for (int i = 0; i < mTextColorSet.length; i++) {
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                mTextColorSet[i] = Color.TRANSPARENT;
            } else {
                mTextColorSet[i] = Color.GRAY;
            }
        }
    }

    public boolean isTw(Context context) {
        String country = context.getResources().getConfiguration().locale.getCountry();
        if ("TW".equals(country)) {
            return true;
        }
        return false;
    }

    public String[] getAlphabet(boolean isTW) {
        if (isTW) {
            return mTraditional;
        } else {
            return mAlphabet;
        }
    }

    public String[] getFullAlphabet(boolean isTW) {
        if (isTW) {
            return mTraditionalFull;
        } else {
            return mAlphabet;
        }
    }
}
