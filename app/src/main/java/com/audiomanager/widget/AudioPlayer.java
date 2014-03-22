package com.audiomanager.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class AudioPlayer extends LinearLayout {
	public OnDetachListener mOnDetachListener;

	public AudioPlayer(Context context) {
		super(context);
		init(context);
	}

	public AudioPlayer(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public AudioPlayer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		setOrientation(HORIZONTAL);
		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS); // Enhance compatibility with ListView.

		final Button button = new Button(context);
		button.setText("Play");
		final LayoutParams buttonParams =
				new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		final SeekBar seekBar = new SeekBar(context);
		final LayoutParams seekBarParams =
				new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

		addView(button, buttonParams);
		addView(seekBar, seekBarParams);
	}

	public Button getButton() {
		return (Button)getChildAt(0);
	}

	public SeekBar getSeekBar() {
		return (SeekBar)getChildAt(1);
	}

	@Override
	public void onStartTemporaryDetach() {
		super.onStartTemporaryDetach();

		if(mOnDetachListener != null)
			mOnDetachListener.onStartTemporaryDetach(this);
	}

	public void setOnDetachListener(OnDetachListener listener) {
		mOnDetachListener = listener;
	}

	public interface OnDetachListener {
		public void onStartTemporaryDetach(View v);
	}
}
