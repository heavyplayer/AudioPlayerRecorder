package com.audiomanager.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
import com.audiomanager.app.R;
import com.audiomanager.widget.AudioPlayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AudioPlayerService extends Service implements
		MediaPlayer.OnPreparedListener,
		MediaPlayer.OnSeekCompleteListener,
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
		mPlayingId = id;

		if(mPlayer == null) {
			mPlayer = new MediaPlayer();
			mPlayer.setOnBufferingUpdateListener(this);
			mPlayer.setOnCompletionListener(this);
			mPlayer.setOnPreparedListener(this);
			mPlayer.setOnSeekCompleteListener(this);
		}
		else {
			mHandler.removeCallbacks(mProgressUpdater);
			mPlayer.reset();
		}

		try {
			mPlayer.setDataSource(generateInputFilePath(fileName));
			mPlayer.prepare();
		} catch (IOException | IllegalStateException e) {
			Log.w(TAG, e);
		}
	}

	protected String generateInputFilePath(String fileName) {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		final Long id = mPlayingId;
		if(id != null) {
			final AudioPlayer audioPlayer = mAudioPlayers.get(id);
			if (audioPlayer != null) {
				final SeekBar seekBar = audioPlayer.getSeekBar();
				adjustSeekBarProgressToDuration(seekBar, mp.getDuration());

				final ProgressInfo progressInfo = getProgressInfo(id);
				final int progress = progressInfo.getProgress();
				if(progress == 0) {
					mp.start();
					mHandler.post(mProgressUpdater);
				}
				else if(progress < mp.getDuration()) {
					mp.seekTo(seekBar.getProgress());
				}
			}
		}
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		mp.start();
		mHandler.post(mProgressUpdater);
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		mHandler.post(mProgressUpdater);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		mHandler.removeCallbacks(mProgressUpdater);

		final AudioPlayer audioPlayer = mAudioPlayers.get(mPlayingId);
		if(audioPlayer != null) {
			final SeekBar seekBar = audioPlayer.getSeekBar();
			seekBar.setProgress(seekBar.getMax()); // This will indirectly call updateProgressInfo().
		}
	}

	protected void adjustSeekBarProgressToDuration(SeekBar seekBar, int duration) {
		if(seekBar.getMax() != duration) {
			final float percent = seekBar.getProgress() / (float)seekBar.getMax();
			final int progress = (int)(percent * duration);

			// Update SeekBar.
			seekBar.setMax(duration);
			seekBar.setProgress(progress); // This will indirectly call updateProgressInfo().
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

	protected ProgressInfo updateProgressInfo(long id, SeekBar seekBar) {
		final ProgressInfo progressInfo = getProgressInfo(id);

		progressInfo.setMax(seekBar.getMax());
		progressInfo.setProgress(seekBar.getProgress());

		return progressInfo;
	}

	protected void resumeProgress(AudioPlayer audioPlayer, long id) {
		final ProgressInfo progressInfo = getProgressInfo(id);
		final SeekBar seekBar = audioPlayer.getSeekBar();
		seekBar.setMax(progressInfo.getMax());
		seekBar.setProgress(progressInfo.getProgress());
	}

	protected void addAudioPlayer(final AudioPlayer audioPlayer, final long id, final String fileName) {
		mAudioPlayers.put(id, audioPlayer);

		resumeProgress(audioPlayer, id);

		final Button button = audioPlayer.getButton();
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startPlay(id, fileName);
			}
		});

		final SeekBar seekBar = audioPlayer.getSeekBar();
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				updateProgressInfo(id, seekBar);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				if(mPlayingId != null && mPlayingId == id)
					mHandler.removeCallbacks(mProgressUpdater);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if(mPlayingId != null && mPlayingId == id)
					mPlayer.seekTo(seekBar.getProgress());
			}
		});

		audioPlayer.setOnDetachListener(new AudioPlayer.OnDetachListener() {
			@Override
			public void onStartTemporaryDetach(View v) {
				removeAudioPlayer(id);
			}
		});

		if(mPlayingId != null && mPlayingId == id)
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
				final AudioPlayer audioPlayer = mAudioPlayers.get(mPlayingId);
				if(audioPlayer != null) {
					final SeekBar seekBar = audioPlayer.getSeekBar();
					seekBar.setProgress(mPlayer.getCurrentPosition()); // This will indirectly call updateProgressInfo().
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
