package com.android.contacts.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ComponentName;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.android.contacts.ContactsApplication;
import com.android.contacts.R;
import com.android.contacts.list.GroupMemberCallListAdapter;
import com.android.contacts.list.GroupMemberCallListLoader;

public class GroupMemberCallListActivity extends Activity implements
		OnClickListener {

	private ListView lv;
	//private ImageButton mlocation;

	private List<Map<String, Object>> mList = new ArrayList<Map<String, Object>>(); //
	private HashMap<String, Object> mMap;
	private int mdefaultGroup;
	private int mshowSpecilGroup;
	private boolean groupBrowFlag = false;
	private static final int LOADER_GROUPS = 1;
	private GroupMemberCallListAdapter mGroupMemberAdapter;

	private String TAG = "GroupMemberCallListActivity";
	private final static String[] COLUMNS = new String[] { Groups.ACCOUNT_NAME,
			Groups.ACCOUNT_TYPE, Groups.DATA_SET, Groups._ID, Groups.TITLE,
			Groups.SUMMARY_COUNT, Groups.GROUP_NUMBER, Groups.GROUP_CURRENT_MODE,
			Groups.GROUP_BY_IMPORT_TYPE, Groups._ID

	};
	public static final int EVENT_UPDATE_ADAPTER = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.groupmember_list_call_list);

		if (getIntent() != null) {
			mshowSpecilGroup = getIntent().getIntExtra("group_number", -1);
			groupBrowFlag = getIntent().getBooleanExtra("group_number_flag", false);
					
		}
//		getLoaderManager()
//				.initLoader(LOADER_GROUPS, null, mGroupLoaderListener);
		this.initView(); //

	}

	private void initView() {
		lv = (ListView) findViewById(R.id.list);
		lv.setEmptyView((TextView)findViewById(R.id.empty_text)); 	
		//mlocation = (ImageButton) findViewById(R.id.locaton_show);
		//mlocation.setOnClickListener(this);

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		String defaultMember;
		if (groupBrowFlag) { // get group member ............
			defaultMember = String.valueOf(mshowSpecilGroup);
			Cursor curcor = getContentResolver().query(
					Groups.CONTENT_SUMMARY_URI,
					COLUMNS,
					Groups.GROUP_NUMBER + " =" + defaultMember + " AND "
							+ Groups.GROUP_CURRENT_MODE + " = "
							+ String.valueOf(ContactsApplication.getApplication().mMode), null,
					null);
			if (curcor != null) {
				while (curcor.moveToNext()) {
					mdefaultGroup = curcor.getInt(curcor
							.getColumnIndex(Groups._ID));
				}
			}
			curcor.close();
		} else{
			Cursor curcor = getContentResolver().query(Groups.CONTENT_SUMMARY_URI,
					COLUMNS, Groups.GROUP_DEFAULT + " = " + String.valueOf(Groups.GROUP_DEFAULT_FLAG), null, null);
			if (curcor != null) {
				while (curcor.moveToNext()) {

					mdefaultGroup = curcor
							.getInt(curcor.getColumnIndex(Groups._ID));
					Log.d(TAG, "default_number="+curcor
							.getInt(curcor.getColumnIndex(Groups.GROUP_NUMBER))+"");
				}
			}
			curcor.close();
		}
		getLoaderManager().restartLoader(LOADER_GROUPS, null,
				mGroupLoaderListener);
	}

	private void setListView() {
		mGroupMemberAdapter = new GroupMemberCallListAdapter(mList, this);
		lv.setAdapter(mGroupMemberAdapter);
	}

	private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderListener = new LoaderCallbacks<Cursor>() {

		@Override
		public CursorLoader onCreateLoader(int id, Bundle args) {
			// mEmptyView.setText(null);

			return GroupMemberCallListLoader
					.constructLoaderForGroupDetailQuery(getBaseContext(),
							mdefaultGroup);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			Log.d(TAG, "onLoadFinished.....in==");

			if (data == null || data.isClosed()) {

				return;
			} else {
				mList = new ArrayList<Map<String, Object>>();
				if (data != null) {
					while (data.moveToNext()) {
						mMap = new HashMap<String, Object>(); //

						Log.d(TAG,
								"defaultMember=="
										+ data.getString(GroupMemberCallListLoader.GroupEditorQuery.CONTACT_ID));
						mMap.put("member_id",
								data.getString(GroupMemberCallListLoader.GroupEditorQuery.CONTACT_ID));
						mMap.put("member_name",
								data.getString(GroupMemberCallListLoader.GroupEditorQuery.DISPLAY_NAME_PRIMARY));
						mMap.put("member_lever", "队员");
						//mMap.put("member_status", "空闲");
						mList.add(mMap);

					}
				}

			}
			setListView();
		}

		public void onLoaderReset(Loader<Cursor> loader) {
		}
	};

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

/*		Intent lbsIntent = new Intent(Intent.ACTION_MAIN);
		lbsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		lbsIntent.setComponent(new ComponentName("com.neolink.location",
				"com.neolink.location.SplashActivity"));
		lbsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		this.startActivity(lbsIntent);*/

	}

}
