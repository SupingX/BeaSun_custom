package com.mycj.beasun.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;



import com.mycj.beasun.service.util.SharedPreferenceUtil;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;

public class XBlueUtils {
	public static String SHARE_DEVICES = "SHARE_DEVICES";
	public static String NON_DEVICES = "NON_DEVICES";
	public static String SPLIT_REG = ";";
	
	
	public static void saveBlue(Context context,HashMap<String,BluetoothGatt> gatts){
		if (gatts==null || gatts.size()==0) {
			return ;
		}
		StringBuffer sb = new StringBuffer();
		Set<Entry<String, BluetoothGatt>> entrySet = gatts.entrySet();
		Iterator<Entry<String, BluetoothGatt>> iterator = entrySet.iterator();
		while(iterator.hasNext()){
			Entry<String, BluetoothGatt> next = iterator.next();
			String key = next.getKey();
			sb.append(key + ";");
		}
		sb.substring(0, sb.length()-1);
		SharedPreferenceUtil.put(context, SHARE_DEVICES, sb.toString());
	}
	
	public static String getBluesJson(Context context){
		String devicesJson = (String) SharedPreferenceUtil.get(context, SHARE_DEVICES, NON_DEVICES);
		return devicesJson;
	}
	
	public static HashSet<String> getBlues(Context context){
		HashSet<String> set = new HashSet<String>();
		String bluesJson = getBluesJson(context);
		if (bluesJson.equals(NON_DEVICES)) {
			return null;
		}else{
			String[] addresses = bluesJson.split(SPLIT_REG);
			for (int i = 0; i < addresses.length; i++) {
				set.add(addresses[i]);
			}
		}
		return set;
	}
}
