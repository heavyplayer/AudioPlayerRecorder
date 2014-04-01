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

	private int mCurrentPosition;
	private int mDuration;

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

	@Override
	public void prepare() throws IllegalStateException {
		prepareAsync();
	}

	@Override
	public void prepareAsync() throws IllegalStateException {
		super.prepareAsync();
		mIsGoingToPlay = false;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mState = State.PREPARED;

		adjustCurrentPositionToDuration();

		if(mOnPreparedListener != null)
			mOnPreparedListener.onPrepared(mp);

		if(mIsGoingToPlay)
			start();
	}

	@Override
	public void start() throws IllegalStateException {
		mIsGoingToPlay = true;

		if(mState != State.CREATED) {
			final boolean isStarting = !isPlaying();
			super.start();
			mState = State.STARTED;

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
		if(mState == State.STARTED)
			super.seekTo(msec);
		else
			mCurrentPosition = msec < 0 ? 0 : (msec > mDuration ? mDuration : msec);
	}

	@Override
	public void stop() throws IllegalStateException {
		mIsGoingToPlay = false;

		if(mState != State.CREATED) {
			super.stop();
			mCurrentPosition = 0;
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
		return mState == State.STARTED ? super.getCurrentPosition() : mCurrentPosition;
	}

	@Override
	public int getDuration() {
		return mState == State.STARTED ? super.getDuration() : mDuration;
	}

	private void adjustCurrentPositionToDuration() {
		final int duration = super.getDuration();
		if(mDuration != duration) {
			mCurrentPosition = (int)((mCurrentPosition / (float)mDuration) * duration);
			mDuration = duration;

			final int currentPosition = super.getCurrentPosition();
			if(mCurrentPosition != currentPosition)
				super.seekTo(mCurrentPosition);
		}
	}

	public static interface OnStartListener {
		public void onStart(MediaPlayer mp);
	}
}
