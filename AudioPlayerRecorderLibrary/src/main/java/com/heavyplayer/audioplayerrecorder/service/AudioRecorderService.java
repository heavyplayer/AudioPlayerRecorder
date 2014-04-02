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

	private MediaRecorder mRecorder;

	public static Intent getLaunchIntent(Context context, Uri fileUri) {
		final Intent intent = new Intent(context, AudioRecorderService.class);
		intent.putExtra(EXTRA_FILE_URI, fileUri);
		return intent;
	}

	@Override
	public void onCreate() {
		gainAudioFocus();

		mHandler = new Handler();

		// Tell the user we started.
		Toast.makeText(this, R.string.local_service_started, Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(mRecorder == null)
			initMediaRecorder((Uri)intent.getParcelableExtra(EXTRA_FILE_URI));

		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	protected void initMediaRecorder(Uri fileUri) {
		try {
			mRecorder = new MediaRecorder();
			mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mRecorder.setOutputFile(fileUri.getPath());
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

			mRecorder.prepare();

			// Start recording.
			mRecorder.start();
			mHandler.post(mAmplitudeUpdater);
		}
		catch(Exception e){
			Log.w(TAG, e);
		}
	}

	@Override
	public void onDestroy() {
		if(mRecorder != null) {
			try {
				mRecorder.stop();
				mRecorder.reset();
				mRecorder.release();
				mRecorder = null;
			}
			catch(Exception e) {
				Log.w(TAG, e);
			}
		}

		abandonAudioFocus();

		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
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

	public class LocalBinder extends Binder {
		public void registerAudioRecorderMicrophone(AudioRecorderMicrophone microphone) {
			mMicrophones.add(microphone);

			mHandler.removeCallbacks(mAmplitudeUpdater);
			mHandler.post(mAmplitudeUpdater);
		}

		public void unregisterAudioRecorderMicrophone(AudioRecorderMicrophone microphone) {
			mMicrophones.remove(microphone);
		}
	}

	private class AmplitudeUpdater implements Runnable {
		@Override
		public void run() {
			if(mRecorder != null && mMicrophones.size() > 0) {
				final int amplitude = mRecorder.getMaxAmplitude();

				for(AudioRecorderMicrophone microphone : mMicrophones)
					microphone.updateAmplitude(amplitude, UPDATE_INTERVAL_MS);

				// Post animation runnable to update the animation.
				mHandler.postDelayed(mAmplitudeUpdater, UPDATE_INTERVAL_MS);
			}
		}
	}
}
