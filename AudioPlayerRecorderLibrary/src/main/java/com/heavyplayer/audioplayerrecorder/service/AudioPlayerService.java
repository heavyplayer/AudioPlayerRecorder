package com.heavyplayer.audioplayerrecorder.service;

import com.heavyplayer.audioplayerrecorder.BuildConfig;
import com.heavyplayer.audioplayerrecorder.util.AudioPlayerHandler;
import com.heavyplayer.audioplayerrecorder.widget.AudioPlayerLayout;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AudioPlayerService extends Service {
    private static final String LOG_TAG = AudioPlayerService.class.getSimpleName();

    private IBinder mBinder;

    private Handler mHandler;

    private Map<Long, AudioPlayerHandler> mPlayers = new HashMap<>(6);

    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "Local service started");
        }

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

    public void destroyAll() {
        final Iterator<AudioPlayerHandler> it = mPlayers.values().iterator();
        while (it.hasNext()) {
            it.next().destroy();
            it.remove();
        }
    }

    @Override
    public void onDestroy() {
        destroyAll();

        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "Local service stopped");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public void register(long id, Uri fileUri, boolean showBufferIfPossible, AudioPlayerLayout view) {
            AudioPlayerHandler player = mPlayers.get(id);
            if (player == null) {
                player = onCreateAudioPlayerHandler(
                        AudioPlayerService.this, id, fileUri, showBufferIfPossible, mHandler);

                mPlayers.put(id, player);
            } else {
                player.recreate(fileUri);
            }

            player.registerView(view);
        }

        public void destroyPlayers() {
            destroyAll();
        }

        public void destroyPlayer(long id) {
            AudioPlayerHandler player = mPlayers.get(id);
            if (player != null) {
                player.destroy();
            }
        }
    }

    public AudioPlayerHandler onCreateAudioPlayerHandler(Context context, long id, Uri fileUri,
                                                         boolean showBufferIfPossible, Handler handler) {
        return new AudioPlayerHandler(context, fileUri, showBufferIfPossible, handler);
    }
}
