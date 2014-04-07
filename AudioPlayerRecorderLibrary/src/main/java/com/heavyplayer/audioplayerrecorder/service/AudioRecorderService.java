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
import android.widget.Toast;
import com.heavyplayer.audioplayerrecorder.R;
import com.heavyplayer.audioplayerrecorder.widget.AudioRecorderMicrophone;

import java.util.HashSet;
import java.util.Set;

public class AudioRecorderService extends Service implements AudioManager.OnAudioFocusChangeListener {
	public static final String TAG = AudioRecorderService.class.getSimpleName();

	private final static int UPDATE_INTERVAL_MS = 100;

	public static final String EXTRA_FILE_URI = "extra_file_uri";

	private final IBinder mBinder = new LocalBinder();

	private Handler mHandler;
	private AmplitudeUpdater mAmplitudeUpdater = new AmplitudeUpdater();

	private Set<AudioRecorderMicrophone> mMicrophones = new HashSet<>(1);

	private Uri mFileUri;

	private MediaRecorder mRecorder;
	private boolean mIsRecording;

	public static Intent getLaunchIntent(Context context, Uri fileUri) {
		final Intent intent = new Intent(context, AudioRecorderService.class);
		intent.putExtra(EXTRA_FILE_URI, fileUri);
		return intent;
	}

	@Override
	public void onCreate() {
		mHandler = new Handler();

		mIsRecording = false;

		// Tell the user we started.
		Toast.makeText(this, R.string.local_service_started, Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final Uri fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);
		if(mFileUri != fileUri) {
			stop();
			mFileUri = fileUri;
			start();
		}

		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	protected void start() {
		if(!mIsRecording) {
			gainAudioFocus();

			if (mRecorder == null)
				mRecorder = new MediaRecorder();

			try {
				// Configure recorder.
				mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				mRecorder.setOutputFile(mFileUri.getPath());
				mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

				mRecorder.prepare();

				// Start recording.
				mRecorder.start();

				mIsRecording = true;

				updateMicrophoneState();

				startMicrophoneUpdater();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	protected void stop() {
		if(mIsRecording) {
			if (mRecorder != null) {
				try {
					mRecorder.stop();
					mRecorder.reset();

					mIsRecording = false;

					updateMicrophoneState();
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}

			abandonAudioFocus();
		}
	}


	@Override
	public void onDestroy() {
		stop();
		mRecorder.release();
		mRecorder = null;

		// Tell the user we stopped.
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

	protected void updateMicrophoneState() {
		for(AudioRecorderMicrophone microphone : mMicrophones) {
			microphone.setSelected(mIsRecording);

			if(!mIsRecording)
				microphone.updateAmplitude(0, UPDATE_INTERVAL_MS * 3);
		}
	}

	protected void startMicrophoneUpdater() {
		// Star updating microphones amplitude.
		mHandler.removeCallbacks(mAmplitudeUpdater);
		mHandler.post(mAmplitudeUpdater);
	}

	private class AmplitudeUpdater implements Runnable {
		@Override
		public void run() {
			if(mIsRecording && mRecorder != null && mMicrophones.size() > 0) {
				final int amplitude = mRecorder.getMaxAmplitude();

				for(AudioRecorderMicrophone microphone : mMicrophones)
					microphone.updateAmplitude(amplitude, UPDATE_INTERVAL_MS);

				// Post animation runnable to update the animation.
				mHandler.postDelayed(mAmplitudeUpdater, UPDATE_INTERVAL_MS);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class LocalBinder extends Binder {
		public void registerAudioRecorderMicrophone(AudioRecorderMicrophone microphone) {
			mMicrophones.add(microphone);

			// Configure microphone state.
			microphone.setSelected(mIsRecording);

			// Start microphone update.
			startMicrophoneUpdater();
		}

		public void unregisterAudioRecorderMicrophone(AudioRecorderMicrophone microphone) {
			mMicrophones.remove(microphone);
		}

		public boolean isRecording() {
			return mIsRecording;
		}

		public void startRecorder() {
			start();
		}

		public void stopRecorder() {
			stop();
		}
	}
}
