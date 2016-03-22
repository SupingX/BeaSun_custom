package com.mycj.beasun.view;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class LineDrawable extends Drawable {

	private Paint mPaint;
	private int length;
	private int width;

	public LineDrawable(int length,int width) {
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		this.length = length;
		this.width = width;
	}

	@Override
	public void draw(Canvas canvas) {
//		canvas.drawLine(0, startY, stopX, stopY, paint);
	}

	@Override
	public void setAlpha(int alpha) {
		
	}

	@Override
	public void setColorFilter(ColorFilter cf) {

	}

	@Override
	public int getOpacity() {
		return 0;
	}

}
