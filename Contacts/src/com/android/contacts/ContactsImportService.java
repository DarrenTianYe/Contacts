package com.android.contacts;

import java.util.ArrayList;
import java.util.List;

import neolink.telephony.GroupInfo;
import neolink.telephony.PrivateContactContract;
import neolink.telephony.PrivateIntents;
import neolink.telephony.PrivateMode;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts.Data;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.pctool.ConfigurationManager;
import android.pctool.ConfigurationManager.ConfigurationException;
import android.pctool.ConfigurationManager.ValueList;
import com.neolink.internal.telephony.IPrivateTelephony;
import com.neolink.internal.telephony.IPrivateRegistry;

public class ContactsImportService extends Service {
	private static final String TAG = "ContactsImportService";

	private static final int EVENT_IMPORT_CONTACTS = 1;
	private static final int EVENT_IMPORT_GROUPMEMBER = 2;
	private static final int EVENT_IMPORT_GROUP = 3;
	private static final int EVENT_IMPPRT_DIG_NORMAO_CONTACTS_AND_GROUP=4;
	private static final int EVENT_DO_FINISH = 5;
	private int mMode = PrivateMode.MODE_UNKNOWN;

	private ContentResolver mContentResolver;
	private GroupHandler mHandler;
	private Context mContext;
	private String controlSync = new String();
	private List<GroupInfo> groupinList;

	private ContactsHandler mContactsHander;
	private static final String READ_GROUP_MEMBER_KEY_PDT = "T1327";
	private static final String READ_GROUP_MEMBER_KEY_MPT = "T20104";
	private static final String READ_DIG_NORMAL_DATA_KEY="T31101";
	
	private static final String READ_CONTACTS_MPT_KEY="T21801";
	private static final String READ_CONTACTS_PDT_KEY="T1316";

	private static final String PREF_FILE_PDT = "default_group_pdt";
	private static final String PREF_FILE_MPT = "default_group_mpt";
	private static final String PREF_CHANGED_CONTACTS_FLAG = "default_changed_flag";
	private static final String PREF_FILE_NUMBER = "default_group_number";
	/**
	 * add the definication about zone default group 
	 * xuhong.tian
	 * 2015.3.18
	 */
	private static final String PDT_GROUP_ZONE_1 = "group_zone_default_group_1";
	private static final String PDT_GROUP_ZONE_2 = "group_zone_default_group_2";
	private static final String PDT_GROUP_ZONE_3 = "group_zone_default_group_3";
	private static final String PDT_GROUP_ZONE_4 = "group_zone_default_group_4";
	private static final String PDT_GROUP_ZONE_5 = "group_zone_default_group_5";
	private static final String PDT_GROUP_ZONE_6 = "group_zone_default_group_6";
	private static final String PDT_GROUP_ZONE_7 = "group_zone_default_group_7";
	private static final String PDT_GROUP_ZONE_8 = "group_zone_default_group_8";
	private static final String PDT_GROUP_ZONE_9 = "group_zone_default_group_9";
	private static final String PDT_GROUP_ZONE_10 = "group_zone_default_group_10";
	private static final String PDT_GROUP_ZONE_11 = "group_zone_default_group_11";
	private static final String PDT_GROUP_ZONE_12 = "group_zone_default_group_12";
	private static final String PDT_GROUP_ZONE_13 = "group_zone_default_group_13";
	private static final String PDT_GROUP_ZONE_14 = "group_zone_default_group_14";
	private static final String PDT_GROUP_ZONE_15 = "group_zone_default_group_15";
	private static final String PDT_GROUP_ZONE_16 = "group_zone_default_group_16";
	private static final String PDT_GROUP_ZONE_17 = "group_zone_default_group_17";
	private static final String PDT_GROUP_ZONE_18 = "group_zone_default_group_18";
	private static final String PDT_GROUP_ZONE_19 = "group_zone_default_group_19";
	private static final String PDT_GROUP_ZONE_20 = "group_zone_default_group_20";
	private static final String PDT_GROUP_ZONE_21 = "group_zone_default_group_21";

	private String pctoolsFlag;
	private boolean importGroupDone = false;
	private boolean importContactsDone = false;

	private int i;

	private SharedPreferences mSharePreference;
	private final static String[] COLUMNS = new String[] { Groups.ACCOUNT_NAME,
			Groups.ACCOUNT_TYPE, Groups.DATA_SET, Groups._ID, Groups.TITLE,
			Groups.SUMMARY_COUNT, Groups.GROUP_NUMBER,
			Groups.GROUP_CURRENT_MODE, Groups.GROUP_BY_IMPORT_TYPE, Groups._ID,
			Groups.GROUP_DEFAULT

	};

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onCreate() {
		Log.d(TAG, "onCreate ...");
		mContentResolver = getContentResolver();
		HandlerThread handlerThread = new HandlerThread(TAG);
		handlerThread.start();

		HandlerThread contactshandlerThread = new HandlerThread(
				"ContactsHandler");
		contactshandlerThread.start();

		mHandler = new GroupHandler(handlerThread.getLooper());
		mContactsHander = new ContactsHandler(contactshandlerThread.getLooper());
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand intent:" + intent + " flags:" + flags
				+ " startId:" + startId);
		int result = super.onStartCommand(intent, flags, startId);
		mContext = getBaseContext();

		if (intent != null) {
			pctoolsFlag = SystemProperties.get("pctool.update.flag");
			Log.d(TAG, "pctoolsFlag>>" + pctoolsFlag
					+ ">>pctoolsFlag.length()=" + pctoolsFlag.length());
			//importDigNormalContactsAndGroup();

			String action = intent.getAction();
			if ("com.neolink.modechange".equals(action)) {
				mMode = intent
						.getIntExtra(PrivateIntents.EXTRA_PRIVATE_MODE, 0);
				Log.d(TAG, "intent extra mode=" + mMode);
				if (PrivateMode.MODE_PDT_DIGITAL_NORMAL == mMode) {
					//deleteOldGroupInfo("");
					if (pctoolsFlag.charAt(9) == '1') {
					mContactsHander.sendEmptyMessage(EVENT_IMPPRT_DIG_NORMAO_CONTACTS_AND_GROUP);
					}
					mContentResolver.notifyChange(
							Groups.CONTENT_GROUP_FOR_PHONE, null, false);
					
					mHandler.sendEmptyMessage(EVENT_DO_FINISH);
				} else if (PrivateMode.MODE_MPT1327_ANALOG_TRUNKING == mMode) {
					mHandler.sendEmptyMessage(EVENT_IMPORT_GROUP);
					if (pctoolsFlag.charAt(6) == '1') {
						mContactsHander.sendEmptyMessage(EVENT_IMPORT_CONTACTS);
					}else{
						mContactsHander.sendEmptyMessage(EVENT_DO_FINISH);	
					}

				} else if (PrivateMode.MODE_MPT1327_ANALOG_NORMAL == mMode) {
					//deleteOldGroupInfo("");
					mContentResolver.notifyChange(
							Groups.CONTENT_GROUP_FOR_PHONE, null, false);
					mHandler.sendEmptyMessage(EVENT_DO_FINISH);

				} else if (PrivateMode.MODE_PDT_DIGITAL_TRUNKING == mMode) {

					mHandler.sendEmptyMessage(EVENT_IMPORT_GROUP);
					if (pctoolsFlag.charAt(6) == '1') {
						mContactsHander.sendEmptyMessage(EVENT_IMPORT_CONTACTS);
					}else{
						mHandler.sendEmptyMessage(EVENT_DO_FINISH);
					}
				} else {
					//deleteOldGroupInfo("");
					mHandler.sendEmptyMessage(EVENT_DO_FINISH);
				}

			} else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
//				if (!TextUtils.isEmpty(pctoolsFlag)) {
//
//					if (pctoolsFlag.charAt(6) == '1') {
//						mContactsHander.sendEmptyMessage(EVENT_IMPORT_CONTACTS);
//					} else {
//						mHandler.sendEmptyMessage(EVENT_DO_FINISH);
//					}
//				} else {
//					Log.d(TAG, "pctoolsFlag>> error");
//					mHandler.sendEmptyMessage(EVENT_DO_FINISH);
//				}

			} else if ("com.neolink.action.dynamic_group".equals(action)) {

				boolean dynamicFlag = intent.getBooleanExtra("group_changed",
						true);// true >add，false
								// > delete
				String group_name = (String) intent.getExtra("group_name", "");
				String group_number = (String) intent.getExtra("group_number",
						"");
				Log.d(TAG, "group_name==" + group_name + "group_number=="
						+ group_number + "dynamicFlag==" + dynamicFlag);
				if (dynamicFlag) {
					if (!TextUtils.isEmpty(group_number)) {

						deleteDynamicGroup("");
						addDynamicGroup(group_name, group_number);
						mContentResolver.notifyChange(
								Groups.CONTENT_GROUP_FOR_PHONE, null, false);
						mHandler.sendEmptyMessage(EVENT_DO_FINISH);
					}
				} else {
					deleteDynamicGroup("");
					mContentResolver.notifyChange(
							Groups.CONTENT_GROUP_FOR_PHONE, null, false);
					mHandler.sendEmptyMessage(EVENT_DO_FINISH);
				}
			}
			return result;
		} else {
			//mHandler.sendEmptyMessage(EVENT_IMPORT_GROUP);
		}
		return 0;
	}

	private class GroupHandler extends Handler {
		public GroupHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			Log.d(TAG, "welcomein GroupHandler handleMessage" + msg.what);
			switch (msg.what) {

			case EVENT_IMPORT_GROUP:
				removeMessages(EVENT_DO_FINISH);

				synchronized (controlSync) {
					importGroupContacts();
					// updateDefaultGroup("");
					mContentResolver.notifyChange(
							Groups.CONTENT_GROUP_FOR_PHONE, null, false);
					importGroupDone = true;
					Log.d(TAG, "updateimportGroupMember()======"+updateimportGroupMember());
					if (updateimportGroupMember() && pctoolsFlag.charAt(6) == '1' && mMode ==0) {
						mContactsHander
								.sendEmptyMessage(EVENT_IMPORT_GROUPMEMBER);
						importGroupDone = false;
					}else if(updateimportGroupMember() && groupinList.size() >0 && mMode ==0){
						mContactsHander
						.sendEmptyMessage(EVENT_IMPORT_GROUPMEMBER);
				         importGroupDone = false;
					}
				//	resetProperties();
					mContactsHander.sendEmptyMessage(EVENT_DO_FINISH);
				}
				break;
			case EVENT_DO_FINISH:
				Log.d(TAG, "do kill the ContactsImportService");
				ContactsImportService.this.stopSelf();
				break;
			}
		}
	}

	private class ContactsHandler extends Handler {
		public ContactsHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {

			Log.d(TAG, "welcome in mContactsHander" + msg.what);
			switch (msg.what) {
			case EVENT_IMPORT_CONTACTS:
				removeMessages(EVENT_DO_FINISH);
				synchronized (controlSync) {
					importPrivateContacts();
					importContactsDone = true;
					Log.d(TAG, "updateimportGroupMember()======"+updateimportGroupMember());
					if (updateimportGroupMember() && pctoolsFlag.charAt(6) == '1' && mMode ==0) {
						mContactsHander
								.sendEmptyMessage(EVENT_IMPORT_GROUPMEMBER);
					}else if(updateimportGroupMember() && groupinList.size() >0 && mMode ==0){
						mContactsHander
						.sendEmptyMessage(EVENT_IMPORT_GROUPMEMBER);
					}
					//resetProperties();
					mContactsHander.sendEmptyMessage(EVENT_DO_FINISH);
				}
				break;
			case EVENT_IMPORT_GROUPMEMBER: {
				removeMessages(EVENT_DO_FINISH);
				importPrivateGroupMember();
			}
				break;
			case EVENT_IMPPRT_DIG_NORMAO_CONTACTS_AND_GROUP: {
				removeMessages(EVENT_DO_FINISH);
				importDigNormalContactsAndGroup();
			}
				break;
			}

		}
	}

	private void importPrivateGroupMember() {
		// TODO Auto-generated method stub

		Log.d(TAG, "importPrivateGroupMember..........start");
		ConfigurationManager.Item contactsMemberItem = null;
		ContentValues values = new ContentValues();
		ConfigurationManager manager = new ConfigurationManager();
		try {

			if (ContactsApplication.getApplication().mMode == PrivateMode.MODE_MPT1327_ANALOG_TRUNKING) {
				contactsMemberItem = manager
						.getItemInfo(READ_GROUP_MEMBER_KEY_MPT);
			} else if (ContactsApplication.getApplication().mMode == PrivateMode.MODE_PDT_DIGITAL_TRUNKING) {
				contactsMemberItem = manager
						.getItemInfo(READ_GROUP_MEMBER_KEY_PDT);
			} else {
				Log.d(TAG, "get current mode error or do not import");
			}
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		ArrayList<String> groupNumberAndNumberinfo = new ArrayList<String>();
		if (contactsMemberItem != null) {
			List<ValueList> groupMemberItemList = contactsMemberItem.valueList;

			if (groupMemberItemList != null && groupMemberItemList.size() > 0) {

				for (ValueList valueList : groupMemberItemList) { // group_number,number
																	// ,
					Log.d(TAG, "Pctools group Body is :" + valueList.value
							+ "id==" + valueList.id);
					String groupMemberinfoString = valueList.value;
					if (!TextUtils.isEmpty(groupMemberinfoString)) {
						groupNumberAndNumberinfo.add(groupMemberinfoString);
					} else {
						continue;
					}
				}

			} else {
				return;
			}

		} else {
			return;
		}

		if (!groupNumberAndNumberinfo.isEmpty()) {
			for (int i = 0; i < groupNumberAndNumberinfo.size(); i++) {
				String groupNumberAndNumberinfoString = groupNumberAndNumberinfo
						.get(i);
				String[] groupNumberAndNumberinfoSplit = groupNumberAndNumberinfoString
						.split(",");
				ArrayList<ContentProviderOperation> rawContactOperations = new ArrayList<ContentProviderOperation>();
				int count = 0;
				for (int j = 1; j < groupNumberAndNumberinfoSplit.length; j++) {

					int group_id = -1;
					if (!TextUtils.isEmpty(groupNumberAndNumberinfoSplit[0])) {
						group_id = getGroupId(groupNumberAndNumberinfoSplit[0],mMode);
						Log.d(TAG, "group_id==" + group_id);
					} else {
						break;
					}
					if (!TextUtils.isEmpty(groupNumberAndNumberinfoSplit[j])) {
						Log.d(TAG, "Cotnactsnumber="
								+ groupNumberAndNumberinfoSplit[j]);
						int rawcontacts_id = getContactId(groupNumberAndNumberinfoSplit[j]);
						Log.d(TAG, "rawContactId==" + rawcontacts_id);

						// Build an insert operation to add the contact to
						// the group
						final ContentProviderOperation.Builder insertBuilder = ContentProviderOperation
								.newInsert(android.provider.ContactsContract.Data.CONTENT_URI);
						insertBuilder.withValue(Data.RAW_CONTACT_ID,
								rawcontacts_id);
						insertBuilder.withValue(Data.MIMETYPE,
								GroupMembership.CONTENT_ITEM_TYPE);
						insertBuilder.withValue(GroupMembership.GROUP_ROW_ID,
								group_id);
						insertBuilder.withYieldAllowed(true);
						rawContactOperations.add(insertBuilder.build());
					} else {
						continue;
					}
					try {
						mContentResolver.applyBatch(ContactsContract.AUTHORITY,
								rawContactOperations);
					} catch (Exception e) {
						Log.e(TAG, Log.getStackTraceString(e));
					}
					rawContactOperations.clear();
				}

			}

		} else {
			return;
		}
		importGroupDone = false;
		importContactsDone = false;
		//resetProperties();
		mHandler.sendEmptyMessage(EVENT_DO_FINISH);
		Log.d(TAG, "importPrivateGroupMember............done...end");
	}

	private int importGroupContacts() {

		Log.d(TAG, "Contacts GROUP import done............start");
/*		pctoolsFlag = SystemProperties.get("pctool.update.flag");
		int modeType = -1;
		if (pctoolsFlag.charAt(0) == '1') {
			modeType = PrivateMode.MODE_MPT1327_ANALOG_TRUNKING;
		} else if (pctoolsFlag.charAt(0) == '0') {
			modeType = PrivateMode.MODE_PDT_DIGITAL_TRUNKING;	
		}*/
		try {
			Context c = mContext.createPackageContext(
					"com.android.providers.contacts",
					Context.CONTEXT_IGNORE_SECURITY);
			mSharePreference = c.getSharedPreferences(PREF_FILE_PDT, Context.MODE_WORLD_READABLE
					| Context.MODE_MULTI_PROCESS);

		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final IPrivateTelephony privateTelephony = IPrivateTelephony.Stub
				.asInterface(ServiceManager.checkService("privatephone"));
		try {

			groupinList = privateTelephony.getAllGroups();
			Log.d(TAG, "list.size()=="+groupinList.size());
			if(groupinList.size() >0){
				deleteOldGroupInfo("");
			}
			String name = "";
			String num = "";
			int zoneId= 0;
			int responseFlag= 0;
			int groupId =0;
			String group_type = "";
			int type = -1;
			boolean isHaveDefault = true;
			ArrayList<String> defaultZoneGroup = new ArrayList<String>();
			if (groupinList != null) {
				ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
				for (GroupInfo groupInfo : groupinList) {
					
					name = rmsemicolon(groupInfo.groupName);
					num = rmsemicolon(groupInfo.groupNumber);
					zoneId= groupInfo.groupZoneId;
					groupId= groupInfo.groupId;
					responseFlag = groupInfo.responseGroup;
					//zoneId=rmsemicolon(String.valueOf(groupInfo.groupZoneId));
					//responseFlag=rmsemicolon(String.valueOf(groupInfo.responseGroup));
					group_type = rmsemicolon(String.valueOf(groupInfo.groupType));
/*					
					Log.d(TAG, "getprivateManager groupinList===name==" + num
							+ "zoneId=" + groupInfo.groupZoneId
							+ "responseFlag=" + groupInfo.responseGroup
							+ "groupID=" + groupId);
*/
					ContentProviderOperation.Builder builder = ContentProviderOperation
							.newInsert(Groups.CONTENT_URI);
					builder.withValue(Groups.ACCOUNT_TYPE,
							"sprd.com.android.account.phone");
					builder.withValue(
							Groups.ACCOUNT_NAME,
							getApplicationContext().getString(
									R.string.digital_mode_contact));
//					builder.withValue(Groups.DATA_SET, " ");
					if (Integer.valueOf(group_type) == 2) { // dynamic group type
						builder.withValue(Groups.TITLE, getString(R.string.dynamic_group_name));
					} else {
						builder.withValue(Groups.TITLE, name);
					}
					builder.withValue(Groups.GROUP_NUMBER, num);
					builder.withValue(Groups.GROUP_TYPE,
							Integer.valueOf(group_type));
					builder.withValue(Groups.GROUP_DEFAULT, 0);
					builder.withValue(Groups.GROUP_BY_IMPORT_TYPE,
							Groups.GROUP_IMPORT_TYPE);
					builder.withValue(Groups.GROUP_ZONE_ID,
							zoneId);
					builder.withValue(Groups.GROUP_ID,
							groupId);
					builder.withValue(Groups.GROUP_RESPONSE_FLAG,
							responseFlag);
					builder.withValue(Groups.GROUP_BY_IMPORT_TYPE,
							Groups.GROUP_IMPORT_TYPE);
					builder.withValue(Groups.GROUP_CURRENT_MODE,
							Settings.Secure.getInt(ContactsApplication
									.getApplication().getContentResolver(),
									Settings.Secure.PRIVATE_PHONE_MODE,
									PrivateMode.MODE_UNKNOWN));
					String saveDeaultid = "";
/*					if (ContactsApplication.getApplication().mMode == PrivateMode.MODE_PDT_DIGITAL_TRUNKING) { // PDT
						String saveDeaultid_pdt = getPreferredDefaultGroupNumber(groupInfo.groupZoneId);
						Log.d(TAG, "saveDeaultid_pdt=="+saveDeaultid_pdt);
						if (saveDeaultid_pdt.equals(num)) {
							Log.d(TAG, "the default zone group had setted");
							builder.withValue(Groups.GROUP_DEFAULT,
									Groups.GROUP_DEFAULT_FLAG);
						}
					}*/ 
					//else if (ContactsApplication.getApplication().mMode == PrivateMode.MODE_MPT1327_ANALOG_TRUNKING) {// mpt
						//String saveDeaultid_mpt = getPreferredDefaultGroupNumber(PREF_FILE_MPT);
//						if (saveDeaultid_mpt.equals(num)) {
//							builder.withValue(Groups.GROUP_DEFAULT,
//									Groups.GROUP_DEFAULT_FLAG);
//							isHaveDefault = false;
//							updateDefaultGroup(num, zoneId);
//						}
//					}
					operationList.add(builder.build());
				}
				try {

					mContentResolver.applyBatch(ContactsContract.AUTHORITY,
							operationList);
					Log.d(TAG, "mContentResolver done....");
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.d(TAG, "importGroupContacts RemoteException");
				} catch (OperationApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

					Log.d(TAG,
							"importGroupContacts OperationApplicationException");
				}		
//				if (isHaveDefault) {
//					if (groupinList != null && groupinList.get(0) != null) {
//						updateDefaultGroup("1",1);
//					}
//				}

			} else {
				Log.d(TAG, "importGroupContacts group list is null.");
			}

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Log.d(TAG, "getprivateManager groupinList11>>");
		}
		// mHandler.sendEmptyMessage(EVENT_DO_FINISH);
		Log.d(TAG, "Contacts GROUP import done..........endddd..");
		return 0;
	}

	private void importPrivateContacts() {
		Log.d(TAG, "..........................importPrivateContacts..");
			/**
			 * new read contacts method start 
			 */
			ConfigurationManager.Item contactsItem = null;
			ContentValues values = new ContentValues();
			ArrayList<String> pctoolsName = new ArrayList<String>();
			ArrayList<String> pctoolsNumber = new ArrayList<String>();
			ConfigurationManager manager = new ConfigurationManager();
			try {

				if (ContactsApplication.getApplication().mMode == PrivateMode.MODE_MPT1327_ANALOG_TRUNKING) {
					contactsItem = manager
							.getItemInfo(READ_CONTACTS_MPT_KEY);
				} else if (ContactsApplication.getApplication().mMode == PrivateMode.MODE_PDT_DIGITAL_TRUNKING) {
					contactsItem = manager
							.getItemInfo(READ_CONTACTS_PDT_KEY);
				} else {
					Log.d(TAG, "get current mode error or do not import");
				}
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
			if (contactsItem != null) {
				List<ValueList> contactsItemList = contactsItem.valueList;

				if (contactsItemList != null && contactsItemList.size() > 0) {

					Log.d(TAG,
							"contactsItemList.size()=="
									+ contactsItemList.size());
					/**
					 * when the new data is not empty and start delete the old data 
					 */
					deleteOldImportContacts(mMode);
					for (ValueList valueList : contactsItemList) { // group_number,number
																	// ,
						Log.d(TAG, "contacts Body is :" + valueList.value
								+ "id==" + valueList.id);
						String contactsIteminfoString = valueList.value;
						if (!TextUtils.isEmpty(contactsIteminfoString)) {

							String[] contactsIteminfoStringSplit = contactsIteminfoString
									.split(",");
							if (contactsIteminfoStringSplit[2] != null) {
								pctoolsName
										.add(rmsemicolon(contactsIteminfoStringSplit[2]));
							}
							if (contactsIteminfoStringSplit[3] != null) {
								pctoolsNumber
										.add(rmsemicolon(contactsIteminfoStringSplit[3]));
							}
						} else {
							continue;
						}
					}

				} else {
					return;
				}

			}
			for (int i = 0; i < pctoolsName.size(); i++) {

				if (!pctoolsName.isEmpty() && !pctoolsNumber.isEmpty()) {
					ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
					int rawContactInsertIndex = 0;
					int size = ops.size();
					ops.add(ContentProviderOperation
							.newInsert(RawContacts.CONTENT_URI)
							.withValue(RawContacts.ACCOUNT_TYPE,
									"neolink.com.android.account.phone")
							.withValue(
									RawContacts.ACCOUNT_NAME,
									R.string.show_privatecontacts)
							.withValue(RawContacts.RAW_CONTACT_MODE_TYPE,
									Settings.Secure.getInt(ContactsApplication
											.getApplication().getContentResolver(),
											Settings.Secure.PRIVATE_PHONE_MODE,
											PrivateMode.MODE_UNKNOWN))
							.withValue(RawContacts.RAW_CONTACT_IMPORT_TYPE,
									RawContacts.CONTACTS_BY_PCTOOLS).build());
					ops.add(ContentProviderOperation
							.newInsert(
									android.provider.ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID,
									rawContactInsertIndex)
							.withValue(Data.MIMETYPE,
									StructuredName.CONTENT_ITEM_TYPE)
							.withValue(StructuredName.GIVEN_NAME,
									pctoolsName.get(i)).build());
					ops.add(ContentProviderOperation
							.newInsert(
									android.provider.ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID,
									rawContactInsertIndex)
							.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
							.withValue(Phone.NUMBER, pctoolsNumber.get(i))
							.withValue(Phone.TYPE, Phone.TYPE_MOBILE)
							.withValue(Phone.LABEL, " ").build());

					ops.add(ContentProviderOperation
							.newInsert(
									android.provider.ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID,
									rawContactInsertIndex)
							.withValue(
									ContactsContract.CommonDataKinds.Photo.PHOTO,
									null)
							.withValue(
									ContactsContract.Data.MIMETYPE,
									ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
							.build());
					try {

						getApplicationContext().getContentResolver()
								.applyBatch(ContactsContract.AUTHORITY, ops);
					} catch (RemoteException e) {
						Log.e(TAG,
								String.format("%s: %s", e.toString(),
										e.getMessage()));
					} catch (OperationApplicationException e) {
						Log.e(TAG,
								String.format("%s: %s", e.toString(),
										e.getMessage()));
					}
				}
			}
		Log.d(TAG, "....end .........importPrivateContacts..");
	}
	
	private int importDigNormalContactsAndGroup() {

		Log.e(TAG, "importDigNormalContactsAndGroup");
		ArrayList<String> DigNormalDatainfo = new ArrayList<String>();
		try {
			ConfigurationManager manager = new ConfigurationManager();
			ConfigurationManager.Item DigNormalItem = null;

			try {

				DigNormalItem = manager.getItemInfo(READ_DIG_NORMAL_DATA_KEY);
				if (DigNormalItem != null) {
					List<ValueList> DigNormalItemList = DigNormalItem.valueList;
					if (DigNormalItemList != null
							&& DigNormalItemList.size() > 0) {
						for (ValueList valueList : DigNormalItemList) {

							Log.d(TAG, " valueList.value==" + valueList.value);

							String digNormalinfoString = valueList.value;
							if (!TextUtils.isEmpty(digNormalinfoString)) {
								DigNormalDatainfo.add(digNormalinfoString);
								Log.d(TAG, " valueList.value==in..");
							} else {
								continue;
							}
						}

					} else {
						Log.d(TAG,
								"DigNormalItem is null or  DigNormalItemList.size() is 0");
						return -1;
					}
				} else {
					Log.d(TAG, "DigNormalItem is null");
					return -1;
				}
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.d(TAG, "import pctool data done" + DigNormalDatainfo.isEmpty()
				+ DigNormalDatainfo.size());
		if (!DigNormalDatainfo.isEmpty()) {
			
			deleteOldNormalDataInfo(Settings.Secure.getInt(ContactsApplication
					.getApplication().getContentResolver(),
					Settings.Secure.PRIVATE_PHONE_MODE,
					PrivateMode.MODE_UNKNOWN));
			ArrayList<ContentProviderOperation> groupOperationList = new ArrayList<ContentProviderOperation>();
			ArrayList<ContentProviderOperation> contactsOperationList = new ArrayList<ContentProviderOperation>();
			for (int i = 0; i < DigNormalDatainfo.size(); i++) {
				String contactsAndGroupinfoString = DigNormalDatainfo.get(i);
/*				Log.d(TAG, "contactsAndGroupinfoString=="
						+ contactsAndGroupinfoString);*/
				String[] contactsAndGroupinfoStringSplit = contactsAndGroupinfoString
						.split(",");
				if (contactsAndGroupinfoStringSplit[2] != null) {
/*					Log.d(TAG, "contactsAndGroupinfoStringSplit[1]=="
							+ contactsAndGroupinfoStringSplit[2]);*/
					//if ("1".equals(contactsAndGroupinfoStringSplit[1])) {// 1:
																			// group
																			// number

						ContentProviderOperation.Builder builder = ContentProviderOperation
								.newInsert(Groups.CONTENT_URI);
						builder.withValue(Groups.ACCOUNT_TYPE,
								"neolink.com.android.account.phone");
						if("0".equals(contactsAndGroupinfoStringSplit[1])){
							builder.withValue(
									Groups.ACCOUNT_NAME,
									getApplicationContext().getString(
											R.string.normal_mode_contact));
							builder.withValue(Groups.ACCOUNT_TYPE,
									"neolink.com.android.account.phone");
						}else{
							builder.withValue(
									Groups.ACCOUNT_NAME,
									getApplicationContext().getString(
											R.string.normal_mode_group));
							builder.withValue(Groups.ACCOUNT_TYPE,
									"neolink.com.android.account.phone");
						}

/*						Log.d(TAG, "contactsAndGroupinfoStringSplit[1])=="
								+ "=" + contactsAndGroupinfoStringSplit[1]);*/
						// builder.withValue(Groups.DATA_SET, " ");
						
					if (contactsAndGroupinfoStringSplit[2] != null) {
						builder.withValue(Groups.TITLE,
								contactsAndGroupinfoStringSplit[2].substring(
										contactsAndGroupinfoStringSplit[2]
												.indexOf("\"") + 1,
										contactsAndGroupinfoStringSplit[2]
												.lastIndexOf("\"")));
					}
						if (contactsAndGroupinfoStringSplit[3] != null) {
							builder.withValue(Groups.GROUP_NUMBER,
									contactsAndGroupinfoStringSplit[3].substring(
											contactsAndGroupinfoStringSplit[2]
													.indexOf("\"") + 1,
											contactsAndGroupinfoStringSplit[3]
													.lastIndexOf("\"")));
						}
//						if("1".equals(contactsAndGroupinfoStringSplit[1])){
//							builder.withValue(Groups.GROUP_TYPE, 1);
//						}else{
//							
//						}
						builder.withValue(Groups.GROUP_TYPE, 0);
						// builder.withValue(Groups.GROUP_DEFAULT,
						// Groups.GROUP_DEFAULT_FLAG);
						builder.withValue(Groups.GROUP_DEFAULT, 0);
						builder.withValue(Groups.GROUP_BY_IMPORT_TYPE,
								Groups.GROUP_IMPORT_TYPE);
						builder.withValue(Groups.GROUP_CURRENT_MODE,
								Settings.Secure.getInt(ContactsApplication
										.getApplication().getContentResolver(),
										Settings.Secure.PRIVATE_PHONE_MODE,
										PrivateMode.MODE_UNKNOWN));
						/*
						 * builder.withValue(Groups.GROUP_LOCATION,
						 * getCurrentLocatin());
						 */
						groupOperationList.add(builder.build());
					//} /**else if ("0".equals(contactsAndGroupinfoStringSplit[1])) {// 0
																				// :
																				// private
																				// numer
					/*	if (contactsAndGroupinfoStringSplit[2].isEmpty()) {
							contactsAndGroupinfoStringSplit[2] = mContext
									.getString(R.string.name_unknow);
						}
						if (contactsAndGroupinfoStringSplit[3].isEmpty()) {
							contactsAndGroupinfoStringSplit[3] = mContext
									.getString(R.string.number_unknow);
						}
						Log.d(TAG, "contactsAndGroupinfoStringSplit[1])=="
								+ "=" + contactsAndGroupinfoStringSplit[1]);
						int rawContactInsertIndex = 0;
						int size = contactsOperationList.size();
						contactsOperationList
								.add(ContentProviderOperation
										.newInsert(RawContacts.CONTENT_URI)
										.withValue(RawContacts.ACCOUNT_TYPE,
												"neolink.com.android.account.phone")
										.withValue(RawContacts.ACCOUNT_NAME,
												R.string.show_privatecontacts)
										.withValue(
												RawContacts.RAW_CONTACT_MODE_TYPE,
												ContactsApplication
														.getApplication().mMode)
										.withValue(
												RawContacts.RAW_CONTACT_IMPORT_TYPE,
												RawContacts.CONTACTS_BY_PCTOOLS)
										.build());
						contactsOperationList
								.add(ContentProviderOperation
										.newInsert(
												android.provider.ContactsContract.Data.CONTENT_URI)
										.withValueBackReference(
												Data.RAW_CONTACT_ID,
												rawContactInsertIndex)
										.withValue(
												Data.MIMETYPE,
												StructuredName.CONTENT_ITEM_TYPE)
										.withValue(
												StructuredName.GIVEN_NAME,
												contactsAndGroupinfoStringSplit[2])
										.build());
						contactsOperationList
								.add(ContentProviderOperation
										.newInsert(
												android.provider.ContactsContract.Data.CONTENT_URI)
										.withValueBackReference(
												Data.RAW_CONTACT_ID,
												rawContactInsertIndex)
										.withValue(Data.MIMETYPE,
												Phone.CONTENT_ITEM_TYPE)
										.withValue(
												Phone.NUMBER,
												contactsAndGroupinfoStringSplit[3])
										.withValue(Phone.TYPE,
												Phone.TYPE_MOBILE)
										.withValue(Phone.LABEL, " ").build());

						contactsOperationList
								.add(ContentProviderOperation
										.newInsert(
												android.provider.ContactsContract.Data.CONTENT_URI)
										.withValueBackReference(
												Data.RAW_CONTACT_ID,
												rawContactInsertIndex)
										.withValue(
												ContactsContract.CommonDataKinds.Photo.PHOTO,
												null)
										.withValue(
												ContactsContract.Data.MIMETYPE,
												ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
										.build());
					} else {
						Log.d(TAG, "make sure the type is correct");
					}**/
				}else{
					Log.d(TAG, "data type is error");
					continue;
				}
			}
			if (!groupOperationList.isEmpty()) {

				try {

					ContentProviderResult[] result = mContentResolver
							.applyBatch(ContactsContract.AUTHORITY,
									groupOperationList);
					Log.d(TAG, "addDigNormalGroup done....");

				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.d(TAG, "addDigNormalGroup RemoteException");
					mHandler.sendEmptyMessage(EVENT_DO_FINISH);
				} catch (OperationApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					mHandler.sendEmptyMessage(EVENT_DO_FINISH);
					Log.d(TAG,
							"addDigNormalGroup OperationApplicationException");
				}
			}
			if (!contactsOperationList.isEmpty()) {
				try {
					getApplicationContext().getContentResolver().applyBatch(
							ContactsContract.AUTHORITY, contactsOperationList);
				} catch (RemoteException e) {
					Log.e(TAG,
							String.format("%s: %s", e.toString(),
									e.getMessage()));
					mHandler.sendEmptyMessage(EVENT_DO_FINISH);
				} catch (OperationApplicationException e) {
					Log.e(TAG,
							String.format("%s: %s", e.toString(),
									e.getMessage()));
					mHandler.sendEmptyMessage(EVENT_DO_FINISH);
				}
			}

		} else {
			Log.d(TAG, "DigNormalDatainfo is empty ");
			mHandler.sendEmptyMessage(EVENT_DO_FINISH);
			return -1;
		}
		mHandler.sendEmptyMessage(EVENT_DO_FINISH);
		Log.d(TAG, "DigNormalDatainfo............done...end");
		return 0;
	}
	
	
	private int addDynamicGroup(String group_name, String group_number){
		
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
		ContentProviderOperation.Builder builder = ContentProviderOperation
				.newInsert(Groups.CONTENT_URI);
		builder.withValue(Groups.ACCOUNT_TYPE, "neolink.com.android.account.phone");
		builder.withValue(Groups.ACCOUNT_NAME, getApplicationContext()
				.getString(R.string.label_phone));
		// builder.withValue(Groups.DATA_SET, " ");
		builder.withValue(Groups.TITLE, group_name);
		builder.withValue(Groups.GROUP_NUMBER, group_number);
		builder.withValue(Groups.GROUP_TYPE, 2);
		// builder.withValue(Groups.GROUP_DEFAULT, Groups.GROUP_DEFAULT_FLAG);
		builder.withValue(Groups.GROUP_DEFAULT, 0);
		builder.withValue(Groups.GROUP_BY_IMPORT_TYPE, Groups.GROUP_IMPORT_TYPE);
		builder.withValue(Groups.GROUP_CURRENT_MODE, Settings.Secure.getInt(
				ContactsApplication.getApplication().getContentResolver(),
				Settings.Secure.PRIVATE_PHONE_MODE, PrivateMode.MODE_UNKNOWN));
		/*
		 * builder.withValue(Groups.GROUP_LOCATION, getCurrentLocatin());
		 */
		operationList.add(builder.build());
		try {

			ContentProviderResult[] result = mContentResolver.applyBatch(
					ContactsContract.AUTHORITY, operationList);
			Log.d(TAG, "addDynamicGroup done....");

			return 0;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d(TAG, "addDynamicGroup RemoteException");
		} catch (OperationApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			Log.d(TAG, "addDynamicGroup OperationApplicationException");
		}
		return -1;
	}

	public int deleteOldImportContacts(int modeType) {

		final String[] CONTACT_PROJECTION_ALTERNATIVE = new String[] {
				Contacts._ID, // 0
				Contacts.NAME_RAW_CONTACT_ID, // 0
				Contacts.CONTACT_IMPORT_TYPE, Contacts.CONTACAT_MODE_TYPE,

		};

		Uri uri = Contacts.CONTENT_URI;
		Cursor curcor = getApplicationContext().getContentResolver().query(uri,
				CONTACT_PROJECTION_ALTERNATIVE, null, null, null);
		while (curcor.moveToNext()) {

			int import_type = curcor.getInt(curcor
					.getColumnIndex(Contacts.CONTACT_IMPORT_TYPE));

			int mode_type = curcor.getInt(curcor
					.getColumnIndex(Contacts.CONTACAT_MODE_TYPE));

			int mode_id = curcor.getInt(curcor
					.getColumnIndex(Contacts.NAME_RAW_CONTACT_ID));
			Log.d(TAG, "import_type" + import_type + "modeType==" + modeType
					+ "mode_id=" + mode_id + "mode_type=" + mode_type);
			if (import_type == RawContacts.CONTACTS_BY_PCTOOLS
					&& mode_type == modeType) {
				int deleteCurcor = getApplicationContext().getContentResolver()
						.delete(ContactsContract.RawContacts.CONTENT_URI,
								Contacts._ID + "= " + String.valueOf(mode_id),
								null);
			}
		}
		curcor.close();
		return Log.w(TAG, "**delete end**");
	}
	
	private void deleteDynamicGroup(String group_number) {

		String selecion = "";
		selecion = Groups.GROUP_TYPE + " = " + "2";

		getContentResolver().delete(
				Groups.CONTENT_URI
						.buildUpon()
						.appendQueryParameter(
								ContactsContract.CALLER_IS_SYNCADAPTER, "true")
						.build(), selecion, null);

	}

	private void deleteOldGroupInfo(String group_number) {

		String selecion = Groups.GROUP_CURRENT_MODE + " = "
				+ String.valueOf(Settings.Secure
						.getInt(ContactsApplication.getApplication()
								.getContentResolver(),
								Settings.Secure.PRIVATE_PHONE_MODE,
								PrivateMode.MODE_UNKNOWN));

		getContentResolver().delete(
				Groups.CONTENT_URI
						.buildUpon()
						.appendQueryParameter(
								ContactsContract.CALLER_IS_SYNCADAPTER, "true")
						.build(), selecion, null);
		
		Log.d(TAG, "deleteOldGroupInfo done");
	}
	
	/**
	 * delete the old normal data 2015.4.8 
	 * @param mode
	 */
	private void deleteOldNormalDataInfo(int mode) {

		String selecion = Groups.GROUP_CURRENT_MODE + " = "
				+ String.valueOf(mode);

		getContentResolver().delete(
				Groups.CONTENT_URI
						.buildUpon()
						.appendQueryParameter(
								ContactsContract.CALLER_IS_SYNCADAPTER, "true")
						.build(), selecion, null);
		
		Log.d(TAG, "deleteOldNormalDataInfo done");
	}
	
	
	

	public int updateDefaultGroup(String defaultGroupNumber, int zoneId) {

		Log.d(TAG, "..........................updateDefaultGroup start.."
				+ "defaultGroupNumber==" + defaultGroupNumber+"zoneId="+zoneId);
		ContentValues updateDefaultGroupValue = new ContentValues();
		updateDefaultGroupValue.put(Groups.GROUP_DEFAULT,
				Groups.GROUP_DEFAULT_FLAG);
		updateDefaultGroupValue.put(Groups.GROUP_ZONE_ID,
				zoneId);
		updateDefaultGroupValue.put(Groups.GROUP_CURRENT_MODE, Settings.Secure
				.getInt(ContactsApplication.getApplication()
						.getContentResolver(),
						Settings.Secure.PRIVATE_PHONE_MODE,
						PrivateMode.MODE_UNKNOWN));
		updateDefaultGroupValue.put(Groups.GROUP_NUMBER, defaultGroupNumber);
		String[] args = { defaultGroupNumber };

		mContext.getContentResolver().update(Groups.CONTENT_URI,
				updateDefaultGroupValue, Groups.GROUP_NUMBER + "=?", args);
		Log.d(TAG, "..........................updateDefaultGroup end..");
		return 0;
	}
	
	public int getContactId(String number) {
		Cursor rawcontacatsCurcor = null;
		try {
			rawcontacatsCurcor = getContentResolver().query(Phone.CONTENT_URI,
					new String[] { Phone.CONTACT_ID, Phone.NUMBER }, null,
					null, null);
			if (rawcontacatsCurcor != null && rawcontacatsCurcor.moveToFirst()) {
				while (!rawcontacatsCurcor.isAfterLast()) {
					if (PhoneNumberUtils.compare(number,
							rawcontacatsCurcor.getString(1))) {
						return rawcontacatsCurcor.getInt(0);
					}
					rawcontacatsCurcor.moveToNext();
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "getContactId error:", e);
			e.printStackTrace();
		} finally {
			if (rawcontacatsCurcor != null) {
				rawcontacatsCurcor.close();
			}
		}
		return -1;
	}

	public int getGroupId(String number,int mode) {

		Uri GROUP_LIST_URI = Groups.CONTENT_SUMMARY_URI;
		final String[] COLUMNS = new String[] { Groups._ID,
				Groups.GROUP_NUMBER, Groups.GROUP_CURRENT_MODE };
		int group_id = -1;
		Cursor groupcurcor = getContentResolver().query(GROUP_LIST_URI,
				COLUMNS, Groups.GROUP_NUMBER + " =? "+" AND "+Groups.GROUP_CURRENT_MODE + " =? ", new String[] { number, String.valueOf(mode)},
				null);
		try {
			while (groupcurcor.moveToNext()) {
				group_id = groupcurcor.getInt(0);
				return group_id;
			}

		} catch (Exception e) {
			Log.e(TAG, "getGroupId error:", e);
			e.printStackTrace();
		} finally {
			if (groupcurcor != null) {
				groupcurcor.close();
			}
		}
		return -1;
	}

	public String rmsemicolon(String htmlStr) {
		String result = "";
		boolean flag = true;
		if (htmlStr == null) {
			return null;
		}
		htmlStr = htmlStr.replace("\"", "");
		char[] a = htmlStr.toCharArray();
		int length = a.length;
		for (int i = 0; i < length; i++) {
			if (a[i] == '<') {
				flag = false;
				continue;
			}
			if (a[i] == '>') {
				flag = true;
				continue;
			}
			if (flag == true) {
				result += a[i];
			}
		}
		return result.toString();
	}

	private void resetProperties() {
		
		char[] ch=pctoolsFlag.toCharArray();	
		ch[6]='0';
		Log.d(TAG, "ch.toString()"+String.valueOf(ch));
		SystemProperties.set("pctool.update.flag", String.valueOf(ch));
	}

	/*
	 * String getCurrentLocatin() {
	 * 
	 * int radomNUmber = (int) (Math.random() * 6 + 1);
	 * 
	 * String[] location = new String[] {
	 * mContext.getString(R.string.location_1),
	 * mContext.getString(R.string.location_2),
	 * mContext.getString(R.string.location_3),
	 * mContext.getString(R.string.location_4),
	 * mContext.getString(R.string.location_5,
	 * mContext.getString(R.string.location_6)) };
	 * 
	 * return location[3]; }
	 */

	boolean updateimportGroupMember() {
		Log.d(TAG, "importGroupDone=="+importGroupDone+"importContactsDone=="+importContactsDone+"pctoolsFlag.charAt(6) == '0'="+pctoolsFlag.charAt(6));
		if (importGroupDone && importContactsDone) { // start
			return true;
		} else if (importGroupDone && pctoolsFlag.charAt(6) == '0') { // mode changed
			return true;
		} else {
			return false;
		}

	}

	private String getPreferredDefaultGroupNumber(int ZoneId) {

		Log.d(TAG, "ZoneId=="+ZoneId);
		switch (ZoneId) {
		case 0:
			return mSharePreference.getString(PDT_GROUP_ZONE_1, "0");
		case 1:
			return mSharePreference.getString(PDT_GROUP_ZONE_2, "0");
		case 2:
			return mSharePreference.getString(PDT_GROUP_ZONE_3, "0");
		case 3:
			return mSharePreference.getString(PDT_GROUP_ZONE_4, "0");
		case 4:
			return mSharePreference.getString(PDT_GROUP_ZONE_5, "0");
		case 5:
			return mSharePreference.getString(PDT_GROUP_ZONE_6, "0");
		case 6:
			return mSharePreference.getString(PDT_GROUP_ZONE_7, "0");
		case 7:
			return mSharePreference.getString(PDT_GROUP_ZONE_8, "0");
		case 8:
			return mSharePreference.getString(PDT_GROUP_ZONE_9, "0");
		case 9:
			return mSharePreference.getString(PDT_GROUP_ZONE_10, "0");
		case 10:
			return mSharePreference.getString(PDT_GROUP_ZONE_11, "0");
		case 11:
			return mSharePreference.getString(PDT_GROUP_ZONE_12, "0");
		case 12:
			return mSharePreference.getString(PDT_GROUP_ZONE_13, "0");
		case 13:
			return mSharePreference.getString(PDT_GROUP_ZONE_14, "0");
		case 14:
			return mSharePreference.getString(PDT_GROUP_ZONE_15, "0");
		case 15:
			return mSharePreference.getString(PDT_GROUP_ZONE_16, "0");
		case 16:
			return mSharePreference.getString(PDT_GROUP_ZONE_17, "0");
		case 17:
			return mSharePreference.getString(PDT_GROUP_ZONE_18, "0");
		case 18:
			return mSharePreference.getString(PDT_GROUP_ZONE_19, "0");
		case 19:
			return mSharePreference.getString(PDT_GROUP_ZONE_20, "0");
		case 20:
			return mSharePreference.getString(PDT_GROUP_ZONE_21, "0");
		}
		return mSharePreference.getString(PDT_GROUP_ZONE_1, "0");
	}
	
	/**
	 * record the data whether the contacts was read xuhong.tian on 2015.3.25
	 * @param defaultFlag
	 */
    private void setPreferredChangedContactsFlag(int defaultFlag) {
    	Log.d(TAG, "updateFlag=="+defaultFlag);
        SharedPreferences sp = mContext.getSharedPreferences(PREF_FILE_PDT, Context.MODE_WORLD_READABLE|Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_CHANGED_CONTACTS_FLAG, defaultFlag);
        editor.apply();
    }
	
}