package com.audiomanager.widget;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class AudioPlayerSeekBar extends SeekBar implements
		SeekBar.OnSeekBarChangeListener,
		MediaPlayer.OnBufferingUpdateListener,
		MediaPlayer.OnCompletionListener {
	private final static int UPDATE_INTERVAL = 200;

	private MediaPlayer mMediaPlayer;

	private boolean mIsPlaying = false;
	private boolean mIsCompleted = false;

	private int mBufferProgress;

	private ProgressUpdater mProgressUpdate = new ProgressUpdater();

	public AudioPlayerSeekBar(Context context) {
		super(context);
		init();
	}

	public AudioPlayerSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public AudioPlayerSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setOnSeekBarChangeListener(this);
	}

	@Override
	public void onStartTemporaryDetach() {
		super.onStartTemporaryDetach();

		resetPlayerState();
	}

	public void resetPlayerState() {
		// Reset progress state.
		mMediaPlayer = null;
		setProgress(0);
	}

	public void setMediaPlayer(MediaPlayer mediaPlayer) {
		mMediaPlayer = mediaPlayer;

		if(mMediaPlayer != null) {
			setMax(mediaPlayer.getDuration());
			mMediaPlayer.setOnBufferingUpdateListener(this);
			mMediaPlayer.setOnCompletionListener(this);

			startPlay();
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		mBufferProgress = (int)((percent / 100.0f) * mp.getDuration()) - 10;

		if(!mIsPlaying)
			startPlay();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		stopPlay();
	}

	public void startPlay() {
		mIsCompleted = false;
		mIsPlaying = mMediaPlayer.isPlaying();

		if(mIsPlaying) {
			stopProgressUpdater();
			post(mProgressUpdate);
		}
	}

	public void stopPlay() {
		mIsCompleted = true;
		mIsPlaying = false;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		// Do nothing.
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		if(mIsPlaying)
			stopProgressUpdater();
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		if(mMediaPlayer != null) {
			mMediaPlayer.seekTo(getProgress());

			if(mIsPlaying)
				startProgressUpdater();
		}
	}

	protected void startProgressUpdater() {
		post(mProgressUpdate);
	}

	protected void stopProgressUpdater() {
		final Handler handler = getHandler();
		if(handler != null)
			handler.removeCallbacks(mProgressUpdate);
	}

	private class ProgressUpdater implements Runnable {
		@Override
		public void run() {
			if(mMediaPlayer != null) {
				if(mMediaPlayer.isPlaying()) {
					setProgress(mMediaPlayer.getCurrentPosition());
					setSecondaryProgress(mBufferProgress);
					postDelayed(this, UPDATE_INTERVAL);
				}
				else if(mIsCompleted) {
					setProgress(getMax());
				}
			}
		}
	}
}
