package com.heavyplayer.audioplayerrecorder.service.manager;

import android.app.Activity;
import com.heavyplayer.audioplayerrecorder.service.AudioRecorderService;

public class AudioRecorderServiceManager extends ServiceManager {
	public AudioRecorderServiceManager(Activity activity) {
		super(activity, AudioRecorderService.class);
	}

	public AudioRecorderService.LocalBinder getBinder() {
		return (AudioRecorderService.LocalBinder)super.getBinder();
	}
}
