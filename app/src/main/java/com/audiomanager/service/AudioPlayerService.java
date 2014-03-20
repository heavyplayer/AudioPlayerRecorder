package com.audiomanager.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.audiomanager.app.R;

import java.io.IOException;

public class AudioPlayerService extends Service {
	public static final String TAG = AudioPlayerService.class.getSimpleName();

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	private MediaPlayer mPlayer;

	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class LocalBinder extends Binder {
		public AudioPlayerService getService() {
			return AudioPlayerService.this;
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

		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(generateInputFilePath(fileName));
			mPlayer.prepare();
		} catch (IOException e) {
			Log.w(TAG, e);
		}

		// Start playing.
		mPlayer.start();
	}

	protected String generateInputFilePath(String fileName) {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	public MediaPlayer getMediaPlayer() {
		return mPlayer;
	}

	public long getId() {
		return 2;
	}

	@Override
	public void onDestroy() {
		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();

		mPlayer.stop();
		mPlayer.reset();
		mPlayer.release();
		mPlayer = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}
