package com.heavyplayer.audioplayerrecorder.service.manager;

import com.heavyplayer.audioplayerrecorder.BuildConfig;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class ServiceManager implements ServiceConnection {
    private IBinder mBinder;

    private Activity mActivity;
    private Class<?> mServiceClass;

    private StateListener mStateListener;

    public <T extends Service> ServiceManager(Activity activity, Class<T> serviceClass) {
        mActivity = activity;
        mServiceClass = serviceClass;

        startService();
    }

    final public void onStart() {
        onActivateService();
    }

    final public void onStop() {
        onDeactivateService(!mActivity.isChangingConfigurations());
    }

    protected void onActivateService() {
        bindService();

        // Ensure service keeps running until we explicitly stop it,
        // which is always except when configuration changes occur.
        startService();
    }

    protected void onDeactivateService(boolean stopService) {
        if (stopService) {
            stopService();
        }

        unbindService();
    }

    protected void startService() {
        mActivity.startService(new Intent(mActivity, mServiceClass));
    }

    protected void stopService() {
        if (mStateListener != null) {
            mStateListener.onServiceStop();
        }

        mActivity.stopService(new Intent(mActivity, mServiceClass));
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
