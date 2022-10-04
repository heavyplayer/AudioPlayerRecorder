package com.heavyplayer.audioplayerrecorder.service.manager;

import com.heavyplayer.audioplayerrecorder.service.AudioPlayerService;

import android.app.Activity;

public class AudioPlayerServiceManager<ID> extends ServiceManager {
    public AudioPlayerServiceManager(Activity activity) {
        this(activity, AudioPlayerService.class);
    }

    public <T extends AudioPlayerService<ID>> AudioPlayerServiceManager(Activity activity, Class<T> serviceClass) {
        super(activity, serviceClass);
    }

    @Override
    protected void onDeactivateService(boolean stopService) {
        if (stopService) {
            final AudioPlayerService<ID>.LocalBinder binder = getBinder();
            if (binder != null) {
                // Make sure the players are destroyed.
                binder.destroyPlayers();
            }
        }

        super.onDeactivateService(stopService);
    }

    public AudioPlayerService<ID>.LocalBinder getBinder() {
        return (AudioPlayerService<ID>.LocalBinder) super.getBinder();
    }
}
