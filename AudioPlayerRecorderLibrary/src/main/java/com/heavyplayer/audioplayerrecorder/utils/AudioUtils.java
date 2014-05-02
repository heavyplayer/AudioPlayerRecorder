package com.heavyplayer.audioplayerrecorder.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public class AudioUtils {
	public static boolean isMicrophoneAvailable(Context context) {
		final PackageManager pm = context.getPackageManager();
		return pm != null && pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
	}
}
