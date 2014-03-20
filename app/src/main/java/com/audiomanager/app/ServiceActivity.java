package com.audiomanager.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.Toast;
import com.audiomanager.obj.Item;
import com.audiomanager.service.AudioPlayerService;
import com.audiomanager.widget.AudioPlayerSeekBar;

public class ServiceActivity extends HomeActivity {
	private static final String STATE_SERVICE_IS_STARTED = "service_is_started";

	private AudioPlayerService mBoundService;
	private boolean mServiceIsBound;
	private boolean mServiceIsStarted;
	private AudioPlayerServiceConnection mServiceConnection = new AudioPlayerServiceConnection();

	@Override
	protected ListAdapter onCreateAdapter(Context context, Item[] objects) {
		return new SeekBarItemAdapter(context, objects);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(mServiceIsStarted)
			bindService();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if(mServiceIsBound)
			unbindService();
	}

	protected void bindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(
				new Intent(this, AudioPlayerService.class),
				mServiceConnection,
				Context.BIND_AUTO_CREATE);
		mServiceIsBound = true;
	}

	protected void unbindService() {
		if (mServiceIsBound) {
			// Detach our existing connection.
			unbindService(mServiceConnection);
			onServiceUnbind();
		}
	}

	public void onPlay(View v) {
		final Button button = (Button)v;
		if("Play".equals(button.getText())) {
			startService(new Intent(this, AudioPlayerService.class));
			mServiceIsStarted = true;
			bindService();
			button.setText("Stop");
		}
		else {
			unbindService();
			stopService(new Intent(this, AudioPlayerService.class));
			mServiceIsStarted = false;
			button.setText("Play");
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_SERVICE_IS_STARTED, mServiceIsStarted);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mServiceIsStarted = savedInstanceState != null && savedInstanceState.getBoolean(STATE_SERVICE_IS_STARTED, false);
	}

	private class AudioPlayerServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.

			mBoundService = ((AudioPlayerService.LocalBinder)service).getService();

			// Force mListView items to invalidate, in order to refresh the seekbar state, if needed.
			mListView.invalidateViews();

			// Tell the user about this for our demo.
			Toast.makeText(ServiceActivity.this, R.string.local_service_connected, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			onServiceUnbind();
			Toast.makeText(ServiceActivity.this, R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
		}
	}

	protected void onServiceUnbind() {
		mBoundService = null;
		mServiceIsBound = false;

		// Force mListView items to invalidate, in order to refresh the seekbar state, if needed.
		mListView.invalidateViews();
	}

	protected class SeekBarItemAdapter extends ItemAdapter {
		public SeekBarItemAdapter(Context context, Item[] objects) {
			super(context, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final View view = super.getView(position, convertView, parent);

			if(mServiceIsBound && mBoundService != null) {
				final AudioPlayerSeekBar seekBar = (AudioPlayerSeekBar) view.findViewById(R.id.item_seek_bar);
				if(seekBar != null && mBoundService.getId() == getItem(position).getId())
					seekBar.setMediaPlayer(mBoundService.getMediaPlayer());
			}

			return view;
		}
	}
}
