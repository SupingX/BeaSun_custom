package com.mycj.beasun.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;

import com.mycj.beasun.bean.DeviceJson;
import com.mycj.beasun.bean.MassageInfo;
import com.mycj.beasun.broadcastreceiver.BluetoothStateBroadcastReceiver;
import com.mycj.beasun.business.BluetoothJson;
import com.mycj.beasun.business.MassageUtil;
import com.mycj.beasun.business.ProtoclNotify;
import com.mycj.beasun.business.ProtoclWrite;
import com.mycj.beasun.service.util.SharedPreferenceUtil;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.location.Address;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class XplBluetoothService extends Service {
	private final String TAG = XplBluetoothService.class.getSimpleName();
	private final int MSG_DEVICE_FOUND = 0xA4;
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_DEVICE_FOUND:
				int rssi = msg.arg1;
				BluetoothDevice device = (BluetoothDevice) msg.obj;
				doDeviceFound(device, rssi);
				break;
			// // 开始搜索蓝牙
			// case MSG_SCAN:
			// // mHandler.postDelayed(runnableScan, OFFSET_SCAN);
			// break;
			// // SERVICE发现
			// case MSG_SERVICE_DISCOVERED:
			//
			// break;
			// // 蓝牙连接状态改变
			// case MSG_CONNECTED_CHANGE:
			//
			// break;
			case 0xFC:
				if (currentMassageInfo != null) {
					int timePoint = currentMassageInfo.getTime();
					// Log.v("", "===========================================");
					// Log.v("", "=== 	当前剩余按摩时间： " + timePoint);
					// Log.v("", "===========================================");
					timePoint--;
					if (timePoint <= 0) {
						// stopTimer();
						// writeCharacteristicForMap(ProtoclWrite.instance().protoclWriteForControl(0x03));
						stopMassage();
						massageState = MASSAGE_STATE_STOP;
						timePoint = 0;
						sendBroadcastForMassageStop(timePoint);
					} else {
						currentMassageInfo.setTime(timePoint);
						sendBroadcastForMassageStop(timePoint);
						mHandler.postDelayed(runTimer, 60 * 1000);
					}
				}
				break;
			default:
				break;
			}
		}

	};

	private XplBluetoothManager xplBluetoothManager;

	/**
	 * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> service
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 **/
	/*
	 * 主要是service的生命周期方法，一些初始化值
	 */
	@Override
	public void onCreate() {
		// Log.i(TAG, "onCreate()");

		super.onCreate();
		receiver = new BluetoothStateBroadcastReceiver() {
			@Override
			public void onDeviceAclDisconnected(final BluetoothDevice device) {
				super.onDeviceAclDisconnected(device);
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub

						System.out.println("================onDeviceAclDisconnected()===掉线啦！！！！===================");
						scanDevice(false);
						// gatt.disconnect();
						BluetoothGatt gatt = xplBluetoothManager.getConnectDeviceMap().get(device.getAddress());
						xplBluetoothManager.removeGattFromMap(device.getAddress());
						if (gatt!=null) {
							gatt.close();
							gatt=null;
						}
						searchDevices.remove(device);
						scanDevice(true);
						System.out.println("================xplBluetoothManager()==========size:" + xplBluetoothManager.getConnectedGattSize());
					}
				});

			}
		};
		// 绑定手机蓝牙信号状态
		IntentFilter stateChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		stateChangeFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		stateChangeFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		stateChangeFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
		registerReceiver(receiver, stateChangeFilter);

		deviceJsons = BluetoothJson.loadJson(getApplicationContext());
		if (deviceJsons != null) {
			for (DeviceJson json : deviceJsons) {
				if (!addresses.contains(addresses)) {
					addresses.add(json.getAddress());
				}
			}
		}

		xplBluetoothManager = new XplBluetoothManager(this) {
			@Override
			protected void doLeScanCallBack(BluetoothDevice device, int rssi, byte[] scanRecord) {
				super.doLeScanCallBack(device, rssi, scanRecord);
				doDeviceFound(device, rssi);
			}

			@Override
			protected void doScanStateChangeCallBack(boolean isScanning) {
				super.doScanStateChangeCallBack(isScanning);
			}

			@Override
			protected void doCharacteristicChangedCallback(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
				super.doCharacteristicChangedCallback(gatt, characteristic);
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						doCharacteristicChanged(gatt, characteristic);
					}

				});
			}

			@Override
			protected void doConnectionStateChangeCallback(final BluetoothGatt gatt, final int newState, int status) {
				super.doConnectionStateChangeCallback(gatt, newState, status);
				if (status == BluetoothGatt.GATT_SUCCESS) {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							switch (newState) {
							case BluetoothGatt.STATE_CONNECTED:
								// Log.e(TAG, "已连接 state ：" + newState);
								doConnected(gatt, newState);
								break;
							case BluetoothGatt.STATE_DISCONNECTED:
								// Log.e(TAG, "已断开 state ：" + newState);
								doDisconnected(gatt, newState);
								break;
							default:
								// Log.e(TAG, "其他 state ：" + newState);
								break;
							}
							sendBroadcastConnectState(gatt.getDevice(), newState);
						}
					});
				} else {
					// Log.i(TAG, "onConnectionStateChange() 失败");
				}
			}

			@Override
			protected void doServicesDiscoveredCallback(final BluetoothGatt gatt, int status) {
				// super.doServicesDiscoveredCallback(gatt, status);
				if (status == BluetoothGatt.GATT_SUCCESS) {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							parseGatt(gatt);
						}
					});
				} else if (status == BluetoothGatt.GATT_FAILURE) {
					// Log.i(TAG, "onServicesDiscovered() 失败");
				}
			}

		};

	}

	@Override
	public IBinder onBind(Intent intent) {
		// Log.i(TAG, "onBind()");
		return new XplBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		stopMassage();
		timer.cancel();
		closeAll();
		searchDevices.clear();
		return super.onUnbind(intent);

	}

	@Override
	public void onDestroy() {
		// Log.i(TAG, "onDestroy()");
		unregisterReceiver(receiver);
		stopMassage();
		timer.cancel();
		closeAll();
		searchDevices.clear();
		super.onDestroy();
	}

	public class XplBinder extends Binder {
		public XplBluetoothService getXplBluetoothService() {
			return XplBluetoothService.this;
		}
	}

	/*
	 * 关于蓝牙的一些操作
	 */
	/**
	 * <p>
	 * 是否支持 Ble
	 * </p>
	 * <b> {uses-feature android:name="android.hardware.bluetooth_le"
	 * android:required="true"/></B> required为true时，则应用只能在支持BLE的Android设备上安装运行；<br/>
	 * required为false时，Android设备均可正常安装运行，需要在代码运行时判断设备是否支持BLE feature： <br/>
	 * 
	 * @return isSupport
	 */
	public boolean isSupportBle() {
		return xplBluetoothManager.isSupportBle();
	}

	/**
	 * <p>
	 * 获取BluetoothAdapter
	 * </p>
	 * 注：这里通过getSystemService获取BluetoothManager，
	 * 再通过BluetoothManager获取BluetoothAdapter。BluetoothManager在Android4.3以上支持(API
	 * level 18)。
	 * 
	 * @return BluetoothAdapter
	 */
	public BluetoothAdapter getBluetoothAdapter() {
		return xplBluetoothManager.getBluetoothAdapter();
	}

	/**
	 * <p>
	 * 判断是否打开蓝牙
	 * </p>
	 * 
	 * @return isEnable
	 */
	public boolean isBluetoothEnable() {
		if (xplBluetoothManager.isBluetoothEnable()) {
			return true;
		} else {
			sendBroadcastBleDisnable();
			return false;
		}
	}

	/**
	 * <p>
	 * 通过 intent打开 蓝牙
	 * </p>
	 * 
	 * 在onActivityResult获取结果
	 * 
	 * @param Activity
	 */
	public void enableBluetooth(Activity ac) {
		xplBluetoothManager.enableBluetooth(ac);
	}

	/**
	 * <p>
	 * 搜索设备
	 * </p>
	 * 
	 * @param enable
	 */
	public void scanDevice(final boolean enable) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				xplBluetoothManager.scanDevice(enable);
				sendBroadcastScanState(xplBluetoothManager.isScanning());
			}
		});

	}

	/**
	 * <p>
	 * 搜索指定serviceUuids设备
	 * </p>
	 * 
	 * @param serviceUuids
	 */
	public void scanDevice(UUID[] serviceUuids) {
		xplBluetoothManager.scanDevice(serviceUuids);
		sendBroadcastScanState(xplBluetoothManager.isScanning());
	}

	/**
	 * <p>
	 * 单连
	 * </p>
	 * 
	 * @param device
	 * @return
	 */
	public BluetoothGatt connectOne(final BluetoothDevice device) {
		return xplBluetoothManager.connectOne(device);
	}

	/**
	 * <p>
	 * 连接设备
	 * </p>
	 * 
	 * @param device
	 * @return BluetoothGatt
	 */
	public BluetoothGatt connect(BluetoothDevice device) {
		return xplBluetoothManager.connect(device);
	}

	/**
	 * <p>
	 * 连接地址
	 * </p>
	 * 
	 * @param String
	 *            蓝牙地址
	 * @return BluetoothGatt
	 */
	public BluetoothGatt connect(String address) {
		return xplBluetoothManager.connect(address);
	}

	public int getConnectedGattSize() {
		return xplBluetoothManager.getConnectedGattSize();
	}

	/**
	 * <p>
	 * 获得指定设备是否连接
	 * </p>
	 * 
	 * @param device
	 * @return
	 */
	public boolean isBluetoothConnected(BluetoothDevice device) {
		return xplBluetoothManager.isBluetoothConnected(device);
	}

	/**
	 * <p>
	 * 获得指定地址是否连接
	 * </p>
	 * 
	 * @param device
	 * @return
	 */
	public boolean isBluetoothConnected(String address) {
		return xplBluetoothManager.isBluetoothConnected(address);
	}

	/**
	 * <p>
	 * 关闭对应地址的gatt
	 * </p>
	 * 
	 * @param address
	 */
	public void close(final String address) {
		xplBluetoothManager.close(address);

	}

	/**
	 * <p>
	 * 关闭对应地址的gatt
	 * 
	 * 关闭gatt gatt不能释放的问题？
	 * 
	 * </p>
	 * 
	 * @param address
	 */
	public void close(final BluetoothDevice device) {
		xplBluetoothManager.close(device);
	}

	/**
	 * <p>
	 * 关闭所有gatt
	 * </p>
	 */
	public void closeAll() {
		xplBluetoothManager.closeAll();
	}

	/**
	 * <p>
	 * 写属性
	 * </p>
	 * 
	 * @param gatt
	 * @param order
	 */
	public void writeCharacteristic(BluetoothGatt gatt, byte[] order) {
		xplBluetoothManager.writeCharacteristic(gatt, order);
		;
	}

	/**
	 * <p>
	 * 写属性
	 * </p>
	 * 
	 * @param gatt
	 * @param order
	 */
	public void writeCharacteristicAsync(BluetoothGatt gatt, byte[] order) {
		// xplBluetoothManager.writeCharacteristicAsync(gatt, order);
		xplBluetoothManager.writeCharacteristicForMap(order);
		;
	}

	/**
	 * <p>
	 * 根据地址写数据
	 * </p>
	 * 如果地址在已连接的 gatt中 存在对应的 地址 ，则写数据 。
	 * 
	 * @param address
	 * @param order
	 */
	public void writeCharacteristic(String address, byte[] order) {
		xplBluetoothManager.writeCharacteristic(address, order);
		;
	}

	/**
	 * <p>
	 * 多个gatt写数据
	 * <p>
	 * 
	 * @param order
	 */
	public void writeCharacteristicForMap(byte[] order) {
		xplBluetoothManager.writeCharacteristicForMap(order);
	}

	public void writeCharacteristicForMapAsync(byte[] order) {
		xplBluetoothManager.writeCharacteristicForMapAsync(order);
	}

	/**
	 * <p>
	 * 添加gatt到已连接的gatt中
	 * </p>
	 * 
	 * @param gatt
	 */
	private void addGattToMap(BluetoothGatt gatt) {
		xplBluetoothManager.addGattToMap(gatt);
		;
	}

	/**
	 * <p>
	 * 已连接的gatt中 删除gatt
	 * </p>
	 * 
	 * @param gatt
	 */
	private void removeGattFromMap(BluetoothGatt gatt) {
		xplBluetoothManager.removeGattFromMap(gatt);
	}

	/**
	 * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 蓝牙
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 **/

	/**
	 * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 各种通知
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 **/
	public static final String ACTION_DEVICE_FOUND = "ACTION_DEVICE_FOUND";
	public static final String ACTION_ADAPTER_DISNABLE = "ACTION_ADAPTER_DISNABLE";
	public static final String ACTION_CONNECT_STATE = "ACTION_CONNECT_STATE";
	public static final String ACTION_SERVICE_DISCOVERED = "ACTION_SERVICE_DISCOVERED";
	public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
	public static final String EXTRA_RSSI = "EXTRA_RSSI";
	public static final String EXTRA_CONNECT_STATE = "EXTRA_CONNECT_STATE";

	/**
	 * <p>
	 * 获取所有关于此service的Action
	 * </p>
	 * 
	 * @return IntentFilter
	 */
	public static IntentFilter getIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_DEVICE_FOUND);
		filter.addAction(ACTION_ADAPTER_DISNABLE);
		filter.addAction(ACTION_CONNECT_STATE);
		filter.addAction(ACTION_SERVICE_DISCOVERED);
		filter.addAction(ACTION_ELECTRICITY);
		filter.addAction(ACTION_SERVICE_MASSAGE_TIME);
		return filter;
	}

	/**
	 * <p>
	 * 搜索发现设备，发送通知
	 * </p>
	 * 
	 * @param device
	 * @param rssi
	 */
	private void sendBroadcastDeviceFound(BluetoothDevice device, int rssi) {
		// //Log.i(TAG, "发送通知 ：DeviceFound");
		Intent intent = new Intent(ACTION_DEVICE_FOUND);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_RSSI, rssi);
		sendBroadcast(intent);
	}

	/**
	 * <p>
	 * 蓝牙不可用 ，发送通知
	 * </p>
	 */
	private void sendBroadcastBleDisnable() {
		Intent intent = new Intent(ACTION_ADAPTER_DISNABLE);
		sendBroadcast(intent);
	}

	/**
	 * <p>
	 * 蓝牙连接状态 ，发送通知
	 * </p>
	 */
	private void sendBroadcastConnectState(BluetoothDevice device, int newState) {
		Intent intent = new Intent(ACTION_CONNECT_STATE);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_CONNECT_STATE, newState);
		sendBroadcast(intent);
	};

	private void sendBroadcastScanState(boolean isScanning) {
	}

	/**
	 * <p>
	 * service发现 ，发送通知
	 * </p>
	 */
	private void sendBroadcastForServiceDiscoveredWriteDevice(BluetoothGatt gatt) {
		Intent intent = new Intent(ACTION_SERVICE_DISCOVERED);
		intent.putExtra(EXTRA_DEVICE, gatt.getDevice());
		sendBroadcast(intent);
	}

	/**
	 * <p>
	 * characteristic变化时 ，发送通知
	 * </p>
	 */
	private void sendBroadcastCharacteristicChanged(BluetoothGatt gatt) {
	}

	/**
	 * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 各种通知
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 **/

	/**
	 * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> do 可扩展成抽象方法
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 **/
	/*
	 * 蓝牙一些状态的回调，可分装成抽象方法 ，供子类实现
	 */
	/**
	 * <p>
	 * adapter不可用时，要做处理的事情
	 * </p>
	 */
	private void doBluetoothDisnable() {

	}

	/**
	 * <p>
	 * 找到设备时，要做处理的事情
	 * </p>
	 * 
	 * @param device
	 * @param rssi
	 */
	private void doDeviceFound(final BluetoothDevice device, int rssi) {
		sendBroadcastDeviceFound(device, rssi);
		// 连接本地绑定的设备
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				whenFoundDevice(device);
			}
		}, 800);

	}

	/**
	 * <p>
	 * 断线时，要做处理的事情
	 * </p>
	 * 
	 * @param BluetoothGatt
	 * @param newState
	 */
	private void doDisconnected(BluetoothGatt gatt, int newState) {
		System.out.println("===================掉线啦！！！！===================");
		scanDevice(false);
		if (isReallyExit) {
			closeAll();
			searchDevices.clear();
		} else {
			// gatt.disconnect();
			gatt.close();
			xplBluetoothManager.removeGattFromMap(gatt.getDevice().getAddress());
			searchDevices.remove(gatt.getDevice());
			gatt=null;
			scanDevice(true);
		}
		System.out.println("====size:" + xplBluetoothManager.getConnectedGattSize());
	}

	private boolean isReallyExit = false;

	public void setIsReallyExit(boolean isExit) {
		this.isReallyExit = isExit;
	}

	/**
	 * <p>
	 * 连接好时，要做处理的事情
	 * </p>
	 * 
	 * @param BluetoothGatt
	 * @param newState
	 */
	private void doConnected(final BluetoothGatt gatt, int newState) {
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				gatt.discoverServices();
			}
		}, 500);
	}

	/**
	 * <p>
	 * service发现，要做处理的事情
	 * </p>
	 * 
	 * @param BluetoothGatt
	 */
	private void doServiceDiscoveredWriteDevice(BluetoothGatt gatt) {
		serviceDiscoveredAgain(gatt);
		sendBroadcastForServiceDiscoveredWriteDevice(gatt);
		addGattToMap(gatt);
		saveDevice(gatt.getDevice());
		scanDevice(false);
		searchDevices.remove(gatt.getDevice());
		if (xplBluetoothManager.getConnectedGattSize() < deviceJsons.size()) {
			scanDevice(true);
		}

	}

	public void startScan() {
		xplBluetoothManager.scanDevice(true);
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				xplBluetoothManager.scanDevice(true);
			}
		}, 1000);
	}

	/**
	 * <p>
	 * characteristic变化时，要做处理的事情
	 * </p>
	 * 
	 * @param BluetoothGatt
	 * @param BluetoothGattCharacteristic
	 */
	private void doCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		byte[] data = characteristic.getValue();
		parseData(gatt, data);

	}

	/**
	 * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> do 可扩展成抽象方法
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 **/

	/**
	 * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 扩展
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 **/
	public final static String SHARE_DEVICE_ADDRESS = "SHARE_DEVICE_ADDRESS";

	/**
	 * <p>
	 * 清空本地蓝牙数据
	 * </p>
	 * 
	 * @param device
	 */
	private void clearDevice() {
		SharedPreferenceUtil.put(getApplicationContext(), SHARE_DEVICE_ADDRESS, "");
	}

	public void bindDevice(BluetoothDevice device) {
		saveDevice(device);
	}

	public void disBindDevice() {
		clearDevice();
	}

	/**
	 * <p>
	 * 保存单个蓝牙数据到本地
	 * </p>
	 * 
	 * @param device
	 */
	private void saveDevice(BluetoothDevice device) {
		SharedPreferenceUtil.put(getApplicationContext(), SHARE_DEVICE_ADDRESS, device.getAddress());
	}

	public String getBindedAddress() {
		return (String) SharedPreferenceUtil.get(getApplicationContext(), SHARE_DEVICE_ADDRESS, "");
	}

	/**
	 * <p>
	 * 连接已经绑定本地的设备
	 * </p>
	 * 
	 * @param device
	 */
	private void connectBindedDevice(BluetoothDevice device) {
		// 1.先判断本地是否保存
		final String bindedAddress = getBindedAddress();
		if (bindedAddress.equals("")) {
			return;
		}
		mHandler.postDelayed((new Runnable() {
			@Override
			public void run() {
				connect(bindedAddress);
			}
		}), 1000);
	}

	/**
	 * <p>
	 * 解析数据
	 * </p>
	 * 
	 * @param data
	 * @param BluetoothGatt
	 */
	private void parseData(BluetoothGatt gatt, byte[] data) {
		parseDataBeasa(gatt, data);
		sendBroadcastCharacteristicChanged(gatt);
	}

	/**
	 * <p>
	 * servicediscovered时 解析gatt是否是我们需要找的gatt
	 * </p>
	 * 
	 * @param gatt
	 */
	private void parseGatt(BluetoothGatt gatt) {
		scanDevice(false);
		BluetoothGattService service = gatt.getService(UUID.fromString(XplBluetoothManager.UUID_SERVICE));
		if (service != null) {
			BluetoothGattCharacteristic characteristicNotify = service.getCharacteristic(UUID.fromString(XplBluetoothManager.UUID_CHARACTERISTIC_NOTIFY));
			BluetoothGattCharacteristic characteristicWrite = service.getCharacteristic(UUID.fromString(XplBluetoothManager.UUID_CHARACTERISTIC_WRITE));
			if (characteristicNotify != null && characteristicWrite != null) {
				characteristicWrite.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);// 不带相应？
				gatt.setCharacteristicNotification(characteristicNotify, true);// 设置characteristic属性改变时发送通知
				BluetoothGattDescriptor descriptor = characteristicNotify.getDescriptor(UUID.fromString(XplBluetoothManager.UUID_DESC_CCC));
				if (descriptor != null) {
					descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);// 设置descriptor属性改变时发送通知
					gatt.writeDescriptor(descriptor);
					Log.e("", "------------〉找到服务！！！！");
					addGattToMap(gatt);
					// saveDevice(gatt.getDevice());
					sendBroadcastForServiceDiscoveredWriteDevice(gatt);
					
//					//连接上默认添加一个模式
//					byte[] modelOrder = ProtoclWrite.instance().protoclWriteForSingle("FB");
//					writeCharacteristic(gatt, modelOrder);
					
					serviceDiscoveredAgain(gatt);
					searchDevices.remove(gatt.getDevice());
					if (xplBluetoothManager.getConnectedGattSize() < deviceJsons.size()) {
						Log.e("", "继续搜索");
						scanDevice(true);
					}
				} else {
					Log.e("", "------------〉没有找到descroptor！！！！");
					searchDevices.remove(gatt.getDevice());
					gatt.close();
					scanDevice(true);
				}
			} else {
				Log.e("", "------------〉没有找到characteristic！！！！");
				searchDevices.remove(gatt.getDevice());
				gatt.close();
				scanDevice(true);
			}
		} else {
			Log.e("", "------------〉没有service！！！！");
			searchDevices.remove(gatt.getDevice());
			gatt.close();
			scanDevice(true);
		}
	}

	/**
	 * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> do 可扩展成抽象方法
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 **/

	/**
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<扩展>>>>>>>>>>>>>>>>>>>>>
	 * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	 */

	public static final int MASSAGE_STATE_STOP = 0x0a;
	public static final int MASSAGE_STATE_START = 0x0b;
	private int massageState = MASSAGE_STATE_STOP;
	private MassageInfo currentMassageInfo;
	public final static String ACTION_SERVICE_MASSAGE_TIME = "ACTION_SERVICE_MASSAGE_TIME";
	public final static String ACTION_ELECTRICITY = "ACTION_ELECTRICITY";
	public final static String EXTRA_ELECT = "EXTRA_ELECT";
	public final static String EXTRA_TIME = "EXTRA_TIME";
	public final static String EXTRA_ADDRESS = "EXTRA_ADDRESS";
	private Runnable runTimer = new Runnable() {
		public void run() {
			Message message = new Message();
			message.what = 0xFC;
			mHandler.sendMessage(message);
		}
	};
	private Timer timer;

	/**
	 * 按摩 停止
	 */
	public void stopMassage() {
		// new Thread(new Runnable() {
		// @Override
		// public void run() {
		// writeCharacteristicForMap(ProtoclWrite.instance().protoclWriteForControl(0x03));
	
		final byte[] order =  ProtoclWrite.instance()
				.protoclWriteForControl(0x03);
		new XplBluetoothWriteForMapAsyncTask(xplBluetoothManager.getConnectDeviceMap(), 700).execute(new byte[][] { order });
		
		/**
		 * 多线程
		 */
//		HashMap<String, BluetoothGatt> connectDeviceMap = xplBluetoothManager.getConnectDeviceMap();
//		
//		if (connectDeviceMap!=null && connectDeviceMap.size()>0) {
//			Iterator<Entry<String, BluetoothGatt>> keySet = connectDeviceMap.entrySet().iterator();
////			Iterator<Entry<String, BluetoothGatt>>> iterator = keySet.iterator();
//			while (keySet.hasNext()) {
//				Entry<String, BluetoothGatt> next = keySet.next();
//				String key = next.getKey();
//				final BluetoothGatt gatt = next.getValue();
//				Log.e("", "哈哈 guan address :" +key);
//				Log.e("", "哈哈  guan gatt.getAddress() :" +gatt.getDevice().getAddress());
//				
////				final BluetoothGatt gatt = connectDeviceMap.get(iterator.next());
//				new Thread(new Runnable() {
//					@Override
//					public void run() {
//						try {
//							BluetoothGattService service = gatt.getService(UUID.fromString(XplBluetoothManager.UUID_SERVICE));
//							BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(XplBluetoothManager.UUID_CHARACTERISTIC_WRITE));
//									characteristic.setValue(order);
//									gatt.writeCharacteristic(characteristic);
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//					}
//				}).start();
//				
//			}
//		}
		
		massageState = MASSAGE_STATE_STOP;
		currentMassageInfo = null;
		stopTimer();
		
		// }
		// }).start();

	}

	/**
	 * 按摩 开始 根据当前保存的currentMassageInfo
	 */
	public void startMassage() {
		// new Thread(new Runnable() {
		// @Override
		// public void run() {

		/*
		 * // if (currentMassageInfo == null) { // return; // }
		 */
		// 设置模式
		int model_1 = currentMassageInfo.getModel_1();
		int model_2 = currentMassageInfo.getModel_2();
		int index = currentMassageInfo.getIndex();
		byte[] modelOrder = null;

	
		// 设置强度
		byte[] powerOrder = ProtoclWrite.instance().protoclWriteForLevel(currentMassageInfo.getPower());
		// writeCharacteristicForMap(powerOrder);

		// 开始按摩
		byte[] onOffOrder = ProtoclWrite.instance().protoclWriteForControl(0x01);
		// writeCharacteristicForMap(onOffOrder);
		if (currentMassageInfo.getIsPix() == 1) {
			// 混合模式
			modelOrder = ProtoclWrite.instance().protoclWriteForIntelligent(model_1, model_2);
			// writeCharacteristicForMap(modelOrder);
			byte[] modelOrder1 =ProtoclWrite.instance().protoclWriteForSingle("FB");
			final byte[][] orders = new byte[][] {modelOrder1, modelOrder, powerOrder, onOffOrder };
			new XplBluetoothWriteForMapAsyncTask(xplBluetoothManager.getConnectDeviceMap(), 700).execute(orders);
		} else {
			// 单一模式
			// String singleHex = MassageUtil.getSingleHex(model_1,
			// model_2);
			if (index == 0) {
				String singleHex = MassageUtil.getSingleHex(model_1, model_2);
				modelOrder = ProtoclWrite.instance().protoclWriteForSingle(singleHex);
			} else {
				String singleHex = MassageUtil.getSingleHex(index);
				modelOrder = ProtoclWrite.instance().protoclWriteForSingle(singleHex);
			}
			final byte[][] orders = new byte[][] { modelOrder, powerOrder, onOffOrder };
			new XplBluetoothWriteForMapAsyncTask(xplBluetoothManager.getConnectDeviceMap(), 500).execute(orders);

			// writeCharacteristicForMap(modelOrder);
		}
		/**
		 * async
		 */
		
		
//		/**
//		 * 多线程
//		 */
//		HashMap<String, BluetoothGatt> connectDeviceMap = xplBluetoothManager.getConnectDeviceMap();
//		if (connectDeviceMap!=null && connectDeviceMap.size()>0) {
//			Log.e("", "connectDeviceMap.size() : "+connectDeviceMap.size());
//			Set<Entry<String, BluetoothGatt>> entrySet = connectDeviceMap.entrySet();
////			Set<String> keySet = connectDeviceMap.keySet();
//			Iterator<Entry<String, BluetoothGatt>> iterator = entrySet.iterator();
//			while (iterator.hasNext()) {
////				Log.e("", "遍历address: "+iterator.next());
////				final BluetoothGatt gatt = connectDeviceMap.get(iterator.next());
//				Entry<String, BluetoothGatt> next = iterator.next();
//				String key = next.getKey();
//				final BluetoothGatt gatt = next.getValue();
//				Log.e("", "哈哈 address :" +key);
//				Log.e("", "哈哈 gatt.getAddress() :" +gatt.getDevice().getAddress());
//				
//				new Thread(new Runnable() {
//					@Override
//					public void run() {
//						try {
//							BluetoothGattService service = gatt.getService(UUID.fromString(XplBluetoothManager.UUID_SERVICE));
//							BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(XplBluetoothManager.UUID_CHARACTERISTIC_WRITE));
//							for (int i = 0; i < orders.length; i++) {
//								if (orders != null) {
//									characteristic.setValue(orders[i]);
//									gatt.writeCharacteristic(characteristic);
//									Thread.sleep(600);
//								}
//							}
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//					}
//				}).start();
//				
//			}
//		}
		
		// 计时
		startTimer();
		// 更新状态
		massageState = MASSAGE_STATE_START;
		// }
		// }).start();
	}

	public void stopTimer() {
		mHandler.removeCallbacks(runTimer);
		// writeCharacteristicForMapAsync(ProtoclWrite.instance().protoclWriteForControl(0x03));

	}

	public void startTimer() {
		mHandler.removeCallbacks(runTimer);
		mHandler.postDelayed(runTimer, 60 * 1000);

	}

	public int getMassageState() {
		// Log.v(TAG, "[当前按摩状态：" + massageState + "]");
		return this.massageState;
	}

	public void setMassageState(int state) {
		this.massageState = state;
	}

	protected void sendBroadcastForMassageStop(int time) {
		Intent intent = new Intent(ACTION_SERVICE_MASSAGE_TIME);
		intent.putExtra(EXTRA_TIME, time);
		sendBroadcast(intent);
	}

	public void shutDown(String address) {
		xplBluetoothManager.writeCharacteristic(address, ProtoclWrite.instance().protoclWriteForShutDown());
		// close(address);
	}

	public boolean isSameMassageInfo(MassageInfo info) {
		if (info == null || currentMassageInfo == null) {
			return false;
		}
		Log.e("", "currentMassageInfo :" + currentMassageInfo.getIndex() + "比较");
		Log.e("", "info :" + info.getIndex());
		return currentMassageInfo.equals(info);
	}

	public void setMassageInfo(MassageInfo info) {
		this.currentMassageInfo = info;
	}

	public MassageInfo getMassageInfo() {
		return this.currentMassageInfo;
	}

	public void updateTime(int time) {
		this.currentMassageInfo.setTime(time);
		startTimer();
	}

	private List<DeviceJson> deviceJsons = new ArrayList<DeviceJson>();
	private Set<String> addresses = new HashSet<String>();

	public void setRomentDevice(List<DeviceJson> deviceJsons) {
		this.deviceJsons = deviceJsons;
		if (deviceJsons != null) {
			for (DeviceJson json : deviceJsons) {
				if (!addresses.contains(addresses)) {
					addresses.add(json.getAddress());
				}
			}
		}
	}

	private List<BluetoothDevice> searchDevices = new ArrayList<>();

	private void whenFoundDevice(final BluetoothDevice device) {
		// printDevice();
		// if (!xplBluetoothManager.isBluetoothConnected(device)) {
		// Log.e("", device.getAddress() + "未连接");
		// if (!searchDevices.contains(device)) {
		// Log.e("", device.getAddress() + "添加连接设备至搜索列表");
		// searchDevices.add(device);
		// // 2.判断蓝牙是否连接
		// List<DeviceJson> deviceJsons = BluetoothJson
		// .loadJson(getApplicationContext());
		// if (deviceJsons != null && deviceJsons.size() > 0) {
		// // Log.e("", "deviceJsons.size() :" + deviceJsons.size());
		// for (DeviceJson deviceJson : deviceJsons) {
		// // Log.e("service", "deviceJson 	:	" +
		// // deviceJson.getAddress());
		// // 1.判断本地是否保存
		// if (device.getAddress().equals(deviceJson.getAddress())) {
		// // 3.连接
		// connect(device);
		// }
		// }
		// }
		// }
		// }

		if (xplBluetoothManager.isBluetoothConnected(device)) {
			Log.e("", device.getAddress() + "已连接");
			return;
		}
		if (deviceJsons == null) {
			Log.e("", "本地保存设备为空");
			return;
		}
		if (deviceJsons.size() == 0) {
			Log.e("", "本地保存设备为空");
			return;
		}
		// Log.e("", "本地保存设备：" + deviceJsons.size() + "个");
		for (DeviceJson deviejson : deviceJsons) {
			final String address = deviejson.getAddress();
			if (device.getAddress().equals(address)) {
				if (!searchDevices.contains(device)) {
					searchDevices.add(device);
					final BluetoothGatt connect = xplBluetoothManager.connect(address);
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if (!isBluetoothConnected(address)) {
								Log.e("", address + "20秒还没有连接");
								if (connect != null) {
									// boolean connect2 = connect.connect();
									// if (!connect2) {
									scanDevice(false);
									connect.close();
									searchDevices.remove(device);
									scanDevice(true);
									// }else{
									// }
								}

							}
						}
					}, 20 * 1000);
					return;
				}
			}
		}

		// long now = System.currentTimeMillis();
		// if ((now - old) > 10 * 1000) {
		// old = now;
		// String address = device.getAddress();
		// Log.e("", "addresses :" + addresses.toString());
		// if (addresses.contains(address)) {
		// if (!isBluetoothConnected(address)) {
		// final BluetoothGatt connect = xplBluetoothManager.connect(address);
		// mHandler.postDelayed(new Runnable() {
		//
		// @Override
		// public void run() {
		// Log.e("", "关闭gatt");
		// connect.disconnect();
		// connect.close();
		// }
		// }, 20*1000);
		// }
		// }
		// }

	}

	private long old = 0L;
	private BluetoothStateBroadcastReceiver receiver;

	private void printDevice() {
		if (searchDevices != null) {
			if (searchDevices.size() == 0) {
				Log.e("", "searchDevices为空");
			} else {
				for (BluetoothDevice device : searchDevices) {
					Log.e("", "address ：" + device.getAddress());
				}
			}
		}
	}

	public List<BluetoothDevice> getSearchDevices() {
		return this.searchDevices;
	}

	private void serviceDiscoveredAgain(final BluetoothGatt gatt) {
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// if (xplBluetoothManager.isGattExist(gatt)) {
				if (currentMassageInfo != null) {
					// 设置模式
					int model_1 = currentMassageInfo.getModel_1();
					int model_2 = currentMassageInfo.getModel_2();
					byte[] modelOrder ;
					// 设置强度
//					xplBluetoothManager.writeCharacteristic(gatt, ProtoclWrite.instance().protoclWriteForLevel(currentMassageInfo.getPower()));
					byte[] powerOrder = ProtoclWrite.instance().protoclWriteForLevel(currentMassageInfo.getPower());
					// 开始按摩
//					xplBluetoothManager.writeCharacteristic(gatt, ProtoclWrite.instance().protoclWriteForControl(0x01));
					byte[] onOffOrder = ProtoclWrite.instance().protoclWriteForControl(0x01);
					if (currentMassageInfo.getIsPix() == 1) {
						// 混合模式
//						xplBluetoothManager.writeCharacteristic(gatt, ProtoclWrite.instance().protoclWriteForIntelligent(model_1, model_2));
						byte[] modelOrder1 =ProtoclWrite.instance().protoclWriteForSingle("FB");
						modelOrder = ProtoclWrite.instance().protoclWriteForIntelligent(model_1, model_2);
						final byte[][] orders = new byte[][] {modelOrder1, modelOrder, powerOrder, onOffOrder };
						new XplBluetoothAsyncTask(gatt, 600).execute(orders);
					} else {
						// 单一模式
						// String singleHex =
						// MassageUtil.getSingleHex(model_1,
						// model_2);
						String singleHex = MassageUtil.getSingleHex(currentMassageInfo.getIndex());
						System.out.println("模式 singleHex :" + singleHex);
//						xplBluetoothManager.writeCharacteristic(gatt, ProtoclWrite.instance().protoclWriteForSingle(singleHex));
						modelOrder = ProtoclWrite.instance().protoclWriteForSingle(singleHex);
						new XplBluetoothAsyncTask(gatt).execute(new byte[][]{modelOrder,powerOrder,onOffOrder});
					}
				} else {
					if (!xplBluetoothManager.isEmpty()) {
						stopMassage();
					}
				}
			}
			// }
		}, 2000);
	}
	

	public void parseDataBeasa(BluetoothGatt gatt, byte[] data) {
		ProtoclNotify notify = ProtoclNotify.instance();
		int parseData = notify.parseData(data);
		if (parseData == 3) {
			// 电量
			int electricity = notify.getElectricity(data);
			sendBroadcastForElectricity(electricity, gatt);
		}
	}

	protected void sendBroadcastForElectricity(int electricity, BluetoothGatt gatt) {
		Intent intent = new Intent(ACTION_ELECTRICITY);
		intent.putExtra(EXTRA_ELECT, electricity);
		intent.putExtra(EXTRA_ADDRESS, gatt.getDevice().getAddress());
		sendBroadcast(intent);
	}
}
