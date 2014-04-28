package com.heavyplayer.audioplayerrecorder.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;
import com.heavyplayer.audioplayerrecorder.R;
import com.heavyplayer.audioplayerrecorder.util.AudioPlayerHandler;
import com.heavyplayer.audioplayerrecorder.util.BuildUtils;
import com.heavyplayer.audioplayerrecorder.widget.AudioPlayerLayout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AudioPlayerService extends Service {
	private IBinder mBinder;

	private Handler mHandler;

	private Map<Long, AudioPlayerHandler> mPlayers = new HashMap<>(6);

	@Override
	public void onCreate() {
		if(BuildUtils.isDebug(this))
			Toast.makeText(this, R.string.local_service_started, Toast.LENGTH_SHORT).show();

		mBinder = onCreateLocalBinder();

		mHandler = new Handler();
	}

	protected LocalBinder onCreateLocalBinder() {
		return new LocalBinder();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	public void destroy() {
		final Iterator<AudioPlayerHandler> it = mPlayers.values().iterator();
		while(it.hasNext()) {
			final AudioPlayerHandler player = it.next();
			player.destroy();
			it.remove();
		}
	}

	@Override
	public void onDestroy() {
		destroy();

		if(BuildUtils.isDebug(this))
			Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class LocalBinder extends Binder {
		public void register(long id, Uri fileUri, boolean showBufferIfPossible, AudioPlayerLayout view) {
			AudioPlayerHandler player = mPlayers.get(id);
			if(player == null) {
				player = new AudioPlayerHandler(AudioPlayerService.this, fileUri, showBufferIfPossible, mHandler);
				mPlayers.put(id, player);
			}

			player.registerView(view);
		}

		public void destroyPlayers() {
			destroy();
		}
	}
}
