package com.android.contacts;

import com.android.contacts.ContactsApplication;
import com.android.contacts.R;

import neolink.telephony.PrivateIntents;
import neolink.telephony.PrivateManager;
import neolink.telephony.PrivateMode;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class privateUtil {

	private static final String TAG = "privateCallandSmsUtil";
	public static String PRIVATE_CALL_SMS_NUMBER = "private_call_sms_number";
	public static String PRIVATE_CALL_SMS_IMPORT_MODE = "private_call_sms_import_mode";
	public static String PRIVATE_CALL_SMS_CURRENT_MODE = "private_call_sms_current_mode";
	
	
	public int mMode=PrivateMode.MODE_UNKNOWN;
	private   PrivateManager mManager;

	public static Intent getForwardPrivateSms(String name, String number,
			int mode) {
		Log.d(TAG, "privateCallandSmsUtil.getForwardSmsActionIntent"
				+ "number=" + number + "mode=" + mode);
		if (mode != ContactsApplication.getApplication().mMode) {

//			Toast.makeText(ContactsApplication.getApplication()
//					.getApplicationContext(),ContactsApplication.getApplication()
//					.getApplicationContext().getString(R.string.current_mode_unavailable) , Toast.LENGTH_SHORT).show();
			return null;

		} else {

			Intent intent = new Intent(PrivateIntents.ACTION_PRIVATE_SMS);
			intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			if (TextUtils.isEmpty(name)) {
				name = number;
			}
			if (TextUtils.isEmpty(number)) {
				return null;
			}
			intent.putExtra(PrivateIntents.EXTRA_CONTACT_NAME, name);
			intent.putExtra(PrivateIntents.EXTRA_CONTACT_NUMBER, number);

			return intent;
		}

	}

	public static Intent getForwardPrivatePhone(String name,
			String call_privateNUmber, int mode) {

		Log.d(TAG, "privateCallandSmsUtil.getForwardPrivatePhone"
				+ "call_privateNUmber=" + call_privateNUmber + "mode=" + mode);

		if (mode != ContactsApplication.getApplication().mMode) {
//			Toast.makeText(ContactsApplication.getApplication()
//					.getApplicationContext(), ContactsApplication.getApplication()
//					.getApplicationContext().getString(R.string.current_mode_unavailable), Toast.LENGTH_SHORT).show();
			return null;

		} else {
			Intent privateintent = new Intent(
					PrivateIntents.ACTION_PRIVATE_PHONE);
			if (TextUtils.isEmpty(call_privateNUmber)) {
				return null;
			}
			privateintent.putExtra(PrivateIntents.EXTRA_CONTACT_NAME, "test");
			privateintent.putExtra(PrivateIntents.EXTRA_CONTACT_NUMBER,
					call_privateNUmber.replace(" ", ""));

			return privateintent;
		}
	}
	
	
	
//	public static  long getCurrentMode() {
//
//		int modeType =  0;
//		int mMode =-1;
//		mMode = Settings.Secure.getInt(ContactsApplication.getApplication().getContentResolver(),
//				Settings.Secure.PRIVATE_PHONE_MODE, PrivateMode.MODE_UNKNOWN);
//
//		Log.d("getCurrentMode=", ""+mMode);
//		if (mMode == PrivateMode.MODE_MPT1327_ANALOG_TRUNKING) {
//			modeType =  RawContacts.CONTACTS_MODE_MPT;
//		} else if (mMode == PrivateMode.MODE_PDT_DIGITAL_TRUNKING) {
//			modeType =  RawContacts.CONTACTS_MODE_PDT;
//		}else if(mMode == PrivateMode.MODE_PDT_DIGITAL_NORMAL){
//			modeType =  3;
//		}else if(mMode == PrivateMode.MODE_MPT1327_ANALOG_NORMAL){
//			modeType =  4;
//		}
//		Log.d("getCurrentMode=", ""+modeType);
//		return modeType;
//	}
//	
//	public static long setCurrentMode(String mode) {
//		Log.d("getCurrentMode==", "mode=="+mode);
//		
//		if (!TextUtils.isEmpty(mode)) {
//			int modeType;
//			if (mode.equals(ContactsApplication.getApplication().getString(
//					R.string.private_contacts_mode_pdt))) {
//
//				return modeType = RawContacts.CONTACTS_MODE_PDT;
//			} else if (mode.equals(ContactsApplication.getApplication()
//					.getString(R.string.private_contacts_mode_mpt))) {
//				return modeType =  RawContacts.CONTACTS_MODE_MPT;
//			} else {
//				return -1;
//			}
//		}
//		return -1;
//	}
	
//	public static int getMode() {
//		int mMode = PrivateMode.MODE_UNKNOWN;
//		
//		mManager=new PrivateManager(ContactsApplication.getApplication().getApplicationContext());
//		mMode=mManager.getMode();
//		
//		
//		mMode = Settings.Secure.getInt(ContactsApplication.getApplication().getContentResolver(),
//				Settings.Secure.PRIVATE_PHONE_MODE, PrivateMode.MODE_UNKNOWN);
//
//		return mMode;
//	}

}
