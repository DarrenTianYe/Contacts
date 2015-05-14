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

package com.android.contacts.quickcontact;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.content.Intent;
import android.os.SystemProperties;

import com.android.contacts.common.ContactPresenceIconUtil;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.util.privateCallandSmsUtil;
import com.android.contacts.R;
import com.sprd.contacts.common.util.UniverseUtils;

import java.util.List;

/** A fragment that shows the list of resolve items below a tab */
public class QuickContactListFragment extends Fragment {
    private ListView mListView;
    private List<Action> mActions;
    private RelativeLayout mFragmentContainer;
    private Listener mListener;
    private String mMimeType;
    /**
    * SPRD:
    * @{
    */
    private static final boolean mSupportVt = SystemProperties.getBoolean("persist.sys.support.vt", true);
    /**
    * @}
    */
    public QuickContactListFragment(String mimeType) {
        setRetainInstance(true);
        this.mMimeType = mimeType;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        /**
         * SPRD:
         *
         * Original Android code:
         *
         * @{
         */
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            mFragmentContainer = (RelativeLayout) inflater.inflate(
                    R.layout.quickcontact_list_fragment_newui,
                    container, false);
        } else {
            mFragmentContainer = (RelativeLayout) inflater.inflate(
                    R.layout.quickcontact_list_fragment,
                    container, false);
        }
        /**
         * @}
         */
        mListView = (ListView) mFragmentContainer.findViewById(R.id.list);
        mListView.setItemsCanFocus(true);

        mFragmentContainer.setOnClickListener(mOutsideClickListener);
        configureAdapter();
        return mFragmentContainer;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setActions(List<Action> actions) {
        mActions = actions;
        configureAdapter();
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    private void configureAdapter() {
        if (mActions == null || mListView == null) return;

        mListView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return mActions.size();
            }

            @Override
            public Object getItem(int position) {
                return mActions.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Set action title based on summary value
                final Action action = mActions.get(position);
                String mimeType = action.getMimeType();
                /**
                 * SPRD:
                 *
                 * Original Android code:
                 *   final View resultView = convertView != null ? convertView
                        : getActivity().getLayoutInflater().inflate(
                                mimeType.equals(StructuredPostal.CONTENT_ITEM_TYPE) ?
                                        R.layout.quickcontact_list_item_address :
                                        R.layout.quickcontact_list_item,
                                        parent, false);
                 * @{
                 */

                final View resultView;
                if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                    resultView = convertView != null ? convertView
                            : getActivity().getLayoutInflater().inflate(
                                    mimeType.equals(Phone.CONTENT_ITEM_TYPE) ?
                                            R.layout.quickcontact_list_item_new_ui :
                                            R.layout.quickcontact_list_item_address_new_ui,
                                    parent, false);
                } else {
                    resultView = convertView != null ? convertView
                            : getActivity().getLayoutInflater().inflate(
                                    mimeType.equals(Phone.CONTENT_ITEM_TYPE) ?
                                            R.layout.quickcontact_list_item :
                                            R.layout.quickcontact_list_item_address,
                                    parent, false);
                }
                /**
                 * @}
                 */
                // TODO: Put those findViewByIds in a container
                final TextView text1 = (TextView) resultView.findViewById(
                        android.R.id.text1);
                final TextView text2 = (TextView) resultView.findViewById(
                        android.R.id.text2);
                final View actionsContainer = resultView.findViewById(
                        R.id.actions_view_container);
                final ImageView alternateActionButton = (ImageView) resultView.findViewById(
                        R.id.secondary_action_button);
                final View alternateActionDivider = resultView.findViewById(R.id.vertical_divider);
                final ImageView presenceIconView =
                        (ImageView) resultView.findViewById(R.id.presence_icon);
                /**
                 * SPRD:
                 *
                 * Original Android code:
                 *      actionsContainer.setOnClickListener(mPrimaryActionClickListener);
                        actionsContainer.setTag(action);
                        alternateActionButton.setOnClickListener(mSecondaryActionClickListener);
                        alternateActionButton.setTag(action);
                 * @{
                 */
                final ImageView thirdaryActionButton = (ImageView) resultView.findViewById(
                        R.id.thirdary_action_button);
                if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                    actionsContainer.setBackgroundColor(Color.WHITE);
                    final View fourthActionDivider = resultView
                            .findViewById(R.id.vertical_divider_fourth);
                    alternateActionButton.setOnClickListener(mIpDialActionClickListener);
                    alternateActionButton.setTag(action);

                    thirdaryActionButton.setOnClickListener(mPrimaryActionClickListener);
                    thirdaryActionButton.setTag(action);

                    if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                        final ImageView fourthActionButton = (ImageView) resultView.findViewById(
                                R.id.fourth_action_button);
                        final ImageView fifthActionButton = (ImageView) resultView.findViewById(
                                R.id.fifth_action_button);
                        //fourthActionButton.setOnClickListener(mThirdaryActionClickListener);
                        fourthActionButton.setTag(action);
                        fourthActionButton.setVisibility(mSupportVt ? View.VISIBLE : View.GONE);
                        fourthActionDivider.setVisibility(mSupportVt ? View.VISIBLE : View.GONE);

                        fifthActionButton.setOnClickListener(mSecondaryActionClickListener);
                        fifthActionButton.setTag(action);
                        fifthActionButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    actionsContainer.setOnClickListener(mPrimaryActionClickListener);
                    actionsContainer.setTag(action);
                    alternateActionButton.setOnClickListener(mSecondaryActionClickListener);
                    alternateActionButton.setTag(action);
                   // thirdaryActionButton.setOnClickListener(mThirdaryActionClickListener);
                    thirdaryActionButton.setTag(action);
                }

                final boolean hasAlternateAction = action.getAlternateIntent() != null;
                /**
                 * SPRD:
                 *
                 * Original Android code:
                 *      alternateActionDivider.setVisibility(hasAlternateAction ? View.VISIBLE : View.GONE);
                        alternateActionButton.setImageDrawable(action.getAlternateIcon());
                        alternateActionButton.setContentDescription(action.getAlternateIconDescription());
                        alternateActionButton.setVisibility(hasAlternateAction ? View.VISIBLE : View.GONE);
                 * @{
                 */

                final boolean hasThirdaryAction = action.getThirdaryIntent() != null;
                if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
                    // TODO: Put those findViewByIds in a container
                    final View thirdaryActionDivider = resultView
                            .findViewById(R.id.vertical_divider_thirdary);

                    alternateActionDivider.setVisibility(hasAlternateAction ? View.VISIBLE
                            : View.GONE);
    				if (action != null
    						&& action.getAlternateIntent() != null
    						&& (action
    								.getAlternateIntent()
    								.getIntExtra(
    										privateCallandSmsUtil.PRIVATE_CALL_SMS_IMPORT_MODE,
    										-1) == RawContacts.CONTACTS_BY_PCTOOLS || action
    										.getAlternateIntent()
    										.getIntExtra(
    												privateCallandSmsUtil.PRIVATE_CALL_SMS_IMPORT_MODE,
    												-1) == RawContacts.CONTACTS_BY_EDITE)) {
    					alternateActionButton
    							.setImageResource(R.drawable.private_contacats_sms_icon);
    				} else {
    					alternateActionButton.setImageDrawable(action
    							.getAlternateIcon());
    				}
                    alternateActionButton.setContentDescription(action
                            .getAlternateIconDescription());
                    alternateActionButton.setVisibility(hasAlternateAction ? View.VISIBLE
                            : View.GONE);
                    thirdaryActionDivider
                            .setVisibility(hasThirdaryAction && mSupportVt ? View.VISIBLE
                                    : View.GONE);
                    thirdaryActionButton.setImageDrawable(action.getThirdaryIcon());
                    thirdaryActionButton.setContentDescription(action.getThirdaryIconDescription());
                    thirdaryActionButton
                            .setVisibility(hasThirdaryAction && mSupportVt ? View.VISIBLE
                                    : View.GONE);
                }
                /**
                 * @}
                 */
                if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    // Force LTR text direction for phone numbers
                    text1.setTextDirection(View.TEXT_DIRECTION_LTR);

                    // Special case for phone numbers in accessibility mode
                    text1.setContentDescription(getActivity().getString(
                            R.string.description_dial_phone_number, action.getBody()));
                    if (hasAlternateAction) {
                        alternateActionButton.setContentDescription(getActivity()
                                .getString(R.string.description_send_message, action.getBody()));
                    }
                }
                /**
                 * SPRD:
                 *  , and ; display P and W
                 * Original Android code:
                 *     text1.setText(action.getBody());
                 * @{
                 */
                text1.setTextColor(getResources().getColor(
                        R.color.quick_contact_list_data_text_color));
                text1.setText(ContactsUtils.CommaAndSemicolonTopAndw(action.getBody().toString()));
                /**
                 * @}
                 */
                if (text2 != null) {
                    CharSequence subtitle = action.getSubtitle();
					if (action != null
							&& action.getAlternateIntent() != null
							&& (action
									.getAlternateIntent()
									.getIntExtra(
											privateCallandSmsUtil.PRIVATE_CALL_SMS_IMPORT_MODE,
											-1) == RawContacts.CONTACTS_BY_PCTOOLS || action
									.getAlternateIntent()
									.getIntExtra(
											privateCallandSmsUtil.PRIVATE_CALL_SMS_IMPORT_MODE,
											-1) == RawContacts.CONTACTS_BY_EDITE)) {
						text2.setText(getString(R.string.private_number));
					} else {
						text2.setText(subtitle);
					}
                    text2.setTextColor(getResources().getColor(
                            R.color.quick_contact_list_type_text_color));
                    if (TextUtils.isEmpty(subtitle)) {
                        text2.setVisibility(View.GONE);
                    } else {
                        text2.setVisibility(View.VISIBLE);
                    }
                }
                final Drawable presenceIcon = ContactPresenceIconUtil.getPresenceIcon(
                        getActivity(), action.getPresence());
                if (presenceIcon != null) {
                    presenceIconView.setImageDrawable(presenceIcon);
                    presenceIconView.setVisibility(View.VISIBLE);
                }
                /**
                 * SPRD:
                 *
                 * Original Android code:
                 *  } else {
                       presenceIconView.setVisibility(View.GONE);
                    }
                 * @{
                 */
//                } else {
//                    presenceIconView.setVisibility(View.GONE);
//                }
                /**
                 * @}
                 */
                return resultView;
            }
        });
    }

    /** A data item (e.g. phone number) was clicked */
    protected final OnClickListener mPrimaryActionClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final Action action = (Action) v.getTag();
            /**
             * SPRD:
             *
             * Original Android code:
             *      if (mListener != null) mListener.onItemClicked(action, false);
             * @{
             */
          //  if (mListener != null) mListener.onItemClicked(action, 0);
            /**
             * @}
             */
            /**
             * add by xuhong.tian
             */
			if (action != null
					&& action.getIntent() != null
					&& (action.getIntent().getIntExtra(
							privateCallandSmsUtil.PRIVATE_CALL_SMS_IMPORT_MODE,
							-1) == RawContacts.CONTACTS_BY_PCTOOLS || action.getIntent().getIntExtra(
									privateCallandSmsUtil.PRIVATE_CALL_SMS_IMPORT_MODE,
									-1) == RawContacts.CONTACTS_BY_EDITE )) {
				Intent Privateintent = privateCallandSmsUtil
						.getForwardPrivatePhone("",
								action.getIntent().getStringExtra(privateCallandSmsUtil.PRIVATE_CALL_SMS_NUMBER),
								action.getIntent().getIntExtra(
										privateCallandSmsUtil.PRIVATE_CALL_SMS_NUMBER,
										action.getIntent().getIntExtra(privateCallandSmsUtil.PRIVATE_CALL_SMS_CURRENT_MODE, -1)));

				if (Privateintent != null) {
					getActivity().startActivity(Privateintent);
					getActivity().finish();
				}

			} else {
				if (mListener != null)
					mListener.onItemClicked(action, 0);
			}
        }
    };

    /** A secondary action (SMS) was clicked */
    protected final OnClickListener mSecondaryActionClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final Action action = (Action) v.getTag();
            /**
             * SPRD:
             *
             * Original Android code:
             *
             * @{
             */
            //if (mListener != null) mListener.onItemClicked(action, true);
          //  if (mListener != null) mListener.onItemClicked(action, 1);
            /**
             * @}
             */
            /**
             * add by xuhong.tian
             */
			if (action != null
					&& action.getAlternateIntent() != null
					&& (action.getAlternateIntent().getIntExtra(
							privateCallandSmsUtil.PRIVATE_CALL_SMS_IMPORT_MODE,
							-1) == RawContacts.CONTACTS_BY_PCTOOLS || action.getAlternateIntent().getIntExtra(
									privateCallandSmsUtil.PRIVATE_CALL_SMS_IMPORT_MODE,
									-1) == RawContacts.CONTACTS_BY_EDITE)) {
				Intent Privateintent = privateCallandSmsUtil
						.getForwardPrivateSms(
								"",
								action.getAlternateIntent()
										.getStringExtra(
												privateCallandSmsUtil.PRIVATE_CALL_SMS_NUMBER),
												action.getAlternateIntent()
										.getIntExtra(
												privateCallandSmsUtil.PRIVATE_CALL_SMS_NUMBER,
												action.getAlternateIntent()
														.getIntExtra(
																privateCallandSmsUtil.PRIVATE_CALL_SMS_CURRENT_MODE,
																-1)));

				if (Privateintent != null) {
					getActivity().startActivity(Privateintent);
				}

			} else {
				if (mListener != null)
					mListener.onItemClicked(action, 1);
			}
        }
    };

    private final OnClickListener mOutsideClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {

			final Action action = (Action) v.getTag();
			/**
			 * add by xuhong.tian
			 */
			if (action != null
					&& action.getIntent() != null
					&& (action.getIntent().getIntExtra(
							privateCallandSmsUtil.PRIVATE_CALL_SMS_IMPORT_MODE,
							-1) == RawContacts.CONTACTS_BY_PCTOOLS || action
							.getIntent()
							.getIntExtra(
									privateCallandSmsUtil.PRIVATE_CALL_SMS_IMPORT_MODE,
									-1) == RawContacts.CONTACTS_BY_EDITE)) {
				Intent Privateintent = privateCallandSmsUtil
						.getForwardPrivatePhone(
								"",
								action.getIntent()
										.getStringExtra(
												privateCallandSmsUtil.PRIVATE_CALL_SMS_NUMBER),
								action.getIntent()
										.getIntExtra(
												privateCallandSmsUtil.PRIVATE_CALL_SMS_NUMBER,
												action.getIntent()
														.getIntExtra(
																privateCallandSmsUtil.PRIVATE_CALL_SMS_CURRENT_MODE,
																-1)));

				if (Privateintent != null) {
					getActivity().startActivity(Privateintent);
					getActivity().finish();
				}

			} else {

				if (mListener != null)
					mListener.onOutsideClick();
			}
		}
    };

    public interface Listener {
        void onOutsideClick();
        /**
         * SPRD:
         *
         * Original Android code:
         *    void onItemClicked(Action action, boolean alternate);
         * @{
         */
        void onItemClicked(Action action, int index);
        /**
         * @}
         */
    }
    /**
    * SPRD:
    * @{
    */
    protected final OnClickListener mIpDialActionClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final DataAction action = (DataAction) v.getTag();
            if (action != null) {
                Intent intent = action.getIntent();
                if (intent != null) {
                    intent.putExtra(UniverseUtils.IS_IP_DIAL, true);
                    if (getActivity() != null) {
                        getActivity().startActivity(intent);
                    }
                }
            }
            /**
             * SPRD:
             * bug 278104
             * @{
             */
            if (mListener != null) mListener.onItemClicked(action, -1);
            /**
             * @}
             */
        }
    };

    protected final OnClickListener mThirdaryActionClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
        final Action action = (Action) v.getTag();
        if (mListener != null) mListener.onItemClicked(action, 2);
        }
    };
    /**
    * @}
    */
}
