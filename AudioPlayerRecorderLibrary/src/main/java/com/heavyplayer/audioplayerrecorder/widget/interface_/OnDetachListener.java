package com.heavyplayer.audioplayerrecorder.widget.interface_;

import android.view.View;

public interface OnDetachListener {
	public void onStartTemporaryDetach(View v);
	public void onDetachedFromWindow(View v);
}
