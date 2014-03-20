package com.audiomanager.widget;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class AudioPlayerSeekBar extends SeekBar {
	private final static int UPDATE_INTERVAL = 200;

	private MediaPlayer mMediaPlayer;

	public AudioPlayerSeekBar(Context context) {
		super(context);
	}

	public AudioPlayerSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AudioPlayerSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
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
			postDelayed(new Cena(), UPDATE_INTERVAL);
		}
	}

	private class Cena implements Runnable {
		@Override
		public void run() {
			if(mMediaPlayer != null) {
				setProgress(getProgress() + 1);
				postDelayed(this, UPDATE_INTERVAL);
			}
		}
	}
}
