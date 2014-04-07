package com.heavyplayer.audioplayerrecorder.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import com.heavyplayer.audioplayerrecorder.R;
import com.heavyplayer.audioplayerrecorder.widget.interface_.OnDetachListener;

public class AudioPlayerLayout extends ViewGroup {
	public OnDetachListener mOnDetachListener;

	private PlayPauseImageButton mButton;
	private SeekBar mSeekBar;

	public AudioPlayerLayout(Context context) {
		super(context);
		init(context, null);
	}

	public AudioPlayerLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public AudioPlayerLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS); // Enhance compatibility with ListView.

		// Default values.
		int playResId = 0;
		int pauseResId = 0;
		int buttonWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
		int buttonHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
		int buttonBackgroundResId = 0;
		int seekBarMarginLeftResId = R.dimen.apl_seek_bar_margin_left;

		if(attrs != null) {
			final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AudioPlayerLayout);
			if(ta != null) {
				try {
					playResId = ta.getResourceId(R.styleable.AudioPlayerLayout_playSrc, playResId);
					pauseResId = ta.getResourceId(R.styleable.AudioPlayerLayout_pauseSrc, pauseResId);
					buttonWidth = ta.getDimensionPixelSize(R.styleable.AudioPlayerLayout_buttonWidth, buttonWidth);
					buttonHeight = ta.getDimensionPixelSize(R.styleable.AudioPlayerLayout_buttonHeight, buttonHeight);
					buttonBackgroundResId = ta.getResourceId(
							R.styleable.AudioPlayerLayout_buttonBackground,
							buttonBackgroundResId);
					seekBarMarginLeftResId = ta.getResourceId(
							R.styleable.AudioPlayerLayout_seekBarMarginLeft,
							R.dimen.apl_seek_bar_margin_left);
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
		mButton.setPadding(0, 0, 0, 0); // Remove image button padding, before setting the background.
		if(buttonBackgroundResId != 0)
			mButton.setBackgroundResource(buttonBackgroundResId);
		final LayoutParams buttonParams = new LayoutParams(buttonWidth, buttonHeight);

		mSeekBar = new SeekBar(context);
		final MarginLayoutParams seekBarParams =
				new MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		final Resources resources = getResources();
		if(resources != null)
			seekBarParams.leftMargin = resources.getDimensionPixelSize(seekBarMarginLeftResId);

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
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Measure button.
		measureChild(mButton, widthMeasureSpec, heightMeasureSpec);

		// Measure seek bar.
		final int remainingWidth = MeasureSpec.getSize(widthMeasureSpec) - mButton.getMeasuredWidth();
		final int remainingWidthMeasureSpec = MeasureSpec.makeMeasureSpec(remainingWidth, MeasureSpec.EXACTLY);
		measureChildWithMargins(mSeekBar, remainingWidthMeasureSpec, 0, heightMeasureSpec, 0);

		// Calculate max width and height, taking padding into account.
		final int measuredWidth = mButton.getMeasuredWidth() + mSeekBar.getMeasuredWidth()
				+ getPaddingLeft() + getPaddingRight();
		final int measuredHeight = Math.max(mButton.getMeasuredHeight(), mSeekBar.getMeasuredHeight())
				+ getPaddingTop() + getPaddingBottom();

		setMeasuredDimension(
				resolveSize(measuredWidth, widthMeasureSpec),
				resolveSize(measuredHeight, heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// Layout button.
		int left = getPaddingLeft();
		layoutChild(mButton, left, t, b);

		// Layout seek bar.
		final MarginLayoutParams seekBarParams = (MarginLayoutParams)mSeekBar.getLayoutParams();
		final int seekBarLeftMargin = seekBarParams != null ? seekBarParams.leftMargin : 0;
		left += mButton.getMeasuredWidth() + seekBarLeftMargin;
		layoutChild(mSeekBar, left, t, b);
	}

	protected void layoutChild(View child, int l, int t, int b) {
		final int height = b - t;
		final int measuredHeight = child.getMeasuredHeight();
		final int nt = (int)((height - measuredHeight) / 2 + .5f);
		child.layout(l, nt, l + child.getMeasuredWidth(), nt + measuredHeight);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		if(mOnDetachListener != null)
			mOnDetachListener.onDetachedFromWindow(this);
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
}
