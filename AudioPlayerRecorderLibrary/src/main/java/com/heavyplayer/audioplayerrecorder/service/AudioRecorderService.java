package com.heavyplayer.audioplayerrecorder.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.heavyplayer.audioplayerrecorder.R;
import com.heavyplayer.audioplayerrecorder.util.BuildUtils;
import com.heavyplayer.audioplayerrecorder.widget.AudioRecorderMicrophone;
import com.heavyplayer.audioplayerrecorder.widget.interface_.OnDetachListener;

public class AudioRecorderService extends Service implements AudioManager.OnAudioFocusChangeListener {
	public static final String TAG = AudioRecorderService.class.getSimpleName();

	private final static int UPDATE_INTERVAL_MS = 100;

	private final IBinder mBinder = new LocalBinder();

	private Handler mHandler;

	private AudioRecorderMicrophone mMicrophone;
	private MicrophoneAmplitudeUpdater mMicrophoneAmplitudeUpdater = new MicrophoneAmplitudeUpdater();

	private AudioRecorderStateListener mStateListener;

	private Long mTimeLimit;
	private TimeLimitStopper mTimeLimitStopper = new TimeLimitStopper();

	private Uri mFileUri;

	private MediaRecorder mRecorder;
	private boolean mIsRecording;

	@Override
	public void onCreate() {
		mHandler = new Handler();

		mIsRecording = false;

		if(BuildUtils.isDebug(this))
			Toast.makeText(this, R.string.local_service_started, Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	protected void start(Uri fileUri) {
		// If the output file changes, we want to stop the current recording.
		if(mFileUri == null || !mFileUri.equals(fileUri)) {
			stop();
			mFileUri = fileUri;
		}

		if(!mIsRecording && mFileUri != null) {
			gainAudioFocus();

			if(mRecorder == null)
				mRecorder = new MediaRecorder();

			try {
				// Configure recorder.
				mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
				mRecorder.setOutputFile(mFileUri.getPath());
				mRecorder.setAudioEncoder(Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1 ?
						MediaRecorder.AudioEncoder.DEFAULT :
						MediaRecorder.AudioEncoder.AAC);
				mRecorder.setAudioChannels(1);
				mRecorder.setAudioEncodingBitRate(22050);

				mRecorder.prepare();

				// Start recording.
				mRecorder.start();

				mIsRecording = true;

				scheduleTimeLimitStopper();

				updateMicrophoneState();

				startMicrophoneUpdater();

				if(mStateListener != null)
					mStateListener.onStartRecorder();
			}
			catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	protected void stop() {
		if(mIsRecording) {
			if(mRecorder != null) {
				try {
					mRecorder.stop();
					mRecorder.reset();

					mIsRecording = false;

					removeTimeLimitStopper();

					updateMicrophoneState();

					if(mStateListener != null)
						mStateListener.onStopRecorder();
				}
				catch (Exception e) {
					Log.w(TAG, e);
				}
			}

			abandonAudioFocus();
		}
	}


	@Override
	public void onDestroy() {
		stop();
		if(mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}

		if(BuildUtils.isDebug(this))
			Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
	}

	protected void gainAudioFocus() {
		final int durationHint;
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			durationHint = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
		else
			// Request audio focus for recording without being disturbed by system sounds.
			durationHint = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;

		final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		audioManager.requestAudioFocus(
				this,
				AudioManager.STREAM_MUSIC,
				durationHint);
	}

	protected void abandonAudioFocus() {
		// Abandon audio focus when the recording complete.
		final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		audioManager.abandonAudioFocus(this);
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		// Do nothing.
	}

	protected void scheduleTimeLimitStopper() {
		if(mTimeLimit != null)
			mHandler.postDelayed(mTimeLimitStopper, mTimeLimit);
	}

	protected void removeTimeLimitStopper() {
		mHandler.removeCallbacks(mTimeLimitStopper);
	}

	protected void updateMicrophoneState() {
		if(mMicrophone != null) {
			mMicrophone.setSelected(mIsRecording);

			if(!mIsRecording)
				mMicrophone.updateAmplitude(0, UPDATE_INTERVAL_MS * 3);
		}
	}

	protected void startMicrophoneUpdater() {
		// Star updating microphones amplitude.
		mHandler.removeCallbacks(mMicrophoneAmplitudeUpdater);
		mHandler.post(mMicrophoneAmplitudeUpdater);
	}

	private class MicrophoneAmplitudeUpdater implements Runnable {
		@Override
		public void run() {
			if(mIsRecording && mRecorder != null && mMicrophone != null) {
				final int amplitude = mRecorder.getMaxAmplitude();

				mMicrophone.updateAmplitude(amplitude, UPDATE_INTERVAL_MS);

				// Post animation runnable to update the animation.
				mHandler.postDelayed(mMicrophoneAmplitudeUpdater, UPDATE_INTERVAL_MS);
			}
		}
	}

	/**
	 * Stops the recorder if the time limit is reached.
	 */
	public class TimeLimitStopper implements Runnable {
		@Override
		public void run() {
			stop();

			if(mStateListener != null)
				mStateListener.onTimeLimitExceeded();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class LocalBinder extends Binder {
		public void register(AudioRecorderMicrophone microphone, AudioRecorderStateListener listener) {
			mMicrophone = microphone;
			mStateListener = listener;

			// Configure microphone state.
			microphone.setSelected(mIsRecording);

			microphone.setOnDetachListener(new OnDetachListener() {
				@Override
				public void onDetachedFromWindow(View v) {
					if(mMicrophone == v)
						mMicrophone = null;
				}

				@Override
				public void onStartTemporaryDetach(View v) { }
			});

			// Start microphone update.
			startMicrophoneUpdater();
		}

		/**
		 * Time limit will apply the next time you call {@link #startRecorder(android.net.Uri)}.
		 */
		public void setTimeLimit(long timeLimit) {
			mTimeLimit = timeLimit;
		}

		public void startRecorder(Uri fileUri) {
			start(fileUri);
		}

		public void stopRecorder() {
			stop();
		}

		public boolean isRecording() {
			return mIsRecording;
		}
	}

	public static interface AudioRecorderStateListener {
		public void onStartRecorder();
		public void onStopRecorder();
		public void onTimeLimitExceeded();
	}
}
