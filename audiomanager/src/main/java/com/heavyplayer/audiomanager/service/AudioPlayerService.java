package com.heavyplayer.audiomanager.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import com.heavyplayer.audiomanager.R;
import com.heavyplayer.audiomanager.widget.AudioPlayer;
import com.heavyplayer.audiomanager.widget.PlayPauseImageButton;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AudioPlayerService extends Service implements
		MediaPlayer.OnPreparedListener,
		MediaPlayer.OnBufferingUpdateListener,
		MediaPlayer.OnCompletionListener {
	public static final String TAG = AudioPlayerService.class.getSimpleName();

	private final static int UPDATE_INTERVAL_MS = 200;

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	private Handler mHandler;
	private Runnable mProgressUpdater = new ProgressUpdater();

	private MediaPlayer mPlayer;
	private Long mPlayingId;

	private Map<Long, AudioPlayer> mAudioPlayers = new HashMap<>(6);
	private Map<Long, ProgressInfo> mAudioPlayersProgress = new HashMap<>(6);

	@Override
	public void onCreate() {
		mHandler = new Handler();

		// Tell the user we started.
		Toast.makeText(this, R.string.local_service_started, Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	protected void startPlay(long id, String fileName) {
		if(mPlayingId == null || mPlayingId != id) {
			if(mPlayer == null) {
				mPlayer = new MediaPlayer();
				mPlayer.setOnPreparedListener(this);
				mPlayer.setOnBufferingUpdateListener(this);
				mPlayer.setOnCompletionListener(this);
			}
			else {
				mHandler.removeCallbacks(mProgressUpdater);

				if(mPlayingId != null && mPlayer.isPlaying()) {
					// Before changing player source, stop the previous player if it is playing.
					final ProgressInfo progressInfo = getProgressInfo(mPlayingId);
					progressInfo.setProgress(mPlayer.getCurrentPosition());

					final AudioPlayer audioPlayer = mAudioPlayers.get(mPlayingId);
					if(audioPlayer != null) {
						final PlayPauseImageButton button = audioPlayer.getButton();
						button.setPlayPause(false);
					}
				}

				mPlayer.reset();
			}

			try {
				mPlayingId = id;
				mPlayer.setDataSource(generateFilePath(fileName));
				mPlayer.prepareAsync();
			} catch (IOException | IllegalStateException e) {
				Log.w(TAG, e);
			}
		}
		else {
			mPlayer.start();

			final AudioPlayer audioPlayer = mAudioPlayers.get(id);
			if(audioPlayer != null)
				mHandler.post(mProgressUpdater);
		}
	}

	protected String generateFilePath(String fileName) {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
	}

	protected void pausePlay(long id) {
		if(mPlayingId == id) {
			// Minor hack to fix a bug where after an 'internal/external state mismatch corrected'
			// error it was no longer possible to pause the player.
			mPlayer.start();

			// Pause the player.
			mPlayer.pause();
		}
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		final Long id = mPlayingId;
		if(id != null) {
			mp.start();

			final ProgressInfo progressInfo = getProgressInfo(id);
			final int duration = mp.getDuration();

			// Adjust current progress with the definitive duration.
			final boolean progressAdjusted = adjustProgressInfo(progressInfo, duration);

			final int progress = progressInfo.getProgress();

			final AudioPlayer audioPlayer = mAudioPlayers.get(id);
			if(audioPlayer != null) {
				if(progressAdjusted) {
					final SeekBar seekBar = audioPlayer.getSeekBar();
					seekBar.setMax(duration);
					seekBar.setProgress(progress);
				}

				final PlayPauseImageButton button = audioPlayer.getButton();
				button.setPlayPause(true);

				mHandler.post(mProgressUpdater);
			}


			if(progress > 0 && progress < duration) {
				// Resume progress.
				mp.seekTo(progress);
			}
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		final AudioPlayer audioPlayer = mAudioPlayers.get(mPlayingId);
		if(audioPlayer != null)
			mHandler.post(mProgressUpdater);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		final int duration = mp.getDuration();

		final ProgressInfo progressInfo = getProgressInfo(mPlayingId);
		progressInfo.setProgress(duration);

		final AudioPlayer audioPlayer = mAudioPlayers.get(mPlayingId);
		if(audioPlayer != null) {
			final SeekBar seekBar = audioPlayer.getSeekBar();
			seekBar.setProgress(duration);

			final PlayPauseImageButton button = audioPlayer.getButton();
			button.setPlayPause(false);
		}
	}

	protected ProgressInfo getProgressInfo(long id) {
		ProgressInfo progressInfo = mAudioPlayersProgress.get(id);
		if(progressInfo == null) {
			progressInfo = new ProgressInfo();
			mAudioPlayersProgress.put(id, progressInfo);
		}
		return progressInfo;
	}

	protected boolean adjustProgressInfo(ProgressInfo progressInfo, int duration) {
		final int max = progressInfo.getMax();
		final int progress = progressInfo.getProgress();

		if(max != duration) {
			// Update progress info.
			progressInfo.setMax(duration);
			final float percent = progress / (float)max;
			progressInfo.setProgress((int)(percent * duration));

			return true;
		}

		return false;
	}

	protected void addAudioPlayer(final AudioPlayer audioPlayer, final long id, final String fileName) {
		mAudioPlayers.put(id, audioPlayer);

		// Whether or now the audio player is playing right now.
		final boolean isPlaying = mPlayingId != null && mPlayingId == id && mPlayer != null && mPlayer.isPlaying();

		final ProgressInfo progressInfo = getProgressInfo(id);
		if(isPlaying) {
			// If the player is playing, update progress before updating seek bar.
			progressInfo.setMax(mPlayer.getDuration());
			progressInfo.setProgress(mPlayer.getCurrentPosition());
		}

		final PlayPauseImageButton button = audioPlayer.getButton();
		button.setOnPlausePauseListener(new PlayPauseImageButton.OnPlayPauseListener() {
			@Override
			public void onPlay(View v) {
				startPlay(id, fileName);
			}

			@Override
			public void onPause(View v) {
				pausePlay(id);
			}
		});
		button.setPlayPause(isPlaying);

		final SeekBar seekBar = audioPlayer.getSeekBar();
		seekBar.setMax(progressInfo.getMax());
		seekBar.setProgress(progressInfo.getProgress());
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(fromUser) {
					final ProgressInfo progressInfo = getProgressInfo(id);
					progressInfo.setProgress(progress);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				if(mPlayingId != null && mPlayingId == id)
					mHandler.removeCallbacks(mProgressUpdater);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if(mPlayingId != null && mPlayingId == id) {
					mPlayer.seekTo(seekBar.getProgress());
					mHandler.post(mProgressUpdater);
				}
			}
		});

		audioPlayer.setOnDetachListener(new AudioPlayer.OnDetachListener() {
			@Override
			public void onStartTemporaryDetach(View v) {
				removeAudioPlayer(id);
			}
		});

		// Finally, if the player is playing, resume the progress updater.
		if(isPlaying)
			mHandler.post(mProgressUpdater);
	}

	protected void removeAudioPlayer(long id) {
		final AudioPlayer audioPlayer = mAudioPlayers.remove(id);
		if(audioPlayer != null) {
			audioPlayer.getButton().setOnClickListener(null);
			audioPlayer.getSeekBar().setOnSeekBarChangeListener(null);
			audioPlayer.setOnDetachListener(null);
		}
	}

	@Override
	public void onDestroy() {
		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();

		if(mPlayer != null) {
			mPlayer.stop();
			mPlayer.reset();
			mPlayer.release();
			mPlayer = null;
		}
	}

	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class LocalBinder extends Binder {
		public void registerAudioPlayer(AudioPlayer audioPlayer, long id, String fileName) {
			addAudioPlayer(audioPlayer, id, fileName);
		}
	}

	private class ProgressUpdater implements Runnable {
		@Override
		public void run() {
			if(mPlayer != null && mPlayer.isPlaying() && mPlayingId != null) {
				final ProgressInfo progressInfo = getProgressInfo(mPlayingId);
				progressInfo.setProgress(mPlayer.getCurrentPosition());

				final AudioPlayer audioPlayer = mAudioPlayers.get(mPlayingId);
				if(audioPlayer != null) {
					final SeekBar seekBar = audioPlayer.getSeekBar();
					seekBar.setProgress(mPlayer.getCurrentPosition());

					// If there is an active audio player, keep updating it.
					mHandler.postDelayed(this, UPDATE_INTERVAL_MS);
				}
			}
		}
	}

	private class ProgressInfo {
		private int mMax;
		private int mProgress;

		public ProgressInfo() {
			mMax = 100;
			mProgress = 0;
		}

		public void setMax(int max) {
			mMax = max;
		}

		public int getMax() {
			return mMax;
		}

		public void setProgress(int progress) {
			mProgress = progress;
		}

		public int getProgress() {
			return mProgress;
		}
	}
}
