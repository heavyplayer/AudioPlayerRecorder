package com.heavyplayer.audioplayerrecorder.service;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.widget.ListView;
import android.widget.Toast;
import com.heavyplayer.audioplayerrecorder.R;

public class AudioPlayerServiceHandler {
	private AudioPlayerService.LocalBinder mAudioPlayerBinder;
	private AudioPlayerServiceConnection mServiceConnection = new AudioPlayerServiceConnection();

	private Activity mActivity;
	private Class<?> mServiceClass;

	private boolean mIsPortrait;

	private ListView mListView;

	public AudioPlayerServiceHandler(Activity activity) {
		this(activity, AudioPlayerService.class);
	}

	public <T extends AudioPlayerService> AudioPlayerServiceHandler(Activity activity, Class<T> serviceClass) {
		mActivity = activity;
		mServiceClass = serviceClass;
		mActivity.startService(new Intent(mActivity, mServiceClass));
	}

	public void onActivityResume() {
		bindService();
	}

	public void onActivityPause() {
		unbindService();

		if(mIsPortrait == isPortrait())
			mActivity.stopService(new Intent(mActivity, mServiceClass));
	}

	public void onFragmentResume() {
		onActivityResume();
	}

	public void onFragmentPause() {
		onActivityPause();
	}

	protected void bindService() {
		mActivity.bindService(
				new Intent(mActivity, mServiceClass),
				mServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	protected void unbindService() {
		if(mAudioPlayerBinder != null) {
			mAudioPlayerBinder = null;
			mActivity.unbindService(mServiceConnection);
		}
	}

	protected boolean isPortrait() {
		DisplayMetrics displaymetrics = new DisplayMetrics();
		mActivity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		return displaymetrics.heightPixels > displaymetrics.widthPixels;
	}

	public void setListView(ListView listView) {
		mListView = listView;
	}

	public AudioPlayerService.LocalBinder getAudioPlayerBinder() {
		return mAudioPlayerBinder;
	}

	private class AudioPlayerServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mAudioPlayerBinder = (AudioPlayerService.LocalBinder)service;

			if(mListView != null)
				mListView.invalidateViews();

			Toast.makeText(mActivity, R.string.local_service_connected, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mAudioPlayerBinder = null;

			Toast.makeText(mActivity, R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
		}
	}
}
