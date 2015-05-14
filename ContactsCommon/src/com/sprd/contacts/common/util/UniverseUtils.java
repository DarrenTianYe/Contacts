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

package com.sprd.contacts.common.util;

import android.content.Context;
import com.android.contacts.common.R;
import android.os.SystemProperties;
import android.sim.Sim;
import android.sim.SimManager;
import android.telephony.TelephonyManager;
import java.util.ArrayList;
import android.util.Log;

public class UniverseUtils {
   private static String universeSupportKey = "universe_ui_support";
   public static final boolean UNIVERSEUI_SUPPORT = SystemProperties.getBoolean(universeSupportKey,false); 
   //public static boolean UNIVERSEUI_SUPPORT = true;
   
   public static String TAG = "UniverseUtils";
   public static final String IS_IP_DIAL = "is_ip_dial";
   
//Add for Multi-sim
   public static int getValidSimNumber(){
	   int validSimNum = 0;
	   int phoneCount = TelephonyManager.getPhoneCount();
	   for(int i=0;i<phoneCount;i++){
		   if(TelephonyManager.getDefault(i).getSimState() == TelephonyManager.SIM_STATE_READY){
			   validSimNum ++;
		   }
	   }
	   return validSimNum;
   }
   public static int getValidSimNumberEx(){
	   int validSimNum = 0;
	   int phoneCount = TelephonyManager.getPhoneCount();
	   for(int i=0;i<phoneCount;i++){
		   if(TelephonyManager.getDefault(i).hasIccCard() && TelephonyManager.getDefault(i).getSimState() == TelephonyManager.SIM_STATE_READY){
			   validSimNum ++;
		   }
	   }
	   return validSimNum;
   }

   public static int getValidPhoneId(){
	   int phoneCount = TelephonyManager.getPhoneCount();
	   for(int i=0;i<phoneCount;i++){
		   if(TelephonyManager.getDefault(i).getSimState() == TelephonyManager.SIM_STATE_READY){
			   return i;
		   }
	   }
	   return 0;
   }

   public static Sim[] getValidSim(Context context){
	   Sim[] result = null;
	   if(context == null){
		   return result;
	   }
	   SimManager simManager = SimManager.get(context);
	   ArrayList simList = new ArrayList<Sim>();
	   int phoneCount = TelephonyManager.getPhoneCount();
	   for(int i=0; i<phoneCount;i++){
		   if(TelephonyManager.getDefault(i).getSimState() == TelephonyManager.SIM_STATE_READY){
			   Sim sim = simManager.getSimById(i);
			   simList.add(sim);
		   }
	   }

	   result = new Sim[simList.size()];
	   for(int i=0 ;i<simList.size();i++){
		   result[i] = (Sim)simList.get(i);
	   }
	   return result;
   }

   public static String  getSimName(Context context,int phoneId){
	   String result = null;
	   if(context == null){
		   Log.i(TAG, "context == null");
		   return result;
	   }
	   SimManager simManager = SimManager.get(context);
	   Sim sim = simManager.getSimById(phoneId);
	   if(sim != null){
		   result = sim.getName();
	   }else{
		   Log.i(TAG, "sim == null)");
	   }
	   return result;
   }

   public static int getSimColor(Context context,int phoneId){
	   int result = 0;
	   if(context == null){
		   Log.i(TAG, "context == null");
		   return result;
	   }
	   SimManager simManager = SimManager.get(context);
	   Sim sim = simManager.getSimById(phoneId);
	   if(sim != null){
		   result = sim.getColor();

	   }else{
		   Log.i(TAG, "sim == null)");
	   }
	   return result;
   }
   
}
