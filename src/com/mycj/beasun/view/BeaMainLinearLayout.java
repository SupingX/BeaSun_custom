package com.mycj.beasun.view;

import com.mycj.beasun.R;
import com.mycj.beasun.service.util.DisplayUtil;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

public class BeaMainLinearLayout extends LinearLayout {
	
	private LinearLayout leftLinearLayout;
	private LinearLayout rightLinearLayout;
	private int [] leftImg = new int[]{
			R.drawable.ic_beasun_zhineng
			,R.drawable.ic_beasun_rounie
			,R.drawable.ic_beasun_tuina
			,R.drawable.ic_beasun_guasha
			,R.drawable.ic_beasun_shoushen
			};
	private int [] rightImg = new int[]{
			R.drawable.ic_beasun_qinfu
			,R.drawable.ic_beasun_zhenjiu
			,R.drawable.ic_beasun_chuiji
			,R.drawable.ic_beasun_zhiya
			,R.drawable.ic_beasun_jinzhui
			,R.drawable.ic_beasun_huoguan
	};
	private  int MAG = 4;
	public BeaMainLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public BeaMainLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}



	public BeaMainLinearLayout(Context context) {
		super(context);
		init(context);
	}
	
	public ImageView getImageView(int i){
		if (i<5) {
			return (ImageView) leftLinearLayout.getChildAt(i);
		}else{
			return (ImageView) rightLinearLayout.getChildAt(i-5);
		}
	}
	
	private void init(Context context) {
		this.setWeightSum(2);
		leftLinearLayout = new LinearLayout(context);
		LayoutParams paramsLeft = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		paramsLeft.weight=1;
		leftLinearLayout.setLayoutParams(paramsLeft);
//		leftLinearLayout.setBackgroundColor(Color.RED);
		leftLinearLayout.setOrientation(LinearLayout.VERTICAL);
		leftLinearLayout.setGravity(Gravity.CENTER);
		Point screenMetrics = DisplayUtil.getScreenMetrics(context);
		for (int i = 0; i < leftImg.length; i++) {
			
			ImageView img = new ImageView(context);
			img.setId(i);
			img.setClickable(true);
			img.setScaleType(ScaleType.FIT_XY);
			img.setImageResource(leftImg[i]);
			img.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						v.setAlpha(0.5f);
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						v.setAlpha(1.0f);
						break;
					default:
						break;
					}
					return false;
				}
			});
			int widthImg= img.getWidth();
			int heightImg= img.getHeight();
			int width = screenMetrics.x/2-6*MAG;
			float scale = (float) (1.0*width/widthImg);
//			int heigt = (int) (width * scale);
			int heigt = width;
			if (i==0||i==3) {
				 heigt = width;
			}else{
				 heigt = width/2;
			}
			LayoutParams params = new LayoutParams(width,heigt);
//			LayoutParams params = new LayoutParams(width,LayoutParams.WRAP_CONTENT);
			img.setLayoutParams(params);
			
			float a = 20*1.0F *MAG/16;
			
			int top =0;
			int bottom=0;
			int left = MAG*2;
			int right = MAG;
			
			if (i==0) {
				top = MAG*4;
				bottom=(int) (2*a);
			}else if (i==4) {
				top = (int) (a*2);
				bottom=2*MAG;
			}else{
				top = (int) (a*2);
				bottom=(int) (2*a);
			}
			params.topMargin =top;
			params.bottomMargin = bottom;
			params.leftMargin= left;
			params.rightMargin= right;
			leftLinearLayout.addView(img);
		}
		
		rightLinearLayout = new LinearLayout(context);
		rightLinearLayout.setGravity(Gravity.CENTER);
		LayoutParams paramsRight = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		paramsRight.weight=1;
//		rightLinearLayout.setBackgroundColor(Color.GREEN);
		rightLinearLayout.setLayoutParams(paramsRight);
		rightLinearLayout.setOrientation(LinearLayout.VERTICAL);
		for (int i = 0; i < rightImg.length; i++) {
			ImageView img = new ImageView(context);
			img.setClickable(true);
			img.setId(i+5);
			img.setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						v.setAlpha(0.5f);
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						v.setAlpha(1.0f);
						break;
					default:
						break;
					}
					return false;
				}
			});
			img.setImageResource(rightImg[i]);
			img.setScaleType(ScaleType.FIT_XY);
			int widthImg= img.getWidth();
			int heightImg= img.getHeight();
			int width = screenMetrics.x/2-6*MAG;
			float scale = (float) (1.0*width/widthImg);
//			int heigt = (int) (heightImg * scale);
			int heigt = width;
			if (i==1) {
				 heigt = width;
			}else{
				 heigt = width/2;
			}
			
		
			LayoutParams params = new LayoutParams(width,heigt);
//			LayoutParams params = new LayoutParams(width,LayoutParams.WRAP_CONTENT);
			params.bottomMargin=MAG*2;
			params.leftMargin=MAG;
			params.rightMargin=MAG*2;
			if (i==0) {
				params.topMargin=MAG*4;
			}else{
				params.topMargin=MAG*2;
			}
			img.setLayoutParams(params);
			rightLinearLayout.addView(img);
			
		}
		this.addView(leftLinearLayout);
		this.addView(rightLinearLayout);
	}

}
