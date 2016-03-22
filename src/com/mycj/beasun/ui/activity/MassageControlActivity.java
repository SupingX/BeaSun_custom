package com.mycj.beasun.ui.activity;

import java.util.List;

import android.content.Intent;
import android.nfc.cardemulation.OffHostApduService;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.mycj.beasun.BaseActivity;
import com.mycj.beasun.R;
import com.mycj.beasun.bean.MassageInfo;
import com.mycj.beasun.broadcastreceiver.SimpleBluetoothBroadcastReceiver;
import com.mycj.beasun.business.BeaStaticValue;
import com.mycj.beasun.business.MassageJson;
import com.mycj.beasun.business.MassageUtil;
import com.mycj.beasun.business.MusicLoader;
import com.mycj.beasun.business.ProtoclWrite;
import com.mycj.beasun.service.MusicService;
import com.mycj.beasun.service.XplBluetoothService;
import com.mycj.beasun.service.util.FileUtil;
import com.mycj.beasun.service.util.SharedPreferenceUtil;
import com.mycj.beasun.view.DotsView;
import com.mycj.beasun.view.TimeArcView;
import com.mycj.beasun.view.TimeArcView.OnTimePointChangeListener;
import com.mycj.beasun.view.TimeArcView.onTouchCancelListener;

/**
 * 按摩控制
 * 
 * @author Administrator
 *
 */
public class MassageControlActivity extends BaseActivity implements OnClickListener {
	/** 力度选择View **/
	private DotsView dotsView;
	/** 增加 **/
	private ImageView imgIncrease;
	/** 减少 **/
	private ImageView imgReduce;
	/** 按摩器开关 **/
	private CheckBox cbOnOff;
	/** 音乐开关 **/
	private ImageView imgMusicOnOFF;
	/** 倒计时View **/
	private TimeArcView timeArcView;
	/** 返回 **/
	private TextView tvBack;
	/** 音乐名称 **/
	private TextView tvMusicName;
	/** 当前按摩信息 **/
	private MassageInfo currentMassageInfo;
	/** 当前activity标题 **/
	private TextView tvTitle;
	/** 音乐服务 **/
	private MusicService musicService;
	/** 蓝牙服务 **/
	private XplBluetoothService xplBluetoothService;
	private Handler mHandler = new Handler() {

	};
	private HorizontalScrollView srcoll;
	private RadioGroup rgModel;
	private SimpleBluetoothBroadcastReceiver mReceiver = new SimpleBluetoothBroadcastReceiver() {
		public void doTimeChange(final int time) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					updateUIWhenTimeChange(time);
				}
			});
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_massage_control);
		musicService = getMusicService();
		xplBluetoothService = getXplBluetoothService();
		registerReceiver(mReceiver, XplBluetoothService.getIntentFilter());
		loadIntentData();
		initViews();
		freshUI();
		setListener();
		
	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
		currentMassageInfo = null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				Bundle b = data.getExtras();
				if (b != null) {
					if (musicService.isPlaying()) {
						imgMusicOnOFF.setImageResource(R.drawable.ic_sound_on);
					} else {
						imgMusicOnOFF.setImageResource(R.drawable.ic_sound_off);
					}

					long music = b.getLong("musicId");
					currentMassageInfo.setMusic(music);
					String musicName = b.getString("musicTitle");
					tvMusicName.setText(musicName);
					List<MassageInfo> massageInfosFromJson = MassageJson.loadJson(this);
					if (music != 0) {
						int index = currentMassageInfo.getIndex();
						if (massageInfosFromJson != null && massageInfosFromJson.size() > 0 && index >= 11 && index <= 11 + massageInfosFromJson.size()) {
							massageInfosFromJson.get(index - 11).setMusic(music);
							// FileUtil.writeFileData(BeaStaticValue.JSON_MASSAGE,
							// MassageJson.listToJson(massageInfosFromJson),
							// this);
							SharedPreferenceUtil.put(getApplicationContext(), BeaStaticValue.JSON_MASSAGE, MassageJson.listToJson(massageInfosFromJson));
						} else {

							switch (index) {
							case 0xF1:
								SharedPreferenceUtil.put(this, BeaStaticValue.MUSIC_ROUNIE, music);
								break;
							case 0xF2:
								SharedPreferenceUtil.put(this, BeaStaticValue.MUSIC_TUINA, music);
								break;
							case 0xF3:
								SharedPreferenceUtil.put(this, BeaStaticValue.MUSIC_GUASHA, music);
								break;
							case 0xF4:
								SharedPreferenceUtil.put(this, BeaStaticValue.MUSIC_SHOUSHEN, music);
								break;
							case 0xF5:
								SharedPreferenceUtil.put(this, BeaStaticValue.MUSIC_QINGFU, music);
								break;
							case 0xF6:
								SharedPreferenceUtil.put(this, BeaStaticValue.MUSIC_ZHENJIU, music);
								break;
							case 0xF7:
								SharedPreferenceUtil.put(this, BeaStaticValue.MUSIC_CHUIJI, music);
								break;
							case 0xF8:
								SharedPreferenceUtil.put(this, BeaStaticValue.MUSIC_ZHIYA, music);
								break;
							case 0xF9:
								SharedPreferenceUtil.put(this, BeaStaticValue.MUSIC_JINGZHUI, music);
								break;
							case 0xFA:
								SharedPreferenceUtil.put(this, BeaStaticValue.MUSIC_HUOGUAN, music);
								break;
							default:
								SharedPreferenceUtil.put(this, BeaStaticValue.MUSIC_INTELGENT_ + currentMassageInfo.getIndex(), music);
								break;
							}
						}
					}
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private RadioButton rbQinfu;
	private long lastTime;

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.img_reduce:
			long currentTimeMillis = System.currentTimeMillis();
			if (currentTimeMillis - lastTime < 500) {
				return;
			}
			lastTime = currentTimeMillis;
			dotsView.reduce();

			break;
		case R.id.img_increase:
			// if (!cbOnOff.isChecked()) {
			// return;
			// }
			long currentTimeMillis1 = System.currentTimeMillis();
			if (currentTimeMillis1 - lastTime < 500) {
				return;
			}
			lastTime = currentTimeMillis1;
			dotsView.increase();

			break;
		case R.id.tv_back:
			finish();
			break;
		case R.id.tv_music_name:
			startActivityForResult(new Intent(this, MusicActivity.class), 1);
			break;
		case R.id.img_music_voice:
			if (musicService.isPlaying()) {
				musicService.stop();
				imgMusicOnOFF.setImageResource(R.drawable.ic_sound_off);
			} else {
				musicService.play(MusicLoader.getMusicUriById(currentMassageInfo.getMusic()));
				imgMusicOnOFF.setImageResource(R.drawable.ic_sound_on);
			}

			break;

		default:
			break;
		}
	}

	private long last = 0;
	private long deff = 800;

	private void setListener() {
		dotsView.setOnDotChangeListener(new DotsView.OnDotChangeListener() {

			@Override
			public void onDotChange(final int currentDot) {
				if (!cbOnOff.isChecked()) {
					return;
				}
				currentMassageInfo.setPower(currentDot + 1);
				final long now = System.currentTimeMillis();
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (now - last >= deff) {
							last = now;
							if (xplBluetoothService != null && xplBluetoothService.getConnectedGattSize() > 0) {
								// 只有同一模式下才能继续写入操作
								if (xplBluetoothService.isSameMassageInfo(currentMassageInfo)) {

									currentMassageInfo = xplBluetoothService.getMassageInfo();
									// massageInfo.setPower(currentDot + 1);
									int level = dotsView.getCurrenTDotPosition() + 1;
									//改变后的dot为currentDot
									Log.e("", "改变后的dot 为currentDot =" +currentDot);
									currentMassageInfo.setPower((level != 1 && level != 11) ? (level + 1) : level);
									// 力度必须在模式之前设置 否则无效
									xplBluetoothService.setMassageInfo(currentMassageInfo);
									xplBluetoothService.startMassage();

								}
							}
						}
					}
				}, deff + 100);
			}
		});

		// 经典模式选择
		rgModel.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (!cbOnOff.isChecked()) {
					return;
				}
				final long now = System.currentTimeMillis();
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (now - last >= deff) {
							last = now;
							if (xplBluetoothService != null && xplBluetoothService.getConnectedGattSize() > 0) {
								if (xplBluetoothService.isSameMassageInfo(currentMassageInfo)) {
									// Log.v("",
									// "=================================切换模式============================================");
									currentMassageInfo = xplBluetoothService.getMassageInfo();
									int model_1 = 0;
									int model_2 = 0;
									switch (rgModel.getCheckedRadioButtonId()) {
									case R.id.rb_model_chuiji:
										Log.e("", "=====锤击====");
										model_1 = 0b100;
										model_2 = 0b0;

										break;
									case R.id.rb_model_qinfu:
										Log.e("", "=====轻抚====");
										model_1 = 0b0;
										model_2 = 0b10;
										break;
									case R.id.rb_model_rounie:
										Log.e("", "=====揉捏====");
										model_1 = 0b01;
										model_2 = 0b0;
										break;
									case R.id.rb_model_tuina:
										Log.e("", "=====推拿====");
										model_1 = 0b10;
										model_2 = 0b0;
										break;
									case R.id.rb_model_zhenjiu:
										Log.e("", "=====针灸====");
										model_1 = 0b10000;
										model_2 = 0b0;
										break;
									case R.id.rb_model_zhiya:
										Log.e("", "=====指压====");
										model_1 = 0b100000;
										model_2 = 0b0;
										break;

									default:
										model_1 = 0b0;
										model_2 = 0b10;
										break;
									}
									currentMassageInfo.setModel_1(model_1);
									currentMassageInfo.setModel_2(model_2);
									// 力度必须在模式之前设置 否则无效
									xplBluetoothService.setMassageInfo(currentMassageInfo);
									xplBluetoothService.startMassage();
								}
							}

						}
					}
				}, deff + 50);
			}
		});
		timeArcView.setonTouchCancelListener(new onTouchCancelListener() {

			@Override
			public void onCancel(int time) {
				currentTime = time;
				if (xplBluetoothService != null && xplBluetoothService.isSameMassageInfo(currentMassageInfo)) {
					xplBluetoothService.updateTime(time);
					currentMassageInfo = xplBluetoothService.getMassageInfo();
				} else {
				}

			}
		});

		// 按摩 --> 开 关
		cbOnOff.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					// Log.v("", "开");
					if (xplBluetoothService != null && xplBluetoothService.getConnectedGattSize() <= 0) {
						showIosDialog(getString(R.string.control_least_one_device));
						cbOnOff.setChecked(false);
						return;
					}

					if (!cbOnOff.isChecked()) {
						return;
					}

					if (currentMassageInfo.getTime() == 0) {
						return;
					}

					if (xplBluetoothService != null && !xplBluetoothService.isSameMassageInfo(currentMassageInfo)) {// 当前按摩信息不同时，可以开启开关。
						// 开启音乐
						imgMusicOnOFF.setImageResource(R.drawable.ic_sound_on);
						if (currentMassageInfo.getMusic() != -1) {
							musicService.play(MusicLoader.getMusicUriById(currentMassageInfo.getMusic()));
						} else {
							if (musicService.isPlaying()) {
								musicService.stop();
							}
						}
						// 设置力度
						int level = dotsView.getCurrenTDotPosition();
						currentMassageInfo.setPower((level != 1 && level != 11) ? (level + 1) : level);
						// 设置模式
						int index = currentMassageInfo.getIndex();
						if (index == 0) { // 表示index为经典模式
							int model_1 = 0;
							int model_2 = 0;
							int modelType = rgModel.getCheckedRadioButtonId();
							switch (modelType) {
							case R.id.rb_model_chuiji:
								// index = 0xF7;
								model_1 = 0b100;
								model_2 = 0b0;

								break;
							case R.id.rb_model_qinfu:
								model_1 = 0b0;
								model_2 = 0b10;
								// index = 0xF5;
								break;
							case R.id.rb_model_rounie:
								model_1 = 0b01;
								model_2 = 0b0;
								// index = 0xF1;
								break;
							case R.id.rb_model_tuina:
								model_1 = 0b10;
								model_2 = 0b0;
								// index = 0xF2;
								break;
							case R.id.rb_model_zhenjiu:
								model_1 = 0b10000;
								model_2 = 0b0;
								// index = 0xF6;
								break;
							case R.id.rb_model_zhiya:
								model_1 = 0b100000;
								model_2 = 0b0;
								// index = 0xF8;
								break;
							default:
								model_1 = 0b0;
								model_2 = 0b10;
								break;
							}
							// 设置模式
							currentMassageInfo.setModel_1(model_1);
							currentMassageInfo.setModel_2(model_2);
						}
						// 设置时间
						currentMassageInfo.setTime(timeArcView.getTimePoint());
						// 同步按摩信息到service
						xplBluetoothService.setMassageInfo(currentMassageInfo);
						// 开启按摩
						xplBluetoothService.startMassage();
					}
				} else {
					// Log.v("", "按钮 关");
					if (xplBluetoothService != null && xplBluetoothService.getMassageInfo() != null) {
						if (xplBluetoothService.isSameMassageInfo(currentMassageInfo)) {
							xplBluetoothService.stopMassage();
						}
					}
					if (musicService.isPlaying()) {
						musicService.stop();
						imgMusicOnOFF.setImageResource(R.drawable.ic_sound_off);
					}
				}
			}

		});
		imgMusicOnOFF.setOnClickListener(this);
	}

	private void initViews() {
		imgIncrease = (ImageView) findViewById(R.id.img_increase);
		imgReduce = (ImageView) findViewById(R.id.img_reduce);
		dotsView = (DotsView) findViewById(R.id.dots);
		tvBack = (TextView) findViewById(R.id.tv_back);
		tvTitle = (TextView) findViewById(R.id.tv_control_title);
		tvMusicName = (TextView) findViewById(R.id.tv_music_name);

		srcoll = (HorizontalScrollView) findViewById(R.id.srcoll);

		rgModel = (RadioGroup) findViewById(R.id.rg_model);
		rbQinfu = (RadioButton) findViewById(R.id.rb_model_qinfu);

		rgModel.check(R.id.rb_model_qinfu);

		tvMusicName.setOnClickListener(this);
		tvBack.setOnClickListener(this);
		imgIncrease.setOnClickListener(this);
		imgReduce.setOnClickListener(this);
		// 计时器
		timeArcView = (TimeArcView) findViewById(R.id.time_arc);

		// 按摩器开关
		cbOnOff = (CheckBox) findViewById(R.id.cb_on_off);

		imgMusicOnOFF = (ImageView) findViewById(R.id.img_music_voice);

		// radio
		RadioButton rb = (RadioButton) findViewById(R.id.rb_model_chuiji);
		rb.setCompoundDrawablePadding(2);
	}

	/**
	 * 接受数据
	 * 
	 */
	private void loadIntentData() {
		Intent intent = getIntent();
		Bundle b = intent.getExtras();
		if (b != null) {
			currentMassageInfo = b.getParcelable("massage");
			Log.e("", "-----------------currentMassageInfo 接受到intent的按摩模式信息---------------------" + "\n" + currentMassageInfo);
		}
	}

	/**
	 * 根据当前currentMassageInfo 加载视图
	 */
	private void freshUI() {

		// 如果当前视图 和 service上的视图 一样，则更新当前currentMassageInfo一些参数
		if (currentMassageInfo.getIndex() == 0) {
			srcoll.setVisibility(View.VISIBLE);
		} else {
			srcoll.setVisibility(View.INVISIBLE);
		}

		if (xplBluetoothService != null && xplBluetoothService.isSameMassageInfo(currentMassageInfo)) {
			currentMassageInfo = xplBluetoothService.getMassageInfo();
			int model_1 = currentMassageInfo.getModel_1();
			int model_2 = currentMassageInfo.getModel_2();
			if (model_1 == 0b01 && model_2 == 0b0) {
				rgModel.check(R.id.rb_model_rounie);
			} else if (model_1 == 0b10 && model_2 == 0b0) {
				rgModel.check(R.id.rb_model_tuina);
			} else if (model_1 == 0b0 && model_2 == 0b10) {
				rgModel.check(R.id.rb_model_qinfu);
			} else if (model_1 == 0b10000 && model_2 == 0b0) {
				rgModel.check(R.id.rb_model_zhenjiu);
			} else if (model_1 == 0b100 && model_2 == 0b0) {
				rgModel.check(R.id.rb_model_chuiji);
			} else if (model_1 == 0b100000 && model_2 == 0b0) {
				rgModel.check(R.id.rb_model_zhiya);
			}
		}

		// 开关
		// if (isSameMassageInfo) {
		if (xplBluetoothService != null && xplBluetoothService.isSameMassageInfo(currentMassageInfo)) {
			// cbOnOff.setChecked(mSimpleBlueService.getMassageState() == 1 ?
			// true : false);
			cbOnOff.setChecked(xplBluetoothService.getMassageState() == XplBluetoothService.MASSAGE_STATE_START ? true : false);

		} else {
			cbOnOff.setChecked(false);
		}

		if (xplBluetoothService != null && xplBluetoothService.isSameMassageInfo(currentMassageInfo)) {
			if (musicService.isPlaying()) {
				imgMusicOnOFF.setImageResource(R.drawable.ic_sound_on);
			} else {
				imgMusicOnOFF.setImageResource(R.drawable.ic_sound_off);
			}
		}

		// 力度
		int power = currentMassageInfo.getPower();
		Log.e("", "初始化currentDot ：" + power);
		if (power ==1) {
			power = 0;
		}
		else if (power ==11) {
			power = 10;
		}else {
			power -=2;
		}
		dotsView.setCurrentDot(power);
	
		// 音乐
		if (currentMassageInfo.getMusic() == -1L) {
			tvMusicName.setText(getString(R.string.custom_choose_music));
		} else {
			String musicName = MusicLoader.getTitleFromId(currentMassageInfo.getMusic());
			if (musicName != null) {
				tvMusicName.setText(musicName);
			} else {
				tvMusicName.setText(getString(R.string.custom_choose_music));
			}
		}
		// 标题
		tvTitle.setText(currentMassageInfo.getText());
		// 倒计时
		timeArcView.setTimePoint(currentMassageInfo.getTime());

	}

	private int currentTime = 20;

	/**
	 * 根据service传来的时间更新UI
	 * 
	 * @param time
	 */
	private void updateUIWhenTimeChange(int time) {
		if (!cbOnOff.isChecked()) {
			return;
		}
		// Log.e("", "currentMassageInfo.getTime();	" +
		// currentMassageInfo.getTime());
		timeArcView.setTimePoint(time);
		if (time == 0) {

			dotsView.setCurrentDot(currentMassageInfo.getPower() - 1);
			// Log.e("", "currentMassageInfo :" +currentMassageInfo.getTime());
			// timeArcView.setTimePoint(currentMassageInfo.getTime());
			timeArcView.setTimePoint(currentTime);
			cbOnOff.setChecked(false);
			if (musicService.isPlaying()) {
				musicService.stop();
				imgMusicOnOFF.setImageResource(R.drawable.ic_sound_off);
			}
		}

	}
}
