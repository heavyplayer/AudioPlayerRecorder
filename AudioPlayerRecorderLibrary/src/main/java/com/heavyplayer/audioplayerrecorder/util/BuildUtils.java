package com.heavyplayer.audioplayerrecorder.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;

public class BuildUtils {
	private static Boolean sIsDebug;

	public static boolean isDebug(Context context) {
		if(sIsDebug == null && context != null) {
			final ApplicationInfo info = context.getApplicationInfo();
			if(info != null)
				sIsDebug = (info.flags &= ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		}

		return sIsDebug != null && sIsDebug;
	}
}
