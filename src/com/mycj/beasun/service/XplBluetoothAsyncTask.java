package com.mycj.beasun.service;

import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.AsyncTask;
import android.util.Log;

public class XplBluetoothAsyncTask extends AsyncTask<byte[][], Void, Void> {
	private BluetoothGatt gatt;
	private long sleepTime = 600L;

	public XplBluetoothAsyncTask(BluetoothGatt gatt) {
		this.gatt = gatt;
	}

	public XplBluetoothAsyncTask(BluetoothGatt gatt, long sleepTime) {
		this.gatt = gatt;
		this.sleepTime = sleepTime;
	}

	@Override
	protected Void doInBackground(byte[][]... params) {
		try {
			byte[][] orders = params[0];
			BluetoothGattService service = gatt.getService(UUID.fromString(XplBluetoothManager.UUID_SERVICE));
			BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(XplBluetoothManager.UUID_CHARACTERISTIC_WRITE));
			for (int i = 0; i < orders.length; i++) {
				if (orders != null) {
					characteristic.setValue(orders[i]);
					gatt.writeCharacteristic(characteristic);
					Thread.sleep(sleepTime);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

}
