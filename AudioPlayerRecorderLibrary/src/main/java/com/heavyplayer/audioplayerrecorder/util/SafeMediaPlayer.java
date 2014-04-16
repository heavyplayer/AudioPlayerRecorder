package com.heavyplayer.audioplayerrecorder.util;

import android.media.MediaPlayer;

public class SafeMediaPlayer extends MediaPlayer
		implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

	private OnPreparedListener mOnPreparedListener;
	private OnStartListener mOnStartListener;
	private OnCompletionListener mOnCompletionListener;
	private OnErrorListener mOnErrorListener;

	private State mState;
	private boolean mIsGoingToPlay;

	private Integer mCurrentPosition;
	private Integer mDuration;

	private enum State {
		CREATED, PREPARING, PREPARED, STARTED
	}

	public SafeMediaPlayer() {
		mState = State.CREATED;
		mIsGoingToPlay = false;

		mCurrentPosition = 0;
		mDuration = 100;

		super.setOnPreparedListener(this);
		super.setOnCompletionListener(this);
		super.setOnErrorListener(this);
	}

	@Override
	public void setOnPreparedListener(OnPreparedListener listener) {
		mOnPreparedListener = listener;
	}

	public void setOnStartListener(OnStartListener listener) {
		mOnStartListener = listener;
	}

	@Override
	public void setOnCompletionListener(OnCompletionListener listener) {
		mOnCompletionListener = listener;
	}

	@Override
	public void setOnErrorListener(OnErrorListener listener) {
		mOnErrorListener = listener;
	}

	public boolean isGoingToPlay() {
		return mIsGoingToPlay;
	}

	public boolean isPrepared() {
		return mState != State.CREATED;
	}

	protected boolean isPreparedInner() {
		return mState == State.PREPARED || mState == State.STARTED;
	}

	@Override
	public void prepare() throws IllegalStateException {
		prepareAsync();
	}

	@Override
	public void prepareAsync() throws IllegalStateException {
		mIsGoingToPlay = false;

		super.prepareAsync();
		mState = State.PREPARING;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mState = State.PREPARED;

		adjustCurrentPositionAndDuration();

		if(mOnPreparedListener != null)
			mOnPreparedListener.onPrepared(mp);

		if(mIsGoingToPlay)
			start();
	}

	@Override
	public void start() throws IllegalStateException {
		mIsGoingToPlay = true;

		if(isPreparedInner()) {
			final boolean isStarting = !isPlaying();
			super.start();
			mState = State.STARTED;
			mCurrentPosition = null;

			if(isStarting && mOnStartListener != null)
				mOnStartListener.onStart(this);
		}
	}

	@Override
	public void pause() throws IllegalStateException {
		mIsGoingToPlay = false;

		if(mState == State.STARTED)
			super.pause();
	}

	@Override
	public void seekTo(int msec) throws IllegalStateException {
		if(isPreparedInner()) {
			super.seekTo(msec);
			mCurrentPosition = null;
		}
		else
			mCurrentPosition = msec < 0 ? 0 : (msec > mDuration ? mDuration : msec);
	}

	@Override
	public void stop() throws IllegalStateException {
		mIsGoingToPlay = false;

		if(isPreparedInner()) {
			super.stop();
			mState = State.PREPARED;
		}
	}

	@Override
	public void reset() {
		super.reset();
		mCurrentPosition = 0;
		mDuration = 100;
		mState = State.CREATED;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		mIsGoingToPlay = false;

		mCurrentPosition = getDuration();

		if(mOnCompletionListener != null)
			mOnCompletionListener.onCompletion(mp);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// After an error, we'll need to prepare the media player again.
		reset();

		return mOnErrorListener != null && mOnErrorListener.onError(mp, what, extra);
	}

	@Override
	public int getCurrentPosition() {
		return mCurrentPosition != null ? mCurrentPosition : super.getCurrentPosition();
	}

	@Override
	public int getDuration() {
		return mDuration != null ? mDuration : super.getDuration();
	}

	private void adjustCurrentPositionAndDuration() {
		final float percent = getCurrentPosition() / (float)getDuration();
		final int duration = super.getDuration();
		if(mDuration != duration) {
			mDuration = duration;
			seekTo((int)(mDuration * percent));
		}
	}

	public static interface OnStartListener {
		public void onStart(MediaPlayer mp);
	}
}
