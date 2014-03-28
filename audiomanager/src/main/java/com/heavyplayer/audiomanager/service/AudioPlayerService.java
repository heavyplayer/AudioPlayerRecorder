package com.heavyplayer.audiomanager.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;
import com.heavyplayer.audiomanager.R;
import com.heavyplayer.audiomanager.obj.Player;
import com.heavyplayer.audiomanager.widget.AudioPlayerLayout;

import java.util.HashMap;
import java.util.Map;

public class AudioPlayerService extends Service {
	private final IBinder mBinder = new LocalBinder();

	private Handler mHandler;

	private Map<Long, Player> mPlayers = new HashMap<>(6);

	@Override
	public void onCreate() {
		// Tell the user we started.
		Toast.makeText(this, R.string.local_service_started, Toast.LENGTH_SHORT).show();

		mHandler = new Handler();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();

		for(Player player : mPlayers.values())
			player.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class LocalBinder extends Binder {
		public void register(long id, String fileName, AudioPlayerLayout view) {
			Player player = mPlayers.get(id);
			if(player == null) {
				player = new Player(AudioPlayerService.this, fileName, mHandler);
				mPlayers.put(id, player);
			}

			player.registerView(view);
		}
	}
}
