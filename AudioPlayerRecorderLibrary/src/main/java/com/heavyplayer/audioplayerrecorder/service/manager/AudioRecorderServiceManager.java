package com.heavyplayer.audioplayerrecorder.service.manager;

import com.heavyplayer.audioplayerrecorder.service.AudioRecorderService;

import android.app.Activity;

public class AudioRecorderServiceManager extends ServiceManager {
    public AudioRecorderServiceManager(Activity activity) {
        super(activity, AudioRecorderService.class);
    }

    @Override
    protected void onDeactivateService(boolean stopService) {
        if (stopService) {
            final AudioRecorderService.LocalBinder binder = getBinder();
            if (binder != null) {
                // Force recorder stop, to make sure the output file is ready to be read.
                binder.destroyRecorder();
            }
        }

        super.onDeactivateService(stopService);
    }

    public AudioRecorderService.LocalBinder getBinder() {
        return (AudioRecorderService.LocalBinder) super.getBinder();
    }
}
