package com.heavyplayer.audioplayerrecorder.service.manager;

import android.app.Activity;
import com.heavyplayer.audioplayerrecorder.service.AudioPlayerService;

public class AudioPlayerServiceManager extends ServiceManager {
	public AudioPlayerServiceManager(Activity activity) {
		this(activity, AudioPlayerService.class);
	}

	public <T extends AudioPlayerService> AudioPlayerServiceManager(Activity activity, Class<T> serviceClass) {
		super(activity, serviceClass);
	}

	public AudioPlayerService.LocalBinder getBinder() {
		return (AudioPlayerService.LocalBinder)super.getBinder();
	}
}
