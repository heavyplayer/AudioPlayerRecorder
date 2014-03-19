package com.audiomanager.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.audiomanager.app.R;

public class AudioRecorderService extends Service {
	public static final String TAG = AudioRecorderService.class.getSimpleName();

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	private MediaRecorder mRecorder;

	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class LocalBinder extends Binder {
		public AudioRecorderService getService() {
			return AudioRecorderService.this;
		}
	}

	@Override
	public void onCreate() {
		initMediaRecorder();

		// Tell the user we started.
		Toast.makeText(this, R.string.local_service_started, Toast.LENGTH_SHORT).show();
	}

	protected void initMediaRecorder() {
		final String fileName = "filename1";

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
	}

	protected String generateOutputFilePath(String fileName) {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	public MediaRecorder getMediaRecorder() {
		return mRecorder;
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
}
