package com.heavyplayer.audiomanager.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.heavyplayer.audiomanager.R;
import com.heavyplayer.audiomanager.widget.AudioRecorderMicrophone;

import java.util.HashSet;
import java.util.Set;

public class AudioRecorderService extends Service {
	public static final String TAG = AudioRecorderService.class.getSimpleName();

	private final static int UPDATE_INTERVAL_MS = 100;

	public static final String EXTRA_FILE_NAME = "extra_file_name";

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	private Handler mHandler;
	private AmplitudeUpdater mAmplitudeUpdater = new AmplitudeUpdater();

	private Set<AudioRecorderMicrophone> mMicrophones = new HashSet<>(1);

	private MediaRecorder mRecorder;

	public static Intent getLaunchIntent(Context context, String fileName) {
		final Intent intent = new Intent(context, AudioRecorderService.class);
		intent.putExtra(EXTRA_FILE_NAME, fileName);
		return intent;
	}

	@Override
	public void onCreate() {
		mHandler = new Handler();

		// Tell the user we started.
		Toast.makeText(this, R.string.local_service_started, Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(mRecorder == null)
			initMediaRecorder(intent.getStringExtra(EXTRA_FILE_NAME));

		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	protected void initMediaRecorder(String fileName) {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setOutputFile(generateOutputFilePath(fileName));
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
		} catch (Exception e){
			Log.w(TAG, e);
		}

		// Start recording.
		mRecorder.start();
		mHandler.post(mAmplitudeUpdater);
	}

	protected String generateOutputFilePath(String fileName) {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
	}

	@Override
	public void onDestroy() {
		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();

		mRecorder.stop();
		mRecorder.reset();
		mRecorder.release();
		mRecorder = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
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
