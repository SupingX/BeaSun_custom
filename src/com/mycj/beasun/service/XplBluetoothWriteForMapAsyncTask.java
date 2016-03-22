package com.mycj.beasun.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.AsyncTask;
import android.util.Log;

public class XplBluetoothWriteForMapAsyncTask extends AsyncTask<byte[][], Void, Void> {
	private HashMap<String, BluetoothGatt> mapGatts;
	private long sleepTime = 500L;

	public XplBluetoothWriteForMapAsyncTask(HashMap<String, BluetoothGatt> mapGatts) {
		this.mapGatts = mapGatts;
	}

	public XplBluetoothWriteForMapAsyncTask(HashMap<String, BluetoothGatt> mapGatts, long sleepTime) {
		this.mapGatts = mapGatts;
		this.sleepTime = sleepTime;
	}

	@Override
	protected Void doInBackground(byte[][]... params) {
		if (mapGatts != null) {
			Set<Entry<String, BluetoothGatt>> entrySet = mapGatts.entrySet();
			Iterator<Entry<String, BluetoothGatt>> iterator= entrySet.iterator();
//			Set<String> keySet = mapGatts.keySet();
//			Iterator<String> iterator = keySet.iterator();
			byte[][] orders = params[0];
			while (iterator.hasNext()) {
				Entry<String, BluetoothGatt> next = iterator.next();
				BluetoothGatt gatt = next.getValue();
				BluetoothGattService service = gatt.getService(UUID.fromString(XplBluetoothManager.UUID_SERVICE));
				BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(XplBluetoothManager.UUID_CHARACTERISTIC_WRITE));
				for (int i = 0; i < orders.length; i++) {
					try {
						if (orders!=null) {
							characteristic.setValue(orders[i]);
							gatt.writeCharacteristic(characteristic);
							Thread.sleep(sleepTime);
						}
						Log.e("", gatt.getDevice().getAddress() + "写第 "+ i +" 个数据，时间："+System.currentTimeMillis());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		
		}
		return null;
	}

}
