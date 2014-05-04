package com.heavyplayer.audioplayerrecorder.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import com.heavyplayer.audioplayerrecorder.R;
import com.heavyplayer.audioplayerrecorder.widget.interface_.OnDetachListener;

public class AudioPlayerLayout extends ViewGroup {
	private static final int SECOND_MILLIS = 1000;
	private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
	private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;

	public OnDetachListener mOnDetachListener;

	private PlayPauseImageButton mButton;
	private SeekBar mSeekBar;
	private TextView mTimeCurrentPositionTextView;
	private TextView mTimeDurationTextView;

	// Attributes.
	private Integer mPlayResId;
	private Integer mPauseResId;
	private Integer mButtonWidth;
	private Integer mButtonHeight;
	private Integer mButtonBackgroundResId;
	private Integer mSeekBarMarginLeft;
	private Integer mSeekBarMarginRight;
	private ColorStateList mTimeCurrentPositionColor;
	private ColorStateList mTimeDurationColor;
	private Integer mMaxWidth;

	// Time variables.
	private int mTimeCurrentPosition = -1;
	private int mTimeDuration = -1;

	private boolean mIsPlaying = false;

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

		if(attrs != null) {
			final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AudioPlayerLayout);
			if(ta != null) {
				try {
					mPlayResId = getResourceId(ta, R.styleable.AudioPlayerLayout_playSrc);
					mPauseResId = getResourceId(ta, R.styleable.AudioPlayerLayout_pauseSrc);
					mButtonWidth = getDimensionPixelSize(ta, R.styleable.AudioPlayerLayout_buttonWidth);
					mButtonHeight = getDimensionPixelSize(ta, R.styleable.AudioPlayerLayout_buttonHeight);
					mButtonBackgroundResId = getResourceId(ta, R.styleable.AudioPlayerLayout_buttonBackground);
					mSeekBarMarginLeft = getDimensionPixelSize(ta, R.styleable.AudioPlayerLayout_seekBarMarginLeft);
					mSeekBarMarginRight = getDimensionPixelSize(ta, R.styleable.AudioPlayerLayout_seekBarMarginRight);
					mTimeCurrentPositionColor = getColorStateList(ta, R.styleable.AudioPlayerLayout_timeCurrentPositionColor);
					mTimeDurationColor = getColorStateList(ta, R.styleable.AudioPlayerLayout_timeDurationColor);
					mMaxWidth = getDimensionPixelSize(ta, R.styleable.AudioPlayerLayout_android_maxWidth);
				} finally {
					ta.recycle();
				}
			}
		}
	}

	private Integer getResourceId(TypedArray ta, int index) {
		return ta.hasValue(index) ? ta.getResourceId(index, 0) : null;
	}

	private Integer getDimensionPixelSize(TypedArray ta, int index) {
		return ta.hasValue(index) ? ta.getDimensionPixelSize(index, 0) : null;
	}

	private ColorStateList getColorStateList(TypedArray ta, int index) {
		return ta.hasValue(index) ? ta.getColorStateList(index) : null;
	}

	@Override
	public void addView(View child, int index, LayoutParams params) {
		final int id = child.getId();
		switch(id) {
			case android.R.id.button1:
				if(!(child instanceof PlayPauseImageButton))
					throw new IllegalStateException("View with id android.R.id.button1 must extend PlayPauseImageButton.");
				mButton = (PlayPauseImageButton)child;

				if(mButtonWidth != null || mButtonHeight != null) {
					if(params == null)
						params = new LayoutParams(
								mButtonWidth != null ? mButtonWidth : LayoutParams.WRAP_CONTENT,
								mButtonHeight!= null ? mButtonHeight : LayoutParams.WRAP_CONTENT);
					else {
						if(mButtonWidth != null)
							params.width = mButtonWidth;
						if(mButtonHeight != null)
							params.height = mButtonHeight;
					}
				}

				// Configure button.
				if(mPlayResId != null)
					mButton.setPlayDrawableResource(mPlayResId);
				if(mPauseResId != null)
					mButton.setPauseDrawableResource(mPauseResId);
				if(mButtonBackgroundResId != null) {
					mButton.setPadding(0, 0, 0, 0); // Remove image button padding, before setting the background.
					mButton.setBackgroundResource(mButtonBackgroundResId);
				}

				break;

			case android.R.id.progress:
				if(!(child instanceof SeekBar))
					throw new IllegalStateException("View with id android.R.id.progress must extend SeekBar.");
				mSeekBar = (SeekBar)child;

				// Configure seek bar layout params.
				if(!(params instanceof MarginLayoutParams)) {
					params = params == null ?
							new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT) :
							new MarginLayoutParams(params);
				}
				if(mSeekBarMarginLeft != null)
					((MarginLayoutParams)params).leftMargin = mSeekBarMarginLeft;
				if(mSeekBarMarginRight != null)
					((MarginLayoutParams)params).rightMargin = mSeekBarMarginRight;

				break;

			case android.R.id.text1:
				if(!(child instanceof TextView))
					throw new IllegalStateException("View with android.R.id.text1 must extend TextView.");
				mTimeCurrentPositionTextView = (TextView)child;
				mTimeCurrentPositionTextView.setIncludeFontPadding(false);
				if(mTimeCurrentPositionColor != null)
					mTimeCurrentPositionTextView.setTextColor(mTimeCurrentPositionColor);
				break;

			case android.R.id.text2:
				if(!(child instanceof TextView))
					throw new IllegalStateException("View with android.R.id.text2 must extend TextView.");
				mTimeDurationTextView = (TextView)child;
				mTimeDurationTextView.setIncludeFontPadding(false);
				if(mTimeDurationColor != null)
					mTimeDurationTextView.setTextColor(mTimeDurationColor);
				break;

			default:
				break;
		}

		super.addView(child, index, params);
	}

	@Override
	protected void onFinishInflate() {
		final Context context = getContext();

		if(context != null) {
			if(findViewById(android.R.id.button1) == null) {
				final PlayPauseImageButton button = new PlayPauseImageButton(context);
				button.setId(android.R.id.button1);
				addView(button);
			}

			if(findViewById(android.R.id.progress) == null) {
				final SeekBar seekBar = new SeekBar(context);
				seekBar.setId(android.R.id.progress);


				final MarginLayoutParams seekBarParams =
						new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				final Resources resources = getResources();
				// Default margins.
				if(resources != null) {
					seekBarParams.leftMargin = resources.getDimensionPixelSize(R.dimen.apl_seek_bar_margin_left);
					seekBarParams.rightMargin = resources.getDimensionPixelSize(R.dimen.apl_seek_bar_margin_right);
				}

				addView(seekBar, seekBarParams);
			}

			if(findViewById(android.R.id.text1) == null) {
				final TextView currentPositionTextView = new TextView(context);
				currentPositionTextView.setId(android.R.id.text1);
				addView(currentPositionTextView);
			}

			if(findViewById(android.R.id.text2) == null) {
				final TextView durationTextView = new TextView(context);
				durationTextView.setId(android.R.id.text2);
				addView(durationTextView);
			}
		}

		// Init time.
		setTimeDuration(0);
		setTimeCurrentPosition(0);

		super.onFinishInflate();
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

		// Measure current position text view.
		measureChild(mTimeCurrentPositionTextView, widthMeasureSpec, heightMeasureSpec);

		// Measure duration text view.
		measureChild(mTimeDurationTextView, widthMeasureSpec, heightMeasureSpec);

		// Measure seek bar.
		final int availableWidth = mMaxWidth != null ?
				Math.min(mMaxWidth, MeasureSpec.getSize(widthMeasureSpec)) :
				MeasureSpec.getSize(widthMeasureSpec);

		final int remainingWidth =
				availableWidth -
				mButton.getMeasuredWidth() -
				mTimeCurrentPositionTextView.getMeasuredWidth() -
				mTimeDurationTextView.getMeasuredWidth();

		final int seekBarWidth = Math.max(remainingWidth, 0);
		final int seekBarWidthMeasureSpec = MeasureSpec.makeMeasureSpec(seekBarWidth, MeasureSpec.EXACTLY);
		measureChildWithMargins(mSeekBar, seekBarWidthMeasureSpec, 0, heightMeasureSpec, 0);


		// Calculate max width and height, taking padding into account.

		final int measuredWidth =
				mButton.getMeasuredWidth() +
				mSeekBar.getMeasuredWidth() +
				mTimeCurrentPositionTextView.getMeasuredWidth() +
				mTimeDurationTextView.getMeasuredWidth() +
				getPaddingLeft() + getPaddingRight();

		final int measuredHeight =
				Math.max(
					Math.max(mButton.getMeasuredHeight(), mSeekBar.getMeasuredHeight()),
					Math.max(mTimeCurrentPositionTextView.getMeasuredHeight(), mTimeDurationTextView.getMeasuredHeight())) +
				getPaddingTop() + getPaddingBottom();


		// Resolve width and height.

		final int width = mMaxWidth != null ?
				Math.min(mMaxWidth, resolveSize(measuredWidth, widthMeasureSpec)) :
				resolveSize(measuredWidth, widthMeasureSpec);

		final int height = resolveSize(measuredHeight, heightMeasureSpec);

		setMeasuredDimension(width, height);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// Layout button.
		int left = getPaddingLeft();
		layoutChild(mButton, left, t, b);

		// Layout seek bar.
		final MarginLayoutParams seekBarParams = (MarginLayoutParams)mSeekBar.getLayoutParams();
		final int seekBarMarginLeft = seekBarParams != null ? seekBarParams.leftMargin : 0;
		final int seekBarMarginRight = seekBarParams != null ? seekBarParams.rightMargin : 0;
		left += mButton.getMeasuredWidth() + seekBarMarginLeft;
		layoutChild(mSeekBar, left, t, b);

		// Layout current position text view.
		left += mSeekBar.getMeasuredWidth() + seekBarMarginRight;
		layoutChild(mTimeCurrentPositionTextView, left, t, b);

		// Layout current position text view.
		left += mTimeCurrentPositionTextView.getMeasuredWidth();
		layoutChild(mTimeDurationTextView, left, t, b);
	}

	protected void layoutChild(View child, int l, int t, int b) {
		final int height = b - t;
		final int measuredHeight = child.getMeasuredHeight();
		final int nt = (int)((height - measuredHeight) / 2 + .5f);
		child.layout(l, nt, l + child.getMeasuredWidth(), nt + measuredHeight);
	}

	public void setTimeCurrentPosition(int currentPosition) {
		if(mTimeCurrentPosition != currentPosition) {
			mTimeCurrentPosition = currentPosition;

			if(mTimeCurrentPositionTextView != null)
				mTimeCurrentPositionTextView.setText(millisToTimeString(mTimeCurrentPosition));
		}
	}

	public void setTimeDuration(int duration) {
		if(mTimeDuration != duration) {
			// Update length current position if it needs to include or exclude hours
			// depending on the duration.
			final boolean updateCurrentPosition = hasHours(mTimeDuration) != hasHours(duration);

			// Update time duration before updating current position.
			// It is used to include or exclude hours.
			mTimeDuration = duration;

			if(updateCurrentPosition)
				setTimeCurrentPosition(mTimeCurrentPosition);

			if(mTimeDurationTextView != null)
				mTimeDurationTextView.setText(String.format(" / %s", millisToTimeString(mTimeDuration)));
		}
	}

	private String millisToTimeString(long millis) {
		final long seconds = (millis / SECOND_MILLIS) % 60 ;
		final long minutes = ((millis / MINUTE_MILLIS) % 60);
		final long hours = ((millis / HOUR_MILLIS) % 24);

		return hasHours(mTimeDuration) ?
				String.format("%02d:%02d:%02d", hours, minutes, seconds) :
				String.format("%02d:%02d", minutes, seconds);
	}

	public void setIsPlaying(boolean isPlaying) {
		if(mIsPlaying != isPlaying) {
			mIsPlaying = isPlaying;

			if(mTimeCurrentPositionTextView != null)
				mTimeCurrentPositionTextView.setSelected(isPlaying);

			if(mTimeDurationTextView != null)
				mTimeDurationTextView.setSelected(isPlaying);
		}
	}

	private boolean hasHours(long millis) {
		return millis >= HOUR_MILLIS;
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
		ss.timeCurrentPosition = mTimeCurrentPosition;
		ss.timeDuration = mTimeDuration;
		ss.isPlaying = mIsPlaying;
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
		// Update time.
		setTimeDuration(ss.timeDuration);
		setTimeCurrentPosition(ss.timeCurrentPosition);
		setIsPlaying(ss.isPlaying);
	}

	static class SavedState extends BaseSavedState {
		Parcelable buttonSavedState;
		Parcelable seekBarSavedState;
		int timeCurrentPosition;
		int timeDuration;
		boolean isPlaying;

		public SavedState(Parcel source) {
			super(source);
			buttonSavedState = source.readParcelable(SavedState.class.getClassLoader());
			seekBarSavedState = source.readParcelable(SavedState.class.getClassLoader());
			timeCurrentPosition = source.readInt();
			timeDuration = source.readInt();
			isPlaying = source.readInt() == 1;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeParcelable(buttonSavedState, 0);
			dest.writeParcelable(seekBarSavedState, 0);
			dest.writeInt(timeCurrentPosition);
			dest.writeInt(timeDuration);
			dest.writeInt(isPlaying ? 1 : 0);
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
