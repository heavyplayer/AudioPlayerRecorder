package com.heavyplayer.audioplayerrecorder.service.manager;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.widget.Toast;
import com.heavyplayer.audioplayerrecorder.R;
import com.heavyplayer.audioplayerrecorder.util.BuildUtils;

public class ServiceManager implements ServiceConnection {
	private IBinder mBinder;

	private Activity mActivity;
	private Class<?> mServiceClass;

	private boolean mIsPortrait;
	private boolean mIsServiceRunning;

	private StateListener mStateListener;

	public <T extends Service> ServiceManager(Activity activity, Class<T> serviceClass) {
		mActivity = activity;
		mServiceClass = serviceClass;

		mIsPortrait = isPortrait();

		startService();
	}

	final public void onActivityResume() {
		onActivateService(!mIsServiceRunning);
	}

	final public void onActivityPause() {
		onDeactivateService(mIsPortrait == isPortrait());
	}

	final public void onFragmentResume() {
		onActivityResume();
	}

	final public void onFragmentPause() {
		onActivityPause();
	}

	protected void onActivateService(boolean startService) {
		if(startService)
			startService();

		bindService();
	}

	protected void onDeactivateService(boolean stopService) {
		unbindService();

		if(stopService)
			stopService();
	}

	protected void startService() {
		mActivity.startService(new Intent(mActivity, mServiceClass));

		mIsServiceRunning = true;
	}

	protected void stopService() {
		if(mStateListener != null)
			mStateListener.onServiceStop();

		mActivity.stopService(new Intent(mActivity, mServiceClass));

		mIsServiceRunning = false;
	}

	protected void bindService() {
		mActivity.bindService(
				new Intent(mActivity, mServiceClass),
				this,
				Context.BIND_AUTO_CREATE);
	}

	protected void unbindService() {
		if(mBinder != null) {
			if(mStateListener != null)
				mStateListener.onServiceUnbind(mBinder);

			mBinder = null;

			mActivity.unbindService(this);
		}
	}

	protected boolean isPortrait() {
		DisplayMetrics displaymetrics = new DisplayMetrics();
		mActivity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		return displaymetrics.heightPixels > displaymetrics.widthPixels;
	}

	public IBinder getBinder() {
		return mBinder;
	}

	@Override
	final public void onServiceConnected(ComponentName name, IBinder service) {
		mBinder = service;

		if(mStateListener != null)
			mStateListener.onServiceBind(mBinder);

		if(BuildUtils.isDebug(mActivity))
			Toast.makeText(mActivity, R.string.local_service_connected, Toast.LENGTH_SHORT).show();
	}

	@Override
	final public void onServiceDisconnected(ComponentName name) {
		mBinder = null;
		if(mStateListener != null)
			mStateListener.onServiceUnbind(null);

		if(BuildUtils.isDebug(mActivity))
			Toast.makeText(mActivity, R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
	}

	public void setServiceStateListener(StateListener listener) {
		mStateListener = listener;
	}

	public static interface StateListener {
		public void onServiceBind(IBinder binder);
		/**
		 * @param binder may be null if the service disconnects.
		 */
		public void onServiceUnbind(IBinder binder);
		public void onServiceStop();
	}
}
