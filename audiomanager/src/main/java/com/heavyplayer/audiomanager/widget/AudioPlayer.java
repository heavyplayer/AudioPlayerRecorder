package com.heavyplayer.audiomanager.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import com.heavyplayer.audiomanager.R;

public class AudioPlayer extends LinearLayout {
	public OnDetachListener mOnDetachListener;

	private PlayPauseImageButton mButton;
	private SeekBar mSeekBar;

	public AudioPlayer(Context context) {
		super(context);
		init(context, null);
	}

	public AudioPlayer(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public AudioPlayer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		setOrientation(HORIZONTAL);
		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS); // Enhance compatibility with ListView.

		int playResId = 0;
		int pauseResId = 0;
		if(attrs != null) {
			final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AudioPlayer);
			if(ta != null) {
				try {
					playResId = ta.getResourceId(R.styleable.AudioPlayer_playSrc, 0);
					pauseResId = ta.getResourceId(R.styleable.AudioPlayer_pauseSrc, 0);
				} finally {
					ta.recycle();
				}
			}
		}

		mButton = new PlayPauseImageButton(context);
		if(playResId != 0)
			mButton.setPlayDrawableResource(playResId);
		if(pauseResId != 0)
			mButton.setPauseDrawableResource(pauseResId);
		final LayoutParams buttonParams =
				new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		mSeekBar = new SeekBar(context);
		final LayoutParams seekBarParams =
				new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

		addView(mButton, buttonParams);
		addView(mSeekBar, seekBarParams);
	}

	public PlayPauseImageButton getButton() {
		return mButton;
	}

	public SeekBar getSeekBar() {
		return mSeekBar;
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

	@Override
	protected void dispatchSaveInstanceState(SparseArray container) {
		super.dispatchFreezeSelfOnly(container);
	}

	@Override
	protected void dispatchRestoreInstanceState(SparseArray container) {
		super.dispatchThawSelfOnly(container);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final SavedState ss = new SavedState(super.onSaveInstanceState());
		ss.buttonSavedState = mButton.onSaveInstanceState();
		ss.seekBarSavedState = mSeekBar.onSaveInstanceState();
		return ss;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if(!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		final SavedState ss = (SavedState)state;
		super.onRestoreInstanceState(ss.getSuperState());
		mButton.onRestoreInstanceState(ss.buttonSavedState);
		mSeekBar.onRestoreInstanceState(ss.seekBarSavedState);
	}

	static class SavedState extends BaseSavedState {
		Parcelable buttonSavedState;
		Parcelable seekBarSavedState;

		public SavedState(Parcel source) {
			super(source);
			buttonSavedState = source.readParcelable(SavedState.class.getClassLoader());
			seekBarSavedState = source.readParcelable(SavedState.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeParcelable(buttonSavedState, 0);
			dest.writeParcelable(seekBarSavedState, 0);
		}

		public SavedState(Parcelable superState) {
			super(superState);
		}

		public final static Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel source) {
				return new SavedState(source);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[0];
			}
		};
	}

	public interface OnDetachListener {
		public void onStartTemporaryDetach(View v);
	}
}
