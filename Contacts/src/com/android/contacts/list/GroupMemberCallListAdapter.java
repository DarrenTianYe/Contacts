package com.android.contacts.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import neolink.telephony.PrivateIntents;

import com.android.contacts.R;
import com.android.contacts.activities.GroupMemberCallListActivity;
import com.android.contacts.list.GroupMemberCallListLoader.GroupDetailQuery;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * \
 * 
 * @author xuhong.tian
 * 
 */
public class GroupMemberCallListAdapter extends BaseAdapter {

	private static final String TAG = "GroupMemberCallListAdapter";
	private Context context;
	private List<Map<String, Object>> list;
	private int position;
	private LayoutInflater layoutInflater = null;

	private String tag = "GroupMemberCallListAdapter";

	public GroupMemberCallListAdapter(List<Map<String, Object>> list,
			GroupMemberCallListActivity mainActivity) {
		// TODO Auto-generated constructor stub
		this.list = list;
		this.context = mainActivity;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder viewHolder = new ViewHolder(); //
		Map<String, Object> map = list.get(position); //
		this.position = position;

		if (convertView == null) {

			layoutInflater = LayoutInflater.from(context); //
			convertView = layoutInflater.inflate(
					R.layout.groupmember_list_call_item, null);

			viewHolder.member_name = (TextView) convertView
					.findViewById(R.id.membername);
			viewHolder.member_lever = (TextView) convertView
					.findViewById(R.id.member_evel);
			/*
			 * viewHolder.member_status = (TextView) convertView
			 * .findViewById(R.id.current_status);
			 */
			viewHolder.member_call = (ImageButton) convertView
					.findViewById(R.id.member_call);
			viewHolder.member_call
					.setOnClickListener(new ListitemButtonListener(viewHolder,
							position, list.get(position)));

			convertView.setTag(viewHolder); //
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		if (map.get("member_name") != null) {
			viewHolder.member_name.setText(map.get("member_name").toString());
		} else {
			viewHolder.member_name.setText("test");
		}
		if (map.get("member_lever") != null) {
			viewHolder.member_lever.setText(map.get("member_lever").toString());
		} else {
			viewHolder.member_lever.setText("test");
		}
		/*
		 * if (map.get("member_status") != null) {
		 * viewHolder.member_status.setText(map.get("member_status")
		 * .toString()); } else { viewHolder.member_status.setText("test"); }
		 */
		convertView.setId(position);
		return convertView;
	}

	class ListitemButtonListener implements OnClickListener {
		private ViewHolder mViewHolder;
		private Map<String, Object> mmap;

		ListitemButtonListener(ViewHolder viewHolder, int pos,
				Map<String, Object> map) {
			position = pos;
			mViewHolder = viewHolder;
			mmap = map;
		}

		@Override
		public void onClick(View view) {

			// mViewHolder.member_name.setTextColor(Color.parseColor("#61d10e"));
			// mViewHolder.member_lever.setTextColor(Color.parseColor("#61d10e"));
			// mViewHolder.member_status.setTextColor(Color.parseColor("#61d10e"));

			String member_id = (String) mmap.get("member_id");
			String number = SearchGroupMemberNumber(member_id);

			Log.d(TAG, "number=" + number);

			Intent privateintent = new Intent(
					PrivateIntents.ACTION_PRIVATE_PHONE);
			if (TextUtils.isEmpty(number)) {
				return;
			}
			privateintent.putExtra(PrivateIntents.EXTRA_CONTACT_NAME, "test");
			privateintent.putExtra(PrivateIntents.EXTRA_CONTACT_NUMBER,
					number.replace(" ", ""));
			context.startActivity(privateintent);

		}
	}

	protected class ViewHolder {

		protected TextView member_name; //
		protected TextView member_lever; //
		protected TextView member_status; //
		protected ImageButton member_call; //

	}

	private Uri createUri() {
		Uri uri = Data.CONTENT_URI;
		uri = uri
				.buildUpon()
				.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
						String.valueOf(Directory.DEFAULT)).build();
		return uri;
	}

	private String createSelection() {
		StringBuilder selection = new StringBuilder();
		selection.append(Data.RAW_CONTACT_ID + "=?");
		return selection.toString();
	}

	private String[] createSelectionArgs(String member_id) {
		List<String> selectionArgs = new ArrayList<String>();
		selectionArgs.add(member_id);
		return selectionArgs.toArray(new String[0]);
	}

	public String SearchGroupMemberNumber(String member_id) {

		Uri uri = Data.CONTENT_URI;
		String number = "";
		uri = uri
				.buildUpon()
				.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
						String.valueOf(Directory.DEFAULT)).build();

		Cursor cursor = context.getContentResolver().query(
				uri,
				new String[] { Data.RAW_CONTACT_ID, Data.MIMETYPE, Data.DATA1,
						Data.DATA2 }, createSelection(),
				createSelectionArgs(member_id), null);

		Log.d(TAG, "phone................");
		while (cursor.moveToNext()) {

			String localnumber = cursor.getString(cursor
					.getColumnIndex(Data.DATA1));
			String mimeType = cursor.getString(cursor
					.getColumnIndex(Data.MIMETYPE));

			if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
				Log.d(TAG, "phone=" + localnumber);
				number = localnumber;
			}
		}
		cursor.close();

		Log.d(TAG, "phone...............==." + number.replace(" ", "")
				+ "number==" + number);

		return number.replace(" ", "");
	}

}