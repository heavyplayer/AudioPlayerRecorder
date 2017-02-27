package com.heavyplayer.audioplayerrecorder.service.manager;

import com.heavyplayer.audioplayerrecorder.BuildConfig;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

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

    final public void onActivityStart() {
        onActivateService(!mIsServiceRunning);
    }

    final public void onActivityStop() {
        onDeactivateService(mIsPortrait == isPortrait());
    }

    final public void onFragmentStart() {
        onActivityStart();
    }

    final public void onFragmentStop() {
        onActivityStop();
    }

    protected void onActivateService(boolean startService) {
        if (startService) {
            startService();
        }

        bindService();
    }

    protected void onDeactivateService(boolean stopService) {
        unbindService();

        if (stopService) {
            stopService();
        }
    }

    protected void startService() {
        mActivity.startService(new Intent(mActivity, mServiceClass));

        mIsServiceRunning = true;
    }

    protected void stopService() {
        if (mStateListener != null) {
            mStateListener.onServiceStop();
        }

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
        if (mBinder != null) {
            if (mStateListener != null) {
                mStateListener.onServiceUnbind(mBinder);
            }

            mBinder = null;
        }

        mActivity.unbindService(this);
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

        if (mStateListener != null) {
            mStateListener.onServiceBind(mBinder);
        }

        if (BuildConfig.DEBUG) {
            Log.i(mServiceClass.getSimpleName(), "Local service connected");
        }
    }

    @Override
    final public void onServiceDisconnected(ComponentName name) {
        mBinder = null;
        if (mStateListener != null) {
            mStateListener.onServiceUnbind(null);
        }

        if (BuildConfig.DEBUG) {
            Log.i(mServiceClass.getSimpleName(), "Local service disconnected");
        }
    }

    public void setServiceStateListener(StateListener listener) {
        mStateListener = listener;
    }

    public interface StateListener {
        void onServiceBind(IBinder binder);

        /**
         * @param binder may be null if the service disconnects.
         */
        void onServiceUnbind(IBinder binder);

        void onServiceStop();
    }
}
