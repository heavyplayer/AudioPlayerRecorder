package com.audiomanager.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.Toast;
import com.audiomanager.obj.Item;
import com.audiomanager.service.AudioPlayerService;
import com.audiomanager.widget.AudioPlayer;

public class PlayerActivity extends RecorderActivity {
	private AudioPlayerService.LocalBinder mAudioPlayerBinder;
	private AudioPlayerServiceConnection mServiceConnection = new AudioPlayerServiceConnection();

	private boolean mIsPortrait;

	@Override
	protected ListAdapter onCreateAdapter(Context context, Item[] objects) {
		return new AudioPlayerItemAdapter(context, objects);
	}

	@Override
	protected void onStart() {
		super.onStart();
		mIsPortrait = isPortrait();

		startService(new Intent(this, AudioPlayerService.class));
	}

	protected boolean isPortrait() {
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		return displaymetrics.heightPixels > displaymetrics.widthPixels;
	}

	@Override
	protected void onResume() {
		super.onResume();

		bindService();
	}

	@Override
	protected void onPause() {
		super.onPause();

		unbindService();
	}

	protected void bindService() {
		bindService(
				new Intent(this, AudioPlayerService.class),
				mServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	protected void unbindService() {
		if(mAudioPlayerBinder != null) {
			mAudioPlayerBinder = null;
			// Detach our existing connection.
			unbindService(mServiceConnection);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		if(mIsPortrait == isPortrait())
			stopService(new Intent(this, AudioPlayerService.class));
	}

	private class AudioPlayerServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mAudioPlayerBinder = (AudioPlayerService.LocalBinder)service;

			mListView.invalidateViews();

			// Tell the user about this for our demo.
			Toast.makeText(PlayerActivity.this, R.string.local_service_connected, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mAudioPlayerBinder = null;

			Toast.makeText(PlayerActivity.this, R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
		}
	}

	protected class AudioPlayerItemAdapter extends ItemAdapter {
		public AudioPlayerItemAdapter(Context context, Item[] objects) {
			super(context, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final View view = super.getView(position, convertView, parent);

			if(mAudioPlayerBinder != null) {
				final AudioPlayer audioPlayer = (AudioPlayer)view.findViewById(R.id.item_audio_player);
				if(audioPlayer != null) {
					final Item item = getItem(position);
					mAudioPlayerBinder.registerAudioPlayer(audioPlayer, item.getId(), item.getFileName());
				}
			}

			return view;
		}
	}
}
