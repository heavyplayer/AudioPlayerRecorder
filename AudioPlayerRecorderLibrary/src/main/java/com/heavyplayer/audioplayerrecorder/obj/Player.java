package com.heavyplayer.audioplayerrecorder.obj;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import com.heavyplayer.audioplayerrecorder.widget.AudioPlayerLayout;
import com.heavyplayer.audioplayerrecorder.widget.PlayPauseImageButton;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Player implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener {
	public static final String TAG = Player.class.getSimpleName();

	private final static long PROGRESS_UPDATE_INTERVAL_MS = 200;

	private AudioManager mAudioManager;
	private AudioFocusChangeListener mAudioFocusChangeListener;

	private String mFilePath;

	private Handler mHandler;
	private ProgressUpdater mProgressUpdater;

	private Executor mExecutor;

	private MediaPlayer mMediaPlayer;

	private int mMax;
	private int mProgress;

	private boolean mIsPrepared;
	private boolean mIsPlaying;

	private AudioPlayerLayout mView;
	private PlayPauseImageButton mButton;
	private SeekBar mSeekBar;

	public Player(Context context, String fileName, Handler handler) {
		mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

		mFilePath = generateFilePath(fileName);

		mHandler = handler;
		mProgressUpdater = new ProgressUpdater();

		mExecutor = Executors.newSingleThreadExecutor();

		mMax = 100;
		mProgress = 0;

		mIsPrepared = false;
		mIsPlaying = false;
	}

	protected String generateFilePath(String fileName) {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
	}

	protected void start(boolean gainAudioFocus) {
		if(gainAudioFocus)
			gainAudioFocus();

		if(mMediaPlayer == null) {
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setOnBufferingUpdateListener(this);
			mMediaPlayer.setOnCompletionListener(this);
			try {
				mMediaPlayer.setDataSource(mFilePath);
				mMediaPlayer.prepare();
			}
			catch (IOException e) {
				Log.w(TAG, e);
				return;
			}

			adjustWithCurrentProgress();
		}

		mIsPrepared = true;
		mMediaPlayer.start();

		// Start updater.
		startSeekBarUpdate();
	}

	protected void adjustWithCurrentProgress() {
		final int duration = mMediaPlayer.getDuration();

		if(mMax != duration) {
			mProgress = (int)((mProgress / (float)mMax) * duration);
			mMax = duration;

			// Adjust seek bar scale.
			if(mSeekBar != null) {
				mSeekBar.setMax(mMax);
				mSeekBar.setProgress(mProgress);
			}
		}

		if(mProgress > 0 && mProgress < duration)
			dispatchSeekTo(mProgress);
	}

	protected void startSeekBarUpdate() {
		// Update seek bar.
		mHandler.removeCallbacks(mProgressUpdater);
		mHandler.post(mProgressUpdater);
	}

	protected void dispatchStart(final boolean gainAudioFocus, final boolean updateButton) {
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				start(gainAudioFocus);
				dispatchUpdateButton(updateButton, true);
			}
		});
	}

	protected void pause(boolean abandonAudioFocus) {
		// Minor hack to fix a bug where after an 'internal/external state mismatch corrected'
		// error it was no longer possible to pause the player.
		mMediaPlayer.start();

		mMediaPlayer.pause();

		if(abandonAudioFocus)
			abandonAudioFocus();
	}

	protected void dispatchPause(final boolean abandonAudioFocus, final boolean updateButton) {
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				pause(abandonAudioFocus);
				dispatchUpdateButton(updateButton, false);
			}
		});
	}

	protected void seekTo(int msec) {
		if(mMediaPlayer != null)
			mMediaPlayer.seekTo(msec);
	}

	protected void dispatchSeekTo(final int msec) {
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				seekTo(msec);
			}
		});
	}

	protected void updateButton(boolean isPlaying) {
		mIsPlaying = isPlaying;
		if(mButton != null)
			mButton.setIsPlaying(isPlaying);
	}

	protected void dispatchUpdateButton(boolean updateButton, final boolean isPlaying) {
		if(updateButton) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					updateButton(isPlaying);
				}
			});
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.i(TAG, "buffering percent: "+percent);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		mProgress = mp.getDuration();
		mMax = mp.getDuration();
		mIsPrepared = false;

		updateButton(false);

		abandonAudioFocus();
	}

	public void registerView(AudioPlayerLayout view) {
		mView = view;
		mView.setOnDetachListener(new AudioPlayerLayout.OnDetachListener() {
			@Override
			public void onStartTemporaryDetach(View v) {
				clearView();
			}
		});

		registerButton(view.getButton());

		registerSeekBar(view.getSeekBar());

		// Resume updater.
		startSeekBarUpdate();
	}

	protected void registerButton(PlayPauseImageButton button) {
		mButton = button;

		mButton.setOnPlayPauseListener(new PlayPauseImageButton.OnPlayPauseListener() {
			@Override
			public void onPlay(View v) {
				mIsPlaying = true;
				dispatchStart(true, false);
			}

			@Override
			public void onPause(View v) {
				mIsPlaying = false;
				dispatchPause(true, false);
			}
		});

		// Resume button state.
		mButton.setIsPlaying(mIsPlaying);
	}

	protected void registerSeekBar(SeekBar seekBar) {
		mSeekBar = seekBar;

		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(fromUser) {
					if(mIsPrepared)
						dispatchSeekTo(progress);
					else
						mProgress = progress;
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mHandler.removeCallbacks(mProgressUpdater);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mHandler.post(mProgressUpdater);
				dispatchSeekTo(seekBar.getProgress());
			}
		});

		// Resume progress.
		if(mIsPrepared) {
			mSeekBar.setMax(mMediaPlayer.getDuration());
			mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
		}
		else {
			mSeekBar.setMax(mMax);
			mSeekBar.setProgress(mProgress);
		}
	}

	protected void clearView() {
		mView.setOnDetachListener(null);
		mView = null;

		mButton.setOnClickListener(null);
		mButton = null;

		mSeekBar.setOnSeekBarChangeListener(null);
		mSeekBar = null;
	}

	public void onDestroy() {
		abandonAudioFocus();

		if(mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	protected void gainAudioFocus() {
		if(mAudioFocusChangeListener == null)
			mAudioFocusChangeListener = new AudioFocusChangeListener();

		// Request audio focus for playback
		mAudioManager.requestAudioFocus(
				mAudioFocusChangeListener,
				AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
	}

	protected void abandonAudioFocus() {
		// Abandon audio focus when playback complete.
		if(mAudioFocusChangeListener != null)
			mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
	}

	private class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
		@Override
		public void onAudioFocusChange(int focusChange) {
			switch(focusChange) {
				case AudioManager.AUDIOFOCUS_LOSS:
					dispatchPause(true, true);
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					dispatchPause(false, true);
					break;
				case AudioManager.AUDIOFOCUS_GAIN:
					dispatchStart(false, true);
					break;
			}
		}
	}

	protected class ProgressUpdater implements Runnable {
		@Override
		public void run() {
			if(mSeekBar != null && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
				mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
				mHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS);
			}
		}
	}
}
