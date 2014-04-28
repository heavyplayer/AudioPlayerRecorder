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

	@Override
	protected void onDeactivateService(boolean stopService) {
		if(stopService) {
			final AudioPlayerService.LocalBinder binder = getBinder();
			if(binder != null)
				// Make sure the players are destroyed.
				binder.destroyPlayers();
		}

		super.onDeactivateService(stopService);
	}

	public AudioPlayerService.LocalBinder getBinder() {
		return (AudioPlayerService.LocalBinder)super.getBinder();
	}
}
