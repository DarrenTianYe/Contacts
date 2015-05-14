
package com.sprd.contacts.list;

import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactEntryListAdapter;

import java.util.HashMap;
import java.util.Set;

public class AllInOneCallLogPickerFragment extends AllInOneDataPickerFragment {
    private static final String TAG = AllInOneCallLogPickerFragment.class.getSimpleName();
    private OnAllInOneDataMultiPickerActionListener mMultiPickerListener;

    @Override
    protected void onItemClick(int position, long id) {
        Uri dataUri = null;

        AllInOneCallLogListAdapter adapter = (AllInOneCallLogListAdapter) getAdapter();
        dataUri = adapter.getDataUri(position);

        if (dataUri != null) {
            pickAllInOneData(dataUri);
        }
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        AllInOneCallLogListAdapter adapter = new AllInOneCallLogListAdapter(getActivity());
        adapter.setDisplayPhotos(true);
        return adapter;
    }

    public void setOnAllInOneDataMultiPickerActionListener(
            OnAllInOneDataMultiPickerActionListener listener) {
        this.mMultiPickerListener = listener;
    }

    @Override
    public void onMultiPickerSelected() {
        HashMap<String, String> ret = new HashMap<String, String>();
        ContactEntryListAdapter adapter = getAdapter();
        Set<Integer> checkedItems = adapter.getCheckedItems();
        for (Integer i : checkedItems) {
            Cursor c = (Cursor) adapter.getItem(i);

            if (c == null) {
                continue;
            }
            int columnIndex = c.getColumnIndex(Calls.DATE);
            if (columnIndex == -1) {
                continue;
            }
            String num = c.getString(c.getColumnIndex(Calls.CACHED_FORMATTED_NUMBER));
            String name = c.getString(c.getColumnIndex(Calls.CACHED_NAME));
            ret.put(num, name);
        }
        if (ret.size() == 0) {
            Toast.makeText(getActivity(), R.string.toast_no_contact_selected,
                    Toast.LENGTH_SHORT).show();
        }
        mMultiPickerListener.onPickAllInOneDataAction(ret);
    }
    /*
    * SPRD:
    * 
    * @{
    */
    @Override
    protected void prepareEmptyView() {
        super.prepareEmptyView();
        setEmptyText(R.string.recentCalls_empty);
    }
    /*
    * @}
    */
}
