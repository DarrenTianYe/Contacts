package com.sprd.contacts.list;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsApplication;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.common.interactions.ImportExportDialogFragment;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.R;
import com.android.contacts.common.util.AccountFilterUtil;
import com.sprd.contacts.common.model.account.PhoneAccountType;

public class ContactMoreFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "ContactMoreFragment";

    public interface Listener {
        public void onContactMoreSelected(Uri contactUri, Rect targetRect);
    }

    private Listener mListener;
    private BaseAdapter mAdapter;
    private ListView mListView;
    private ContactListFilter mFilter;

    private Intent intent;
    private static final int SUBACTIVITY_ACCOUNT_FILTER = 6;

    private final class ContactMoreListAdapter extends BaseAdapter {
        private final Activity mActivity;
        private SharedPreferences mPrefs;

        public ContactMoreListAdapter(Activity activity) {
            mActivity = activity;
        }

        public int getCount() {
            // TODO Auto-generated method stub
            return 4;
        }

        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return null;
        }

        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            LayoutInflater inflater = mActivity.getLayoutInflater();
            LinearLayout listItemView = (LinearLayout) inflater.inflate(R.layout.contact_more_item,
                    null);
            TextView titleView = (TextView) listItemView.findViewById(R.id.title);
            titleView
                    .setText(mActivity.getResources().getStringArray(R.array.contact_more_item)[position]);
            if (position == 0) {
                TextView infoView = (TextView) listItemView.findViewById(R.id.info);
                infoView.setVisibility(View.VISIBLE);
                mPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
               // Log.d("tian","mmmmmmmmmmmmmmmmmttttttttttttttttttttttm");
                mFilter = ContactListFilter.restoreDefaultPreferences(mPrefs,getActivity());
                String displayAccount;
                switch (mFilter.filterType) {
                    case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS: {
                        displayAccount = mActivity.getString(R.string.display_all_contacts);
                        break;
                    }
                    case ContactListFilter.FILTER_TYPE_ACCOUNT: {
                        if (PhoneAccountType.ACCOUNT_TYPE.equals(mFilter.accountType)) {
                            displayAccount = mActivity.getString(R.string.label_phone);
                        } else {
                            displayAccount = mFilter.accountName;
                        }
                        break;
                    }
                    case ContactListFilter.FILTER_TYPE_CUSTOM: {
                        displayAccount = mActivity.getString(R.string.list_filter_custom);
                        break;
                    }
                    default:
                        displayAccount = mActivity.getString(R.string.display_all_contacts);
                }
                infoView.setText(displayAccount);
            }
            return listItemView;
        }
    }

    private final OnItemClickListener mListItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mAdapter == null || ((PeopleActivity) getActivity()).getDisableOptionItemSelected()) {
                return;
            }
            switch (position) {
                case 0:
                    AccountFilterUtil.startAccountFilterActivityForResult(getActivity(),
                            SUBACTIVITY_ACCOUNT_FILTER, mFilter);
                    break;
                case 1:
                    if (mListener != null && (!ContactsApplication.sApplication.isBatchOperation()
                            && !ContactSaveService.mIsGroupSaving)) {
                        PeopleActivity activity = (PeopleActivity) getActivity();
                        ImportExportDialogFragment.show(getFragmentManager(),
                                activity.areContactsAvailable(),
                                PeopleActivity.class);
                    } else {
                        Toast.makeText(getActivity(), R.string.toast_batchoperation_is_running,
                                Toast.LENGTH_LONG).show();
                    }
                    break;
                case 2:
                    intent = new Intent();
                    intent.setClassName("com.android.phone",
                            "com.sprd.phone.callsetting.FastDialSettingUUI");
                    startActivity(intent);
                    break;
                case 3:
                    // intent = new Intent();
                    // intent.setClassName("com.android.settings","com.android.settings.accounts.AddAccountSettings");
                    // startActivity(intent);
                    intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
                    intent.putExtra(Settings.EXTRA_AUTHORITIES, new String[] {
                            ContactsContract.AUTHORITY
                    });
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(intent);
                    break;
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (Listener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflateAndSetupView(inflater, container, savedInstanceState,
                R.layout.contact_more_fragment);

    }

    protected View inflateAndSetupView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState, int layoutResourceId) {
        View listLayout = inflater.inflate(layoutResourceId, container, false);

        mListView = (ListView) listLayout.findViewById(R.id.contact_more_list);
        mAdapter = new ContactMoreListAdapter(getActivity());
        mListView.setItemsCanFocus(true);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(mListItemClickListener);

        return listLayout;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        // TODO Auto-generated method stub

    }

    public BaseAdapter getAdapter() {
        return mAdapter;
    }

}

